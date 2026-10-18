package app.morphe.extension.chmate;

import android.content.Context;
import java.io.*;

/** Keep the existing bounded page cache in app files, migrating accessed legacy entries. */
final class WacchoiPageCache {
    static File file(Context c,String name)throws IOException{
        File dir=new File(c.getFilesDir(),"hissi-public-pages");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("キャッシュを作成できません");
        File result=new File(dir,name),old=new File(new File(c.getCacheDir(),"hissi-public-pages"),name);
        if(!result.exists()&&old.isFile()&&old.length()<=2*1024*1024){
            try(InputStream in=new FileInputStream(old);OutputStream out=new FileOutputStream(result)){
                byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1)out.write(buffer,0,read);
            }catch(IOException e){result.delete();throw e;}
            result.setLastModified(old.lastModified());
        }
        return result;
    }
    private WacchoiPageCache(){}
}
