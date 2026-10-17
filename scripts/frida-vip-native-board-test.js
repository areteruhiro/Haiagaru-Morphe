// Read-only native parser/board identity checks in the fixed test applications.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const pkg=String(app.getPackageName());
 if(!/\.haiagaru\.(?:uploadhosts191|responsearchive(?:226|242))$/.test(pkg))throw Error('Test package required');
 const Class=Java.use('java.lang.Class'), Modifier=Java.use('java.lang.reflect.Modifier');
 const type=Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo').class;
 const methods=type.getDeclaredMethods(); let count=0;
 for(let i=0;i<methods.length;i++){
  const m=methods[i],p=m.getParameterTypes();
  if(!Modifier.isStatic(m.getModifiers())||!m.getReturnType().equals(type)||p.length!==1||String(p[0].getName())!=='java.lang.String')continue;
  m.setAccessible(true);
  const model=m.invoke(null,Java.array('java.lang.Object',['https://ex14.vip2ch.com/test/read.cgi/news4ssnip/1464424549/']));
  if(model===null)continue;
  for(let j=0;j<methods.length;j++){
   const a=methods[j],q=a.getParameterTypes(),s=Modifier.isStatic(a.getModifiers());
   if(!String(a.getReturnType().getName()).endsWith('BoardID'))continue;
   if((!s&&q.length===0)||(s&&q.length===1&&q[0].equals(type))){
    a.setAccessible(true);
    const board=a.invoke(s?null:model,Java.array('java.lang.Object',s?[model]:[]));
    console.log('NATIVE '+pkg+' parser='+m.getName()+' board='+board);count++;
   }
  }
 }
 if(count===0)throw Error('Native board identity unavailable');
 console.log('PASS '+count+' native VIP board parser checks');
});
