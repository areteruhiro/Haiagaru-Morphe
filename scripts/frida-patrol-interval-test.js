// Native request/timing tests only. Does not run a patrol or change user settings.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const match=String(app.getPackageName()).match(/\.haiagaru\.responsearchive(191|226|242)$/);
    if(!match)throw Error('Verification app required');
    const version=match[1];
    const config={
        '191':['o.mdExternalSyntheticLambda0','o.a7aExternalSyntheticLambda4$IconCompatParcelizer','o.r8lambdadoaQtiUaFqNFKJn5PTp_Lim60DI','a','m','g','k','r','f','c'],
        '226':['o.settings','o.isAdLoaded$ComponentActivity','o.setStarRatingContentViewGroupId','d','k','j','n','p','i','c'],
        '242':['o.TTRewardVideoActivity8','o.CloseGuard$RemoteActionCompatParcelizer','o.hmac','c','m','f','k','q','g','a']
    }[version];
    const factory=Java;
    const Scheduler=factory.use(config[0]);
    const Builder=factory.use(config[1]);
    const Worker=factory.use('jp.syoboi.a2chMate.autocheck.PeriodicAutoCheckWork');
    const Unit=Java.use('java.util.concurrent.TimeUnit');
    const Spec=factory.use(config[2]);
    const seed=1700000000000;
    let checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;}
    function field(object,name) {
        for(let type=object.getClass();type!==null;type=type.getSuperclass()) {
            try{const f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(ignored){}
        }
        throw Error('Missing field '+name);
    }
    function spec(builder){return Java.cast(field(builder,config[3]).get(builder),Spec);}
    function number(object,name){return Number(field(object,name).getLong(object));}
    function setLong(object,name,value){field(object,name).setLong(object,value);}
    for(const minutes of [5,10,15,30,60,180,360,1440]){
        const builder=Builder.$new(Worker.class,minutes,Unit.MINUTES.value);
        const work=spec(builder);
        const previous=number(work,config[4]);
        check(previous===Math.max(15,minutes)*60000,'generic minimum before scoped correction '+minutes);
        Scheduler.haiagaruPatrolInterval(builder,minutes);
        check(number(work,config[4])===minutes*60000,'interval milliseconds '+minutes);
        check(number(work,config[5])===minutes*60000,'flex milliseconds '+minutes);
        // Native WorkSpec's own next-run calculation, not a replacement timer.
        setLong(work,config[6],seed);field(work,config[7]).setInt(work,1);
        const next=Number(work[config[9]]());
        check(next===seed+minutes*60000,'native next-run time '+minutes+': '+next);
        if(minutes<15){
            field(work,config[7]).setInt(work,0);setLong(work,config[8],minutes*60000);
            check(Number(work[config[9]]())===seed+minutes*60000,'native first-run delay '+minutes);
            const unrelated=Builder.$new(Worker.class,minutes,Unit.MINUTES.value);
            check(number(spec(unrelated),config[4])===900000,'unrelated builders retain minimum '+minutes);
        }
    }
    const resources=app.getResources();
    // Resolve the array by contents so regenerated resource IDs are not assumed.
    let labelId=0,valueId=0;
    for(let id=0x7f030000;id<0x7f030100;id++){
        try{
            const values=resources.getStringArray(id);const items=[];for(let n=0;n<values.length;n++)items.push(String(values[n]));
            if(items.join('|')==='しない|5分|10分|15分|30分|1時間|3時間|6時間|24時間')labelId=id;
            if(items.join('|')==='0|5|10|15|30|60|180|360|1440')valueId=id;
        }catch(ignored){}
    }
    check(labelId!==0,'native label choices contain 5/10 minutes');
    check(valueId!==0,'native stored values map to 5/10');
    console.log('PASS '+checks+' native patrol interval checks ('+version+'); no patrol started');
});
