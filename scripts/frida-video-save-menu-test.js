// Exercise the actual viewer menu and cancel its confirmation. Playback/network is suppressed.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive191')
        throw Error('Isolated verification package required');
    const Viewer = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const Intent = Java.use('android.content.Intent');
    const Uri = Java.use('android.net.Uri');
    let checked = false;
    Viewer.load.implementation = function () { console.log('PASS playback suppressed for menu test'); };
    Java.scheduleOnMainThread(function () {
        const intent = Intent.$new('android.intent.action.VIEW', Uri.parse('https://i.imgur.com/A54Xv8k.mp4'));
        intent.setClass(app, Viewer.class); intent.addFlags(0x10000000); app.startActivity(intent);
    });
    let attempts = 0;
    function inspect() {
        Java.perform(function () {
            let found = false;
            Java.choose('app.morphe.extension.chmate.VideoViewerActivity', {
                onMatch: function (activity) {
                    if (found || activity.isFinishing() || !activity.foreground.value) return;
                    found = true;
                    const saved = Java.retain(activity);
                    Java.scheduleOnMainThread(function () {
                        try {
                            saved.showMenu(saved.toolbar.value.getChildAt(2));
                            Java.choose('android.widget.PopupMenu', {
                                onMatch: function (popup) {
                                    const contextField = popup.getClass().getDeclaredField('mContext');
                                    contextField.setAccessible(true);
                                    if (!contextField.get(popup).equals(saved)) return;
                                    const item = popup.getMenu().findItem(6);
                                    if (!item || String(item.getTitle()) !== '動画を保存') return;
                                    console.log('PASS actual viewer menu contains 動画を保存');
                                    popup.getMenu().performIdentifierAction(6, 0);
                                    Java.choose('android.app.AlertDialog', {
                                        onMatch: function (dialog) {
                                            if (!dialog.isShowing()) return;
                                            const text = dialog.findViewById(16908299);
                                            if (!text || String(Java.cast(text, Java.use('android.widget.TextView')).getText()).indexOf('Download/Haiagaru') < 0) return;
                                            if (String(dialog.getButton(-1).getText()) !== '保存') throw Error('Save button missing');
                                            checked = true;
                                            dialog.getButton(-2).performClick();
                                            console.log('PASS destination/save/cancel confirmation workflow');
                                        }, onComplete: function () { }
                                    });
                                    popup.dismiss();
                                }, onComplete: function () { }
                            });
                        }
                        catch (error) { console.log('FAIL ' + error); }
                        finally {
                            saved.finish(); Viewer.load.implementation = null;
                            console.log(checked ? 'PASS viewer save menu workflow' : 'FAIL confirmation not reached');
                        }
                    });
                }, onComplete: function () {
                    if (!found && ++attempts < 12) setTimeout(inspect, 1000);
                    else if (!found) console.log('FAIL viewer did not open');
                }
            });
        });
    }
    setTimeout(inspect, 1500);
});
