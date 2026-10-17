package app.morphe.extension.chmate;

import java.lang.reflect.Field;
import java.util.*;

/** Run on Android with app_process. Synthetic native models; no app data/network required. */
public final class HiddenIdRuntimeTest {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    static void enabled(boolean value) throws Exception {
        Field f = HiddenIdNg.class.getDeclaredField("enabled"); f.setAccessible(true); f.setBoolean(null, value);
    }
    static void set(Object row, String field, Object value) throws Exception {
        row.getClass().getField(field).set(row, value);
    }
    public static final class Board {
        final String host;
        Board(String host) { this.host=host; }
        public String d(int n) { return "https://"+host+"/test/read.cgi/fixture/1234567890/"+n; }
        public String e(int n) { return d(n); }
        public String c(int n) { return d(n); }
    }
    public static final class Result {
        public Object a,d;
        Result(Object list, Object board) { a=list;d=board; }
    }
    public static final class Rows extends ArrayList<Object> {
        public Object mBase;
    }
    static o.KeJ11 row(int n,String id) { var r=new o.KeJ11();r.o=n;r.g=id;return r; }
    public static void main(String[] args) throws Exception {
        enabled(true);
        // Native 191/226 field bindings, independent of the advanced-NG controller.
        for (int version:new int[]{191,226}) {
            Object normal, hidden, owner;
            String no,id;
            if(version==191) {
                normal=new o.processAdDisplayErrorPostbackForUserError();hidden=new o.processAdDisplayErrorPostbackForUserError();
                no="o";id="h";
                var adapter=new o.m9ExternalSyntheticLambda1();adapter.M.f=new Board("egg.5ch.io");
                adapter.K=new ArrayList<>(List.of(normal,hidden));owner=adapter;
            } else {
                normal=new o.BouncyCastleSocketAdapterCompanion();hidden=new o.BouncyCastleSocketAdapterCompanion();
                no="l";id="j";
                var adapter=new o.listener();adapter.E.j=new Board("greta.5ch.io");
                adapter.J=new ArrayList<>(List.of(normal,hidden));owner=adapter;
            }
            set(normal,no,1);set(hidden,no,2);set(hidden,id,"");
            HiddenIdNg.prepare(owner);
            check(HiddenIdNg.merge(owner,hidden,0x40)==0x1048, version+" transparent NGID preserves native flags");
            check(HiddenIdNg.merge(owner,normal,0x40)==0x40, version+" normal ID retained");
            enabled(false);
            check(HiddenIdNg.merge(owner,hidden,0x40)==0x40, version+" OFF restores visibility");
            enabled(true);set(normal,id,"");HiddenIdNg.prepare(owner);
            check(HiddenIdNg.merge(owner,hidden,0x40)==0x40, version+" fully IDless thread retained");
            set(normal,id,"Abc+/123");HiddenIdNg.prepare(owner);
            check(HiddenIdNg.merge(owner,hidden,0)==0x1008, version+" incoming ID evidence re-evaluated");
            Object board=new Board("talk.jp");
            if(version==191)((o.m9ExternalSyntheticLambda1)owner).M.f=board;
            else ((o.listener)owner).E.j=board;
            HiddenIdNg.prepare(owner);
            check(HiddenIdNg.merge(owner,hidden,0)==0, version+" other board not hidden");
        }
        var normal=row(1,"Abc+/123");var hidden=row(2,"");var deleted=row(3,"DELETED");
        var rows=new Rows();rows.addAll(List.of(normal,hidden,deleted));
        HiddenIdNg.bind242(new Result(rows,new Board("egg.5ch.io")));
        Map<Integer,Integer> original=Collections.unmodifiableMap(Map.of(2,0x40,3,0x80));
        HiddenIdNg.pending(rows);Map shown=HiddenIdNg.modernFlags(original);
        check(shown.get(2).equals(0x1048),"242 transparent NG overlay");
        check(shown.get(3).equals(0x80),"242 deleted response untouched");
        check(original.get(2)==0x40,"242 shared immutable map never mutated");
        var filtered=new Rows();filtered.mBase=rows;filtered.add(hidden);
        HiddenIdNg.pending(filtered);
        check(HiddenIdNg.modernFlags(original).get(2).equals(0x1048),"242 filtered list uses full-thread evidence");
        enabled(false);HiddenIdNg.pending(rows);
        check(HiddenIdNg.modernFlags(original)==original,"242 OFF returns original map");
        enabled(true);normal.g="";HiddenIdNg.pending(rows);
        check(HiddenIdNg.modernFlags(original)==original,"242 no regular IDs leaves thread intact");
        normal.g="Abc+/123";HiddenIdNg.pending(rows);
        check(HiddenIdNg.modernFlags(original).get(2).equals(0x1048),"242 new ID evidence on same list");
        var other=new Rows();other.addAll(List.of(row(1,"Abc+/123"),row(2,"")));
        HiddenIdNg.bind242(new Result(other,new Board("talk.jp")));HiddenIdNg.pending(other);
        check(HiddenIdNg.modernFlags(original)==original,"242 other thread does not inherit 5ch origin");
        var unknown=new Rows();unknown.addAll(List.of(row(1,"Abc+/123"),row(2,"")));HiddenIdNg.pending(unknown);
        check(HiddenIdNg.modernFlags(original)==original,"242 unknown origin fails safe");
        HiddenIdNg.pending(rows);HiddenIdNg.modernFlags(original);
        check(HiddenIdNg.modernFlags(original)==original,"242 pending context removed after use");
        System.out.println("PASS "+checks+" Android hidden-ID runtime checks (191/226/242)");
    }
}
