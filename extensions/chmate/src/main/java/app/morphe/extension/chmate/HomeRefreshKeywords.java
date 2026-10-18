package app.morphe.extension.chmate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 242's refresh shortcuts pass null instead of the active keyword list. */
public final class HomeRefreshKeywords {
    private static final Map<Object,List<?>> LATEST=new WeakHashMap<>();
    private HomeRefreshKeywords(){}
    public static synchronized List<?> remember(Object sorter,List<?> keywords){
        if(sorter==null)return keywords;
        // The native settings flow represents an empty keyword list as null.
        // Only the two refresh callers restore snapshots; a normal null must
        // therefore invalidate old keywords, not resurrect deleted groups.
        if(keywords==null)LATEST.remove(sorter);
        else LATEST.put(sorter,new ArrayList<>(keywords));
        return keywords;
    }
    public static synchronized List<?> refresh(Object sorter){
        List<?> saved=LATEST.get(sorter);
        return saved==null?null:new ArrayList<>(saved);
    }
}
