package app.morphe.extension.chmate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Expands one rule in a single loaded thread snapshot, independently of predicate caches. */
final class ProgrammableNgChain {
    private static final int MAX_LINKS = 100_000;
    private static final Pattern ANCHOR = Pattern.compile(
            "(?:>>|＞＞)([0-9]{1,7})(?![0-9])(?:\\s*[-－–]\\s*([0-9]{1,7})(?![0-9]))?((?:\\s*[,、]\\s*[0-9]{1,7}(?![0-9]))*)");

    static boolean[] expand(List<ProgrammableNgScript.Input> inputs, int[] decisions, long deadline) {
        boolean[] result = new boolean[inputs.size()], chain = new boolean[inputs.size()];
        boolean expand = false;
        for (int i = 0; i < decisions.length; i++) {
            result[i] = (decisions[i] & ProgrammableNgScript.MATCH) != 0;
            expand |= result[i] && decisions[i] != ProgrammableNgScript.MATCH;
        }
        if (!expand) return result;
        Map<List<Object>, Group> groups = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            checkTime(deadline);
            ProgrammableNgScript.Input input = inputs.get(i);
            if (!"body".equals(input.values.get("target"))) continue;
            List<Object> key = Arrays.asList(ProgrammableNgRuleSet.canonicalBoard((String) input.values.get("boardUrl")),
                    input.values.get("threadId"));
            Group group = groups.get(key);
            if (group == null) { group = new Group(); groups.put(key, group); }
            group.positions.add(i);
            Object number = input.values.get("responseNumber");
            if (number instanceof Number && ((Number) number).longValue() > 0
                    && ((Number) number).longValue() <= Integer.MAX_VALUE)
                list(group.numbers, ((Number) number).intValue()).add(i);
            String id = id(input);
            if (id != null) list(group.authors, id).add(i);
            if (result[i]) chain[i] = (decisions[i] & ProgrammableNgScript.CHAIN_REPLIES) != 0;
        }
        int links = 0;
        for (Group group : groups.values()) {
            Map<String, Integer> expandAuthors = new HashMap<>();
            for (int i : group.positions) {
                if (!result[i] || (decisions[i] & ProgrammableNgScript.SAME_POSTER) == 0) continue;
                String author = id(inputs.get(i));
                if (author != null) {
                    Integer previous = expandAuthors.get(author);
                    expandAuthors.put(author, (previous == null ? 0 : previous) | decisions[i]);
                }
            }
            for (Map.Entry<String, Integer> author : expandAuthors.entrySet()) {
                checkTime(deadline);
                List<Integer> same = group.authors.get(author.getKey());
                if (same == null) continue;
                // Expand IDs of predicate matches only; a replying author does not taint their other posts.
                boolean replyChain = (author.getValue() & ProgrammableNgScript.CHAIN_REPLIES) != 0;
                for (int sibling : same) { result[sibling] = true; chain[sibling] |= replyChain; }
            }
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (int i : group.positions) if (chain[i]) queue.add(i);
            if (queue.isEmpty()) continue;
            Map<Integer, List<Integer>> replies = new HashMap<>();
            for (int child : group.positions) {
                checkTime(deadline);
                Set<Integer> parents = new HashSet<>();
                Matcher matcher = ANCHOR.matcher(inputs.get(child).text);
                while (matcher.find()) {
                    checkTime(deadline);
                    int first = Integer.parseInt(matcher.group(1));
                    int last = matcher.group(2) == null ? first : Integer.parseInt(matcher.group(2));
                    if (first <= last) for (List<Integer> positions : group.numbers.subMap(first, true, last, true).values())
                        parents.addAll(positions);
                    if (!matcher.group(3).isEmpty()) for (String part : matcher.group(3).split("[,、]")) {
                        if (part.trim().isEmpty()) continue;
                        List<Integer> positions = group.numbers.get(Integer.parseInt(part.trim()));
                        if (positions != null) parents.addAll(positions);
                    }
                    if (parents.size() + links > MAX_LINKS) throw new IllegalArgumentException("返信参照数の上限を超えました");
                }
                parents.remove(child);
                links += parents.size();
                if (links > MAX_LINKS) throw new IllegalArgumentException("返信参照数の上限を超えました");
                for (int parent : parents) list(replies, parent).add(child);
            }
            while (!queue.isEmpty()) {
                checkTime(deadline);
                List<Integer> children = replies.get(queue.removeFirst());
                if (children == null) continue;
                for (int child : children) if (!chain[child]) {
                    chain[child] = true; result[child] = true; queue.addLast(child);
                }
            }
        }
        return result;
    }

    private static String id(ProgrammableNgScript.Input input) {
        Object value = input.values.get("responseId");
        if (!(value instanceof String)) return null;
        String id = (String) value;
        return id.trim().isEmpty() || id.startsWith("?") || id.equals("DELETED") || id.equals("CAP_USER")
                || id.equals("OVER1000") ? null : id;
    }

    private static void checkTime(long deadline) {
        if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
            throw new IllegalArgumentException("投稿者・返信連鎖の実行時間を超えました");
    }

    // Map's Java 8 default methods are unavailable on the supported API 21/23 hosts.
    private static <K, V> List<V> list(Map<K, List<V>> map, K key) {
        List<V> value = map.get(key);
        if (value == null) { value = new ArrayList<>(); map.put(key, value); }
        return value;
    }

    private static final class Group {
        final List<Integer> positions = new ArrayList<>();
        final NavigableMap<Integer, List<Integer>> numbers = new TreeMap<>();
        final Map<String, List<Integer>> authors = new HashMap<>();
    }
    private ProgrammableNgChain() { }
}
