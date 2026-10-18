package app.morphe.extension.chmate;

import java.util.*;
import java.util.regex.*;

/** Counts distinct direct replying posts, never replies to those replies. */
final class PopularReplyRules {
    private static final Pattern ANCHOR = Pattern.compile("(?:>>|＞＞)([0-9]{1,7})(?:\\s*[-－–]\\s*([0-9]{1,7}))?((?:\\s*[,、]\\s*[0-9]{1,7})*)");
    static Map<Integer,Integer> count(Map<Integer,String> posts) {
        Map<Integer,Integer> counts = new HashMap<>();
        NavigableSet<Integer> loaded = new TreeSet<>(posts.keySet());
        for (Map.Entry<Integer,String> post : posts.entrySet()) {
            Set<Integer> targets = new HashSet<>();
            Matcher matcher = ANCHOR.matcher(post.getValue());
            while (matcher.find()) {
                int first = Integer.parseInt(matcher.group(1));
                int last = matcher.group(2) == null ? first : Integer.parseInt(matcher.group(2));
                // Bound ranges by actual loaded posts, not by an attacker-controlled range size.
                int end = Math.min(last,post.getKey()-1);
                if(first<=end)targets.addAll(loaded.subSet(first,true,end,true));
                if (!matcher.group(3).isEmpty()) for (String part : matcher.group(3).split("[,、]")) {
                    part = part.trim(); if (part.isEmpty()) continue;
                    int target = Integer.parseInt(part);
                    if (target < post.getKey() && posts.containsKey(target)) targets.add(target);
                }
            }
            for (int target : targets) counts.put(target, counts.getOrDefault(target,0)+1);
        }
        return counts;
    }
    private PopularReplyRules() { }
}
