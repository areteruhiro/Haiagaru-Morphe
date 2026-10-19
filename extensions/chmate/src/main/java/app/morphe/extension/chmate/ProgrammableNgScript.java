package app.morphe.extension.chmate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/** Sandboxed, interpreted evaluator for one local NG rule. */
final class ProgrammableNgScript {
    static final int MAX_SOURCE_LENGTH = 32768;
    static final int MATCH = 1, SAME_POSTER = 2, CHAIN_REPLIES = 4;

    static final class Input {
        final String text;
        final Map<String, Object> values;

        Input(String text, Map<String, Object> values) {
            this.text = text == null ? "" : text;
            this.values = new LinkedHashMap<>(values);
        }

        @Override public boolean equals(Object other) {
            if (!(other instanceof Input)) return false;
            Input input = (Input) other;
            return text.equals(input.text) && values.equals(input.values);
        }

        @Override public int hashCode() {
            return 31 * text.hashCode() + values.hashCode();
        }
    }

    private static final class LimitReached extends Error {}

    private static final class LimitedFactory extends ContextFactory {
        long deadline;
        int instructions;

        @Override protected Context makeContext() {
            Context context = super.makeContext();
            context.setLanguageVersion(Context.VERSION_ES6);
            context.setOptimizationLevel(-1);
            context.setMaximumInterpreterStackDepth(128);
            context.setInstructionObserverThreshold(1000);
            context.setClassShutter(className -> false);
            return context;
        }

        @Override protected void observeInstructionCount(Context context, int count) {
            instructions += count;
            if (instructions > 2_000_000 || System.nanoTime() > deadline
                    || Thread.currentThread().isInterrupted()) throw new LimitReached();
        }
    }

    private final String source;
    private volatile String error;
    private final Map<Input, Integer> cache = new LinkedHashMap<Input, Integer>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Input, Integer> eldest) {
            return size() > 4096;
        }
    };

    ProgrammableNgScript(String source) {
        this.source = source == null ? "" : source;
    }

    String error() { return error; }

    synchronized int[] evaluate(List<Input> inputs) {
        int[] result = new int[inputs.size()];
        if (source.trim().isEmpty() || error != null) return result;
        if (inputs.size() > 6000) {
            error = "一度に判定できる上限を超えました";
            return result;
        }
        ArrayList<Integer> missing = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            Integer cached = cache.get(inputs.get(index));
            if (cached == null) missing.add(index); else result[index] = cached;
        }
        if (missing.isEmpty()) return result;

        try {
            LimitedFactory factory = new LimitedFactory();
            factory.call(context -> {
                ScriptableObject scope = context.initSafeStandardObjects();
                factory.deadline = System.nanoTime() + 250_000_000L;
                Object compiled = context.evaluateString(
                        scope, "\"use strict\"; (" + source + "\n)", "Haiagaru NG", 1, null);
                if (!(compiled instanceof Function)) throw new IllegalArgumentException("関数全体を入力してください");
                Function predicate = (Function) compiled;
                for (int index : missing) {
                    if (System.nanoTime() > factory.deadline) throw new LimitReached();
                    Input input = inputs.get(index);
                    if (input.text.length() > 131072) throw new IllegalArgumentException("判定対象が長すぎます");
                    Scriptable options = context.newObject(scope);
                    for (Map.Entry<String, Object> entry : input.values.entrySet()) {
                        Object value = entry.getValue();
                        if (value != null && !(value instanceof String)
                                && !(value instanceof Number) && !(value instanceof Boolean)) {
                            throw new IllegalArgumentException("未対応の引数型です");
                        }
                        ScriptableObject.putProperty(options, entry.getKey(), value);
                    }
                    Object createdAt = input.values.get("createdAtMs");
                    ScriptableObject.putProperty(options, "createdAt", createdAt == null ? null
                            : context.newObject(scope, "Date", new Object[]{createdAt}));
                    Object value = predicate.call(context, scope, scope, new Object[]{input.text, options});
                    result[index] = decision(value, "body".equals(input.values.get("target")));
                }
                return null;
            });
            for (int index : missing) cache.put(inputs.get(index), result[index]);
        } catch (LimitReached | StackOverflowError error) {
            this.error = "実行時間・命令数・再帰の上限を超えました。保存し直すと再試行します";
            cache.clear();
            return new int[inputs.size()];
        } catch (LinkageError error) {
            this.error = "JavaScriptエンジンを初期化できません: " + error.getClass().getSimpleName();
            cache.clear();
            return new int[inputs.size()];
        } catch (RuntimeException error) {
            this.error = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            cache.clear();
            return new int[inputs.size()];
        }
        return result;
    }

    private static int decision(Object value, boolean body) {
        if (value instanceof Boolean) return (Boolean) value ? MATCH : 0;
        if (!(value instanceof Scriptable))
            throw new IllegalArgumentException("戻り値はtrue/false、または{match, samePoster, chainReplies}にしてください");
        Scriptable object = (Scriptable) value;
        boolean match = option(object, "match", true);
        boolean poster = option(object, "samePoster", false);
        boolean chain = option(object, "chainReplies", false);
        if (!body && (poster || chain))
            throw new IllegalArgumentException("samePosterとchainRepliesはレス本文専用です");
        return match ? MATCH | (poster ? SAME_POSTER : 0) | (chain ? CHAIN_REPLIES : 0) : 0;
    }

    private static boolean option(Scriptable object, String key, boolean required) {
        Object value = ScriptableObject.getProperty(object, key);
        if (value == Scriptable.NOT_FOUND && !required) return false;
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(key + "はtrue/falseで指定してください");
        return (Boolean) value;
    }
}
