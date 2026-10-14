// Logs only static menu templates and source-host identity, not response contents.
Java.perform(function () {
    var compat = Java.use('app.morphe.extension.chmate.HissiMenuCompatibility');
    var rewrite = compat.rewriteTemplate.overload('java.lang.String');
    rewrite.implementation = function (template) {
        var result = rewrite.call(this, template);
        if (template && template.indexOf('hissi.org') >= 0)
            console.log('[poverty] template=' + template + '\n[poverty] rewritten=' + result);
        return result;
    };
    console.log('[poverty] menu trace ready');
});
