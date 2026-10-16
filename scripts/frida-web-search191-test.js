// Offline test: a ContextWrapper records requests instead of launching any app.
Java.perform(function(){Java.scheduleOnMainThread(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).endsWith('.websearch191'))throw Error('Isolated app only');
 let calls=[],mode='ok',checks=0;
 const Missing=Java.use('android.content.ActivityNotFoundException');
 const Capture=Java.registerClass({name:'app.morphe.test.WebSearchContext',superClass:Java.use('android.content.ContextWrapper'),methods:{
  $init:[{returnType:'void',argumentTypes:['android.content.Context'],implementation:function(context){this.$super.$init(context);}}],
  getApplicationContext:[{returnType:'android.content.Context',argumentTypes:[],implementation:function(){return Java.cast(this,Java.use('android.content.Context'));}}],
  startActivity:[{returnType:'void',argumentTypes:['android.content.Intent'],implementation:function(intent){
   calls.push({action:String(intent.getAction()),query:String(intent.getStringExtra('query')),url:String(intent.getDataString()),flags:intent.getFlags()});
   if(mode==='missing'||mode==='fallback'&&calls.length===1)throw Missing.$new('Synthetic missing handler');
  }}]
 }});
 const capture=Java.retain(Capture.$new(app));
 const Caller=Java.registerClass({name:'app.morphe.test.WebSearchCaller',superClass:Java.use('android.content.ContextWrapper'),methods:{
  $init:[{returnType:'void',argumentTypes:['android.content.Context'],implementation:function(context){this.$super.$init(context);}}],
  getApplicationContext:[{returnType:'android.content.Context',argumentTypes:[],implementation:function(){return Java.cast(capture,Java.use('android.content.Context'));}}],
  startActivity:[{returnType:'void',argumentTypes:['android.content.Intent'],implementation:function(){throw Error('Caller context used instead of application');}}]
 }});
 const caller=Java.retain(Caller.$new(app));
 function check(ok,msg){if(!ok)throw Error(msg);checks++;console.log('PASS '+msg);}
 function invoke(query){Java.use('o.lo').c.overload('android.content.Context','java.lang.String').call(Java.use('o.lo'),caller,query);}
 const query='Haiagaru + / 日本語 & test';invoke(query);
 check(calls.length===1,'native entry dispatches once through application context');
 check(calls[0].action==='android.intent.action.WEB_SEARCH'&&calls[0].query===query,'provider action and selected text preserved');
 check((calls[0].flags&0x10000000)!==0,'search requests separate task');
 calls=[];mode='fallback';invoke(query);
 check(calls.length===2&&calls[1].action==='android.intent.action.VIEW','missing search provider falls back to browser');
 check(calls[1].url==='https://www.google.com/search?q='+String(Java.use('android.net.Uri').encode(query)),'fallback URL encodes plus slash Unicode ampersand');
 check((calls[1].flags&0x10000000)!==0,'browser fallback also requests separate task');
 calls=[];mode='missing';invoke(query);check(calls.length===2,'no browser handled without crash');
 calls=[];mode='ok';invoke('  ');invoke(null);check(calls.length===0,'blank/null selection ignored');
 console.log('WEB SEARCH CHECKS='+checks);
});});
