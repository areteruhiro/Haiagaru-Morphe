// Temporary diagnostic: only bypass the native context menu on opt-in body views.
Java.perform(function () {
 const T=Java.use('android.widget.TextView');
 const D=Java.use('app.morphe.extension.chmate.DirectBodySelection');
 const dispatch=D.dispatch;dispatch.implementation=function(v,e){
  if(e.getActionMasked()===0 && v.getParent()!==null)v.getParent().requestDisallowInterceptTouchEvent(true);
  return dispatch.call(this,v,e);
 };
 ['showContextMenu'].forEach(function(name){T[name].overloads.forEach(function(m){
  m.implementation=function(){
   if(D.prepare(this)){console.log('BYPASS body context menu');return false;}
   return m.apply(this,arguments);
  };
 });});
 console.log('CONTEXT PROBE READY');
 const touch=T.onTouchEvent;touch.implementation=function(e){
  const result=touch.call(this,e);
  if(String(this.getClass().getName())==='o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc' && e.getActionMasked()!==2)
   console.log('TOUCH '+e.getActionMasked()+' selection='+this.getSelectionStart()+','+this.getSelectionEnd()+' movement='+this.getMovementMethod());
  return result;
 };
 const Editor=Java.use('android.widget.Editor');
 ['startSelectionActionModeInternal','startSelectionActionModeAsync','stopTextActionMode'].forEach(function(name){
  if(Editor[name])Editor[name].overloads.forEach(function(m){m.implementation=function(){const result=m.apply(this,arguments);console.log(name+' result='+result);return result;};});
 });
});
