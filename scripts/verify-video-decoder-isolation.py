"""Synthetic file only in the dedicated decoderfix191 app. No network or user media."""
import pathlib, sys, json
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2] / 'frida-local'))
import frida, frida_tools
device=frida.get_device('730eae72',timeout=10)
package='jp.co.airfront.android.a2chMate.haiagaru.decoderfix191'
app=next(a for a in device.enumerate_applications() if a.identifier==package)
if not app.pid: raise RuntimeError('Open dedicated verification app first')
session=device.attach(app.pid)
bridge=(pathlib.Path(frida_tools.__file__).parent/'bridges/java.js').read_text(encoding='utf-8')
source=r'''
const Java=bridge;
rpc.exports={run(){return new Promise((resolve,reject)=>Java.perform(()=>{
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.decoderfix191')throw Error('Wrong package');
 const Task=Java.registerClass({name:'app.haiagaru.test.DecoderCheck'+Date.now(),implements:[Java.use('java.lang.Runnable')],methods:{run(){try{
  const File=Java.use('java.io.File'),dir=File.$new(app.getCacheDir(),'haiagaru-video-frames');dir.mkdirs();
  const input=File.createTempFile('frame-fixture-','.mp4',dir),output=File.createTempFile('preview-fixture-','.jpg',dir);
  const stream=Java.use('java.io.FileOutputStream').$new(input);stream.write(Java.array('byte',[0,0,0,8,102,116,121,112]));stream.close();
  const Decoder=Java.use('app.morphe.extension.chmate.VideoFrameDecodeService');
  const began=Date.now(),ok=Decoder.decode(app,input,output,false,0,4);
  const prefs=app.getSharedPreferences('haiagaru-video-decoder-health',0);
  const blocked=prefs.getBoolean('unsafe',false);
  input.delete();output.delete();
  resolve({ok,blocked,elapsed:Date.now()-began,mainPid:Java.use('android.os.Process').myPid(),mainSurvived:true});
 }catch(e){reject(String(e));}}}});
 Java.use('java.lang.Thread').$new(Task.$new()).start();
}));}};
'''
script=session.create_script(bridge+'\n'+source)
script.on('message',lambda m,d:print(json.dumps(m,ensure_ascii=False),flush=True))
script.load()
try: print(json.dumps(script.exports_sync.run(),ensure_ascii=False),flush=True)
finally: session.detach()
