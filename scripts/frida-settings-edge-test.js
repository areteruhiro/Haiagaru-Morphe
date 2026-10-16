// Offline native SlidingPaneLayout regression, isolated test apps only.
Java.perform(function(){Java.scheduleOnMainThread(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const baseline=String(app.getPackageName()).includes('.titlespacing');
 if(!baseline&&!String(app.getPackageName()).includes('.settingsedge'))throw Error('Isolated app only');
 const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
 let activity=null;for(const r of records){const a=Java.cast(r,Java.use('android.app.ActivityThread$ActivityClientRecord')).activity.value;if(a&&a.hasWindowFocus())activity=a;}
 const VG=Java.use('android.view.ViewGroup'), D=Java.use('android.graphics.drawable.Drawable');
 let pane=null;
 function find(v){if(String(v.getClass().getName())==='androidx.slidingpanelayout.widget.SlidingPaneLayout')pane=Java.cast(v,Java.use('androidx.slidingpanelayout.widget.SlidingPaneLayout'));
 if(VG.class.isInstance(v)){let g=Java.cast(v,VG);for(let i=0;i<g.getChildCount();i++)find(g.getChildAt(i));}}
 find(activity.getWindow().getDecorView());if(!pane)throw Error('Open native settings first');
 const Bitmap=Java.use('android.graphics.Bitmap'),Canvas=Java.use('android.graphics.Canvas'),Config=Java.use('android.graphics.Bitmap$Config');
 let checks=0;function check(ok,msg){if(!ok)throw Error(msg);checks++;console.log('PASS '+msg);}
 const fields=pane.getClass().getDeclaredFields();let shadow=null;
 for(const f of fields){if(String(f.getType().getName())!=='android.graphics.drawable.Drawable')continue;f.setAccessible(true);let value=f.get(pane);if(value)shadow=Java.cast(value,D);}
 check(shadow!==null,'native settings has divider');
 if(!baseline)check(String(shadow.getClass().getName())==='app.morphe.extension.chmate.SettingsPaneDivider','APK hook wraps settings divider');
 // Actual native draw at closed pane: compare the right-edge pixel before/after removing shadow.
 const w=pane.getWidth(),h=pane.getHeight();
 function render(){const b=Bitmap.createBitmap(w,h,Config.ARGB_8888.value);pane.draw(Canvas.$new(b));return b;}
 const fixed=render();pane.setShadowDrawableLeft(null);const none=render();pane.setShadowDrawableLeft(shadow);
 let same=true;for(let y=10;y<h-10;y+=79)for(let x=w-4;x<w;x++)if(fixed.getPixel(x,y)!==none.getPixel(x,y))same=false;
 check(baseline?!same:same,baseline?'REPRODUCED native divider paints at closed settings right edge':'closed native settings right edge has no divider artifact');fixed.recycle();none.recycle();
 if(baseline){console.log('BASELINE CHECKS='+checks);return;}
 const W=Java.use('app.morphe.extension.chmate.SettingsPaneDivider');
 // Geometry regression independent of theme color; preserve tablet/sliding separators.
 const color=Java.use('android.graphics.drawable.ColorDrawable').$new(-65281);
 const wrapped=W.wrap(pane,color);const b=Bitmap.createBitmap(w,20,Config.ARGB_8888.value),c=Canvas.$new(b);
 function draw(left,right,rtl){pane.setLayoutDirection(rtl?1:0);b.eraseColor(0);wrapped.setBounds(left,0,right,20);wrapped.draw(c);}
 draw(w-4,w,false);check(b.getPixel(w-2,5)===0,'LTR offscreen pane suppressed');
 draw(96,100,false);check(b.getPixel(98,5)===-65281,'tablet/interior divider retained');
 draw(-4,0,false);check(b.getPixel(0,5)===0,'fully open pane adds no edge line');
 draw(0,4,true);check(b.getPixel(2,5)===0,'RTL offscreen pane suppressed');
 draw(96,100,true);check(b.getPixel(98,5)===-65281,'RTL interior divider retained');
 pane.setLayoutDirection(0);b.recycle();
 check(W.wrap(pane,null)===null,'null drawable retained');
 console.log('SETTINGS EDGE CHECKS='+checks);
});});
