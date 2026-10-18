// Read-only inspection of the active verification app's board-list adapters.
Java.perform(function () {
  const app = Java.use('android.app.ActivityThread').currentApplication();
  const pkg = String(app.getPackageName());
  if (!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg)) throw Error('Verification app only');
  const D = Java.use('app.morphe.extension.chmate.DeleteReadThreads');
  const type = pkg.endsWith('226') ? 'o.Yhp5' : pkg.endsWith('191') ? 'o.r8lambdaGCnF6WpW_bFarRe7yCX2B6KzQ' : 'jp.syoboi.a2chMate.ui.threadlist.ThreadListFragment';
  function dump(view) {
    try {
      const method = view.getClass().getMethod('getAdapter', Java.array('java.lang.Class', []));
      const adapter = method.invoke(view, Java.array('java.lang.Object', []));
      if (adapter) {
        console.log('ADAPTER ' + adapter.getClass().getName());
        if (Java.use('android.widget.Adapter').class.isInstance(adapter)) {
          const a = Java.cast(adapter, Java.use('android.widget.Adapter'));
          console.log('COUNT ' + a.getCount());
          for (let i = 0; i < Math.min(a.getCount(), 5); i++) {
            const row = a.getItem(i); console.log('ITEM ' + i + ' ' + (row ? row.getClass().getName() : 'null'));
          }
        }
        for (let cls = adapter.getClass(); cls; cls = cls.getSuperclass()) {
          for (const f of cls.getDeclaredFields()) {
            if (String(f.getType().getName()).match(/List|Adapter/)) {
              f.setAccessible(true); const value = f.get(adapter);
              console.log('FIELD ' + f.getName() + ': ' + (value ? value.getClass().getName() : 'null'));
            }
          }
        }
      }
    } catch (_) {}
    if (Java.use('android.view.ViewGroup').class.isInstance(view)) {
      const g = Java.cast(view, Java.use('android.view.ViewGroup'));
      for (let i = 0; i < g.getChildCount(); i++) dump(g.getChildAt(i));
    }
  }
  Java.choose(type, { onMatch: function (f) {
    const fragment = Java.retain(f);
    Java.scheduleOnMainThread(function () {
      if (!fragment.getView()) return;
      console.log('FRAGMENT shown=' + fragment.getView().isShown());
      dump(fragment.getView());
      try { console.log('LOCAL ' + D.localRows(fragment).size()); } catch (e) { console.log('ERROR ' + e); }
    });
  }, onComplete: function () {} });
});
