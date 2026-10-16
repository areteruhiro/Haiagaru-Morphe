/* Read-only inspection: no URLs, credentials, requests or preference changes. */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!app || String(app.getPackageName()) !==
            'jp.co.airfront.android.a2chMate.haiagaru.uploadhistory226') {
        console.log('SKIP: not the isolated 226 verification app');
        return;
    }
    const holder = Java.use('o.isAvailable').class.getDeclaredField('f');
    holder.setAccessible(true);
    const value = holder.get(null);
    if (!value) {
        console.log('UNINITIALIZED: existing native client unavailable');
        return;
    }
    const client = Java.cast(value, Java.use('o.zzdE'));
    console.log(JSON.stringify({event: 'native226-connection-policy',
        fastFallback: Boolean(client.m.value)}));
});
