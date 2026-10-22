package app.morphe.extension.chmate;

import java.util.*;

/** Read-only capture before native popularity filtering. Never toggles the live model. */
public final class PopularNativeRows {
    private static final List<Snapshot> snapshots=new ArrayList<>();
    private static final ThreadLocal<Integer> modernThreshold=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> modernFlat=new ThreadLocal<>();
    static final class Snapshot {
        final Object first;final List<?> rows;final boolean flat;final int threshold;
        Snapshot(List<?> rows,boolean flat,int threshold)throws Exception {
            this.rows=new ArrayList<>(rows);this.flat=flat;this.threshold=threshold;
            Object found=null;for(Object row:rows){Object r=ExtraQuickFilters.rowResponse(row);if(r!=null){found=r;break;}}first=found;
        }
        Set<Integer> selected(int threshold)throws Exception {
            Set<Integer> result=new LinkedHashSet<>();int start=-1;
            for(int i=0;i<rows.size();i++) {
                Object row=rows.get(i),r=ExtraQuickFilters.rowResponse(row);
                if(flat){if(r!=null&&references(r)>=threshold)result.add(ExtraQuickFilters.number(r));continue;}
                if(r==null||depth(row)==0){if(start>=0&&i-start>=threshold)add(result,start,i);start=r==null?-1:i;}
            }
            if(!flat&&start>=0&&rows.size()-start>=threshold)add(result,start,rows.size());
            return result;
        }
        private void add(Set<Integer> result,int start,int end)throws Exception {
            for(int i=start;i<end;i++){Object r=ExtraQuickFilters.rowResponse(rows.get(i));if(r!=null)result.add(ExtraQuickFilters.number(r));}
        }
    }
    private static Object field(Object o,String n)throws Exception{return ResponseArchiveEntry.field(o,n);}
    static int references(Object r)throws Exception {
        String n=r.getClass().getName();return ((Number)field(r,n.equals("o.processAdDisplayErrorPostbackForUserError")?"r":n.equals("o.BouncyCastleSocketAdapterCompanion")?"p":"s")).intValue();
    }
    private static int depth(Object row)throws Exception {
        String n=row.getClass().getName();return ((Number)field(row,n.equals("o.r8lambdakfayx3RllpZib32UTPpeXfQjw")?"i":n.equals("o.setSocketokhttp")?"h":"j")).intValue();
    }
    private static void save(List<?> rows,boolean flat,int threshold) {
        try {
            Snapshot s=new Snapshot(rows,flat,threshold);if(s.first==null)return;
            synchronized(snapshots){snapshots.removeIf(v->v.first==s.first);if(snapshots.size()>=16)snapshots.remove(0);snapshots.add(s);}
        }catch(Exception error){android.util.Log.w("Haiagaru","Native popular rows unavailable",error);}
    }
    public static void captureLegacy(Object adapter,List<?> rows) {
        try {boolean old=adapter.getClass().getName().equals("o.m9ExternalSyntheticLambda1");
            save(rows,((Number)field(adapter,old?"G":"M")).intValue()!=1,((Number)field(adapter,old?"I":"D")).intValue());
        }catch(Exception error){android.util.Log.w("Haiagaru","Native popularity settings unavailable",error);}
    }
    static void config(Object config){modernFlat.remove();try{modernThreshold.set(((Number)field(config,"b")).intValue());}catch(Exception ignored){modernThreshold.remove();}}
    static void mode(Object mode){
        if(!(mode instanceof Enum)){modernFlat.remove();return;}
        String name=((Enum<?>)mode).name();
        if("Tree".equals(name))modernFlat.set(false);else if("Normal".equals(name))modernFlat.set(true);else modernFlat.remove();
    }
    public static void captureModern(List<?> rows,Object unused) {
        try {Integer threshold=modernThreshold.get();Boolean flat=modernFlat.get();if(threshold!=null&&flat!=null)save(rows,flat,threshold);}
        catch(Exception error){android.util.Log.w("Haiagaru","Modern popularity rows unavailable",error);}
        finally{modernThreshold.remove();modernFlat.remove();}
    }
    static Snapshot find(List<?> source) {
        synchronized(snapshots){for(int i=snapshots.size()-1;i>=0;i--){Snapshot s=snapshots.get(i);for(Object r:source)if(r==s.first)return s;}}
        return null;
    }
    static Set<Integer> select(List<?> source,int threshold)throws Exception {
        Snapshot s=find(source);if(s==null)throw new IllegalStateException("Native response layout not ready");return s.selected(threshold);
    }
    private PopularNativeRows(){}
}
