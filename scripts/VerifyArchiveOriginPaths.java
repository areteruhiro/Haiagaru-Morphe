import app.morphe.extension.chmate.ArchiveOriginPaths;
import java.io.File;

public final class VerifyArchiveOriginPaths {
    static int count;
    static void check(boolean value,String label) { if(!value)throw new AssertionError(label); count++; }
    public static void main(String[] args) throws Exception {
        String key="1792102288";
        check(key.equals(ArchiveOriginPaths.threadKey("https://mi.5ch.io/test/read.cgi/news4vip/"+key+"/")),"5ch URL");
        check(key.equals(ArchiveOriginPaths.threadKey("http://egg.5ch.io/test/read.cgi/android/"+key+"/163")),"exact HTTP accepted");
        check(key.equals(ArchiveOriginPaths.threadKey("https://talk.jp/boards/operation/"+key)),"Talk URL");
        check(key.equals(ArchiveOriginPaths.threadKey("https://jbbs.shitaraba.net/bbs/read.cgi/game/1234/"+key+"/")),"Shitaraba URL");
        check(ArchiveOriginPaths.threadKey("file:///storage/archive/thread.dat")==null,"file rejected as origin");
        check(ArchiveOriginPaths.threadKey("https://user:secret@mi.5ch.io/test/read.cgi/news4vip/"+key)==null,"userinfo rejected");
        String relative="mi.5ch.io%2Fnews4vip/title(1)["+key+"]/mi.5ch.io%2Fnews4vip_"+key+".dat";
        check(("https://mi.5ch.io/test/read.cgi/news4vip/"+key+"/").equals(ArchiveOriginPaths.reconstruct(relative)),"encoded board reconstructs");
        check(ArchiveOriginPaths.reconstruct("news4vip/title(1)["+key+"]/news4vip_"+key+".dat")==null,"missing server not guessed");
        check(ArchiveOriginPaths.reconstruct(relative.replace("_"+key,"_1792102299"))==null,"mismatched DAT rejected");
        check(ArchiveOriginPaths.reconstruct(relative.replace("mi.5ch.io%2Fnews4vip","talk.jp%2Foperation")).startsWith("https://talk.jp/boards/"),"Talk reconstruction");
        check(ArchiveOriginPaths.hash(relative).equals(ArchiveOriginPaths.hash(relative)),"portable identity");
        check(!ArchiveOriginPaths.hash(relative).equals(ArchiveOriginPaths.hash(relative+"other")),"distinct identities");
        File root=new File("build/verification/archive-root").getAbsoluteFile();
        File target=new File(root,relative);
        check(relative.equals(ArchiveOriginPaths.relative(root,target)),"relative file mapping");
        try {ArchiveOriginPaths.relative(root,new File(root,"../escape.dat")); throw new AssertionError("traversal accepted");} catch(java.io.IOException expected) {count++;}
        try {ArchiveOriginPaths.relative(root,new File(root,"sibling.idx")); throw new AssertionError("IDX accepted");} catch(java.io.IOException expected) {count++;}
        System.out.println("PASS "+count+" archive original URL/path checks");
    }
}
