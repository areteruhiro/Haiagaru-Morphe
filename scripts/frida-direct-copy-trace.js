// Attach only to the existing isolated emulator app. No content or credentials logged.
Java.perform(function () {
 const View=Java.use('android.view.View');
 const Log=Java.use('android.util.Log'), Ex=Java.use('java.lang.Exception');
 function stack(label){console.log(label+'\n'+Log.getStackTraceString(Ex.$new()));}
 ['performLongClick','showContextMenu'].forEach(function(name){
  View[name].overloads.forEach(function(m){m.implementation=function(){stack(name+' '+this.getClass().getName());return m.apply(this,arguments);};});
 });
 const D=Java.use('app.morphe.extension.chmate.DirectBodySelection');
 const dispatch=D.dispatch;
 dispatch.implementation=function(v,e){console.log('BODY '+v.getClass().getName()+' action='+e.getActionMasked());return dispatch.call(this,v,e);};
 const Dialog=Java.use('android.app.Dialog');const show=Dialog.show;
 show.implementation=function(){stack('DIALOG '+this.getClass().getName());return show.call(this);};
 console.log('TRACE READY');
});
