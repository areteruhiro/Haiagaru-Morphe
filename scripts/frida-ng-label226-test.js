// Isolated app only. Draws the actual native gesture overlay; does not edit NG entries.
Java.perform(function(){Java.scheduleOnMainThread(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).endsWith('.nglabel226'))throw Error('Isolated app only');
 const prefs=Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
 const key='haiagaruGestureNgEdit',old=prefs.getInt(key,0),existed=prefs.contains(key);
 const H=Java.use('app.morphe.extension.chmate.NgEditGesture');
 const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();let activity=null;
 for(const r of records){const a=Java.cast(r,Java.use('android.app.ActivityThread$ActivityClientRecord')).activity.value;if(a&&a.hasWindowFocus())activity=a;}
 if(!activity)throw Error('No focused activity');
 const Native=Java.use('o.TTVideoLandingPageActivity');
 const native=Java.retain(Native.$new(activity,null));native.setBackgroundColor(-16777216);
 activity.setContentView(native);
 const Bitmap=Java.use('android.graphics.Bitmap'),Canvas=Java.use('android.graphics.Canvas');
 function field(object,name){const f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
 function check(ok,msg){if(!ok)throw Error(msg);console.log('PASS '+msg);checks++;}
 let checks=0;
 function label(object){const fields=object.getClass().getDeclaredFields();for(const f of fields){if(String(f.getType().getName())==='java.lang.String'){f.setAccessible(true);return String(f.get(object));}}return null;}
 setTimeout(function(){Java.perform(function(){Java.scheduleOnMainThread(function(){
  try {
   prefs.edit().putInt(key,8).commit();
   const preview=H.preview(native,8,null);
   check(preview!==null&&label(preview)==='NG編集','assigned gesture resolves native preview object and Japanese label');
   check(String(preview.getClass().getName())==='o.TTVideoLandingPageActivity$write','native action object matches dispatchDraw cast');
   check(H.preview(native,6,null)===null,'different gesture has no NG preview');
   prefs.edit().putInt(key,0).commit();check(H.preview(native,8,null)===null,'unassigned preview absent');
   prefs.edit().putInt(key,8).putInt('gestureNgLabelFixture',8).commit();
   check(H.preview(native,8,null)===null,'existing assigned operation has priority');
   prefs.edit().remove('gestureNgLabelFixture').commit();
   const original=Java.use('o.TTVideoLandingPageActivity$write').$new(123,'既存操作');
   check(H.preview(native,8,original).equals(original),'native preview object is never replaced');
   const detector=Java.cast(field(native,'a').get(native),Java.use('o.TTVideoLandingPageActivity3'));
   field(detector,'c').setInt(detector,1);field(detector,'d').setInt(detector,8);
   field(native,'d').setBoolean(native,true);field(native,'o').setBoolean(native,true);
   const paint=Java.cast(field(native,'u').get(native),Java.use('android.graphics.Paint'));paint.setColor(-1);
   field(native,'q').setFloat(native,48);
   const w=native.getWidth(),h=native.getHeight();check(w>0&&h>0,'real native gesture view is laid out');
   function render(assignment){prefs.edit().putInt(key,assignment).commit();let b=Bitmap.createBitmap(w,h,Java.use('android.graphics.Bitmap$Config').ARGB_8888.value);native.draw(Canvas.$new(b));return b;}
   const before=render(0),after=render(8);
   check(!before.sameAs(after),'native dispatchDraw paints additional NG label pixels');
   before.recycle();after.recycle();
   const map=Java.cast(field(native,'b').get(native),Java.use('java.util.Map'));
   check(map.get(Java.use('java.lang.Integer').valueOf(8))===null,'preview does not register synthetic actions for execution');
   console.log('NG LABEL CHECKS='+checks);native.invalidate();
  } finally {
   // Keep the successful synthetic preview on screen for a screenshot. Only this clone is used.
   prefs.edit().remove('gestureNgLabelFixture').commit();
  }
 });});},500);
});});
