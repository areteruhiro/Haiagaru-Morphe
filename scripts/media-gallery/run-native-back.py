import pathlib,os,time,json
# Share the existing local HTTP media fixture, never a live thread or posting endpoint.
source=pathlib.Path('scripts/media-gallery/run.py').read_text(encoding='utf-8-sig').split('\ntry:\n rpc.setup',1)[0]
source=source.replace("adb='D:/Android/Sdk/platform-tools/adb.exe'", "adb="+repr(os.environ.get('ADB','C:/Users/narum/AppData/Local/Android/Sdk/platform-tools/adb.exe')))
if os.environ.get('FRIDA_REMOTE'):
 source=source.replace('frida.get_device(serial,timeout=15)',"frida.get_device_manager().add_remote_device("+repr(os.environ['FRIDA_REMOTE'])+")")
source=source.replace("pathlib.Path('scripts/media-gallery/thread-runtime.js').read_text(encoding='utf-8-sig'))", "pathlib.Path('scripts/media-gallery/thread-runtime.js').read_text(encoding='utf-8-sig')+'\\n'+pathlib.Path('scripts/media-gallery/native-back-runtime.js').read_text(encoding='utf-8'))")
exec(source)
# A display-sized animation avoids treating a 1px native GIF's automatic fit as user zoom.
import io
from PIL import Image
fixture=io.BytesIO();Image.new('RGB',(320,240),'#3090e0').save(fixture,format='GIF',save_all=True,append_images=[Image.new('RGB',(320,240),'#e09030')],duration=100,loop=0);gif=fixture.getvalue()
try:
 rpc.setup('http://10.0.2.2:'+str(server.server_port))
 def launch(index,native=False):
  rpc.reset_viewers();time.sleep(.25)
  rpc.preference('gifUseNativeViewer',native);rpc.launch(index)
  wait_state(['/a.png','/one.gif','/clip.mp4'][index], 'ImagesViewer2Activity' if native or index==0 else 'GifViewerActivity' if index==1 else 'VideoViewerActivity',index!=0 and not native)
  time.sleep(.5);rpc.configure_back(0)
 for index in [0,1]:
  if package.endswith('242'):
   for enabled in [False,True]:
    for dy in [-450,450]:
     launch(index,True);rpc.preference('mediaSwipeWholeThread',enabled);rpc.full_swipe(0,dy);time.sleep(.6)
     state=rpc.state();check(not state['type'].endswith('ViewerActivity') and not state['type'].endswith('ImagesViewer2Activity'),f'native {index} vertical {dy} closes with whole-thread {enabled}: '+str(state))
  else:
   launch(index,True);rpc.full_swipe(0,-450);wait_state('/one.gif' if index==0 else '/clip.mp4','ImagesViewer2Activity' if index==0 else 'VideoViewerActivity',index==1);check(True,f'legacy native {index} retains vertical gallery movement')
  launch(index,True);rpc.full_swipe(-450,0);wait_state('/one.gif' if index==0 else '/clip.mp4','ImagesViewer2Activity' if index==0 else 'VideoViewerActivity',index==1);check(True,f'native {index} horizontal still advances gallery')
  launch(index,True);rpc.configure_back(4);rpc.full_swipe(-450,0);time.sleep(.6);check('url' not in rpc.state(),f'configured native Back wins for {index}')
 for index,suffix in [(1,'/clip.mp4'),(2,'/two.gif')]:
  launch(index);rpc.full_swipe(0,-450);wait_state(suffix,'VideoViewerActivity' if index==1 else 'GifViewerActivity',True);check(True,f'dedicated {index} retains vertical gallery movement')
 print(json.dumps({'checks':len(checks),'passed':checks},ensure_ascii=False),flush=True)
 pathlib.Path('build/media-native-back-'+package[-3:]+'-result.json').write_text(json.dumps({'checks':checks},ensure_ascii=False,indent=2),encoding='utf-8')
finally:
 try:rpc.restore_back();rpc.cleanup()
 finally:session.detach();server.shutdown()
