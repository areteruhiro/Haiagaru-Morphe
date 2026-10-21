package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/** Compilation placeholder: the patch replaces this superclass and super calls with ChMate's own base. */
@SuppressWarnings("deprecation")
public final class NativeToolFragment extends android.app.Fragment {
    private Activity host;
    private NativeToolTabs.Content content;
    @Override public void onAttach(Context context) { super.onAttach(context); host=(Activity)context; }
    @Override public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle state) {
        content=NativeToolTabs.content(this,host,state); return content.view;
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
