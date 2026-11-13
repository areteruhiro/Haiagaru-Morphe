package app.morphe.extension.chmate;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One-shot, loopback-only header receiver. No upstream sockets, TLS interception or body reads. */
final class EdgeCookieReceiver implements Closeable {
    static final String HOST="bbs.eddibb.cc";
    static final String BOARD="https://"+HOST+"/liveedge/";
    static final long LIFETIME=5*60*1000L;
    private final ServerSocket server;
    private final long deadline;
    private volatile Socket client;
    private volatile boolean closed;
    private Map<String,String> captured;
    private volatile String status="通常版ChMateからの接続を待っています";

    EdgeCookieReceiver() throws IOException {
        server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
        server.setSoTimeout(1000);
        deadline=System.nanoTime()+LIFETIME*1000000L;
    }
    int port(){return server.getLocalPort();}
    boolean expired(){return System.nanoTime()>=deadline;}
    String status(){return expired()?"待受の有効期限が切れました。やり直してください":status;}
    synchronized Map<String,String> snapshot(){
        if(expired()){captured=null;return null;}
        return captured==null?null:new LinkedHashMap<>(captured);
    }
    void start(){new Thread(this::serve,"Haiagaru-edge-cookie-receiver").start();}
    private void serve(){
        try{
            while(!closed&&!expired()){
                try(Socket socket=server.accept()){
                    client=socket;socket.setSoTimeout(3000);
                    try{
                        Map<String,String> cookies=read(socket.getInputStream());
                        synchronized(this){if(!closed&&!expired()&&!cookies.isEmpty()){captured=cookies;status=cookies.size()+"件のCookieを受信しました。Haiagaruに戻って確認してください";}}
                    }catch(Encrypted ignored){status="HTTPSの暗号化通信のため取得できません。証明書の追加は行いません";}
                    catch(NoCookies ignored){status="エッヂからCookieを受信できませんでした。通常版のエッヂ画面を更新してください";}
                    catch(IncompleteIdentity ignored){status="認証Cookieの組が揃わないため取り込みませんでした。この通信では移行できません";}
                    catch(IOException|IllegalArgumentException ignored){status="対象外または不正なリクエストを拒否しました";}
                    // Never return an empty subject/DAT or forward a posting request.
                    byte[] body="Cookie transfer only. No request was forwarded. Restore the original proxy setting.\n".getBytes(StandardCharsets.US_ASCII);
                    socket.getOutputStream().write(("HTTP/1.1 503 Cookie Transfer Only\r\nContent-Type: text/plain\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: "+body.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(body);socket.getOutputStream().flush();
                    synchronized(this){if(captured!=null)break;}
                }catch(SocketTimeoutException ignored){}finally{client=null;}
            }
        }catch(IOException ignored){if(!closed)status="受信を終了しました。やり直してください";}
        finally{try{server.close();}catch(IOException ignored){}if(expired())synchronized(this){captured=null;}}
    }
    static final class Encrypted extends IOException {}
    static final class NoCookies extends IOException {}
    static final class IncompleteIdentity extends IOException {}
    static Map<String,String> read(InputStream input)throws IOException{
        // Read exactly the header bytes, never a buffered reader that prefetches a POST body.
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();int tail=0;
        long limit=System.nanoTime()+3000000000L;
        while(bytes.size()<16384){
            if(System.nanoTime()>=limit)throw new IOException("Header timeout");
            int b=input.read();if(b<0)throw new IOException("Incomplete headers");
            if(b>126||(b<32&&b!=13&&b!=10&&b!=9))throw new IOException("Invalid header");
            bytes.write(b);tail=(tail<<8)|b;if(tail==0x0d0a0d0a)break;
        }
        if(tail!=0x0d0a0d0a)throw new IOException("Oversized headers");
        String[] lines=bytes.toString("US-ASCII").split("\r\n");
        String[] request=lines[0].split(" ",-1);
        if(request.length!=3||!request[2].matches("HTTP/1\\.[01]"))throw new IOException("Invalid request");
        if("CONNECT".equals(request[0]))throw new Encrypted();
        if(!Arrays.asList("GET","HEAD","POST").contains(request[0]))throw new IOException("Invalid method");
        String host=null,header=null;
        for(int i=1;i<lines.length;i++){
            int colon=lines[i].indexOf(':');if(colon<=0||Character.isWhitespace(lines[i].charAt(0)))throw new IOException("Invalid header");
            String name=lines[i].substring(0,colon),value=lines[i].substring(colon+1).trim();
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+"))throw new IOException("Invalid header name");
            if(name.equalsIgnoreCase("Host")){if(host!=null)throw new IOException("Duplicate host");host=value;}
            if(name.equalsIgnoreCase("Cookie")){if(header!=null)throw new IOException("Duplicate cookies");header=value;}
        }
        URI uri;
        try{uri=URI.create(request[1].startsWith("/")?"http://"+host+request[1]:request[1]);}catch(RuntimeException e){throw new IOException("Invalid target");}
        if(!HOST.equalsIgnoreCase(uri.getHost())||!"http".equals(uri.getScheme())||uri.getRawUserInfo()!=null||uri.getFragment()!=null
                ||(uri.getPort()!=-1&&uri.getPort()!=80)||!(HOST.equalsIgnoreCase(host)||(HOST+":80").equalsIgnoreCase(host)))throw new IOException("Unrelated origin");
        String path=uri.getRawPath();
        if(path==null||!(path.equals("/liveedge/")||path.startsWith("/liveedge/")||path.startsWith("/test/read.cgi/liveedge/")||path.equals("/test/bbs.cgi")))throw new IOException("Unrelated path");
        if(header==null||header.isEmpty())throw new NoCookies();
        return cookies(header);
    }
    static Map<String,String> cookies(String header)throws IOException{
        LinkedHashMap<String,String> result=new LinkedHashMap<>();
        if(header.length()>8192)throw new IOException("Oversized cookies");
        for(String part:header.split(";",-1)){
            String pair=part.trim();int equal=pair.indexOf('=');if(equal<=0)throw new IOException("Invalid cookie");
            String name=pair.substring(0,equal),value=pair.substring(equal+1);
            if(!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}")||!value.matches("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]{0,4096}"))throw new IOException("Invalid cookie");
            // Cloudflare clearance belongs to the separate reading/browser identity.
            if(name.equalsIgnoreCase("cf_clearance")||name.toLowerCase(Locale.ROOT).startsWith("__cf"))continue;
            if(result.put(name,value)!=null||result.size()>32)throw new IOException("Ambiguous cookies");
        }
        if(result.isEmpty())throw new NoCookies();
        String a=null,b=null;
        for(String name:result.keySet()){
            if(name.equalsIgnoreCase("countsession")){if(a!=null)throw new IOException("Duplicate identity");a=name;}
            if(name.equalsIgnoreCase("securitykey")){if(b!=null)throw new IOException("Duplicate identity");b=name;}
        }
        if((a==null)!=(b==null))throw new IncompleteIdentity();
        if(a!=null&&(result.get(a).isEmpty()||result.get(b).isEmpty()))throw new IncompleteIdentity();
        return result;
    }
    @Override public synchronized void close(){
        closed=true;captured=null;
        try{server.close();}catch(IOException ignored){}
        Socket socket=client;if(socket!=null)try{socket.close();}catch(IOException ignored){}
    }
}
