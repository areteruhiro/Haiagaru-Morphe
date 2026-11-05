package app.morphe.extension.chmate;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/** Process-local thumbnail delivery for ChMate's HTTP-only image downloader. */
final class VideoThumbnailServer {
    private static final ConcurrentHashMap<String, File> FILES = new ConcurrentHashMap<>();
    private static ServerSocket server;
    static synchronized String placeholder() throws IOException {
        android.content.Context context = Haiagaru.applicationContextForExtension();
        if (context == null) throw new IOException("Application context unavailable");
        File file = new File(context.getCacheDir(), "haiagaru-video-placeholder.png");
        if (!file.isFile() || file.length()==0) {
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(160, 90, android.graphics.Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(0xff444444);
            try (OutputStream output = new FileOutputStream(file)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
            } finally { bitmap.recycle(); }
        }
        return register("video-placeholder", file);
    }
    static synchronized String failure(String key,VideoThumbnailFailure failure)throws IOException {
        android.content.Context context=Haiagaru.applicationContextForExtension();
        if(context==null)throw new IOException("Application context unavailable");
        File directory=new File(context.getCacheDir(),"haiagaru-video-errors");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create thumbnail error directory");
        File image=new File(directory,"v2-"+failure.kind+"-"+failure.value+".png");
        if(!image.isFile()||image.length()==0){
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(360,240,android.graphics.Bitmap.Config.ARGB_8888);
            try{
                android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(0xff333333);
                android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setColor(0xffeeeeee);paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                paint.setTextSize(20);canvas.drawText("サムネイルを取得できません",180,30,paint);
                // Keep the native play badge's centre clear and preserve its click behaviour.
                fitErrorLine(canvas,paint,failure.label(),65);fitErrorLine(canvas,paint,failure.hint(),185);
                File staging=File.createTempFile("error-",".png",directory);
                try{
                    try(OutputStream output=new FileOutputStream(staging)){if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output))throw new IOException("Cannot encode thumbnail error");}
                    if(staging.length()==0||!staging.renameTo(image))throw new IOException("Cannot save thumbnail error");
                }finally{staging.delete();}
            }finally{bitmap.recycle();}
        }
        return register("video-error-"+key+"-"+failure.kind+"-"+failure.value,image);
    }
    private static void fitErrorLine(android.graphics.Canvas canvas,android.graphics.Paint paint,String text,float y){
        paint.setTextSize(20);float width=paint.measureText(text);if(width>340)paint.setTextSize(20*340/width);canvas.drawText(text,180,y,paint);
    }
    static synchronized String register(String key, File file) throws IOException {
        if (server == null) {
            server = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
            HttpsTransport.registerThumbnailServerPort(server.getLocalPort());
            Thread worker = new Thread(() -> {
                while (!server.isClosed()) {
                    try (Socket client = server.accept()) { serve(client); }
                    catch (IOException error) { android.util.Log.w("HaiagaruMedia", "Thumbnail delivery failed", error); }
                }
            }, "HaiagaruThumbnailServer");
            worker.setDaemon(true);
            worker.start();
        }
        FILES.put("/" + key + ".jpg", file);
        return "http://127.0.0.1:" + server.getLocalPort() + "/" + key + ".jpg";
    }
    private static void serve(Socket client) throws IOException {
        client.setSoTimeout(3000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "US-ASCII"));
        String line = reader.readLine();
        String[] parts = line == null ? new String[0] : line.split(" ");
        File file = parts.length == 3 && ("GET".equals(parts[0]) || "HEAD".equals(parts[0])) ? FILES.get(parts[1]) : null;
        OutputStream output = client.getOutputStream();
        if (file == null || !file.isFile()) {
            output.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes("US-ASCII"));
            return;
        }
        output.write(("HTTP/1.1 200 OK\r\nContent-Type: " + (file.getName().endsWith(".png") ? "image/png" : "image/jpeg") + "\r\nContent-Length: " + file.length()
                + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
        if ("HEAD".equals(parts[0])) return;
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }
}
