package app.morphe.extension.chmate;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import org.json.JSONObject;

/** Creates an empty anonymous test token, reads its management form. Never uploads or deletes media. */
public final class ImgboxManagementPreflight {
    static MediaUploadClient.ImgboxCookies cookies=new MediaUploadClient.ImgboxCookies();
    static String request(String path, String form, String csrf) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("https://imgbox.com"+path).openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(30000);c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent","Haiagaru/1.0 (Android media uploader)");
        c.setRequestProperty("Cookie",cookies.header());
        if(form!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("X-CSRF-Token",csrf);
            c.setRequestProperty("X-Requested-With","XMLHttpRequest");c.setRequestProperty("Origin","https://imgbox.com");
            c.setRequestProperty("Referer","https://imgbox.com/");c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            try(var out=c.getOutputStream()){out.write(form.getBytes(StandardCharsets.US_ASCII));}}
        try{if(c.getResponseCode()!=200)throw new IOException("Preflight HTTP "+c.getResponseCode());
            cookies.receive(c.getHeaderFields());try(var in=c.getInputStream()){return new String(in.readNBytes(2*1024*1024),StandardCharsets.UTF_8);}}
        finally{c.disconnect();}
    }
    public static void main(String[] args)throws Exception {
        String home=request("/",null,null);
        JSONObject token=new JSONObject(request("/ajax/token/generate","gallery=true&gallery_title=&comments_enabled=0",MediaUploadClient.csrf(home)));
        String id=token.optString("token_id"), secret=token.optString("token_secret");
        String page=request("/upload/edit/"+id+"/"+secret,null,null).replace(id,"TOKEN_ID").replace(secret,"TOKEN_SECRET");
        for(String key:new String[]{"gallery_secret","gallery_id"})if(token.has(key))page=page.replace(token.optString(key),"REDACTED");
        System.out.println("Management GET succeeded; no media sent/deleted");
        Matcher m=Pattern.compile(".{0,160}(?:delete|Delete|destroy|remove|<script|<form).{0,230}").matcher(page);
        while(m.find())System.out.println(m.group());
    }
}
