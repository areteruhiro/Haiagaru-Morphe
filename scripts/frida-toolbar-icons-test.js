// Local synthetic icons only. Restore original visibility and custom files; no network or posts.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication(), pkg=String(app.getPackageName());
    if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification app only');
    const T=Java.use('app.morphe.extension.chmate.ToolbarIcons'), A=Java.use('app.morphe.extension.chmate.ToolbarIconsActivity');
    const p=app.getSharedPreferences('haiagaru_toolbar_icons',0), Files=Java.use('java.nio.file.Files');
    const File=Java.use('java.io.File'), Bitmap=Java.use('android.graphics.Bitmap'), Canvas=Java.use('android.graphics.Canvas');
    const Output=Java.use('java.io.ByteArrayOutputStream'), Input=Java.use('java.io.ByteArrayInputStream');
    const config=Java.use('android.graphics.Bitmap$Config').ARGB_8888.value;
    const drawable=Java.use('android.graphics.drawable.ColorDrawable').$new(-16711936);
    const before=[], keys=[];let checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
    function info(id) {
        const names=['haiagaru_edge_archive','haiagaru_quick_filter','haiagaru_mark_all_read','haiagaru_direct_copy'];
        const icon=id>=0x7e000001&&id<=0x7e000004?app.getResources().getIdentifier(names[id-0x7e000001],'drawable',app.getPackageName()):0;
        if(pkg.endsWith('191'))return Java.use('o.r8lambda98incQ33GAiiY2082BMi9yDa3l0').$new(id,icon,0,0,true);
        const Spec=Java.use('jp.syoboi.a2chMate.feature.toolbar.ToolbarButtonSpec');
        return Java.use(pkg.endsWith('226')?'o.PAGBannerAdLoadCallback':'o.zzcI').$new(Spec.$new(id,icon,0,0,true));
    }
    function render(item) { return pkg.endsWith('191')?item.XB_(app,0):pkg.endsWith('226')?item.b(app,0):item.e(app,0); }
    Java.scheduleOnMainThread(function () {
      try {
        const preview=Bitmap.createBitmap(480,120,config),previewCanvas=Canvas.$new(preview);preview.eraseColor(-14671840);
        for(let i=0;i<4;i++) {
            const key='visible.'+i,file=T.file(app,i);keys.push([key,p.contains(key),p.getBoolean(key,true)]);
            before.push([Java.retain(file),file.exists()?Files.readAllBytes(file.toPath()):null]);
            if(file.exists()&&!file.delete())throw Error('fixture cannot isolate image');T.invalidate(i);
            p.edit().putBoolean(key,true).commit();
            const id=0x7e000001+i;
            const nativeIcon=render(info(id));
            check(nativeIcon!==null,'native icon render '+i);nativeIcon.setBounds(i*120+24,24,i*120+96,96);nativeIcon.draw(previewCanvas);
            check(T.visible(app,id),'enabled '+i);
            check(T.replace(app,info(id),0,drawable).equals(drawable),'native fallback including default tint '+i);
            p.edit().putBoolean(key,false).commit();
            const item=info(id),flag=item.getClass().getDeclaredField(T.visibilityField());flag.setAccessible(true);
            check(!flag.getBoolean(item),'native toolbar hidden '+i);
            p.edit().putBoolean(key,true).commit();check(flag.getBoolean(info(id)),'native selected visibility preserved '+i);
        }
        const previewFile=File.$new(app.getCacheDir(),'toolbar-icons-preview.png'),previewOut=Java.use('java.io.FileOutputStream').$new(previewFile);
        preview.compress(Java.use('android.graphics.Bitmap$CompressFormat').PNG.value,100,previewOut);previewOut.close();preview.recycle();
        check(T.visible(app,123),'stock action visibility preserved');
        const image=Bitmap.createBitmap(100,50,config);image.eraseColor(-65536);
        const bytes=Output.$new();image.compress(Java.use('android.graphics.Bitmap$CompressFormat').PNG.value,100,bytes);image.recycle();
        A.importImage(app,0,Input.$new(bytes.toByteArray()));
        const result=render(info(0x7e000001));
        const expected=Math.round(24*app.getResources().getDisplayMetrics().density.value);
        check(result.getIntrinsicWidth()===expected&&result.getIntrinsicHeight()===expected,'custom icon uses 24dp intrinsic size');
        const bitmap=Bitmap.createBitmap(96,96,config),canvas=Canvas.$new(bitmap);result.setBounds(0,0,96,96);result.draw(canvas);
        check(bitmap.getPixel(48,48)===-65536,'custom color not theme tinted');
        check(bitmap.getPixel(48,1)===0,'aspect ratio preserved with transparent padding');bitmap.recycle();
        let failed=false;try{A.importImage(app,0,Input.$new(Java.array('byte',[1,2,3])));}catch(e){failed=true;}
        check(failed,'invalid image rejected');
        check(T.file(app,0).exists(),'previous valid custom image retained');
        check(T.replace(app,info(123),0,drawable).equals(drawable),'stock drawable untouched');
        T.file(app,0).delete();T.invalidate(0);
        check(T.replace(app,info(0x7e000001),0,drawable).equals(drawable),'reset returns to native icon');
        console.log('COMPLETE '+checks+' toolbar icon checks for '+pkg);
      }catch(e){console.log('FAIL '+e.stack);}
      finally {
        before.forEach((entry,i)=>{const f=entry[0];if(entry[1]!==null){f.getParentFile().mkdirs();const stream=Java.use('java.io.FileOutputStream').$new(f);stream.write(entry[1]);stream.close();}else if(f.exists())f.delete();T.invalidate(i);});
        const editor=p.edit();keys.forEach(v=>v[1]?editor.putBoolean(v[0],v[2]):editor.remove(v[0]));editor.commit();
      }
    });
});
