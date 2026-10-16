Java.perform(function(){Java.scheduleOnMainThread(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).includes('.titlespacing'))throw Error('Isolated app only');
 const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
 function walk(v,d){
  let bg=v.getBackground();let color='';
  if(bg&&Java.use('android.graphics.drawable.ColorDrawable').class.isInstance(bg))color=' color='+Java.cast(bg,Java.use('android.graphics.drawable.ColorDrawable')).getColor();
  console.log(' '.repeat(d)+v.getClass().getName()+' id='+v.getId()+' xy='+v.getLeft()+','+v.getTop()+' wh='+v.getWidth()+','+v.getHeight()+' pad='+v.getPaddingLeft()+','+v.getPaddingTop()+','+v.getPaddingRight()+','+v.getPaddingBottom()+' bg='+bg+color+' scroll='+v.isVerticalScrollBarEnabled()+' style='+v.getScrollBarStyle());
  if(Java.use('android.view.ViewGroup').class.isInstance(v)){const g=Java.cast(v,Java.use('android.view.ViewGroup'));for(let i=0;i<g.getChildCount();i++)walk(g.getChildAt(i),d+1);}
 }
 for(const r of records){const a=Java.cast(r,Java.use('android.app.ActivityThread$ActivityClientRecord')).activity.value;if(a&&a.hasWindowFocus()){console.log('ACTIVITY '+a);walk(a.getWindow().getDecorView(),0);}}
 const prefs=Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app).getAll();console.log('PREFS '+prefs);
});});
