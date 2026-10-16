// Isolated package only. Tests real Android TextView layout; no network or forum actions.
Java.perform(function(){Java.scheduleOnMainThread(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!String(app.getPackageName()).includes('.titlespacing'))throw Error('Isolated app only');
    const H=Java.use('app.morphe.extension.chmate.Haiagaru');
    const S=Java.use('app.morphe.extension.chmate.ThreadTitleSpacing');
    const Text=Java.use('android.widget.TextView');
    const p=H.preferences(app);
    const Linear=Java.use('android.widget.LinearLayout');
    const root=Linear.$new(app);root.setOrientation(1);root.setPadding(16,16,16,16);root.setBackgroundColor(-1);
    const sample='日本語のスレッドタイトルを折り返して確認します。カスタムフォントの行間を確認します。';
    let checks=0;
    function check(ok,msg){if(!ok)throw Error(msg);checks++;console.log('PASS '+msg);}
    function measure(t){t.measure(1073742284,0);t.layout(0,0,t.getMeasuredWidth(),t.getMeasuredHeight());}
    function values(t){const l=t.getLayout();return {lines:l.getLineCount(),first:l.getLineBaseline(0),gap:l.getLineBaseline(1)-l.getLineBaseline(0),height:t.getMeasuredHeight(),top:l.getTopPadding(),bottom:l.getBottomPadding()};}
    for(const family of ['sans-serif','serif','monospace']) for(const includePad of [false,true]){
        const title=Text.$new(app);title.setTextSize(20);title.setTextColor(-16777216);
        title.setLayoutParams(Java.use('android.widget.LinearLayout$LayoutParams').$new(460,-2));
        title.setTypeface(Java.use('android.graphics.Typeface').create(family,0));
        title.setIncludeFontPadding(includePad);title.setPadding(0,0,0,0);
        title.setText.overload('java.lang.CharSequence').call(title,sample);
        measure(title);const before=values(title);let previous=before.gap;
        for(const percent of [100,105,110,150,200,100]){
            p.edit().putInt('threadTitleSpacingPercent',percent).commit();S.apply(title);measure(title);
            const v=values(title);console.log(family+' fontPad='+includePad+' '+percent+'% '+JSON.stringify(v));
            check(v.lines===before.lines,'line wrapping unchanged '+family+' '+percent);
            check(v.first===before.first && v.top===before.top && v.bottom===before.bottom && title.getIncludeFontPadding()===includePad,
                'outer/font padding unchanged '+family+' '+percent);
            if(percent>100){check(v.gap>previous,'inter-line distance grows '+family+' '+percent);previous=v.gap;}
            else check(v.gap===before.gap && v.height===before.height,'100% restored '+family);
            for(let n=1;n<v.lines;n++)check(Math.abs((title.getLayout().getLineBaseline(n)-title.getLayout().getLineBaseline(n-1))-v.gap)<=1,
                'uniform inter-line distance '+family+' '+percent+' line '+n);
            check(Math.abs(v.height-(before.height+(v.lines-1)*(v.gap-before.gap)))<=v.lines,
                'only inter-line gaps increase total height');
            const gap=v.gap;S.apply(title);measure(title);check(values(title).gap===gap,'reapply does not accumulate');
            if(family==='sans-serif' && !includePad && percent!==100){
                const label=Text.$new(app);label.setText.overload('java.lang.CharSequence').call(label,percent+'%');label.setTextColor(-16777216);root.addView(label);
                const copy=Text.$new(app);copy.setTextSize(20);copy.setTextColor(-16777216);copy.setIncludeFontPadding(false);
                copy.setText.overload('java.lang.CharSequence').call(copy,title.getText());root.addView(copy);
            }
        }
        const single=Text.$new(app);single.setTextSize(20);single.setIncludeFontPadding(false);
        single.setLayoutParams(Java.use('android.widget.LinearLayout$LayoutParams').$new(460,-2));
        single.setText.overload('java.lang.CharSequence').call(single,'短いスレタイ');measure(single);
        const height=single.getMeasuredHeight();const baseline=single.getBaseline();
        p.edit().putInt('threadTitleSpacingPercent',200).commit();S.apply(single);measure(single);
        check(single.getMeasuredHeight()===height && single.getBaseline()===baseline,'single-line row unchanged '+family);
    }
    p.edit().putInt('threadTitleSpacingPercent',100).commit();
    const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
    for(const r of records){const a=Java.cast(r,Java.use('android.app.ActivityThread$ActivityClientRecord')).activity.value;
        if(a && a.hasWindowFocus()){a.setContentView(root);break;}}
    console.log('LAYOUT CHECKS='+checks);
});});
