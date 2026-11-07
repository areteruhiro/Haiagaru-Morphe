package app.haiagaru.tests.sites;

import android.app.*;
import android.content.*;
import android.os.*;
import android.content.res.XmlResourceParser;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Isolated real-socket gateway tests; upstream requests never leave this test transport. */
public final class SiteBoardGatewayInstrumentation extends Instrumentation {
 private int checks, gets, posts; private ClassLoader loader; private Class<?> gatewayType, transportType, responseType;
 private Object gateway,previousGateway,previousContext;private Field singleton,applicationContext; private SharedPreferences prefs; private final List<String> targets=new ArrayList<>();private final List<String> fetched=Collections.synchronizedList(new ArrayList<>());
 private boolean denyCheck, disableAtCheck, ui, reader, https; private SharedPreferences originalConfig; private boolean hadHttps, wasHttps; private Context app;
 private volatile String expectedEditorBody,editorTransportMismatch;private volatile boolean editorBodyReceived,rejectEditorPost;
 private String probeAlias,probeThread;private boolean checkOnly;
 private volatile List<?> nativeRows=Collections.emptyList();
 private String probe()throws Exception{
  app=getTargetContext();loader=app.getClassLoader();Bundle status=new Bundle();status.putString("state","waiting for visible verification HomeActivity");sendStatus(1,status);boolean visible=false;for(int n=0;n<40&&!visible;n++){FutureTask<Boolean> ready=new FutureTask<>(()->resumed!=null&&!resumed.isFinishing()&&!resumed.isDestroyed());runOnMainSync(ready);visible=ready.get(5,TimeUnit.SECONDS);if(!visible)Thread.sleep(500);}if(!visible)throw new IOException("probe needs foreground Activity");Class<?> type=loader.loadClass("app.morphe.extension.chmate.SiteBoardGateway");Object live=method(type,"get",Context.class).invoke(null,app);Map<?,?> boards=(Map<?,?>)field(live,"boards");Object board=boards.get(probeAlias);if(board==null)throw new IllegalArgumentException("unknown probe board");String url=probeThread==null?(String)call(board,"indexUrl",new Class<?>[]{}):(String)call(board,"threadUrl",new Class<?>[]{String.class},probeThread);Object transport=field(live,"transport"),response=call(transport,"get",new Class<?>[]{String.class},url);byte[] bytes=(byte[])field(response,"bytes");File saved=new File(app.getCacheDir(),"haiagaru-site-live-probe.raw");try(FileOutputStream out=new FileOutputStream(saved)){out.write(bytes);}String description="url="+url+" status="+field(response,"status")+" type="+field(response,"type")+" bytes="+bytes.length+" file="+saved;
  try{String html=(String)call(response,"text",new Class<?>[]{});
   if(checkOnly){Class<?> protocol=loader.loadClass("app.morphe.extension.chmate.SitePostProtocol");Object plan=method(protocol,"reply",board.getClass(),String.class,String.class,String.class,String.class,String.class,String.class).invoke(null,board,probeThread,html,"","","test","");String check=(String)field(plan,"check");if(!"https://bbs.animanch.com/inc/comment.php?preCheck".equals(check))throw new IOException("only Animanch preCheck is permitted in this probe");Object checked=call(transport,"post",new Class<?>[]{plan.getClass(),String.class},plan,check);try(FileOutputStream out=new FileOutputStream(new File(app.getCacheDir(),"site-live-precheck.raw"))){out.write((byte[])field(checked,"bytes"));}return description+" preCheckStatus="+field(checked,"status")+" finalPostSent=false";}
   String registration=(String)call(live,"registrationUrl",new Class<?>[]{String.class},probeAlias);
   Field transportField=type.getDeclaredField("transport");transportField.setAccessible(true);Class<?> api=transportField.getType();StringBuffer captured=new StringBuffer();java.util.concurrent.atomic.AtomicInteger index=new java.util.concurrent.atomic.AtomicInteger();
   Object tracing=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{api},(proxy,operation,arguments)->{if(!operation.getName().equals("get"))throw new IOException("read-only live probe");if(arguments[0].toString().startsWith("https://bakusai.com/"))Thread.sleep(400);operation.setAccessible(true);Object value;try{value=operation.invoke(transport,arguments);}catch(InvocationTargetException error){throw error.getCause();}int n=index.incrementAndGet();File file=new File(app.getCacheDir(),"site-live-page-"+n+".raw");try(FileOutputStream output=new FileOutputStream(file)){output.write((byte[])field(value,"bytes"));}captured.append(" [").append(n).append(": ").append(arguments[0]).append(" status=").append(field(value,"status")).append(']');return value;});
   String wire;transportField.set(live,tracing);try{wire=request(registration+(probeThread==null?"subject.txt":"dat/"+probeThread+".dat"),"GET",null,null);}finally{transportField.set(live,transport);}description+=captured.toString();int split=wire.indexOf("\r\n\r\n");String payload=split<0?"":wire.substring(split+4);
   if(!wire.startsWith("HTTP/1.1 200"))return description+" gatewayError="+wire.substring(0,Math.min(wire.length(),500));
   if(probeThread!=null)return description+" nativeDatRows="+payload.split("\n").length;
   List<?> topics=(List<?>)method(loader.loadClass("app.morphe.extension.chmate.SiteBoardProtocol"),"topics",board.getClass(),String.class).invoke(null,board,html);return description+" topics="+topics.size()+" nativeSubjectRows="+payload.split("\n").length;
  }catch(Throwable error){return description+" parseError="+android.util.Log.getStackTraceString(error);}
 }
 private volatile Activity resumed;
 private final Set<Activity> activities=Collections.synchronizedSet(new LinkedHashSet<>());
 @Override public void callActivityOnResume(Activity activity){super.callActivityOnResume(activity);resumed=activity;activities.add(activity);}
 /** Remove only our unregistered fixture tabs; retain all real registrations and other tabs. */
 private void cleanFixtureTabs()throws Exception{
  Set<String> real=new HashSet<>();org.json.JSONArray registered=new org.json.JSONArray(app.getSharedPreferences("haiagaru.site-boards",0).getString("boards","[]"));for(int n=0;n<registered.length();n++)real.add(registered.getJSONObject(n).getString("key"));
  SharedPreferences tabs=app.getSharedPreferences("tabletHome2",0);SharedPreferences.Editor edit=tabs.edit();boolean changed=false;
  for(String name:new String[]{"lefttabs","righttabs","closed"}){String saved=tabs.getString(name,null);if(saved==null)continue;org.json.JSONArray rows=new org.json.JSONArray(saved),keep=new org.json.JSONArray();for(int n=0;n<rows.length();n++){org.json.JSONObject row=rows.getJSONObject(n);String raw=new org.json.JSONObject(row.optString("args","{}")).optString("_data","");boolean fixture=false;try{URI uri=URI.create(raw);java.util.regex.Matcher match=java.util.regex.Pattern.compile("/test/read\\.cgi/(h[bfa][a-f0-9]{32})/(?:0*(?:13601530|380363|7589284)|9253601530|9240380363|9247589284|9247288634|9247427435|9247596018|9248142054)/(?:[0-9ln-]*)?").matcher(uri.getPath());fixture="127.0.0.1".equals(uri.getHost())&&match.matches()&&!real.contains(match.group(1));}catch(Exception ignored){}if(!fixture)keep.put(row);}
   if(keep.length()!=rows.length()){edit.putString(name,keep.toString());if(!name.equals("closed")){String selection=name.equals("lefttabs")?"leftselection":"rightselection";edit.putInt(selection,Math.max(0,Math.min(tabs.getInt(selection,0),keep.length()-1)));}changed=true;}
  }if(changed&&!edit.commit())throw new IOException("cannot restore isolated fixture tabs");
 }
 private void ok(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 private Method method(Class<?> c,String name,Class<?>...types)throws Exception{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
 private Object call(Object o,String name,Class<?>[] types,Object...args)throws Exception{return method(o.getClass(),name,types).invoke(o,args);}
 private Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 private String fixture(String name)throws Exception{try(InputStream in=getContext().getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
 private Object response(int code,String body,String location)throws Exception{Constructor<?> c=responseType.getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(code,body.getBytes("UTF-8"),"text/html; charset=UTF-8",location);}
 private Object image()throws Exception{Constructor<?> c=responseType.getDeclaredConstructors()[0];c.setAccessible(true);return c.newInstance(200,Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j7t8AAAAASUVORK5CYII="),"image/png",null);}
 private Object upstream(Object proxy,Method m,Object[] args)throws Throwable{
  if(m.getName().equals("get")){gets++;String u=(String)args[0];fetched.add(u);
   if(u.startsWith("https://bakusai.com/thr_res/")&&u.contains("/tid=8142054/"))return response(200,bakusaiClosedPage(u),null);
   if(u.matches("https://bbs\\.animanch\\.com/img/[1-9][0-9]*/[1-9][0-9]*"))return image();
   // Native board discovery may refresh enabled BBSMENU roots while the reader is open.
   if(u.startsWith("https://bbs-menu.pages.dev/")){String source=u.contains("bakusai_bbsmenu/")?"https://bakusai.com/thr_tl/acode=1/ctgid=137/bid=5071/":"https://bbs.animanch.com/";return response(200,"{\"menu_list\":[{\"category_content\":[{\"board_name\":\"検証板\",\"url\":\""+source+"\",\"category_name\":\"検証\"}]}]}",null);}
   if(u.equals("https://www.2chan.net/bbsmenu.html"))return response(200,"<B>検証</B>\n<A HREF=\"https://dat.2chan.net/t/\">検証料理</A>\n",null);
   if(u.contains("bakusai.com/")&&u.contains("/tid=13539922/"))return response(200,fixture("bakusaiClosedMobile.html"),null);
   if(u.contains("bakusai.com/"))return response(200,fixture(u.contains("thr_res")?"bakusaiThread.txt":"bakusaiBoard.txt").replaceAll("(name=\"loaded_at\"\\s+value=\")[^\"]*", "$1"+gets+".0"),null);
   if(u.equals("https://dat.2chan.net/t/futaba.htm"))return response(200,"<div class=thre data-res=380645><span class=csb>カタログ省略ではない完全タイトル</span><blockquote>本文</blockquote></div>",null);
   if(u.contains("2chan.net/"))return response(200,fixture(u.contains("res/")?"futabaCookingReplies.txt":"futabaCookingCat.txt"),null);
   if(u.equals("https://bbs.animanch.com/board/7288634/"))return response(200,fixture("reportedAnimanchLive.html"),null);
   if(u.equals("https://bbs.animanch.com/board/7427435/"))return response(200,fixture("reportedAnimanchTitle.html"),null);
   if(u.equals("https://bbs.animanch.com/board/7596018/"))return response(200,fixture("reportedAnimanchMobile.html"),null);
   if(u.equals("https://bbs.animanch.com/board/7596018/1/"))return response(200,fixture("reportedAnimanchOlder.html"),null);
   if(u.contains("animanch.com/"))return response(200,fixture(u.contains("/board/")?"animanchThread.html":"animanchIndex.html"),null);
   throw new IOException("unexpected isolated upstream GET: "+u);
  }
  if(m.getName().equals("post")){posts++;Object plan=args[0];String u=(String)args[1];targets.add(u);
   Map<?,?> data=(Map<?,?>)field(plan,"fields");ok(!data.containsKey("bbs")&&!data.containsKey("MESSAGE"),"native-only fields never forwarded");
   String expected=expectedEditorBody;
   if(expected!=null&&!u.endsWith("?preCheck")){
    if(data.containsValue(expected)){checks++;editorBodyReceived=true;}
    else editorTransportMismatch="expected="+expected+" fixtureFields="+data;
   }
   if(u.endsWith("?preCheck")){if(disableAtCheck)prefs.edit().putBoolean("enabled",false).commit();return response(denyCheck?403:200,denyCheck?fixture("animanchPrecheckChallenge.html"):"",null);}
   if(rejectEditorPost)return response(403,"投稿を拒否しました",null);
   if(u.contains("animanch.com"))return response(200,"{\"thread\":7589284,\"number\":80,\"jump\":\"/board/7589284/\"}",null);
   String referer=(String)field(plan,"referer");return response(303,"",referer);
  }
  if(m.getName().equals("toString"))return "IsolatedSiteTransport";
  throw new AssertionError(m.toString());
 }
 private String request(String url,String verb,String body,String hostOverride)throws Exception{
  URI u=URI.create(url);try(Socket socket=new Socket("127.0.0.1",u.getPort())){socket.setSoTimeout(20000);String host=hostOverride==null?"127.0.0.1:"+u.getPort():hostOverride;
   byte[] payload=body==null?new byte[0]:body.getBytes("US-ASCII");String text=verb+" "+u.getRawPath()+" HTTP/1.1\r\nHost: "+host+"\r\n"+(body==null?"":"Content-Type: application/x-www-form-urlencoded; charset=UTF-8\r\nContent-Length: "+payload.length+"\r\n")+"Connection: close\r\n\r\n";
   socket.getOutputStream().write(text.getBytes("US-ASCII"));socket.getOutputStream().write(payload);socket.getOutputStream().flush();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=socket.getInputStream().read(b))!=-1)out.write(b,0,n);return out.toString("windows-31j");
  }
 }
 private String nativeUrl(String key,String thread)throws Exception{return (String)call(gateway,"nativeUrl",new Class<?>[]{String.class,String.class},key,thread);}
 private void enabled(boolean on)throws Exception{call(gateway,"enabled",new Class<?>[]{boolean.class},on);}
 private String body(String key,String id,String text)throws Exception{return "bbs="+key+"&key="+id+"&FROM=fixture&mail=&MESSAGE="+URLEncoder.encode(text,"UTF-8");}
 private Switch findSwitch(View v){if(v instanceof Switch)return (Switch)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Switch s=findSwitch(g.getChildAt(i));if(s!=null)return s;}}return null;}
 private void settings()throws Exception{
  String ns="http://schemas.android.com/apk/res/android";boolean found=false;
  // Only compiled XML resources are inspected; no native class discovery or product scanning.
  for(int type=1;type<32&&!found;type++)for(int n=0;n<100&&!found;n++){
   int id=0x7f000000|(type<<16)|n;try{if(!app.getResources().getResourceTypeName(id).equals("xml"))continue;}catch(Exception missing){continue;}
   try(XmlResourceParser xml=app.getResources().getXml(id)){
    boolean original=false,entry=false,intent=false;int event;int bbsDepth=-1;
    boolean passedBbs=false,passedMenu=false;
    while((event=xml.next())!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT){if(event==org.xmlpull.v1.XmlPullParser.END_TAG&&xml.getDepth()==bbsDepth){bbsDepth=-1;passedBbs=true;}if(event!=org.xmlpull.v1.XmlPullParser.START_TAG)continue;String key=xml.getAttributeValue(ns,"key");if("PreferenceCategory".equals(xml.getName())){if("bbs".equals(key))bbsDepth=xml.getDepth();else if(passedBbs)passedMenu=true;}if("updateBbsMenu".equals(key))original=true;if("haiagaruSiteBoards".equals(key)){entry=true;ok(passedBbs&&!passedMenu&&bbsDepth<0,"entry directly after native board category, outside its typed children");}
     if("intent".equals(xml.getName())&&"app.morphe.extension.chmate.SiteBoardSettingsActivity".equals(xml.getAttributeValue(ns,"targetClass"))){ok(app.getPackageName().equals(xml.getAttributeValue(ns,"targetPackage")),"settings intent follows package rename");intent=true;}
    }if(entry){ok(original&&intent,"entry lives in native BBS preferences XML");found=true;}
   }catch(android.content.res.Resources.NotFoundException ignored){}
  }ok(found,"standard board settings entry exists");
  Field singleton=gatewayType.getDeclaredField("instance");singleton.setAccessible(true);Object saved=singleton.get(null);singleton.set(null,gateway);Activity screen=null;
  try{screen=startActivitySync(new Intent().setClassName(app.getPackageName(),"app.morphe.extension.chmate.SiteBoardSettingsActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final Activity activity=screen;
   FutureTask<Void> ui=new FutureTask<>(()->{ok(findSwitch(activity.getWindow().getDecorView())==null,"no duplicate master switch");String key=(String)((Map<?,?>)field(gateway,"boards")).keySet().iterator().next();AlertDialog dialog=(AlertDialog)call(activity,"boardSettings",new Class<?>[]{String.class,String.class},key,null);try{Switch s=findSwitch(dialog.getWindow().getDecorView());ok(s!=null&&!s.isChecked()&&s.getText().toString().equals("使用する"),"individual board switch starts OFF");s.performClick();ok((Boolean)call(gateway,"boardEnabled",new Class<?>[]{String.class},key),"switch enables only chosen board");ok(s.isChecked(),"ON shown");s.performClick();ok(!(Boolean)call(gateway,"enabled",new Class<?>[]{}),"last board OFF stops gateway");}finally{dialog.dismiss();}return null;});runOnMainSync(ui);ui.get(15,TimeUnit.SECONDS);
  }finally{if(screen!=null){final Activity a=screen;runOnMainSync(a::finish);}singleton.set(null,saved);}
 }
 private void nativeReader(String address,int count)throws Exception{
  nativeReader(address,count,true);
 }
 private void nativeReader(String address,int count,boolean posting)throws Exception{
  app.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(address)).setClassName(app.getPackageName(),address.startsWith("https://bbs.animanch.com/")?"app.morphe.extension.chmate.OpenUrlActivity":"jp.syoboi.a2chMate.activity.ResListActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  Class<?> filters=loader.loadClass("app.morphe.extension.chmate.ExtraQuickFilters");Class<?> h=loader.loadClass("app.morphe.extension.chmate.Haiagaru");Class<?> popular=loader.loadClass("app.morphe.extension.chmate.PopularReplies");String last="no activity";
  for(int attempt=0;attempt<40;attempt++){
   FutureTask<Integer> ui=new FutureTask<>(()->{Activity activity=resumed;if(activity==null)activity=(Activity)method(h,"toolbarActivity",Object.class).invoke(null,new Object[]{null});if(activity==null)return -1;Object fragment=method(filters,"visibleFragment",Activity.class).invoke(null,activity);if(fragment==null)return -2;Object adapter=method(filters,"adapter",Object.class).invoke(null,fragment);String name=adapter.getClass().getName();Object rows=field(adapter,name.equals("o.m9ExternalSyntheticLambda1")?"K":name.equals("o.listener")?"J":"o");List<?> all=(List<?>)method(popular,"fullList",Object.class).invoke(null,rows);nativeRows=new ArrayList<>(all);return all.size();});
   try{runOnMainSync(ui);int actual=ui.get(10,TimeUnit.SECONDS);last="response count="+actual;if(actual==count){ok(true,"native reader displayed "+count+" responses");if(posting){composer(address,true);composer(address,false);}return;}}catch(Exception e){last=e.toString();}Thread.sleep(1000);
  }throw new AssertionError("native reader did not display "+count+" responses: "+last+"; fixture upstream GET="+gets);
 }
 private void composer(String address,boolean rejected)throws Exception{
  rejectEditorPost=rejected;
  ActivityMonitor monitor=addMonitor("jp.syoboi.a2chMate.activity.ResEditActivity$Dialog",null,false);Activity editor=null;
  try{
   app.startActivity(new Intent(Intent.ACTION_SENDTO,android.net.Uri.parse(address)).setClassName(app.getPackageName(),"jp.syoboi.a2chMate.activity.ResEditActivity$Dialog").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   editor=waitForMonitorWithTimeout(monitor,15000);ok(editor!=null,"standard native reply editor opens");final Activity target=editor;boolean found=false;
   for(int n=0;n<15&&!found;n++){FutureTask<Boolean> ready=new FutureTask<>(()->target.getWindow().getDecorView().findViewWithTag("haiagaru.site.image")!=null);runOnMainSync(ready);found=ready.get(5,TimeUnit.SECONDS);if(!found)Thread.sleep(500);}
   ok(found,"standard editor binds exact added-board target and image attachment action");
   if(address.contains("/ha")){FutureTask<Boolean> auth=new FutureTask<>(()->{View button=target.getWindow().getDecorView().findViewWithTag("haiagaru.site.auth");return button instanceof TextView&&((TextView)button).getText().toString().equals("あにまんの認証");});runOnMainSync(auth);ok(auth.get(5,TimeUnit.SECONDS),"standard Animanch composer exposes normal authentication entry");}
   expectedEditorBody="標準画面の模擬投稿 "+checks;editorBodyReceived=false;editorTransportMismatch=null;
   FutureTask<Void> enter=new FutureTask<>(()->{EditText body=findBody(target.getWindow().getDecorView());ok(body!=null,"standard editor multiline body exists");body.setText(expectedEditorBody);return null;});runOnMainSync(enter);enter.get(5,TimeUnit.SECONDS);waitForIdleSync();
   FutureTask<Void> send=new FutureTask<>(()->{View button=findSend(target.getWindow().getDecorView());ok(button!=null,"standard editor native send button exists");ok(button.performClick(),"native send button accepts click");return null;});runOnMainSync(send);send.get(5,TimeUnit.SECONDS);
   for(int n=0;n<40&&!editorBodyReceived;n++)Thread.sleep(500);ok(editorBodyReceived,"native composer sent through isolated site transport; public posts=0; mismatch="+editorTransportMismatch+" UI="+(!editorBodyReceived?screenText(getUiAutomation().getRootInActiveWindow()):""));
   ok(editorTransportMismatch==null,"every observed native fixture POST retained its expected body: "+editorTransportMismatch);
   if(rejected){Thread.sleep(1500);FutureTask<Boolean> retained=new FutureTask<>(()->!target.isFinishing()&&!target.isDestroyed()&&expectedEditorBody.contentEquals(findBody(target.getWindow().getDecorView()).getText()));runOnMainSync(retained);ok(retained.get(5,TimeUnit.SECONDS),"rejected post retains dialog and draft");return;}
   boolean closed=false;for(int n=0;n<40&&!closed;n++){FutureTask<Boolean> done=new FutureTask<>(()->target.isFinishing()||target.isDestroyed());runOnMainSync(done);closed=done.get(5,TimeUnit.SECONDS);if(!closed)Thread.sleep(250);}ok(closed,"confirmed success closes native composer without test cleanup");
  }finally{expectedEditorBody=null;rejectEditorPost=false;removeMonitor(monitor);if(editor!=null){final Activity target=editor;runOnMainSync(target::finish);}}
 }
 private String screenText(android.view.accessibility.AccessibilityNodeInfo node){if(node==null)return "null";StringBuilder text=new StringBuilder();if(node.getText()!=null)text.append(node.getText()).append('|');for(int n=0;n<node.getChildCount();n++)text.append(screenText(node.getChild(n)));return text.toString();}
 private EditText findBody(View view){
  // Name/mail inputs are also multiline on some builds; the body is the largest editor.
  EditText best=view instanceof EditText?(EditText)view:null;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){EditText found=findBody(group.getChildAt(n));if(found!=null&&(best==null||found.getHeight()>best.getHeight()))best=found;}}return best;
 }
 private View findSend(View view){
  if(view instanceof TextView&&view.isShown()&&view.isEnabled()&&view.isClickable()&&((TextView)view).getText().toString().equals("書き込む"))return view;
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++){View found=findSend(group.getChildAt(n));if(found!=null)return found;}}return null;
 }
 private void exercise()throws Exception{
  app=getTargetContext();loader=app.getClassLoader();gatewayType=loader.loadClass("app.morphe.extension.chmate.SiteBoardGateway");transportType=loader.loadClass(gatewayType.getName()+"$Transport");responseType=loader.loadClass(gatewayType.getName()+"$Response");
  reportedPages();
  if(reader)cleanFixtureTabs();
  if(reader){Bundle status=new Bundle();status.putString("state","waiting for visible verification HomeActivity");sendStatus(1,status);boolean visible=false;for(int n=0;n<40&&!visible;n++){FutureTask<Boolean> task=new FutureTask<>(()->resumed!=null&&!resumed.isFinishing()&&!resumed.isDestroyed());runOnMainSync(task);visible=task.get(5,TimeUnit.SECONDS);if(!visible)Thread.sleep(1000);}ok(visible,"foreground Home before native reader test");}
  if(https){originalConfig=app.getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0);hadHttps=originalConfig.contains("forceHttps");wasHttps=originalConfig.getBoolean("forceHttps",false);originalConfig.edit().putBoolean("forceHttps",true).commit();method(loader.loadClass("app.morphe.extension.chmate.HttpsTransport"),"setEnabled",boolean.class).invoke(null,true);}
  prefs=app.getSharedPreferences("haiagaru.site-tests."+System.nanoTime(),0);Object transport=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{transportType},this::upstream);
  Constructor<?> ctor=gatewayType.getDeclaredConstructor(SharedPreferences.class,transportType);ctor.setAccessible(true);gateway=ctor.newInstance(prefs,transport);
  singleton=gatewayType.getDeclaredField("instance");singleton.setAccessible(true);previousGateway=singleton.get(null);singleton.set(null,gateway);
  applicationContext=loader.loadClass("app.morphe.extension.chmate.Haiagaru").getDeclaredField("applicationContext");applicationContext.setAccessible(true);previousContext=applicationContext.get(null);applicationContext.set(null,app.getApplicationContext());
  ok(!(Boolean)call(gateway,"enabled",new Class<?>[]{}),"default OFF");
  String[] addresses={"https://bakusai.com/thr_res/acode=1/ctgid=137/bid=5071/tid=13601530/","https://dat.2chan.net/t/res/380363.htm","https://bbs.animanch.com/board/7589284/"};String[] ids={"13601530","380363","7589284"};int[] counts={17,4,79};String[] keys=new String[3];
  for(int i=0;i<3;i++)keys[i]=(String)call(gateway,"register",new Class<?>[]{String.class,String.class},addresses[i],"試験板"+i);
  try{nativeUrl(keys[0],null);throw new AssertionError("OFF must reject open");}catch(InvocationTargetException expected){ok(expected.getCause() instanceof IOException,"OFF open rejected");}ok(gets==0&&posts==0,"no upstream while OFF");
  ok(call(gateway,"localUrl",new Class<?>[]{String.class},addresses[0])==null,"source links do not enable an OFF board");
  if(ui)settings();for(String key:keys)call(gateway,"boardEnabled",new Class<?>[]{String.class,boolean.class},key,true);Class<?> tabs=loader.loadClass("app.morphe.extension.chmate.OpenTabsRefresh");Method parse=method(tabs,"parseInfo",Context.class,String.class);
  if(https)for(String kind:new String[]{"bakusai","futaba","animanch"}){String menu=(String)call(gateway,"siteMenuUrl",new Class<?>[]{String.class},kind);Class<?> policy=loader.loadClass("app.morphe.extension.chmate.HttpsTransport");ok((Boolean)method(policy,"shouldUpgrade",String.class).invoke(null,"http"),"HTTPS upgrade is ON");HttpURLConnection connection=(HttpURLConnection)method(policy,"openConnection",URL.class).invoke(null,new URL(menu));connection.setConnectTimeout(3000);connection.setReadTimeout(15000);try{ok(!(connection instanceof javax.net.ssl.HttpsURLConnection),"active local menu remains HTTP: "+kind);ok(connection.getResponseCode()==200,"menu loads with HTTPS ON: "+kind);try(InputStream input=connection.getInputStream()){ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);ok(bytes.toString("windows-31j").contains("127.0.0.1:"),"native BBSMENU contains local boards: "+kind);}}finally{connection.disconnect();}}
  String cachedImage=null;for(int i=0;i<3;i++){
   String board=nativeUrl(keys[i],null),thread=nativeUrl(keys[i],ids[i]);Object info=parse.invoke(null,app,thread);ok(info!=null,"native thread URL parser accepts loopback board "+i);ok(parse.invoke(null,app,board)!=null,"native board URL parser accepts loopback board "+i);
   ok(thread.equals(call(gateway,"localUrl",new Class<?>[]{String.class},addresses[i])),"source thread resolves to its existing registered native identity");
   ok(parse.invoke(null,app,addresses[i])!=null,"native entry point parses original site URL");
   ok(addresses[i].equals(call(gateway,"originalUrl",new Class<?>[]{String.class},thread)),"copied thread URL resolves to the real site");
   Class<?> siteHooks=loader.loadClass("app.morphe.extension.chmate.SiteNativeBoards");
   ok(("題名\n"+addresses[i]).equals(method(siteHooks,"copiedText",String.class).invoke(null,"題名\n"+thread)),"title-plus-URL clipboard text exports original thread URL");
   clipboardRoundtrip(siteHooks,thread,addresses[i]);
   ok((board+"../").equals(call(gateway,"originalUrl",new Class<?>[]{String.class},board+"../")),"unrelated malformed paths remain unchanged");
   if(i==0)ok(thread.equals(call(gateway,"localUrl",new Class<?>[]{String.class},addresses[i].replace("acode=1/","acode=20/"))),"redirected Bakusai regional URLs keep the same native board");
   if(i==2){ok((thread+"13").equals(call(gateway,"localUrl",new Class<?>[]{String.class},addresses[i]+"#res13")),"Animanch response fragment opens requested native position");ok((addresses[i]+"#res13").equals(call(gateway,"originalUrl",new Class<?>[]{String.class},thread+"13")),"Animanch response copy preserves source anchor");}
   String version=app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName;ok(Long.valueOf(9240000000L+Long.parseLong(ids[i])).equals(field(info,version.contains("226")?"c":"e")),"short source thread number remains a thread");Object bid=method(loader.loadClass("app.morphe.extension.chmate.RefreshTargets"),"nativeBoard",Object.class,String.class).invoke(null,info,version);ok(URLDecoder.decode(bid.toString(),"UTF-8").contains(":"+URI.create(board).getPort()),"native board identity retains gateway port: "+bid);
   if(!version.contains("191")){Class<?> infoType=info.getClass();Field companionField=infoType.getDeclaredField(version.contains("226")?"a":"ComponentActivity");companionField.setAccessible(true);Object companion=companionField.get(null);Object contextual=method(companion.getClass(),version.contains("226")?"a":"d",Context.class,String.class).invoke(companion,app,thread);ok(Long.valueOf(9240000000L+Long.parseLong(ids[i])).equals(field(contextual,version.contains("226")?"c":"e")),"tablet contextual parser retains short source ID");Object contextualBoard=method(loader.loadClass("app.morphe.extension.chmate.RefreshTargets"),"nativeBoard",Object.class,String.class).invoke(null,contextual,version);ok(URLDecoder.decode(contextualBoard.toString(),"UTF-8").contains(":"+URI.create(board).getPort()),"tablet contextual parser retains loopback port");}
   for(Method producer:info.getClass().getDeclaredMethods())if(producer.getReturnType()==String.class&&producer.getParameterTypes().length==0&&!Modifier.isStatic(producer.getModifiers())&&!producer.getName().startsWith("haiagaru")){producer.setAccessible(true);String value=(String)producer.invoke(info);if(value!=null&&(value.startsWith("http://127.0.0.1:"+URI.create(board).getPort()+"/")||value.startsWith("https://127.0.0.1:"+URI.create(board).getPort()+"/")))ok(value.startsWith("http://"),"native URL producer preserves registered plain loopback transport: "+producer.getName());}
   String settings=request(board+"SETTING.TXT","GET",null,null);ok(settings.contains("BBS_TITLE=試験板"+i),"native board settings "+i);
   String subject=request(board+"subject.txt","GET",null,null);ok(subject.startsWith("HTTP/1.1 200")&&subject.contains(".dat<>"),"subject protocol "+i);
   if(i==1)ok(subject.contains("カタログ省略ではない完全タイトル"),"native subject receives the full OP title rather than a catalogue label");
   Class<?> nonTime=loader.loadClass(version.contains("242")?"o.TopLayoutDislike2":"jp.syoboi.a2chMate.data.BBSThread");Method noDate=method(nonTime,version.contains("191")?"c":version.contains("226")?"d":"b",long.class);ok((Boolean)noDate.invoke(null,9240000000L+Long.parseLong(ids[i])),"native renderer treats additional-board ID as non-timestamp");ok(!(Boolean)noDate.invoke(null,Long.parseLong(ids[i])),"old source ID would incorrectly be formatted as a timestamp");
   String mapped=request(board+"dat/"+(9240000000L+Long.parseLong(ids[i]))+".dat","GET",null,null);ok(mapped.startsWith("HTTP/1.1 200"),"non-timestamp native DAT key fetches original source thread");
   String dat=request(board+"dat/"+ids[i]+".dat","GET",null,null);ok(dat.startsWith("HTTP/1.1 200"),"DAT fetch "+i);String[] rows=dat.substring(dat.indexOf("\r\n\r\n")+4).split("\n");ok(rows.length==counts[i],"all response records "+i);for(String row:rows)ok(row.split("<>",-1).length==5,"DAT field boundaries");
   String padded=String.format(Locale.ROOT,"%010d",Long.parseLong(ids[i]));String paddedDat=request(board+"dat/"+padded+".dat","GET",null,null);ok(paddedDat.startsWith("HTTP/1.1 200")&&paddedDat.substring(paddedDat.indexOf("\r\n\r\n")+4).equals(dat.substring(dat.indexOf("\r\n\r\n")+4)),"native zero-padded DAT maps to original source ID");Object paddedInfo=parse.invoke(null,app,thread.replace("/"+(9240000000L+Long.parseLong(ids[i]))+"/","/"+padded+"/"));ok(Long.valueOf(9240000000L+Long.parseLong(ids[i])).equals(field(paddedInfo,version.contains("226")?"c":"e")),"padded native thread URL retains numeric ID");
   if(i==2){java.util.regex.Matcher media=java.util.regex.Pattern.compile("http://127\\.0\\.0\\.1:[0-9]+/m/ha[a-f0-9]{32}/img/[0-9]+/[0-9]+\\.jpg").matcher(dat);ok(media.find(),"cached DAT contains durable image coordinates");cachedImage=media.group();String image=request(cachedImage,"GET",null,null);ok(image.startsWith("HTTP/1.1 200")&&image.contains("Content-Type: image/png"),"native image URL serves original image bytes");}
   if(ui||reader)nativeReader(thread,counts[i]);
   int previous=posts;String result=request(board+"test/bbs.cgi","POST",body(keys[i],padded,"isolated reply "+i),null);ok(result.contains("2ch_X:true"),"native posting success response "+i);ok(posts==previous+(i==2?2:1),"site post sequence "+i);
   int sent=posts;String duplicate=request(board+"test/bbs.cgi","POST",body(keys[i],ids[i],"isolated reply "+i),null);ok(duplicate.contains("2ch_X:error")&&posts==sent,"duplicate send suppressed "+i);
  }
  if(reader){nativeReader("https://bbs.animanch.com/board/7288634/",308,false);nativeReader("https://bbs.animanch.com/board/7427435/",82,false);verifyRenderedBodies(nativeUrl(keys[2],"7427435"));nativeReader("https://bbs.animanch.com/board/7596018/",275,false);
   String closedKey=(String)call(gateway,"register",new Class<?>[]{String.class,String.class},"https://bakusai.com/thr_tl/acode=7/ctgid=148/bid=1548/","件数検証板");call(gateway,"boardEnabled",new Class<?>[]{String.class,boolean.class},closedKey,true);nativeReader(nativeUrl(closedKey,"8142054"),1001,false);
  }
  int beforeClosed=posts;String closed=request(nativeUrl(keys[0],null)+"test/bbs.cgi","POST",body(keys[0],"13539922",">>1 closed thread test"),null);ok(closed.contains("30日")&&closed.contains("投稿受付が終了"),"native posting error explains real upstream expiry");ok(posts==beforeClosed,"expired thread never submits or fetches old quote pages");
  String board=nativeUrl(keys[2],null);ok(request(board+"dat/987654321.dat","GET",null,"foreign.invalid").startsWith("HTTP/1.1 403"),"host restricted");ok(!fetched.contains("https://bbs.animanch.com/board/987654321/"),"bad host never fetches its target (independent of native background refresh)");
  ok(request(board+"unknown","GET",null,null).startsWith("HTTP/1.1 404"),"unknown path rejected");
  denyCheck=true;int previous=posts;String denied=request(board+"test/bbs.cgi","POST",body(keys[2],ids[2],"denied check"),null);ok(denied.contains("2ch_X:error")&&posts==previous+1,"precheck rejection prevents send");ok(denied.contains("HTTP 403")&&denied.contains("Cloudflare")&&denied.contains("あにまんの認証"),"real challenge explains normal authentication without final submission");denyCheck=false;
  disableAtCheck=true;previous=posts;String disabled=request(board+"test/bbs.cgi","POST",body(keys[2],ids[2],"disabled check"),null);ok(disabled.contains("2ch_X:error")&&posts==previous+1,"OFF after precheck prevents send");disableAtCheck=false;
  enabled(true);int port=prefs.getInt("port",0);enabled(false);if(https)ok(!(Boolean)method(loader.loadClass("app.morphe.extension.chmate.HttpsTransport"),"isHttpUpgradeExempt",String.class,int.class).invoke(null,"127.0.0.1",port),"stopped gateway loses HTTP exception");enabled(true);ok(prefs.getInt("port",0)==port,"stable history port across OFF/ON");if(https)ok((Boolean)method(loader.loadClass("app.morphe.extension.chmate.HttpsTransport"),"isHttpUpgradeExempt",String.class,int.class).invoke(null,"127.0.0.1",port),"restarted gateway restores owned HTTP exception");enabled(false);
  Object restored=ctor.newInstance(prefs,transport);ok(((Map<?,?>)field(restored,"boards")).size()==(reader?4:3),"registry persisted");ok(!(Boolean)call(restored,"enabled",new Class<?>[]{}),"OFF persisted");
  if(cachedImage!=null){SharedPreferences imagePrefs=app.getSharedPreferences("haiagaru.site-image-restart."+System.nanoTime(),0);imagePrefs.edit().putString("boards",prefs.getString("boards","[]")).putInt("port",port).putBoolean("enabled",true).commit();Object fresh=ctor.newInstance(imagePrefs,transport);try{call(fresh,"start",new Class<?>[]{});String image=request(cachedImage,"GET",null,null);ok(image.startsWith("HTTP/1.1 200")&&image.contains("Content-Type: image/png"),"cached native image URL survives gateway reconstruction");call(fresh,"boardEnabled",new Class<?>[]{String.class,boolean.class},keys[2],false);ok(request(cachedImage,"GET",null,null).startsWith("HTTP/1.1 403"),"disabled board cannot serve cached images");}finally{call(fresh,"stop",new Class<?>[]{});imagePrefs.edit().clear().commit();}}
 }
 private void reportedPages()throws Exception{
  Class<?> protocol=loader.loadClass("app.morphe.extension.chmate.SiteBoardProtocol"),boardType=loader.loadClass(protocol.getName()+"$Board"),pageType=loader.loadClass(protocol.getName()+"$ThreadPage"),data=loader.loadClass("app.morphe.extension.chmate.SiteNativeData");
  String glyph="まどか⭐\ufe0eマギカ 😀\ufe0f";byte[] bytes=(byte[])method(data,"wire",String.class).invoke(null,glyph);String encoded=new String(bytes,"MS932");
  for(String address:new String[]{"https://bbs.animanch.com/board/7288634/","https://bakusai.com/thr_res/acode=10/ctgid=148/bid=1560/tid=2145626/","https://zip.2chan.net/6/res/4418967.htm"}){
   Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(address)).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(app.getPackageName());boolean handled=false;
   for(android.content.pm.ResolveInfo target:app.getPackageManager().queryIntentActivities(intent,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY))handled|=target.activityInfo.name.equals("app.morphe.extension.chmate.OpenUrlActivity");ok(handled,"manifest advertises original site thread links");
  }
  ok(!encoded.contains("�"),"Android charset conversion never inserts replacement bytes");ok(encoded.contains("&#xfe0e;")&&encoded.contains("&#xfe0f;"),"Android canEncode false-positive variation selectors use NCRs");ok(android.text.Html.fromHtml(encoded).toString().equals(glyph),"star/variation-selector/emoji survive Android wire roundtrip");
  for(String[] item:new String[][]{{"reportedAnimanchLive.html","7288634","308"},{"reportedAnimanchTitle.html","7427435","82"}}){
   String html=fixture(item[0]);Object board=method(protocol,"address",String.class).invoke(null,"https://bbs.animanch.com/board/"+item[1]+"/");Object page=method(protocol,"thread",boardType,String.class,String.class).invoke(null,board,item[1],html);
   ok(((List<?>)field(page,"entries")).size()==Integer.parseInt(item[2]),"reported Animanch source parses complete saved response list");
   java.util.function.Function<String,String> identity=value->value;byte[] dat=(byte[])method(data,"dat",boardType,pageType,java.util.function.Function.class,java.util.function.Function.class).invoke(null,board,page,identity,identity);
   String title=new String(dat,"MS932").split("\n")[0].split("<>",-1)[4];ok(android.text.Html.fromHtml(title).toString().equals(field(page,"title")),"reported source title preserves all code points on Android");
  }
 }
 private String bakusaiClosedPage(String address)throws Exception{
  String initial=fixture("reportedBakusaiPageTwo.html");java.util.regex.Matcher match=java.util.regex.Pattern.compile("/p=([1-9][0-9]*)/tp=1/$").matcher(address);if(!match.find())return initial;
  int page=Integer.parseInt(match.group(1));String title=android.text.TextUtils.htmlEncode(fixture("reportedBakusaiTitle.txt"));
  StringBuilder html=new StringBuilder("<title>件数試験｜1000レス｜爆サイ</title><div id=res0_whole><div itemprop=headline>"+title+"</div><span class=thr_rotundate>NO.8142054</span></div><ul id=res_list>");
  for(int n=Math.max(1,1000-page*7+1);n<=1000-(page-1)*7;n++)html.append("<li class=res_block id=res").append(n).append("_block><div class=res_body>検証レス").append(n).append("</div></li>");
  html.append("</ul><form action='/thr_res/acode=18/ctgid=148/bid=1548/tid=8142054/tp=1/'><select name=p>");for(int n=1;n<=143;n++)html.append("<option value=").append(n).append(n==page?" selected":"").append(">ページ</option>");return html+"</select></form>";
 }
 private void clipboardRoundtrip(Class<?> hooks,String thread,String original)throws Exception{
  String noPort=thread.replaceFirst(":(\\d+)(?=/)","");ok(original.equals(call(gateway,"originalUrl",new Class<?>[]{String.class},noPort)),"portless copied URLs recover their original source");ok(thread.equals(call(gateway,"localUrl",new Class<?>[]{String.class},noPort)),"portless registered URL reopens with the current port and same board identity");ok(method(hooks,"parse",String.class).invoke(null,noPort)!=null,"native parser accepts registered portless URL");
  Method export=method(hooks,"exportedClip",ClipData.class);ClipData plain=(ClipData)export.invoke(null,ClipData.newPlainText("URL",noPort));ok(original.contentEquals(plain.getItemAt(0).getText()),"plain clipboard data contains original URL");
  ClipData html=ClipData.newHtmlText("URL",noPort,"<a href=\""+noPort+"\">"+noPort+"</a>");ClipData rendered=(ClipData)export.invoke(null,html);ok(original.contentEquals(rendered.getItemAt(0).getText())&&rendered.getItemAt(0).getHtmlText().contains("href=\""+original+"\""),"HTML clipboard text and href both use source URL");
  ClipData uri=new ClipData("URL",new String[]{"text/uri-list"},new ClipData.Item(android.net.Uri.parse(noPort)));ClipData converted=(ClipData)export.invoke(null,uri);ok(original.equals(converted.getItemAt(0).getUri().toString()),"URI clipboard representation uses source URL");
  ClipData multiple=ClipData.newPlainText("URL",noPort);multiple.addItem(new ClipData.Item("keep this item"));ClipData merged=(ClipData)export.invoke(null,multiple);ok(merged.getItemCount()==2&&original.contentEquals(merged.getItemAt(0).getText())&&"keep this item".contentEquals(merged.getItemAt(1).getText()),"multi-item copy preserves unrelated items");
  String foreign=thread.replaceFirst(":\\d+(?=/)",":1");ClipData untouched=ClipData.newPlainText("other service",foreign);ok(export.invoke(null,untouched)==untouched,"other explicit loopback ports remain unchanged");
  if(app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName.contains("191")){
   FutureTask<Void> task=new FutureTask<>(()->{ClipboardManager manager=(ClipboardManager)resumed.getSystemService(Context.CLIPBOARD_SERVICE);ClipData previous=manager.getPrimaryClip();SharedPreferences history=app.getSharedPreferences("haiagaru-clipboard",0);boolean had=history.contains("enabled"),enabled=history.getBoolean("enabled",false);history.edit().putBoolean("enabled",false).commit();try{method(loader.loadClass("o.o8ExternalSyntheticLambda0"),"a",Context.class,String.class,boolean.class).invoke(null,resumed,noPort,false);ClipData actual=manager.getPrimaryClip();ok(actual!=null&&original.contentEquals(actual.getItemAt(0).getText()),"actual native ChMate copy helper places original URL on clipboard; null="+(actual==null)+" focus="+resumed.hasWindowFocus()+" nativeInputRetained="+(actual!=null&&noPort.contentEquals(actual.getItemAt(0).getText())));}finally{if(previous!=null)manager.setPrimaryClip(previous);else if(android.os.Build.VERSION.SDK_INT>=28)manager.clearPrimaryClip();SharedPreferences.Editor restore=history.edit();if(had)restore.putBoolean("enabled",enabled);else restore.remove("enabled");restore.commit();}return null;});runOnMainSync(task);task.get(10,TimeUnit.SECONDS);
  }
 }
 private String bodySpacing(String body){return body.replaceAll("[\\t \\u00a0]+"," ").replaceAll("(?m)^ +| +$","").trim();}
 private String renderedNativeText(Object row)throws Exception{String name=row.getClass().getSimpleName();String body=name.equals("processAdDisplayErrorPostbackForUserError")?"c":name.equals("BouncyCastleSocketAdapterCompanion")?"d":name.equals("setDislikeWidth")?"a":name.equals("zzabv")?"j":"h";Object value=field(row,body);return value==null?"":value.toString();}
 private String difference(String expected,String actual){int n=0;while(n<Math.min(expected.length(),actual.length())&&expected.charAt(n)==actual.charAt(n))n++;return "expectedLength="+expected.length()+" actualLength="+actual.length()+" firstDifference="+n+" expected="+expected.substring(Math.max(0,n-20),Math.min(expected.length(),n+120)).replace("\n","<LF>")+" actual="+actual.substring(Math.max(0,n-20),Math.min(actual.length(),n+120)).replace("\n","<LF>");}
 private void verifyRenderedBodies(String address)throws Exception{
  String registration=address.substring(0,address.indexOf("/test/read.cgi/"));java.util.regex.Matcher route=java.util.regex.Pattern.compile("/test/read\\.cgi/([^/]+)/([0-9]+)/").matcher(address);ok(route.find(),"content verification target is a native thread");String dat=request(registration+"/"+route.group(1)+"/dat/"+route.group(2)+".dat","GET",null,null);ok(dat.startsWith("HTTP/1.1 200"),"source DAT is available for reader comparison");String[] expected=dat.substring(dat.indexOf("\r\n\r\n")+4).split("\n");
  Class<?> controller=loader.loadClass("app.morphe.extension.chmate.ProgrammableNgController");List<?> actual=(List<?>)method(controller,"sharedReportInputs",Object.class,String.class).invoke(null,new ArrayList<>(nativeRows),registration+"/"+route.group(1)+"/");ok(actual.size()==expected.length&&actual.size()==82,"all 82 native responses can be decoded for content verification");
  for(int n=0;n<expected.length;n++){String raw=expected[n].split("<>",-1)[3],decoded=(String)method(controller,"plain",String.class).invoke(null,raw),body=renderedNativeText(nativeRows.get(n));ok(bodySpacing(decoded).equals(bodySpacing(body)),"native response preserves body/line breaks/quotes/image URLs at row "+(n+1)+" "+difference(decoded.trim(),body.trim()));ok(((Map<?,?>)field(actual.get(n),"values")).get("responseNumber").toString().equals(""+(n+1)),"native response number matches source row "+(n+1));ok(body.indexOf('\ufffd')<0,"no replacement character introduced into native response "+(n+1));}
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);ui=args!=null&&args.getBoolean("ui",false);reader=args!=null&&"true".equals(args.getString("reader"));https=args!=null&&"true".equals(args.getString("https"));probeAlias=args==null?null:args.getString("probeAlias");probeThread=args==null?null:args.getString("probeThread");checkOnly=args!=null&&"true".equals(args.getString("checkOnly"));start();}
 @Override public void onStart(){if(probeAlias!=null){Bundle result=new Bundle();try{result.putString("probe",probe());finish(Activity.RESULT_OK,result);}catch(Throwable error){StringWriter trace=new StringWriter();error.printStackTrace(new PrintWriter(trace));result.putString("probeError",trace.toString());finish(Activity.RESULT_CANCELED,result);}return;}Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;try{exercise();code=Activity.RESULT_OK;result.putString("result","PASS "+checks+" gateway/native URL checks; upstream GET="+gets+", isolated POST="+posts+"; public posts=0");}catch(Throwable e){result.putString("error",android.util.Log.getStackTraceString(e));}finally{if(reader)try{runOnMainSync(()->{for(Activity a:new ArrayList<>(activities))if(!a.isFinishing()&&!a.isDestroyed())a.finish();});waitForIdleSync();cleanFixtureTabs();}catch(Throwable error){code=Activity.RESULT_CANCELED;result.putString("cleanupError",android.util.Log.getStackTraceString(error));}if(singleton!=null)try{singleton.set(null,previousGateway);}catch(Exception ignored){}if(applicationContext!=null)try{applicationContext.set(null,previousContext);}catch(Exception ignored){}if(gateway!=null)try{call(gateway,"stop",new Class<?>[]{});}catch(Exception ignored){}if(prefs!=null)prefs.edit().clear().commit();if(originalConfig!=null)try{SharedPreferences.Editor restore=originalConfig.edit();if(hadHttps)restore.putBoolean("forceHttps",wasHttps);else restore.remove("forceHttps");restore.commit();method(loader.loadClass("app.morphe.extension.chmate.HttpsTransport"),"setEnabled",boolean.class).invoke(null,wasHttps);}catch(Exception failure){result.putString("configRestoreError",failure.toString());code=Activity.RESULT_CANCELED;}}finish(code,result);}
}
