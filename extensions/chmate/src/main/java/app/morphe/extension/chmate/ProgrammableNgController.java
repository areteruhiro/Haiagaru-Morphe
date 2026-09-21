package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runtime bridge shared by every supported ChMate profile. */
public final class ProgrammableNgController {
    static final String PREFS = "haiagaru.programmable-ng";
    static final String RULES_KEY = "rules";
    static final String ENABLED_KEY = "enabled";
    private static final Pattern URL_INFO = Pattern.compile("server:([^,}]+),\\s*name:([^,}]+)");
    private static final Pattern REPORTER = Pattern.compile("\\s+\\[([^\\[\\]\\s]+)★\\]\\s*$");
    private static final Map<Object, Set<Integer>> LEGACY_RESPONSE_MATCHES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Object> PENDING_SUBJECT_LIST = new ThreadLocal<>();
    private static final Map<String, ProgrammableNgScript.Input> THREAD_METADATA =
            Collections.synchronizedMap(new LinkedHashMap<String, ProgrammableNgScript.Input>(128, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, ProgrammableNgScript.Input> eldest) {
                    return size() > 3000;
                }
            });

    private static volatile Context context;
    private static volatile boolean enabled;
    private static volatile ProgrammableNgRuleSet rules = new ProgrammableNgRuleSet(Collections.emptyList());
    private static volatile List<ProgrammableNgScript.Input> lastTitles = Collections.emptyList();
    private static volatile List<ProgrammableNgScript.Input> lastBodies = Collections.emptyList();
    private static volatile String runtimeError = "";

    private ProgrammableNgController() {}

    public static void initialize(Context supplied) {
        if (supplied == null || context != null) return;
        Context application = supplied.getApplicationContext();
        context = application == null ? supplied : application;
        try {
            reload();
        } catch (Exception error) {
            report("初期化", error);
        }
    }

    static void reload() {
        Context current = context;
        if (current == null) return;
        SharedPreferences preferences = current.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            rules = ProgrammableNgRuleSet.decode(preferences.getString(RULES_KEY, ""));
            enabled = preferences.getBoolean(ENABLED_KEY, false);
            runtimeError = "";
        } catch (IllegalArgumentException error) {
            enabled = false;
            report("設定読込", error);
        }
        LEGACY_RESPONSE_MATCHES.clear();
    }

    static ProgrammableNgRuleSet currentRules() {
        return rules;
    }

    static boolean isEnabled() {
        return enabled;
    }

    static void save(boolean newEnabled, List<ProgrammableNgRuleSet.Rule> newRules) {
        Context current = context;
        if (current == null) throw new IllegalStateException("Haiagaruが初期化されていません");
        ProgrammableNgRuleSet checked = new ProgrammableNgRuleSet(newRules);
        if (!current.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(ENABLED_KEY, newEnabled)
                .putString(RULES_KEY, checked.encode())
                .commit()) {
            throw new IllegalStateException("設定を保存できませんでした");
        }
        reload();
    }

    static ProgrammableNgRuleSet.Rule newRule() {
        return new ProgrammableNgRuleSet.Rule(
                UUID.randomUUID().toString(), "新しいルール", "title", true, true, "",
                "function (text, options) {\n  return false;\n}"
        );
    }

    static ProgrammableNgRuleSet.Rule templateRule(int template) {
        String id = UUID.randomUUID().toString();
        switch (template) {
            case 0:
                return new ProgrammableNgRuleSet.Rule(id, "スレタイのキーワード", "title",
                        true, true, "", "function (text, options) {\n"
                        + "  return text.includes(\"キーワード\");\n}");
            case 1:
                return new ProgrammableNgRuleSet.Rule(id, "レス本文のキーワード", "body",
                        true, true, "", "function (text, options) {\n"
                        + "  return text.includes(\"キーワード\");\n}");
            case 2:
                return new ProgrammableNgRuleSet.Rule(id, "正規表現", "title",
                        true, true, "", "function (text, options) {\n"
                        + "  return /正規表現/i.test(text);\n}");
            case 3:
                return new ProgrammableNgRuleSet.Rule(id, "エッヂの記者ID", "title",
                        true, true, "", "function (text, options) {\n"
                        + "  return options.posterId === \"記者ID\";\n}");
            default:
                return newRule();
        }
    }

    public static void addSettingsButton(LinearLayout parent, Activity activity) {
        if (context == null) return;
        Button button = new Button(activity);
        button.setText("高度なNGルール（条件・スクリプト）");
        button.setOnClickListener(view -> ProgrammableNgDialog.show(activity));
        parent.addView(button);
    }

    /**
     * Filters the raw subject list before version-specific ViewModels transform it.
     * This boundary exists in all four supported APKs and avoids obfuscated UI types.
     */
    public static Object filterSubjectList(Object result, Object urlOrArguments) {
        if (!(result instanceof List) || context == null) return result;
        String board = boardUrl(urlOrArguments);
        if (board == null) return result;
        List<?> values = (List<?>) result;
        ArrayList<ProgrammableNgScript.Input> inputs = new ArrayList<>(values.size());
        for (Object value : values) inputs.add(threadInput(value, board));
        lastTitles = inputs;
        if (!enabled || values.isEmpty()) return result;
        boolean[] matches = rules.evaluate(inputs);
        try {
            for (int index = matches.length - 1; index >= 0; index--) {
                if (matches[index]) ((List<?>) result).remove(index);
            }
        } catch (RuntimeException error) {
            report("スレ一覧", error);
        }
        return result;
    }

    /** Two-step bridge used where the two source registers cannot be encoded in one invoke. */
    public static void pendingSubjectList(Object result) {
        PENDING_SUBJECT_LIST.set(result);
    }

    public static Object filterPendingSubjectList(Object urlOrArguments) {
        Object result = PENDING_SUBJECT_LIST.get();
        PENDING_SUBJECT_LIST.remove();
        return filterSubjectList(result, urlOrArguments);
    }

    /** 191 dev keeps its native hidden-list behavior, so excluded rows remain revealable. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ArrayList filterLegacyThreadRows(Object fragment, ArrayList candidates, ArrayList hidden) {
        if (candidates == null || context == null) return candidates;
        try {
            Object urlInfo = field(fragment, "e");
            String board = boardUrl(urlInfo);
            if (board == null) return candidates;
            ArrayList<ProgrammableNgScript.Input> inputs = new ArrayList<>();
            for (Object row : candidates) {
                Object data = field(row, "a");
                inputs.add(threadInput(data, board));
            }
            lastTitles = inputs;
            if (!enabled) return candidates;
            boolean[] matches = rules.evaluate(inputs);
            ArrayList kept = new ArrayList();
            ArrayList excluded = new ArrayList();
            for (int index = 0; index < candidates.size(); index++) {
                (matches[index] ? excluded : kept).add(candidates.get(index));
            }
            if (hidden != null) hidden.addAll(excluded);
            return excluded.isEmpty() ? candidates : kept;
        } catch (Exception error) {
            report("191スレ一覧", error);
            return candidates;
        }
    }

    /** Range-friendly overload: the matcher parameter is intentionally left to ChMate. */
    @SuppressWarnings("rawtypes")
    public static ArrayList filterLegacyThreadRows(
            Object fragment, ArrayList candidates, Object matcher, ArrayList hidden
    ) {
        return filterLegacyThreadRows(fragment, candidates, hidden);
    }

    /** Index body-rule matches once per 191 adapter rebuild. */
    public static void prepareLegacyResponses(Object adapter) {
        LEGACY_RESPONSE_MATCHES.remove(adapter);
        if (context == null) return;
        try {
            Object state = field(adapter, "M");
            String board = boardUrl(field(state, "f"));
            if (board == null) return;
            Object responseList = field(adapter, "K");
            if (!(responseList instanceof List)) return;
            ArrayList<ProgrammableNgScript.Input> inputs = new ArrayList<>();
            ArrayList<Integer> numbers = new ArrayList<>();
            String title = stringField(responseList, "title");
            for (Object response : (List<?>) responseList) {
                int number = ((Number) field(response, "o")).intValue();
                Map<String, Object> values = baseValues("body", board);
                values.put("responseNumber", number);
                values.put("responseId", field(response, "h"));
                values.put("threadTitle", title == null ? "" : title);
                values.put("name", plain(stringField(response, "n")));
                values.put("mail", field(response, "j"));
                values.put("dateText", field(response, "q"));
                inputs.add(new ProgrammableNgScript.Input(plain(stringField(response, "c")), values));
                numbers.add(number);
            }
            lastBodies = inputs;
            if (!enabled) return;
            boolean[] matches = rules.evaluate(inputs);
            HashSet<Integer> matched = new HashSet<>();
            for (int index = 0; index < matches.length; index++) if (matches[index]) matched.add(numbers.get(index));
            LEGACY_RESPONSE_MATCHES.put(adapter, matched);
        } catch (Exception error) {
            report("191レス一覧", error);
        }
    }

    /** Adds the standard NGWord bit while preserving every flag ChMate already produced. */
    public static int mergeLegacyResponseFlags(Object adapter, Object response, int original) {
        if (!enabled) return original;
        try {
            Set<Integer> matches = LEGACY_RESPONSE_MATCHES.get(adapter);
            if (matches != null && matches.contains(((Number) field(response, "o")).intValue())) {
                return original | 0x10;
            }
        } catch (Exception error) {
            report("191レスフラグ", error);
        }
        return original;
    }

    private static ProgrammableNgScript.Input threadInput(Object item, String board) {
        long threadId = 0;
        int responseCount = -1;
        String title = "";
        try {
            Object target = unwrapThreadItem(item);
            for (Field field : allFields(target.getClass())) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(target);
                if (field.getType() == long.class || value instanceof Long) {
                    long candidate = ((Number) value).longValue();
                    if (candidate > 100_000_000L && candidate < 10_000_000_000L) threadId = candidate;
                } else if ((field.getType() == int.class || value instanceof Integer) && responseCount < 0) {
                    int candidate = ((Number) value).intValue();
                    if (candidate >= 0) responseCount = candidate;
                } else if (value instanceof String) {
                    String candidate = (String) value;
                    if (!candidate.startsWith("http") && candidate.length() > title.length()) title = candidate;
                }
            }
        } catch (Exception error) {
            report("スレ情報", error);
        }
        Matcher reporter = REPORTER.matcher(title);
        String reporterId = null;
        if (reporter.find()) {
            reporterId = reporter.group(1);
            title = title.substring(0, reporter.start());
        }
        long now = System.currentTimeMillis();
        Long createdAt = threadId > 0 && threadId < 9_240_000_000L ? threadId * 1000L : null;
        Map<String, Object> values = baseValues("title", board);
        values.put("threadId", threadId > 0 ? threadId : null);
        values.put("createdAtMs", createdAt);
        values.put("resCount", responseCount >= 0 ? responseCount : null);
        values.put("speed", createdAt != null && now > createdAt && responseCount >= 0
                ? responseCount * 86_400_000.0 / (now - createdAt) : null);
        values.put("posterId", reporterId);
        ProgrammableNgScript.Input input = new ProgrammableNgScript.Input(title, values);
        if (threadId > 0) THREAD_METADATA.put(board + "#" + threadId, input);
        return input;
    }

    private static Object unwrapThreadItem(Object item) throws ReflectiveOperationException {
        if (item == null) throw new IllegalArgumentException("thread item is null");
        for (Field field : allFields(item.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()
                    || field.getType() == String.class) continue;
            field.setAccessible(true);
            Object nested = field.get(item);
            if (nested == null) continue;
            boolean hasLong = false;
            boolean hasString = false;
            for (Field candidate : allFields(nested.getClass())) {
                hasLong |= candidate.getType() == long.class;
                hasString |= candidate.getType() == String.class;
            }
            if (hasLong && hasString) return nested;
        }
        return item;
    }

    private static Map<String, Object> baseValues(String target, String board) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("target", target);
        values.put("boardUrl", board);
        values.put("threadId", null);
        values.put("createdAtMs", null);
        values.put("resCount", null);
        values.put("speed", null);
        values.put("posterId", null);
        values.put("responseNumber", null);
        values.put("responseId", null);
        return values;
    }

    private static String boardUrl(Object value) {
        if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                String found = boardUrl(item);
                if (found != null) return found;
            }
            return null;
        }
        if (value == null) return null;
        String text = value.toString();
        if (ProgrammableNgRuleSet.canonicalBoard(text) != null) return text;
        Matcher matcher = URL_INFO.matcher(text);
        if (!matcher.find()) return null;
        String result = "https://" + matcher.group(1).trim() + "/" + matcher.group(2).trim() + "/";
        return ProgrammableNgRuleSet.canonicalBoard(result) == null ? null : result;
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static String stringField(Object owner, String name) throws ReflectiveOperationException {
        Object value = field(owner, name);
        return value instanceof String ? (String) value : null;
    }

    private static List<Field> allFields(Class<?> type) {
        ArrayList<Field> result = new ArrayList<>();
        while (type != null && type != Object.class) {
            Collections.addAll(result, type.getDeclaredFields());
            type = type.getSuperclass();
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static String plain(String html) {
        return html == null ? "" : Html.fromHtml(html).toString();
    }

    static String status() {
        StringBuilder value = new StringBuilder();
        if (lastTitles.isEmpty() && lastBodies.isEmpty()) {
            value.append("確認用データ: まだ読み込まれていません\n");
        } else {
            value.append("確認用データ: スレ ").append(lastTitles.size())
                    .append("件 / レス ").append(lastBodies.size()).append("件\n");
        }
        for (ProgrammableNgRuleSet.Rule rule : rules.rules) {
            value.append(rule.name).append(rule.enabled ? ": ON" : ": OFF");
            if (rule.evaluator.error() != null) value.append(" / エラー: ").append(rule.evaluator.error());
            value.append('\n');
        }
        if (!rules.batchError.isEmpty()) value.append(rules.batchError).append('\n');
        if (!runtimeError.isEmpty()) value.append(runtimeError);
        return value.toString();
    }


    static String preview(ProgrammableNgRuleSet.Rule rule) {
        List<ProgrammableNgScript.Input> inputs = "body".equals(rule.target) ? lastBodies : lastTitles;
        if (inputs.isEmpty()) return "判定対象がまだありません。先にスレ一覧またはスレを開いてください。";
        boolean[] matches = new ProgrammableNgRuleSet(Collections.singletonList(rule)).evaluate(inputs);
        int count = 0;
        for (boolean match : matches) if (match) count++;
        String error = rule.evaluator.error();
        return error == null
                ? inputs.size() + "件中 " + count + "件がNGになります。"
                : "JavaScriptエラー: " + error;
    }

    private static void report(String stage, Exception error) {
        runtimeError = stage + ": " + error.getClass().getSimpleName()
                + (error.getMessage() == null ? "" : " - " + error.getMessage());
        Log.w("HaiagaruProgrammableNG", runtimeError, error);
    }
}
