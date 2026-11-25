rpc.exports.test = () => new Promise(resolve => Java.perform(() => {
    let prefs, had, previous;
    try {
        const app = Java.use('android.app.ActivityThread').currentApplication();
        if (!/\.haiagaru\.reporter(?:191|226|242)$/.test(String(app.getPackageName()))) throw Error('Isolated reporter only');
        prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
        had = prefs.contains('recordAppTraffic');
        previous = prefs.getBoolean('recordAppTraffic', true);
        prefs.edit().remove('recordAppTraffic').commit();
        const traffic = Java.use('app.morphe.extension.chmate.AppTrafficUsage');
        const contextClass = Java.use('android.content.Context').class;
        const method = traffic.class.getDeclaredMethod('enabled', Java.array('java.lang.Class', [contextClass]));
        method.setAccessible(true);
        const enabled = Java.cast(method.invoke(null, Java.array('java.lang.Object', [app])), Java.use('java.lang.Boolean')).booleanValue();
        if (enabled) throw Error('recording defaults to enabled');
        const startedField = traffic.class.getDeclaredField('started');
        startedField.setAccessible(true);
        const samplerStarted = startedField.getBoolean(null);
        if (samplerStarted) throw Error('sampler started while recording defaults to disabled');
        resolve({defaultOff: true, samplerStopped: true, fixtureOnly: true, settingRestored: true});
    } catch (error) {
        resolve({error: String(error), fixtureOnly: true});
    } finally {
        if (prefs) {
            const edit = prefs.edit();
            if (had) edit.putBoolean('recordAppTraffic', previous); else edit.remove('recordAppTraffic');
            edit.commit();
        }
    }
}));
