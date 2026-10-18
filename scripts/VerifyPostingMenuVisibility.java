import java.nio.file.*;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;

/** Runs the production visibility helper with minimal Android stand-ins. */
public class VerifyPostingMenuVisibility {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        String source = Files.readString(root.resolve("extensions/chmate/src/main/java/app/morphe/extension/chmate/Haiagaru.java"));
        int start = source.indexOf("    static boolean preparePostingMenu(");
        int end = source.indexOf("\n    }", start) + 6;
        if (start < 0 || end < start) throw new AssertionError("Missing helper");
        String helper = source.substring(start, end);
        Path temp = Files.createTempDirectory("posting-menu-test-");
        String harness = """
                import java.util.*;
                public class MenuHarness {
                    static class Context { Map<String,Boolean> values = new HashMap<>(); }
                    static class Prefs {
                        Context context; Prefs(Context c) {context=c;}
                        boolean getBoolean(String k,boolean d) {return context.values.getOrDefault(k,d);}
                    }
                    static Prefs preferences(Context c) {return new Prefs(c);}
                    static class MenuItem {boolean visible=true; void setVisible(boolean v) {visible=v;}}
                    static class Menu {Map<Integer,MenuItem> items=new HashMap<>(); MenuItem findItem(int id){return items.get(id);}}
                    static void check(boolean ok) {if(!ok)throw new AssertionError();}
                """ + helper + """
                    public static void run() {
                        Context c=new Context(); Menu m=new Menu();
                        String[] keys={"showClipboardHistoryMenu","showUploadHostMenu","showExternalBoardAuthMenu"};
                        for(int i=0;i<keys.length;i++) {
                            check(!preparePostingMenu(c,m,i,keys[i]));
                            c.values.put(keys[i],true);
                            check(preparePostingMenu(c,m,i,keys[i]));
                            c.values.put(keys[i],false);
                            check(!preparePostingMenu(c,m,i,keys[i]));
                            check(m.items.isEmpty());
                            MenuItem item=new MenuItem(); m.items.put(i,item);
                            check(!preparePostingMenu(c,m,i,keys[i])&&!item.visible);
                            c.values.put(keys[i],true);
                            check(!preparePostingMenu(c,m,i,keys[i])&&item.visible);
                            check(!preparePostingMenu(c,m,i,keys[i])&&m.items.size()==1);
                            m.items.clear();
                        }
                        c.values.put(keys[0],false);
                        check(!preparePostingMenu(c,m,0,keys[0]));
                        check(preparePostingMenu(c,m,1,keys[1]));
                        check(preparePostingMenu(c,m,2,keys[2]));
                        check(!preparePostingMenu(null,m,0,keys[0]));
                        check(!preparePostingMenu(c,null,0,keys[0]));
                    }
                }
                """;
        Path file=temp.resolve("MenuHarness.java");
        Files.writeString(file,harness);
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",temp.toString(),file.toString())!=0)
            throw new AssertionError("Compilation failed");
        try(URLClassLoader loader=new URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
            loader.loadClass("MenuHarness").getMethod("run").invoke(null);
        }
        String[] classes={"ClipboardHistory","MediaUploadHosts","ExternalBoardAuth"};
        String[] keys={"showClipboardHistoryMenu","showUploadHostMenu","showExternalBoardAuthMenu"};
        for(int i=0;i<classes.length;i++) {
            String entry=Files.readString(root.resolve("extensions/chmate/src/main/java/app/morphe/extension/chmate/"+classes[i]+".java"));
            if(!entry.contains("Haiagaru.preparePostingMenu(")||!entry.contains("\""+keys[i]+"\""))throw new AssertionError(classes[i]);
            if(!source.contains("{\""+keys[i]+"\", text("))throw new AssertionError("Setting missing: "+keys[i]);
        }
        System.out.println("PASS: defaults, independent toggles, hidden creation, reused menu hide/show, no duplicates, null inputs, all three menu/settings bindings");
    }
}
