// Read-only menu-model and label inspection. Never prints response bodies or session data.
Java.perform(function () {
 const pkg=String(Java.use('android.app.ActivityThread').currentApplication().getPackageName());
 if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(uploadhosts191|archivemenu191)$/.test(pkg))throw Error('Wrong package');
 const Menu=Java.use('android.view.Menu');
 function dump(menu,label){
  menu=Java.cast(menu,Menu);
  const rows=[];
  for(let i=0;i<menu.size();i++){
   const item=menu.getItem(i);
   rows.push({id:item.getItemId(),title:String(item.getTitle()),visible:item.isVisible()});
  }
  console.log(label+' '+JSON.stringify(rows));
 }
 const Dialog=Java.use('o.r8lambdafLXKIgI8H4VR9SponZBKnK7_9gE');
 const build=Dialog.c.overload();
 build.implementation=function(){const m=build.call(this);dump(m,'BUILT');return m;};
 const Item=Java.use('o.r8lambda9uSHt4DumN_rfVPyMzyPRS1qXDU');
 const setTitle=Item.setTitle.overload('java.lang.CharSequence');
 setTitle.implementation=function(title){
  if(this.getItemId()>=0x7e000000){
   console.log('CUSTOM_TITLE '+this.getItemId()+' '+title+' '+Java.use('android.util.Log').getStackTraceString(Java.use('java.lang.Exception').$new()));
  }
  return setTitle.call(this,title);
 };
 const adapter=Java.use('o.m6f'),getView=adapter.getView.overload('int','android.view.View','android.view.ViewGroup');
 getView.implementation=function(position,view,parent){
  const item=Java.cast(this.getItem(position),Java.use('android.view.MenuItem'));
  if(item.getItemId()>=0x7e000000)console.log('BIND '+item.getItemId()+' '+item.getTitle());
  return getView.call(this,position,view,parent);
 };
 Java.choose('o.m6f',{onMatch:function(a){try{dump(a.a.value,'EXISTING');}catch(e){console.log(String(e));}},onComplete:function(){console.log('PROBE_READY');}});
 Java.choose('o.r8lambdafLXKIgI8H4VR9SponZBKnK7_9gE',{
  onMatch:function(d){const held=Java.retain(d);Java.scheduleOnMainThread(function(){
   try{console.log('DIALOG added='+held.isAdded()+' parent='+held.getParentFragment());if(held.isAdded())dump(held.c(),'REBUILT');}
   catch(e){console.log('DIALOG_ERROR '+e);}
  });},onComplete:function(){}
 });
});
