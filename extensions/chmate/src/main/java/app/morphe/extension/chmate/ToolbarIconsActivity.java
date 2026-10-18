package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.util.concurrent.Executors;

/** System document picker only; imports a bounded private copy, without broad storage access. */
public final class ToolbarIconsActivity extends Activity {
    private int selected = -1;
    private boolean busy;
    private LinearLayout rows;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) selected = state.getInt("selected", -1);
        setTitle("ツールバー表示・追加アイコン");
        ScrollView scroll = new ScrollView(this); rows = new LinearLayout(this); rows.setOrientation(1);
        int pad = (int)(16 * getResources().getDisplayMetrics().density); rows.setPadding(pad,pad,pad,pad);
        scroll.setFitsSystemWindows(true);
        scroll.addView(rows); setContentView(scroll); render();
    }
    private void render() {
        rows.removeAllViews();
        TextView help = new TextView(this);
        help.setText("追加アイコンの表示を切り替え、端末の画像に変更できます。ここで明示したオン／オフは標準ツールバーの表示チェックより優先し、配置順は標準設定に従います。変更後は再起動の確認が出ます。検索機能自体がオフの場合、検索ボタンは表示しません。画像の色はそのまま使います。\n画像は端末内にコピーするため、元画像を移動しても利用できます。"); rows.addView(help);
        for (int i=0;i<ToolbarIcons.NAMES.length;i++) {
            final int n=i;
            CheckBox visible = new CheckBox(this); visible.setText(ToolbarIcons.NAMES[i]);
            visible.setChecked(ToolbarIcons.visible(this,0x7e000001+i));
            visible.setOnCheckedChangeListener((v,on)->{getSharedPreferences(ToolbarIcons.PREFS,0).edit().putBoolean("visible."+n,on).apply();SettingsRestart.confirm(this);});
            rows.addView(visible);
            ImageView preview = new ImageView(this);
            preview.setImageDrawable(ToolbarIcons.preview(this, i, visible.getCurrentTextColor()));
            preview.setContentDescription(ToolbarIcons.NAMES[i]+"のアイコン");
            int iconSize=Math.round(40*getResources().getDisplayMetrics().density);
            rows.addView(preview,new LinearLayout.LayoutParams(iconSize,iconSize));
            Button image = new Button(this); image.setText(ToolbarIcons.file(this,i).isFile()?"画像を変更（カスタム画像使用中）":"画像を選択");
            image.setOnClickListener(v->{
                if(busy)return;selected=n;
                Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
                try {startActivityForResult(picker,71);}catch(RuntimeException e){toast("画像を選択できるアプリがありません");}
            });rows.addView(image);
            Button reset=new Button(this);reset.setText("標準のアイコンに戻す");
            reset.setOnClickListener(v->new AlertDialog.Builder(this).setMessage(ToolbarIcons.NAMES[n]+"の画像を標準に戻しますか？")
                .setNegativeButton("キャンセル",null).setPositiveButton("戻す",(d,w)->{
                    File f=ToolbarIcons.file(this,n); if(f.exists()&&!f.delete()){toast("画像を削除できませんでした");return;}
                    ToolbarIcons.invalidate(n);render();SettingsRestart.confirm(this);
                }).show());rows.addView(reset);
        }
        Button close=new Button(this);close.setText("閉じる");close.setOnClickListener(v->finish());rows.addView(close);
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("selected",selected);super.onSaveInstanceState(state);}
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=71||result!=RESULT_OK||data==null||data.getData()==null||selected<0||selected>=ToolbarIcons.NAMES.length||busy)return;
        final int target=selected; final android.net.Uri uri=data.getData(); busy=true;
        java.util.concurrent.ExecutorService io=Executors.newSingleThreadExecutor();
        io.execute(()->{
            String message;boolean saved=false;
            try(InputStream in=getContentResolver().openInputStream(uri)) {
                importImage(this,target,in);saved=true;message="画像を保存しました。ChMateを再起動すると反映されます";
            }catch(Exception|OutOfMemoryError e){message="画像を保存できませんでした。16MiB以下のPNG/JPEG/WebP等を選んでください";}
            final String done=message;final boolean success=saved;
            runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed()){toast(done);render();if(success)SettingsRestart.confirm(this);}});io.shutdown();
        });
    }
    static void importImage(android.content.Context context,int target,InputStream in)throws IOException {
        if(target<0||target>=ToolbarIcons.NAMES.length||in==null)throw new IOException("Invalid image");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(bytes.size()+n>16*1024*1024)throw new IOException("Image too large");bytes.write(buffer,0,n);}
        byte[] data=bytes.toByteArray();BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(data,0,data.length,options);
        if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Invalid image format");
        options.inJustDecodeBounds=false;options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>256)options.inSampleSize*=2;
        Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);if(bitmap==null)throw new IOException("Decode failed");
        // Keep aspect ratio within a square canvas; never stretch a rectangular user image.
        Bitmap square=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(square);
        float scale=256f/Math.max(bitmap.getWidth(),bitmap.getHeight()),w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
        canvas.drawBitmap(bitmap,null,new android.graphics.RectF((256-w)/2,(256-h)/2,(256+w)/2,(256+h)/2),new android.graphics.Paint(3));
        File file=ToolbarIcons.file(context,target);File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create directory");
        android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream out=null;
        try {out=atomic.startWrite();if(!square.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("Encode failed");atomic.finishWrite(out);}
        catch(IOException|RuntimeException e){atomic.failWrite(out);throw e;}
        finally {bitmap.recycle();square.recycle();}
        ToolbarIcons.invalidate(target);
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
