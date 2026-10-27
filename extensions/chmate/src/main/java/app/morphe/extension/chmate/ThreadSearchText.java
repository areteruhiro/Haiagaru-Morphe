package app.morphe.extension.chmate;

import java.util.regex.*;
/** Search-only projection. Native response fields and the highlight matcher remain untouched. */
public final class ThreadSearchText {
    private static final java.util.Map<Matcher,Pattern> SOURCES=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public static String text(Object response){
        String[] fields;
        switch(response.getClass().getName()){
            case "o.processAdDisplayErrorPostbackForUserError":fields=new String[]{"c","n","q"};break;
            case "o.BouncyCastleSocketAdapterCompanion":fields=new String[]{"d","m","t"};break;
            case "o.KeJ11":fields=new String[]{"h","n","p"};break;
            default:return "";
        }
        StringBuilder result=new StringBuilder();
        for(String field:fields)try{Object value=ResponseArchiveEntry.field(response,field);if(value!=null)result.append(value).append('\n');}catch(ReflectiveOperationException ignored){}
        return result.toString();
    }
    public static Matcher reset(Matcher original,Object response){
        Pattern source=SOURCES.get(original);if(source==null)source=original.pattern();
        String query=source.pattern();String body=text(response);
        // Native regex remains the fallback. Plain multiword searches use AND across fields.
        if(query.indexOf(' ')>=0&&query.matches("[\\p{L}\\p{N}_:./+ -]+")){
            String[] terms=query.trim().split("(?i)\\s+(?:AND\\s+)?");boolean match=true;
            String value=body.toLowerCase(java.util.Locale.ROOT);
            for(String term:terms)if(!value.contains(term.toLowerCase(java.util.Locale.ROOT))){match=false;break;}
            Matcher result=Pattern.compile(match?"":"(?!)").matcher(body);SOURCES.put(result,source);return result;
        }
        Matcher result=source.matcher(body);return result;
    }
    private ThreadSearchText(){}
}
