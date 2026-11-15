package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.Html;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** In-place selection over the original response list. Native rows and listeners are untouched. */
public final class ResponseMultiSelection {
    private static final int SELECT=0x4847382,COPY=0x4847383,NG=0x4847384,TOOLS=0x4847385,SAVE=0x4847386;
    private static final Map<Activity,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<MenuItem,Runnable> ACTIONS=new WeakHashMap<>();
    private static final ThreadLocal<Boolean> NATIVE_TOOLS=new ThreadLocal<>();
    static boolean nativeToolsRequested(){return Boolean.TRUE.equals(NATIVE_TOOLS.get());}
    static boolean selecting(){return !SESSIONS.isEmpty();}
    static boolean hideSearch(int id){return selecting()&&(id==75||id==0x7e000077||id==0x7e000078);}
    private static final String[] COPY_FIELDS={"レス番号","名前","本文","レスのURL","ヘッダー＋本文","選択（項目を組み合わせる・編集）"};
    static void start(MultiResponseNg.Target target,ArrayList<MultiResponseNg.Plan> plans)throws Exception{
        Session old=SESSIONS.get(target.activity);if(old!=null)old.close();
        Session next=new Session(target,plans);SESSIONS.put(target.activity,next);next.begin();
    }
    private static MultiResponseNg.Target target(Object owner,Object board)throws Exception{
        Activity a=Haiagaru.toolbarActivity(owner);if(a==null)a=Haiagaru.toolbarActivity(null);if(a==null)return null;
        String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
        Object fragment=FilterPosition.currentThread(a,NativeThreadContext.url(board,version,1));
        return fragment==null?null:new MultiResponseNg.Target(fragment);
    }
    private static void selectResponse(MultiResponseNg.Target target,Object response){
        Session active=SESSIONS.get(target.activity);
        try{if(active!=null&&active.target.url.equals(target.url)){active.toggle(ExtraQuickFilters.number(response));return;}}
        catch(Exception ignored){}
        MultiResponseNg.open(target.fragment);
        // Collection runs off the UI thread. Apply the exact original menu target when ready.
        try{int number=ExtraQuickFilters.number(response);Handler h=new Handler(Looper.getMainLooper());
            Runnable pending=new Runnable(){int attempts;public void run(){Session s=SESSIONS.get(target.activity);
                if(s!=null&&s.target.url.equals(target.url)){s.toggle(number);return;}
                if(++attempts<30)h.postDelayed(this,100);}};h.postDelayed(pending,100);
        }catch(Exception ignored){}
    }
    public static void append(Object owner,Object menuObject,Object response,Object board){
        if(!(menuObject instanceof Menu)||response==null||board==null)return;
        try{MultiResponseNg.Target t=target(owner,board);if(t==null)return;Menu menu=(Menu)menuObject;
            add(menu,SELECT,"選択",()->selectResponse(t,response));
            Session s=SESSIONS.get(t.activity);
            Runnable copy=()->{
                Session current=SESSIONS.get(t.activity);
                if(current!=null&&current.target.url.equals(t.url))copy(t,current.selected());
                else try{MultiResponseNg.Plan p=new MultiResponseNg.Plan(response);p.selected=true;copy(t,new ArrayList<>(Arrays.asList(p)));}catch(Exception e){report(t.activity,e);}
            };
            MenuItem nativeCopy=null,nativeNg=null;
            for(int i=0;i<menu.size();i++){MenuItem item=menu.getItem(i);String title=String.valueOf(item.getTitle());
                if(title.startsWith("コピー"))nativeCopy=item;
                if(title.startsWith("NG")&&(title.contains("追加")||title.contains("登録"))&&!title.contains("Thread"))nativeNg=item;
            }
            if(nativeCopy==null)add(menu,COPY,s==null?"コピー・選択":"コピー（選択中のレス）",copy);
            else{nativeCopy.setTitle(s==null?"コピー・選択":"コピー（選択中のレス）");ACTIONS.put(nativeCopy,copy);nativeCopy.setOnMenuItemClickListener(ResponseMultiSelection::handle);}
            if(s!=null&&s.target.url.equals(t.url)){
                Runnable ng=()->MultiResponseNg.ng(t,s.selected());
                if(nativeNg==null)add(menu,NG,"選択中のレスをNG登録",ng);
                else{nativeNg.setTitle("NG登録（選択中のレス）");ACTIONS.put(nativeNg,ng);nativeNg.setOnMenuItemClickListener(ResponseMultiSelection::handle);}
            }
        }catch(Exception e){android.util.Log.w("Haiagaru","Response selection menu",e);}
    }
    static void appendSheet(Object builder,Object response,Object board)throws Exception{
        MultiResponseNg.Target t=target(null,board);if(t==null)return;
        ResponseArchive.appendSheetAction(builder,"選択",android.R.drawable.checkbox_on_background,()->selectResponse(t,response));
        ResponseArchive.appendSheetAction(builder,"コピー・選択",android.R.drawable.ic_menu_edit,()->{
            Session s=SESSIONS.get(t.activity);if(s!=null&&s.target.url.equals(t.url))copy(t,s.selected());
            else try{MultiResponseNg.Plan p=new MultiResponseNg.Plan(response);p.selected=true;copy(t,new ArrayList<>(Arrays.asList(p)));}catch(Exception e){report(t.activity,e);}
        });
        Session s=SESSIONS.get(t.activity);if(s!=null&&s.target.url.equals(t.url))
            ResponseArchive.appendSheetAction(builder,"選択中のレスをNG登録",android.R.drawable.ic_delete,()->MultiResponseNg.ng(t,s.selected()));
    }
    private static void add(Menu menu,int id,String title,Runnable action){
        MenuItem item=menu.findItem(id);if(item==null)item=menu.add(0,id,menu.size(),title);
        ACTIONS.put(item,action);item.setOnMenuItemClickListener(v->handle(v));
    }
    public static boolean handle(Object item){Runnable action=ACTIONS.get(item);if(action==null)return false;action.run();return true;}
    static boolean saveSelected(Activity activity){
        Session s=SESSIONS.get(activity);if(s==null)return false;
        ArrayList<MultiResponseNg.Plan> selected=s.selected();if(selected.isEmpty()){Toast.makeText(activity,"レスをタップして選択してください",0).show();return true;}
        new HaiagaruDialog.Builder(activity).setTitle(selected.size()+"レスを保存")
            .setItems(new String[]{"1レスずつ別々に保存","選択したレスを1件にまとめて保存"},(d,which)->{
                try{s.target.current();ArrayList<ResponseArchiveEntry> entries=new ArrayList<>();
                    for(MultiResponseNg.Plan p:selected)entries.add(ResponseArchiveEntry.snapshot(p.response,s.target.board,String.valueOf(activity.getTitle())));
                    ResponseArchive.saveSelection(activity,entries,which==1);
                }catch(Exception e){report(activity,e);}
            }).setNegativeButton("キャンセル",null).show();return true;
    }
    private static void report(Activity a,Exception e){android.util.Log.w("Haiagaru","Response selection",e);Toast.makeText(a,"操作できませんでした。スレを開き直してください。",1).show();}
    private static final class Session implements ActionMode.Callback {
        final MultiResponseNg.Target target;final ArrayList<MultiResponseNg.Plan> plans;
        final Map<Integer,MultiResponseNg.Plan> byNumber=new HashMap<>();final ViewGroup list;
        final Method item;final Window.Callback original,proxy;final Tint tint;
        ActionMode mode;boolean closed,tracking,scrolling;MotionEvent down;
        final ViewTreeObserver.OnScrollChangedListener scrolled;
        final ViewTreeObserver.OnGlobalLayoutListener layout;
        Session(MultiResponseNg.Target t,ArrayList<MultiResponseNg.Plan> p)throws Exception{
            target=t;plans=p;for(MultiResponseNg.Plan x:p)byNumber.put(x.number,x);
            View root=(View)t.fragment.getClass().getMethod("getView").invoke(t.fragment);
            list=(ViewGroup)FilterPosition.findList(root);if(list==null)throw new IllegalStateException("Response list missing");
            Object adapter=list.getClass().getMethod("getAdapter").invoke(list);
            item=list instanceof ListView?null:FilterPosition.findItem(adapter);if(item!=null)item.setAccessible(true);
            original=t.activity.getWindow().getCallback();
            proxy=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(obj,m,args)->{
                if(m.getName().equals("dispatchTouchEvent")&&touch((MotionEvent)args[0]))return true;
                if(m.getName().equals("dispatchKeyEvent")&&args[0] instanceof KeyEvent){KeyEvent e=(KeyEvent)args[0];if(e.getKeyCode()==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)close();return true;}}
                try{return m.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
            });
            tint=new Tint(t.activity,this);scrolled=()->tint.invalidate();
            layout=()->{if(!list.isAttachedToWindow()){close();return;}tint.layout(0,0,list.getWidth(),list.getHeight());tint.invalidate();};
        }
        void begin(){
            target.activity.getWindow().setCallback(proxy);list.getOverlay().add(tint);tint.layout(0,0,list.getWidth(),list.getHeight());
            list.getViewTreeObserver().addOnScrollChangedListener(scrolled);list.getViewTreeObserver().addOnGlobalLayoutListener(layout);
            mode=target.activity.startActionMode(this);if(mode==null){close();return;}
            update();Toast.makeText(target.activity,"レスをタップして選択・解除。操作から既存の長押しメニューを開けます。",1).show();
        }
        int number(View child)throws Exception{
            Object row;if(list instanceof ListView){ListView v=(ListView)list;int at=v.getPositionForView(child);if(at<0)return -1;row=v.getItemAtPosition(at);}
            else{int at=(Integer)list.getClass().getMethod("getChildAdapterPosition",View.class).invoke(list,child);if(at<0)return -1;
                Object adapter=list.getClass().getMethod("getAdapter").invoke(list);row=item.invoke(adapter,at);}
            Object response=ExtraQuickFilters.rowResponse(row);return response==null?-1:ExtraQuickFilters.number(response);
        }
        View childAt(float x,float y){for(int i=list.getChildCount()-1;i>=0;i--){View child=list.getChildAt(i);if(x>=child.getLeft()&&x<child.getRight()&&y>=child.getTop()&&y<child.getBottom())return child;}return null;}
        boolean touch(MotionEvent e){
            if(closed)return false;
            try{target.current();if(!list.isShown()){close();return false;}}catch(Exception changed){close();return false;}
            int action=e.getActionMasked();int[] loc=new int[2];list.getLocationOnScreen(loc);
            float x=e.getRawX()-loc[0],y=e.getRawY()-loc[1];
            if(action==MotionEvent.ACTION_DOWN){
                tracking=x>=0&&y>=0&&x<list.getWidth()&&y<list.getHeight();scrolling=false;
                if(!tracking)return false;down=MotionEvent.obtain(e);return true;
            }
            if(!tracking)return false;
            if(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_POINTER_DOWN){
                float dx=e.getRawX()-down.getRawX(),dy=e.getRawY()-down.getRawY();
                int slop=ViewConfiguration.get(list.getContext()).getScaledTouchSlop();
                if(!scrolling&&(e.getPointerCount()>1||dx*dx+dy*dy>slop*slop)){scrolling=true;original.dispatchTouchEvent(down);}
                if(scrolling)original.dispatchTouchEvent(e);return true;
            }
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
                if(scrolling)original.dispatchTouchEvent(e);
                else if(action==MotionEvent.ACTION_UP){View child=childAt(x,y);if(child!=null)try{toggle(number(child));}catch(Exception error){report(target.activity,error);}}
                tracking=false;if(down!=null){down.recycle();down=null;}return true;
            }return true;
        }
        void toggle(int number){MultiResponseNg.Plan p=byNumber.get(number);if(p==null)return;
            if(!p.selected&&selected().size()>=100){Toast.makeText(target.activity,"最大100件まで選択できます",0).show();return;}
            p.selected=!p.selected;update();
        }
        ArrayList<MultiResponseNg.Plan> selected(){ArrayList<MultiResponseNg.Plan> out=new ArrayList<>();for(MultiResponseNg.Plan p:plans)if(p.selected)out.add(p);return out;}
        void update(){if(mode!=null)mode.setTitle(selected().size()+"レス選択中");tint.invalidate();}
        void close(){if(closed)return;closed=true;
            if(target.activity.getWindow().getCallback()==proxy)target.activity.getWindow().setCallback(original);
            if(down!=null){if(scrolling){MotionEvent cancel=MotionEvent.obtain(down);cancel.setAction(MotionEvent.ACTION_CANCEL);original.dispatchTouchEvent(cancel);cancel.recycle();}down.recycle();down=null;}
            list.getOverlay().remove(tint);if(list.getViewTreeObserver().isAlive()){list.getViewTreeObserver().removeOnScrollChangedListener(scrolled);list.getViewTreeObserver().removeOnGlobalLayoutListener(layout);}
            if(SESSIONS.get(target.activity)==this)SESSIONS.remove(target.activity);if(mode!=null){ActionMode old=mode;mode=null;old.finish();}
        }
        public boolean onCreateActionMode(ActionMode mode,Menu menu){
            menu.add(0,NG,0,"NG登録").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0,COPY,1,"コピー").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(0,SAVE,2,"レスを保存");menu.add(0,TOOLS,3,"操作（レス長押しメニュー）");menu.add(0,SELECT,3,"選択をすべて解除");return true;
        }
        public boolean onPrepareActionMode(ActionMode m,Menu menu){return false;}
        public boolean onActionItemClicked(ActionMode m,MenuItem entry){
            ArrayList<MultiResponseNg.Plan> selected=selected();
            if(entry.getItemId()==SELECT){for(MultiResponseNg.Plan p:plans)p.selected=false;update();return true;}
            if(selected.isEmpty()){Toast.makeText(target.activity,"レスをタップして選択してください",0).show();return true;}
            if(entry.getItemId()==NG){MultiResponseNg.ng(target,selected);return true;}
            if(entry.getItemId()==COPY){copy(target,selected);return true;}
            if(entry.getItemId()==SAVE){saveSelected(target.activity);return true;}
            if(entry.getItemId()==TOOLS){nativeTools(selected);return true;}return false;
        }
        void nativeTools(ArrayList<MultiResponseNg.Plan> selected){
            Set<Integer> numbers=new HashSet<>();for(MultiResponseNg.Plan p:selected)numbers.add(p.number);
            for(int i=0;i<list.getChildCount();i++)try{View child=list.getChildAt(i);if(!numbers.contains(number(child)))continue;
                View body=body(child);if(body!=null){NATIVE_TOOLS.set(true);try{if(body.performLongClick())return;}finally{NATIVE_TOOLS.remove();}}
            }catch(Exception ignored){}
            Toast.makeText(target.activity,"選択中のレスを画面内に表示してから操作を開いてください",1).show();
        }
        View body(View v){String n=v.getClass().getName();if(n.equals("o.r8lambda1VMNzQcdpAFQ0ukC0aRPi4Owdo")||n.equals("o.PAGClientBidding")||n.equals("o.zzbhr"))return v;
            if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=body(g.getChildAt(i));if(found!=null)return found;}}return null;
        }
        public void onDestroyActionMode(ActionMode mode){close();}
    }
    private static final class Tint extends View {
        final Session session;final Paint fill=new Paint(),outline=new Paint();
        Tint(Activity a,Session s){super(a);session=s;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);fill.setColor(0x5555bdf4);outline.setColor(0xff55bdf4);outline.setStrokeWidth(3*a.getResources().getDisplayMetrics().density);outline.setStyle(Paint.Style.STROKE);}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.clipRect(0,session.list.getPaddingTop(),getWidth(),getHeight()-session.list.getPaddingBottom());
            for(int i=0;i<session.list.getChildCount();i++)try{View child=session.list.getChildAt(i);MultiResponseNg.Plan p=session.byNumber.get(session.number(child));if(p!=null&&p.selected){canvas.drawRect(child.getLeft(),child.getTop(),child.getRight(),child.getBottom(),fill);canvas.drawRect(child.getLeft()+2,child.getTop()+2,child.getRight()-2,child.getBottom()-2,outline);}}catch(Exception ignored){}
            canvas.restore();
        }
    }
    static String copied(MultiResponseNg.Target target,List<MultiResponseNg.Plan> plans,boolean[] fields)throws Exception{
        StringBuilder out=new StringBuilder();for(MultiResponseNg.Plan p:plans){if(!p.selected)continue;
            ArrayList<String> parts=new ArrayList<>();String url=NativeThreadContext.url(target.board,target.version,p.number);
            ResponseArchiveEntry e=ResponseArchiveEntry.snapshot(p.response,target.board,"");String fullBody=Html.fromHtml(e.body).toString();
            if(fields[0])parts.add(", >>"+p.number);if(fields[1])parts.add(p.name);if(fields[2])parts.add(fullBody);if(fields[3])parts.add(url);
            if(fields[4]){parts.add(", "+p.number+" "+p.name+" "+Html.fromHtml(e.date)+(p.id==null||p.id.isEmpty()?"":" ID:"+p.id)+Character.toString(10)+fullBody);}
            if(out.length()>0)out.append((char)10).append((char)10);out.append(String.join(Character.toString(10),parts));
            if(out.length()>1000000)throw new IllegalArgumentException("コピー内容が大きすぎます。選択するレスを減らしてください");
        }return out.toString();
    }
    static void copy(MultiResponseNg.Target target,ArrayList<MultiResponseNg.Plan> plans){
        if(plans.isEmpty()){Toast.makeText(target.activity,"レスをタップして選択してください",0).show();return;}
        new HaiagaruDialog.Builder(target.activity).setTitle(plans.size()+"レスをコピー").setItems(COPY_FIELDS,(d,i)->{
            boolean[] fields=new boolean[5];if(i<5){fields[i]=true;copyText(target,plans,fields,false);}else{
                fields[0]=fields[1]=fields[2]=true;
                new HaiagaruDialog.Builder(target.activity).setTitle("コピーする項目を選択").setMultiChoiceItems(Arrays.copyOf(COPY_FIELDS,5),fields,(dialog,at,checked)->fields[at]=checked)
                    .setNegativeButton("キャンセル",null).setPositiveButton("内容を選択・編集",(dialog,which)->copyText(target,plans,fields,true)).show();
            }
        }).setNegativeButton("キャンセル",null).show();
    }
    private static void copyText(MultiResponseNg.Target target,ArrayList<MultiResponseNg.Plan> plans,boolean[] fields,boolean edit){
        try{target.current();String text=copied(target,plans,fields);if(text.trim().isEmpty()){Toast.makeText(target.activity,"コピーする項目を選択してください",0).show();return;}
            if(edit){EditText input=new EditText(target.activity);input.setText(text);input.setMinLines(3);input.setMaxLines(12);
                new HaiagaruDialog.Builder(target.activity).setTitle("コピー内容の選択・編集").setView(input).setNegativeButton("キャンセル",null).setPositiveButton("コピー",(d,w)->clipboard(target.activity,input.getText().toString())).show();
            }else clipboard(target.activity,text);
        }catch(Exception e){report(target.activity,e);}
    }
    private static void clipboard(Activity a,String text){((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("選択したレス",text));Toast.makeText(a,"コピーしました",0).show();}
    private ResponseMultiSelection(){}
}
