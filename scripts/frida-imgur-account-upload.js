// Isolated emulator test only. Never log cookies, passwords, tokens or delete hashes.
// probeImgurAccount() is read-only. uploadOneImgurFixture() posts ONE synthetic PNG;
// invoke only after the user has logged in and authorized an account upload test.
function imgurFixtureContext() {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(app.getPackageName().toString()!=='jp.co.airfront.android.a2chMate.haiagaru.imgur191')
        throw new Error('Isolated 191 test package required');
    return app;
}
globalThis.probeImgurAccount=function() { Java.perform(function() {
    const app=imgurFixtureContext(), upload=Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    console.log('Experimental patch installed='+upload.supported(app)+', enabled='+upload.enabled());
    try { upload.checkAccount(); console.log('PASS authenticated account preflight (identity not logged)'); }
    catch(error) { console.log('Account preflight failed: '+error); }
}); };
globalThis.uploadOneImgurFixture=function() { Java.perform(function() {
    const app=imgurFixtureContext(), upload=Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    if(!upload.supported(app)||!upload.enabled())throw new Error('User must verify and enable login first');
    const Bitmap=Java.use('android.graphics.Bitmap'), Config=Java.use('android.graphics.Bitmap$Config');
    const file=Java.use('java.io.File').$new(app.getCacheDir(),'haiagaru-imgur-account-test.png');
    const bitmap=Bitmap.createBitmap(64,64,Config.ARGB_8888.value);
    let output=null;
    try {
        bitmap.eraseColor(-16744320);
        output=Java.use('java.io.FileOutputStream').$new(file);
        if(!bitmap.compress(Java.use('android.graphics.Bitmap$CompressFormat').PNG.value,100,output))
            throw new Error('PNG fixture encoding failed');
        output.close();output=null;
        const result=Java.use('app.morphe.extension.chmate.Haiagaru')
            .uploadLegacyImage(Java.array('java.lang.Object',[file]));
        globalThis.lastImgurFixtureResponse=Java.retain(result);
        // Obfuscated data.d also names a method; use actual DEX fields, not Frida property aliases.
        const data=result.getClass().getField('d').get(result);
        const link=data.getClass().getField('d').get(data);
        if(link===null||!/^https:\/\/i\.imgur\.com\/[A-Za-z0-9]+\.png$/.test(link.toString()))
            throw new Error('Uploaded response has no valid PNG link (DO NOT resend)');
        console.log('PASS one account-owned PNG via 191 uploader wrapper: '+link);
    } catch(error) {
        console.log('Upload failed; NO automatic retry. Check Imgur account before retrying: '+error);
    } finally {
        if(output!==null)output.close();bitmap.recycle();file.delete();
    }
}); };
