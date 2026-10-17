// Synthetic rows and a cancel-only confirmation. Never delete a real bookmark or log.
Java.perform(function () {
  Java.deoptimizeEverything();
  const Thread=Java.use('android.app.ActivityThread'), app=Thread.currentApplication();
  const pkg=String(app.getPackageName());
  if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification package required');
  const D=Java.use('app.morphe.extension.chmate.DeleteReadThreads');
  let checks=0;function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
  Java.scheduleOnMainThread(function(){
    try {
      check(D.eligible(1,10,10),'fully read included');
      check(!D.eligible(-1,10,10),'unopened excluded');
      check(!D.eligible(1,10,9),'unread excluded');
      check(!D.eligible(1,0,0),'empty excluded');
      const legacy=pkg.endsWith('191'), mid=pkg.endsWith('226');
      const Board=Java.use(legacy?'jp.syoboi.a2chMate.client.BBSUrlInfo$BoardID':'jp.syoboi.a2chMate.client.BoardID');
      const board=D.parseBoard(Board.class,'android');
      check(board!==null&&String(board.toString())==='android','native board parser round trip');
      const Table=Java.use(legacy?'o.r8lambdaEBvvDaQDWIaS7WUoordU_4sxR3Y':mid?'o.TrustRootIndex':'o.initView');
      const tableMethod=Table.class.getDeclaredMethod(legacy?'e':mid?'b':'a',Java.array('java.lang.Class',[Board.class,Java.use('java.lang.Long').TYPE.value]));
      check(String(tableMethod.getReturnType().getName())==='void','native delete-with-cache API exists');
      const Unsafe=Java.use('sun.misc.Unsafe'), uf=Unsafe.class.getDeclaredField('theUnsafe');uf.setAccessible(true);
      const unsafe=Java.cast(uf.get(null),Unsafe);
      const modelType=Java.use(legacy?'o.MaxAdViewImplExternalSyntheticLambda2':mid?'o.verifyHostname':'o.zzauw');
      function set(object,name,value,long){const f=object.getClass().getDeclaredField(name);f.setAccessible(true);if(long)f.setLong(object,value);else f.setInt(object,value);}
      function row(id,total,read){
        const model=unsafe.allocateInstance(modelType.class);
        set(model,legacy?'e':mid?'b':'d',id,true);set(model,legacy?'d':mid?'c':'a',total,false);set(model,legacy?'b':mid?'e':'g',read,false);
        if(!legacy&&!mid){const wrap=unsafe.allocateInstance(Java.use('o.zzavl$read').class),field=wrap.getClass().getDeclaredField('a');field.setAccessible(true);field.set(wrap,model);return wrap;}
        return model;
      }
      const rows=Java.use('java.util.ArrayList').$new();rows.add(row(101,10,10));rows.add(row(102,10,9));rows.add(row(-1,10,10));rows.add(row(103,0,0));
      const list=Java.use('android.widget.ListView').$new(app);
      list.setAdapter(Java.use('android.widget.ArrayAdapter').$new(app,17367043,rows));
      const found=Java.use('java.util.LinkedHashMap').$new();D.collectRows(list,found);
      check(found.size()===1&&found.containsKey(Java.use('java.lang.Long').valueOf(101)),'actual native row types filter only fully-read local row');
      let active=null;
      for(const object of Thread.currentActivityThread().mActivities.value.values().toArray()){
        const record=Java.cast(object,Java.use('android.app.ActivityThread$ActivityClientRecord'));
        if(record.activity.value&&!record.paused.value)active=record.activity.value;
      }
      if(!active)throw Error('No resumed activity');
      const fragment=Java.use(legacy?'o.r8lambdaGCnF6WpW_bFarRe7yCX2B6KzQ':mid?'o.Yhp5':'jp.syoboi.a2chMate.ui.threadlist.ThreadListFragment').$new();
      const Candidate=Java.use('app.morphe.extension.chmate.DeleteReadThreads$Candidate');
      const candidates=Java.use('java.util.ArrayList').$new();
      for(let i=0;i<3;i++)candidates.add(Candidate.$new(900000+i,'5ch.net/android',1700000000+i,'【検証用・実データではありません】既読スレ '+(i+1)));
      D.confirm(active,fragment,candidates);
      setTimeout(function(){Java.choose('android.app.AlertDialog',{
        onMatch:function(value){if(value.isShowing() && value.getButton(-1)!==null && value.getButton(-1).getText().toString()==='削除（3件）') {const dialog=Java.retain(value);Java.scheduleOnMainThread(function(){try{verify(dialog);}catch(error){console.log('FAIL '+error.stack);}});return 'stop';}},
        onComplete:function(){}
      });},500);
      function verify(dialog){
      if(!dialog)throw Error('Confirmation not shown');
      function caption(){return dialog.getButton(-1).getText().toString();}
      check(caption()==='削除（3件）','confirmation initially selects all: '+caption());
      const choices=dialog.getListView();
      choices.performItemClick(choices.getAdapter().getView(1,null,choices),1,1);
      check(caption()==='削除（2件）','individual checkbox toggles off');
      dialog.getButton(-3).performClick();
      check(caption()==='削除（3件）','partial selection toggles all on');
      dialog.getButton(-3).performClick();
      check(!dialog.getButton(-1).isEnabled()&&caption()==='削除（0件）','clear all disables deletion');
      choices.performItemClick(choices.getAdapter().getView(0,null,choices),0,0);
      check(dialog.getButton(-1).isEnabled()&&caption()==='削除（1件）','individual checkbox reselects');
      dialog.getButton(-2).performClick();
      setTimeout(function(){Java.scheduleOnMainThread(function(){try{
      check(!dialog.isShowing(),'cancel closes without deletion');
      console.log('COMPLETE '+checks+' read-delete runtime checks for '+pkg);
      }catch(error){console.log('FAIL '+error.stack);}});},1500);
      }
    }catch(error){console.log('FAIL '+error.stack);}
  });
});
