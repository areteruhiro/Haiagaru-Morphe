package app.morphe.extension.chmate;

import java.util.*;

/** Immutable, settings-filtered index. Only keyword candidates require body normalization. */
final class SharedNgIndex {
    static final String[] KINDS = {"id", "wacchoi", "prefix", "suffix", "keyword", "name", "trip"};
    private final Map<String, Bucket> boards = new HashMap<>();
    private static final class Bucket {
        final Set<String> ids=new HashSet<>(), slips=new HashSet<>(), prefixes=new HashSet<>(), suffixes=new HashSet<>();
        final List<String> names=new ArrayList<>(), words=new ArrayList<>();
        void add(SharedNgRules.Rule r) {
            switch(r.kind) {
                case "id":ids.add(r.value);break;
                case "wacchoi":slips.add(r.value);break;
                case "prefix":prefixes.add(r.value);break;
                case "suffix":suffixes.add(r.value);break;
                case "name":case "trip":names.add(r.value);break;
                case "keyword":words.add(r.value);break;
            }
        }
        boolean identity(String id,String name) {
            if(ids.contains(id))return true;
            for(String value:names)if(name.contains(value))return true;
            if(slips.isEmpty()&&prefixes.isEmpty()&&suffixes.isEmpty())return false;
            String slip=SharedNgRules.wacchoi(name);
            return slips.contains(slip)||(slip.length()==9&&(prefixes.contains(slip.substring(0,4))||suffixes.contains(slip.substring(5))));
        }
        boolean body(String body) {for(String word:words)if(body.contains(word))return true;return false;}
    }
    SharedNgIndex(List<SharedNgRules.Rule> rules,int votes,Set<String> exceptions,Set<String> excludedBoards,int kinds) {
        for(SharedNgRules.Rule rule:rules) {
            int kind=Arrays.asList(KINDS).indexOf(rule.kind);
            if(kind<0||(kinds&(1<<kind))==0||rule.revoked||rule.votes<Math.max(votes,rule.minVotes)
                    ||exceptions.contains(rule.id)||excludedBoards.contains(rule.board))continue;
            // Global name/trip conditions were never applied by the legacy matcher.
            if(rule.board.equals("*")&&!rule.kind.equals("keyword"))continue;
            boards.computeIfAbsent(rule.board,key->new Bucket()).add(rule);
        }
    }
    boolean matches(String board,String id,String name,String rawBody) {
        Bucket local=boards.get(board),global=boards.get("*");
        if(local!=null&&local.identity(id,name))return true;
        if((local==null||local.words.isEmpty())&&(global==null||global.words.isEmpty()))return false;
        String body=SharedNgRules.text(rawBody);
        return local!=null&&local.body(body)||global!=null&&global.body(body);
    }
}
