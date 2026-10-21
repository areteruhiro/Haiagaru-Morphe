package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import java.util.function.Consumer;

/** Owns one screen's state, not an Activity. Shared by standalone screens and native tabs. */
abstract class ToolScreenController extends ContextThemeWrapper {
    final Activity host;
    final boolean embedded;
    private Intent intent;
    private final Runnable back;
    private final Consumer<View> render;
    private boolean closed;
    ToolScreenController(Activity host,Intent intent,boolean embedded,Runnable back,Consumer<View> render){
        super(SettingsTabUi.context(host),android.R.style.Theme_Material_Light_NoActionBar);
        this.host=host;this.intent=new Intent(intent);this.embedded=embedded;this.back=back;this.render=render;
    }
    final Intent getIntent(){return intent;}
    final void setIntent(Intent value){intent=new Intent(value);}
    final Window getWindow(){return host.getWindow();}
    final boolean isFinishing(){return closed||host.isFinishing();}
    final boolean isDestroyed(){return closed||host.isDestroyed();}
    final void finish(){back.run();}
    final void runOnUiThread(Runnable action){host.runOnUiThread(()->{if(!isDestroyed())action.run();});}
    final void requestPermissions(String[] permissions,int code){host.requestPermissions(permissions,code);}
    final void showModel(String title,View model){render.accept(ToolScreenUi.model(host,title,model,this::finish));}
    final void showSearch(HissiWacchoiSearchController controller){render.accept(SearchComposeUi.embed(host,controller));}
    final void showArchive(android.widget.LinearLayout model){render.accept(ToolScreenUi.archive(host,model,this::finish));}
    abstract void create(Bundle state);
    void resume(){}
    void pause(){}
    void save(Bundle state){}
    void close(){closed=true;}
}
