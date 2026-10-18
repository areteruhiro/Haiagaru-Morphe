import pathlib, json, time, sys
import frida, frida_tools
device=frida.get_device('730eae72',timeout=10)
app=next(a for a in device.enumerate_applications() if a.identifier=='jp.co.airfront.android.a2chMate.haiagaru.xpreviewfix191')
session=device.attach(app.pid)
bridge=(pathlib.Path(frida_tools.__file__).parent/'bridges/java.js').read_text(encoding='utf-8')
source=r'''
const Java=bridge;
rpc.exports={run:function(){return new Promise((resolve,reject)=>Java.perform(()=>{
 Java.choose('o.pa',{onMatch(f){
  if(!f.getActivity()||!f.getView()||!f.getView().isShown())return;
  const fragment=Java.retain(f);
  Java.scheduleOnMainThread(()=>{try{
   const app=Java.use('android.app.ActivityThread').currentApplication();
   const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
   const old=prefs.getBoolean('popularReplyPopup',false);
   prefs.edit().putBoolean('popularReplyPopup',true).commit();
   const field=(o,n)=>{let c=o.getClass();while(c){try{let x=c.getDeclaredField(n);x.setAccessible(true);return x.get(o);}catch(e){c=c.getSuperclass();}}throw Error(n);};
   const list=Java.cast(fragment.getView(),Java.use('android.view.ViewGroup'));
   function find(v){if(Java.use('android.widget.ListView').class.isInstance(v))return Java.cast(v,Java.use('android.widget.ListView'));if(Java.use('android.view.ViewGroup').class.isInstance(v)){let g=Java.cast(v,Java.use('android.view.ViewGroup'));for(let i=0;i<g.getChildCount();i++){let r=find(g.getChildAt(i));if(r)return r;}}return null;}
   const nativeList=find(list);if(!nativeList)throw Error('ListView missing');
   function state(){return {first:nativeList.getFirstVisiblePosition(),top:nativeList.getChildAt(0).getTop(),count:nativeList.getCount(),popular:String(field(field(field(fragment,'c'),'Y'),'c'))};}
   const before=state();
   const callback=Java.cast(field(field(fragment,'c'),'B'),Java.use('o.pa$21'));
   const obs=field(field(field(fragment,'c'),'Y'),'c');
   const vf=obs.getClass().getDeclaredField('mValue');vf.setAccessible(true);vf.setBoolean(obs,true);
   const Popular=Java.use('app.morphe.extension.chmate.PopularReplies');
   const show=Popular.show.overload('java.lang.Object');
   show.implementation=function(f){send({showCalled:true});return show.call(this,f);};
   const handled=Popular.interceptLegacy(fragment);
   show.call(Popular,fragment);
   const after=state();
   send({before,after,handled,flag:vf.getBoolean(obs),previousSetting:old});resolve({before,after,handled,previousSetting:old});
  }catch(e){reject(String(e));}});return 'stop';
 },onComplete(){}});
}));}};
'''
script=session.create_script(bridge+'\n'+source)
script.on('message',lambda m,d:print(json.dumps(m,ensure_ascii=False),flush=True))
script.load()
try: print(json.dumps(script.exports_sync.run(),ensure_ascii=False),flush=True)
finally: session.detach()
