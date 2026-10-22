package app.morphe.extension.chmate;

import android.content.Context;
import java.util.*;
import java.lang.reflect.*;

/** Display-only unread adjustment. Never changes the native read position or database. */
public final class HiddenNgUnread {
    public static final String KEY="excludeHiddenNgUnread";
    private static final Map<Object,String> OWNERS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object,String> MODERN=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String,Set<Integer>> HIDDEN=new LinkedHashMap<String,Set<Integer>>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Set<Integer>> e){return size()>128;}
    };
    // Opening/closing a thread can ask for its unread total repeatedly. Cache the
    // hidden response numbers as a sorted array and answer each read-position query
    // with binary search instead of scanning every hidden response on the UI path.
    private static final Map<String,int[]> SORTED=new LinkedHashMap<String,int[]>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,int[]> e){return size()>128;}
    };
    private static Object field(Object o,String n)throws Exception{return ResponseArchiveEntry.field(o,n);}
    private static String responseUrl(Object response,Object board)throws Exception{
        boolean old=response.getClass().getName().equals("o.processAdDisplayErrorPostbackForUserError"),middle=response.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion");
        Method method=board.getClass().getDeclaredMethod(old?"d":middle?"e":"c",int.class);method.setAccessible(true);
        return (String)method.invoke(board,((Number)field(response,middle?"l":"o")).intValue());
    }
    static String key(String url){
        try{java.net.URI u=java.net.URI.create(url);String id=ArchiveOriginPaths.threadKey(url);
            if(id==null)return null;String[] parts=u.getPath().split("/");
            int i=java.util.Arrays.asList(parts).indexOf(id);if(i<1)return null;
            return u.getHost().toLowerCase(Locale.ROOT).replaceFirst("\\.(?:2ch\\.net|5ch\\.net)$",".5ch.io")+"/"+parts[i-1]+"/"+id;
        }catch(Exception ignored){return null;}
    }
    static void begin(Object owner){
        if(!FeatureSwitches.enabled(KEY,false))return;
        try{boolean old=owner.getClass().getName().equals("o.m9ExternalSyntheticLambda1");
            Object list=field(owner,old?"K":"J"),board=field(field(owner,old?"M":"E"),old?"f":"j");
            List<?> rows=PopularReplies.fullList(list);if(rows.isEmpty())return;
            String k=key(responseUrl(rows.get(0),board));if(k==null)return;
            OWNERS.put(owner,k);synchronized(HIDDEN){HIDDEN.put(k,new HashSet<>());invalidate(k);}
        }catch(Exception ignored){OWNERS.remove(owner);}
    }
    static int observe(Object owner,Object response,int flags){
        if(!FeatureSwitches.enabled(KEY,false))return flags;
        try{String k=OWNERS.get(owner);if(k==null){begin(owner);k=OWNERS.get(owner);}if(k==null)return flags;
            int n=((Number)field(response,response.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion")?"l":"o")).intValue();
            synchronized(HIDDEN){Set<Integer> rows=HIDDEN.get(k);if(rows!=null&&n>0&&rows.size()<100000){boolean changed=(flags&0x1000)!=0?rows.add(n):rows.remove(n);if(changed)invalidate(k);}}
        }catch(Exception ignored){}return flags;
    }
    private static void invalidate(String key){SORTED.remove(key);}
    private static int[] sorted(String key,Set<Integer> hidden){
        int[] values=SORTED.get(key);if(values!=null)return values;
        values=new int[hidden.size()];int i=0;for(Integer n:hidden)values[i++]=n;
        Arrays.sort(values);SORTED.put(key,values);return values;
    }
    private static int unreadHidden(String key,int read,int total,Set<Integer> hidden){
        int[] values=sorted(key,hidden);int lo=0,hi=values.length;
        while(lo<hi){int mid=(lo+hi)>>>1;if(values[mid]<=read)lo=mid+1;else hi=mid;}int first=lo;
        lo=first;hi=values.length;while(lo<hi){int mid=(lo+hi)>>>1;if(values[mid]<=total)lo=mid+1;else hi=mid;}
        return lo-first;
    }
    static int subtract(int total,int read,Set<Integer> hidden){
        int count=0;for(int n:hidden)if(n>read&&n<=total)count++;
        return Math.max(Math.min(read,total),total-count);
    }
    static void bind242(Object result){try{List<?> rows=PopularReplies.fullList(field(result,"a"));if(rows.isEmpty())return;String k=key(responseUrl(rows.get(0),field(result,"d")));if(k!=null)MODERN.put(rows.get(0),k);}catch(Exception ignored){}}
    static Map modern(Object source,Map flags){
        if(!FeatureSwitches.enabled(KEY,false)||flags==null)return flags;
        try{List<?> rows=PopularReplies.fullList(source);if(rows.isEmpty())return flags;String k=MODERN.get(rows.get(0));if(k==null)return flags;
            Set<Integer> hidden=new HashSet<>();for(Object row:rows){int n=((Number)field(row,"o")).intValue();Object f=flags.get(n);if(f instanceof Number&&(((Number)f).intValue()&0x1000)!=0&&n>0&&hidden.size()<100000)hidden.add(n);}
            synchronized(HIDDEN){HIDDEN.put(k,hidden);invalidate(k);}
        }catch(Exception ignored){}return flags;
    }
    public static int totalFromStoredRead(Object row){
        String name=row.getClass().getName();
        String read=name.equals("o.onAdDisplayed$setIcon")?"l":name.equals("o.Yy2$open")?"i":"g";
        try{return total(row,((Number)field(row,read)).intValue());}
        catch(Exception ignored){return total(row,Integer.MAX_VALUE);}
    }
    public static int total(Object row,int read){
        String name=row.getClass().getName();String total=name.equals("o.onAdDisplayed$setIcon")?"k":name.equals("o.Yy2$open")?"m":name.equals("o.zzauw")?"a":name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ")?"o":null;
        if(total==null)throw new IllegalArgumentException("Unsupported unread row");
        int nativeTotal;
        try{nativeTotal=((Number)field(row,total)).intValue();}catch(Exception e){throw new IllegalStateException(e);}
        if(!FeatureSwitches.enabled(KEY,false))return nativeTotal;
        try{Context c=Haiagaru.applicationContextForExtension();String url;
            if(name.equals("o.zzauw")){Object info=field(row,"i");url=(String)info.getClass().getMethod(ArchiveOrigins.nativeUrlMethod()).invoke(info);}
            else url=DroppedThreadStatus.rowUrl(row,c);
            String k=key(url);synchronized(HIDDEN){Set<Integer> hidden=HIDDEN.get(k);if(hidden==null)return nativeTotal;
                int count=unreadHidden(k,read,nativeTotal,hidden);return Math.max(Math.min(read,nativeTotal),nativeTotal-count);}
        }catch(Exception ignored){return nativeTotal;}
    }
    private static void invalidateAll(){SORTED.clear();}
    static void clear(){OWNERS.clear();MODERN.clear();synchronized(HIDDEN){HIDDEN.clear();invalidateAll();}}
    private HiddenNgUnread(){}
}
