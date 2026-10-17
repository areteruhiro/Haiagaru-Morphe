/* Diagnostic flags only: never prints a code, Cookie, draft, or full URL. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive242')
        throw Error('Verification 242 only');
    const Auth = Java.use('app.morphe.extension.chmate.ExternalBoardAuth');
    const Rules = Java.use('app.morphe.extension.chmate.BoardAuthRules');
    Auth.observe.implementation = function (message) {
        if (message !== null) {
            const value = String(message);
            if (Rules.required(value)) console.log('AUTH_ERROR observed; length=' + value.length
                + '; labelledCode=' + (String(Rules.code(value)).length > 0));
        }
        return this.observe(message);
    };
    console.log('Authentication error observer attached; no sensitive values logged');
});
