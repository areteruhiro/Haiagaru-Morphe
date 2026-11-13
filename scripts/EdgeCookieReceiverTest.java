package app.morphe.extension.chmate;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Synthetic credentials only; no Android app data or public network access. */
public final class EdgeCookieReceiverTest {
    static int checks;
    static void ok(boolean result){checks++;if(!result)throw new AssertionError("Check "+checks);}
    static byte[] wire(String s){return s.getBytes(StandardCharsets.US_ASCII);}
    static Map<String,String> parse(String request)throws IOException{return EdgeCookieReceiver.read(new ByteArrayInputStream(wire(request)));}
    static String request(String target,String headers){return "GET "+target+" HTTP/1.1\r\nHost: bbs.eddibb.cc\r\n"+headers+"\r\n";}
    static void reject(String request)throws Exception{try{parse(request);throw new AssertionError("Accepted invalid request");}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        String h="Cookie: countsession=synthetic-session; securitykey=synthetic-key\r\n";
        ok(parse(request("http://bbs.eddibb.cc/liveedge/subject.txt",h)).size()==2);
        ok(parse(request("/liveedge/subject.txt",h)).get("securitykey").equals("synthetic-key"));
        ok(parse(request("/test/bbs.cgi",h).replace("GET ","POST ")).size()==2);
        reject(request("http://evil.example/liveedge/",h));
        reject(request("http://bbs.eddibb.cc.evil.example/liveedge/",h));
        reject(request("http://user@bbs.eddibb.cc/liveedge/",h));
        reject(request("http://bbs.eddibb.cc:8080/liveedge/",h));
        reject(request("https://bbs.eddibb.cc/liveedge/",h));
        reject(request("/unrelated/",h));
        reject(request("/liveedge/#fragment",h));
        reject(request("/liveedge/",h+"Host: evil.example\r\n"));
        reject(request("/liveedge/",h+h));
        reject(request("/liveedge/","Cookie: countsession=partial\r\n"));
        reject(request("/liveedge/","Cookie: countsession=; securitykey=\r\n"));
        reject(request("/liveedge/","Cookie: countsession=a; countsession=b; securitykey=c\r\n"));
        reject(request("/liveedge/","Cookie: countsession=a; CountSession=b; securitykey=c\r\n"));
        reject(request("/liveedge/","Cookie: cf_clearance=synthetic\r\n"));
        ok(parse(request("/liveedge/",h.replace("\r\n","; cf_clearance=synthetic; __cf_bm=synthetic\r\n"))).size()==2);
        reject(request("/liveedge/","Cookie: broken\r\n"));
        reject(request("/liveedge/","Cookie: a=one two\r\n"));
        reject(request("/liveedge/","Cookie: a="+"x".repeat(17000)+"\r\n"));
        reject("CONNECT bbs.eddibb.cc:443 HTTP/1.1\r\nHost: bbs.eddibb.cc:443\r\n\r\n");
        reject(request("/liveedge/",""));
        byte[] headers=wire(request("/test/bbs.cgi",h).replace("GET ","POST "));
        InputStream noBody=new InputStream(){int i;public int read(){if(i==headers.length)throw new AssertionError("POST body was read");return headers[i++]&255;}};
        ok(EdgeCookieReceiver.read(noBody).size()==2);
        try(EdgeCookieReceiver receiver=new EdgeCookieReceiver()){
            receiver.start();
            try(Socket socket=new Socket("127.0.0.1",receiver.port())){
                socket.setSoTimeout(4000);
                socket.getOutputStream().write(wire(request("/liveedge/subject.txt",h)));
                String response=new String(socket.getInputStream().readAllBytes(),StandardCharsets.US_ASCII);
                ok(response.startsWith("HTTP/1.1 503"));ok(!response.contains("synthetic-session"));
            }
            ok(receiver.snapshot()!=null&&receiver.snapshot().size()==2);
            receiver.close();ok(receiver.snapshot()==null);
        }
        System.out.println("EdgeCookieReceiver: "+checks+" passed; loopback only, no real credentials or posts");
    }
}
