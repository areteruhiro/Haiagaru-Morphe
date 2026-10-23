package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Compilation placeholder: the patch replaces this superclass and super calls with ChMate's own base. */
@SuppressWarnings("deprecation")
public final class NativeToolFragment extends android.app.Fragment {
    private Activity host;
    private NativeToolTabs.Content content;
    @Override public void onAttach(Context context) { super.onAttach(context); host=(Activity)context; }
    @Override public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle state) {
        content=NativeToolTabs.content(this,host,state);
        FrameLayout frame=new FrameLayout(host);
        frame.addView(content.view,new FrameLayout.LayoutParams(-1,-1));
        TextView close=new TextView(host);
        close.setText("×");
        close.setTextSize(24);
        close.setTextColor(Color.rgb(55,55,60));
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("このタブを閉じる");
        GradientDrawable background=new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.argb(235,245,245,248));
        try{
            int[] palette=NativeThemeColors.colors(host);
            if(palette!=null&&palette.length==7){
                close.setTextColor(palette[1]);
                // A zero dialog color means "use the window", not a black surface.
                int surface=Color.alpha(palette[3])>0?palette[3]:palette[0];
                if(Color.alpha(surface)==0)surface=palette[6];
                if(Color.alpha(surface)==0)surface=Color.red(palette[1])+Color.green(palette[1])+Color.blue(palette[1])>382?Color.BLACK:Color.WHITE;
                background.setColor((surface&0x00ffffff)|0xeb000000);
            }
        }catch(RuntimeException ignored){ /* Retain the fallback if the native theme is unavailable. */ }
        close.setBackground(background);
        close.setElevation(4*host.getResources().getDisplayMetrics().density);
        close.setOnClickListener(view -> {
            Activity current=host;
            if(current!=null&&!NativeToolTabs.closeCurrentTab(current,NativeToolFragment.this))
                android.widget.Toast.makeText(current,"この画面のタブを閉じられませんでした",android.widget.Toast.LENGTH_SHORT).show();
        });
        int size=Math.round(44*host.getResources().getDisplayMetrics().density);
        int margin=Math.round(6*host.getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams closeParams=new FrameLayout.LayoutParams(size,size,Gravity.TOP|Gravity.END);
        closeParams.setMargins(0,margin,margin,0);
        frame.addView(close,closeParams);
        return frame;
    }
    // Native list-fragment initialization assumes a native ListView. This tool owns its view tree.
    @Override public void onViewCreated(View view,Bundle state) { }
    @Override public void onPause() {
        if(content!=null)content.pause();
        if(content!=null)content.save(new Bundle());
        super.onPause();
    }
    @Override public void onResume(){super.onResume();if(content!=null)content.resume();}
    @Override public void onSaveInstanceState(Bundle state) {
        if(content!=null)content.save(state);
        super.onSaveInstanceState(state);
    }
    @Override public void onDestroyView() {
        if(content!=null){content.save(new Bundle());content.close();content=null;}
        super.onDestroyView();
    }
    public void openSettingsFile(){
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),SettingsProfiles.OPEN);
    }
    @Override public void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==SettingsProfiles.OPEN&&result==Activity.RESULT_OK&&data!=null&&data.getData()!=null&&host!=null)
            host.startActivity(new Intent(host,SettingsImportActivity.class).setData(data.getData()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }
    @Override public void onDetach(){host=null;super.onDetach();}
}
