package app.morphe.extension.chmate;

import android.content.*;
import android.text.*;
import android.text.style.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** Real Android spans/preferences, synthetic poster models; no network or native NG store access. */
public final class HighlightTest {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    public static final class Board {
        public String d(int no){return "https://example.invalid/test/read.cgi/fixture/1234567890/"+no;}
        public String e(int no){return d(no);}
        public String c(int no){return d(no);}
    }
    static boolean tinted(Spanned text){
        return Arrays.stream(text.getSpans(0,text.length(),BackgroundColorSpan.class))
                .anyMatch(span->span.getClass().getName().contains("PosterHighlight$Tint"));
    }
    public static String run(Context app) throws Exception {
        String fixture="haiagaru-highlight-fixture-"+UUID.randomUUID();
        Context context=new ContextWrapper(app){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return app.getSharedPreferences(fixture,mode);}
        };
        try {
            PosterHighlight.initialize(context);
            SharedPreferences prefs=context.getSharedPreferences(PosterHighlight.PREFS,0);
            String original="名前 ID:Abc+/123\n本文 https://example.invalid/";
            SpannableStringBuilder source=new SpannableStringBuilder(original);
            URLSpan url=new URLSpan("https://example.invalid/");
            int start=original.indexOf("https:");source.setSpan(url,start,original.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            BackgroundColorSpan nativeColor=new BackgroundColorSpan(0xff334455);
            source.setSpan(nativeColor,0,2,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String thread="example.invalid/fixture/1234567890";
            for(Object response:new Object[]{new o.processAdDisplayErrorPostbackForUserError(),new o.BouncyCastleSocketAdapterCompanion(),new o.KeJ11()}) {
                var poster=PosterHighlight.poster(response);
                check(poster.id.equals("Abc+/123") && poster.suffix.equals("cD+/"),"Native-profile header bindings");
                prefs.edit().putBoolean("enabled",true).apply();
                PosterHighlight.save(List.of(new PosterHighlightRules.Rule(3,0,thread,"cD+/",true)));
                Spanned shown=PosterHighlight.decorateResponse(source,response,new Board(),null);
                check(tinted(shown),"Matching poster tinted");
                Spanned rebound=PosterHighlight.decorateResponse(shown,response,new Board(),null);
                int identityCount=0;
                for(Object span:rebound.getSpans(0,rebound.length(),Object.class))
                    if(span.getClass().getName().endsWith("PosterHighlight$Identity"))identityCount++;
                check(identityCount==1,"Re-decoration retains one current identity");
                check(shown.toString().equals(original) && source.toString().equals(original),"Display and source text unchanged");
                check(shown.getSpanStart(url)==start && shown.getSpanEnd(url)==original.length(),"Link offsets and destination retained");
                check(shown.getSpanStart(nativeColor)==0,"Native color retained");
                TextView view=new TextView(app);view.setText(shown,TextView.BufferType.SPANNABLE);
                PosterHighlight.save(List.of(new PosterHighlightRules.Rule(3,0,thread,"xxxx",true)));
                PosterHighlight.apply(view);
                check(!tinted((Spanned)view.getText()),"Body quotation not mistaken for poster suffix");
                PosterHighlight.save(List.of(new PosterHighlightRules.Rule(1,0,thread,poster.id,true)));
                PosterHighlight.apply(view);
                check(tinted((Spanned)view.getText()),"Live re-enable by ID");
                prefs.edit().putBoolean("enabled",false).apply();PosterHighlight.save(List.of(new PosterHighlightRules.Rule(1,0,thread,poster.id,true)));
                PosterHighlight.apply(view);
                check(!tinted((Spanned)view.getText()) && ((Spanned)view.getText()).getSpanStart(nativeColor)==0,"Master OFF removes only own styles");
                prefs.edit().putBoolean("enabled",true).apply();PosterHighlight.save(Collections.emptyList());
                PosterHighlight.apply(view);
                check(!tinted((Spanned)view.getText()),"Deleted rule removed");
                // Persistence reload is exercised through the actual SharedPreferences JSON path.
                PosterHighlight.save(List.of(new PosterHighlightRules.Rule(4,2,"","trip+/",true)));
                check(tinted(PosterHighlight.decorateResponse(source,response,new Board(),null)),"Persisted trip rule");
                PosterHighlight.save(List.of(new PosterHighlightRules.Rule(0,0,thread,"",true)));
                check(!tinted(PosterHighlight.decorateResponse(source,response,new Board(),null)),"Missing starter never guessed");
                check(tinted(PosterHighlight.decorateResponse(source,response,new Board(),poster)),"Starter exact identity");
                EditText editor=new EditText(app);editor.setText(shown);
                CharSequence before=editor.getText();PosterHighlight.apply(editor);
                check(editor.getText()==before,"Posting editor untouched");
            }
            // 242 list-side observation does not use the first visible filtered response as OP.
            var first=new o.KeJ11();first.o=1;
            var next=new o.KeJ11();next.o=50;
            PosterHighlight.observeResponses(Arrays.asList(first,next));
            check(tinted(PosterHighlight.decorateResponse(source,next,new Board(),null)),"242 starter known even when response 1 not visible");
            var unknown=new o.KeJ11();unknown.o=60;
            PosterHighlight.observeResponses(Arrays.asList(unknown));
            check(!tinted(PosterHighlight.decorateResponse(source,unknown,new Board(),null)),"Filtered list is not inferred as starter");
            var menu=new android.widget.PopupMenu(app,new android.view.View(app)).getMenu();
            PosterHighlight.append(menu,next,new Board());PosterHighlight.append(menu,next,new Board());
            check(menu.size()==1 && menu.getItem(0).getTitle().toString().contains("強調"),"Native Menu entry unique");
            check(PosterHighlight.handle(menu.getItem(0)),"Native popup dispatch consumes highlight action");
            check(!PosterHighlight.handle(null),"Unrelated popup action is not consumed");
            return "PASS "+checks+" Android poster highlight checks; synthetic identities, no network/NG changes";
        } catch(Throwable error) {
            android.util.Log.e("HaiagaruHighlightTest","FAIL",error);throw error;
        } finally {app.deleteSharedPreferences(fixture);}
    }
}
