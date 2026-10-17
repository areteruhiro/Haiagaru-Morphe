Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')throw Error('Wrong verification app');
 const loader=Java.use('dalvik.system.DexClassLoader').$new('/data/local/tmp/haiagaru-video-preview-variable.dex',
  String(app.getCodeCacheDir().getPath()),null,Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
 const clazz=loader.loadClass('app.morphe.extension.chmate.PreviewGridTest');
 const result=clazz.getMethod('run',Java.array('java.lang.Class',[Java.use('android.content.Context').class]))
  .invoke(null,Java.array('java.lang.Object',[app]));
 console.log(String(result));
});
