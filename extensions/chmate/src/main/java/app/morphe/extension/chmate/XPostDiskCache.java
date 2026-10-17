package app.morphe.extension.chmate;

import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/** Public metadata only. All callers run on workers; no network or UI work here. */
final class XPostDiskCache {
    static final long MAX_AGE = 7L * 24 * 60 * 60 * 1000;
    static final long MAX_BYTES = 16L * 1024 * 1024;
    static final int MAX_ENTRIES = 128, MAX_ITEM = 512000;
    private final File directory;
    XPostDiskCache(File directory) { this.directory = directory; }
    private File file(String id) {
        if (id == null || !id.matches("[0-9]{1,25}")) throw new IllegalArgumentException("Invalid X post ID");
        return new File(directory,id+".json");
    }
    synchronized String read(String id, long now) {
        AtomicFile file = new AtomicFile(file(id));
        try (InputStream input=file.openRead(); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int n;
            while((n=input.read(buffer))!=-1) { if(bytes.size()+n>MAX_ITEM+1024)throw new IOException("Oversized cache"); bytes.write(buffer,0,n); }
            JSONObject record=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            long saved=record.getLong("saved");
            if(record.getInt("version")!=1 || saved<=0 || saved>now+300000 || now-saved>=MAX_AGE)throw new IOException("Expired cache");
            String json=record.getJSONObject("post").toString();
            if(!valid(id,json))throw new IOException("Invalid cached post");
            return json;
        } catch(Exception ignored) { file.delete(); return null; }
    }
    static boolean valid(String id,String json) {
        try { return json!=null && json.getBytes(StandardCharsets.UTF_8).length<=MAX_ITEM
                && id.equals(new JSONObject(json).optString("id_str")); }
        catch(Exception ignored){return false;}
    }
    synchronized void write(String id,String json,long now) {
        if(!valid(id,json))return;
        AtomicFile target=new AtomicFile(file(id)); FileOutputStream stream=null;
        try {
            if(!directory.isDirectory()&&!directory.mkdirs())return;
            // A JSON object, not a quoted JSON string, keeps the disk bound close to wire size.
            byte[] bytes=new JSONObject().put("version",1).put("saved",now).put("post",new JSONObject(json)).toString().getBytes(StandardCharsets.UTF_8);
            if(bytes.length>MAX_ITEM+1024)return;
            stream=target.startWrite(); stream.write(bytes); target.finishWrite(stream); stream=null;
            target.getBaseFile().setLastModified(now);
            prune(now);
        } catch(Exception ignored){if(stream!=null)target.failWrite(stream);}
    }
    private void prune(long now) {
        File[] files=directory.listFiles(f->f.isFile()&&f.getName().matches("[0-9]{1,25}\\.json"));
        if(files==null)return;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());
        long size=0;int count=0;
        for(File file:files) {
            long length=file.length();
            if(now-file.lastModified()>=MAX_AGE || ++count>MAX_ENTRIES || size+length>MAX_BYTES) new AtomicFile(file).delete();
            else size+=length;
        }
    }
    synchronized void clear() {
        File[] files=directory.listFiles(); if(files==null)return;
        for(File file:files)if(file.isFile()&&file.getName().matches("[0-9]{1,25}\\.json(?:\\.bak|\\.new)?"))file.delete();
    }
}
