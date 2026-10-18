// Isolated navigation test with detached controls; never changes stored settings.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).endsWith('haiagaru.responsearchive242'))throw Error('Test app only');
 const loader=Java.use('dalvik.system.DelegateLastClassLoader').$new('/data/local/tmp/settings-navigation.dex',app.getClassLoader());
 const factory=Java.ClassFactory.get(loader), Sections=factory.use('app.morphe.extension.chmate.SettingsSections'), Category=factory.use('app.morphe.extension.chmate.SettingsSections$Category');
 let found=false;
 Java.choose('android.app.Activity',{onMatch(a){
  if(found||a.isFinishing()||!a.getWindow().getDecorView().isShown())return;found=true;
  const activity=Java.retain(a);
  Java.scheduleOnMainThread(function(){try{
   const Linear=Java.use('android.widget.LinearLayout'), Text=Java.use('android.widget.TextView'), Edit=Java.use('android.widget.EditText');
   const root=Linear.$new(activity);root.setOrientation(1);root.setPadding(32,24,32,24);
   const sections=Sections.$new(root,false), values=Category.values();
   const inputs=[];
   for(let i=0;i<values.length;i++){
    sections.begin(values[i]);
    const label=Text.$new(activity);label.setText(Java.use('java.lang.String').$new('設定の説明 '+i));root.addView(label);
    const input=Edit.$new(activity);input.setText(Java.use('java.lang.String').$new('unchanged '+i),Java.use('android.widget.TextView$BufferType').EDITABLE.value);root.addView(input);inputs.push(Java.retain(input));
   }
   sections.finish();let count=0;
   function check(ok,s){if(!ok)throw Error(s);count++;console.log('PASS '+s);}
   check(!sections.goBack(),'home back can close');
   for(let i=0;i<values.length;i++){
    sections.revealCategory(values[i]);
    check(Java.cast(inputs[i].getParent(),Linear).getVisibility()===0,'category '+i+' visible');
    check(sections.goBack(),'category '+i+' back to index');
    check(Java.cast(inputs[i].getParent(),Linear).getVisibility()===8,'category '+i+' hidden on index');
    check(String(Java.cast(inputs[i].getText(),Java.use('java.lang.Object')).toString())==='unchanged '+i,'category '+i+' value retained');
   }
   sections.reveal(inputs[3]);check(Java.cast(inputs[3].getParent(),Linear).getVisibility()===0,'invalid input revealed');sections.goBack();
   const scroll=Java.use('android.widget.ScrollView').$new(activity);scroll.addView(root);
   const S=Java.use('java.lang.String');
   const dialog=Java.use('android.app.AlertDialog$Builder').$new(activity).setTitle(S.$new('Haiagaru設定・表示検証')).setView(scroll).setPositiveButton(S.$new('保存して閉じる'),null).setNegativeButton(S.$new('変更を保存せず閉じる'),null).create();
   dialog.show();dialog.getWindow().setLayout(-1,-1);
   console.log('COMPLETE '+count+' navigation checks');
  }catch(e){console.log('FAIL '+e.stack);}});
 },onComplete(){if(!found)console.log('FAIL no visible activity');}});
});
