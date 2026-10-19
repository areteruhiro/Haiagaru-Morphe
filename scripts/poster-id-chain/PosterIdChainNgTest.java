import app.morphe.extension.chmate.PosterIdChainNg;
import jp.syoboi.a2chMate.ng.NGWord.Item;
import java.util.*;

/** Exercises native model layouts, source-rule flags, thread isolation and reply-chain behavior. */
public class PosterIdChainNgTest {
    static int assertions;
    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    static class Owner { Object K, C, J, A, e, b, a, c; }
    static class Matcher { List<Item> active; Matcher(List<Item> items) { active = items; } }
    static class BaseView extends ArrayList<Object> { List<Object> mBase; BaseView(List<Object> rows) { mBase = rows; } }
    static class processAdDisplayErrorPostbackForUserError { String h, c; }
    static class BouncyCastleSocketAdapterCompanion { String j, d; }
    static class setDislikeWidth { String i, g; }
    static class KeJ11 { String g, h; }
    static class zzabv { String h, j; }
    record Model(Class<?> row, String rows, String word, String id, String body) { }
    static final Model[] MODELS = {
        new Model(processAdDisplayErrorPostbackForUserError.class,"K","C","h","c"),
        new Model(BouncyCastleSocketAdapterCompanion.class,"J","A","j","d"),
        new Model(setDislikeWidth.class,"e","b","i","g"),
        new Model(KeJ11.class,"a","c","g","h"),
        new Model(zzabv.class,"b","a","h","j")
    };
    static Object row(Model m, String id, String body) throws Exception {
        Object row = m.row.getDeclaredConstructor().newInstance();
        m.row.getDeclaredField(m.id).set(row,id);
        m.row.getDeclaredField(m.body).set(row,body);
        return row;
    }
    static Owner owner(Model m, List<Object> rows, Item... items) throws Exception {
        Owner owner = new Owner();
        Owner.class.getDeclaredField(m.rows).set(owner,rows);
        Owner.class.getDeclaredField(m.word).set(owner,new Matcher(Arrays.asList(items)));
        return owner;
    }
    public static void main(String[] args) throws Exception {
        for (Model m : MODELS) {
            Object first = row(m,"AUTHOR_A","bad");
            Object other = row(m,"AUTHOR_A","ordinary post");
            Object reply = row(m,"AUTHOR_B",">>2");
            List<Object> rows = new ArrayList<>(List.of(first,other,reply));
            Owner both = owner(m,rows,new Item("bad",0x118));
            check(PosterIdChainNg.merge(both,other,0)==1,"same author becomes chain root: "+m.row);
            // Native reply traversal checks the root state and retains it for grandchildren.
            int parent = PosterIdChainNg.merge(both,other,0);
            int child = PosterIdChainNg.merge(both,reply,parent==1?1:0);
            check(child==1,"reply to other response chains");
            check(PosterIdChainNg.merge(both,row(m,"AUTHOR_C",">>3"),child)==1,"grandchild preserves native chain");
            check(PosterIdChainNg.merge(both,reply,0)==0,"reply author is not added by the extension");
            for(int flags:new int[]{0x18,0x110,0x10,0x11c,0x198,0x158}) {
                check(PosterIdChainNg.merge(owner(m,rows,new Item("bad",flags)),other,0)==0,
                        "requires both flags on same enabled non-highlight/non-anchor rule: "+flags);
            }
            check(PosterIdChainNg.merge(owner(m,rows,new Item("bad",0x18),new Item("bad",0x110)),other,0)==0,
                    "separate rules cannot combine their flags");
            check(PosterIdChainNg.merge(owner(m,rows,new Item("B.D",0x128)),other,0)==1,"case-insensitive regex");
            check(PosterIdChainNg.merge(owner(m,rows,new Item("[",0x128)),other,0)==0,"invalid regex ignored");
            check(PosterIdChainNg.merge(owner(m,rows,new Item("/b.d",0x108)),other,0)==1,"legacy slash-prefix regex");
            check(PosterIdChainNg.merge(owner(m,rows,new Item("/bad",0x118)),other,0)==0,"explicit literal slash");
            check(PosterIdChainNg.merge(owner(m,List.of(other),new Item("bad",0x118)),other,0)==0,"thread isolation");
            check(PosterIdChainNg.merge(owner(m,rows),other,0)==0,"no applicable board-scoped rule");
            for(String id:new String[]{"", "???0", "DELETED", "CAP_USER", "OVER1000"}) {
                Object source = row(m,id,"bad"), target = row(m,id,"ordinary");
                check(PosterIdChainNg.merge(owner(m,List.of(source,target),new Item("bad",0x118)),target,0)==0,"missing/system ID: "+id);
            }
            BaseView view = new BaseView(rows); view.add(other);
            check(PosterIdChainNg.merge(owner(m,view,new Item("bad",0x118)),other,0)==1,"full thread behind filtered list");
            check(PosterIdChainNg.merge(both,other,7)==7,"existing native state retained");
            Owner reused = owner(m,rows,new Item("bad",0x118));
            check(PosterIdChainNg.merge(reused,other,0)==1,"initial adapter snapshot");
            Owner.class.getDeclaredField(m.rows).set(reused,List.of(other));
            PosterIdChainNg.reset(reused);
            check(PosterIdChainNg.merge(reused,other,0)==0,"rebuild invalidates prior thread/rules");
        }
        System.out.println("PASS "+assertions+" poster ID chain assertions across 191/226/241/242/243 models");
    }
}
