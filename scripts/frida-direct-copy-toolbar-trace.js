Java.perform(function(){
 const H=Java.use('app.morphe.extension.chmate.Haiagaru');
 console.log('enabled='+H.directBodySelectionEnabled());
 const m=H.handleEdgeArchiveToolbarClick;m.implementation=function(owner,id){
  console.log('toolbar id='+id+' owner='+owner.getClass().getName());
  const r=m.call(this,owner,id);console.log('handled='+r+' enabled='+H.directBodySelectionEnabled());return r;
 };
 const View=Java.use('android.view.View'), click=View.performClick;
 click.implementation=function(){console.log('click '+this.getClass().getName()+' id='+this.getId());return click.call(this);};
});
