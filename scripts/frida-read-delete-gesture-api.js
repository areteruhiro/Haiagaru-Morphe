// Read-only probe of the exact AndroidX signatures used by DeleteReadThreads.
// No deletion, database writes, or settings changes.
Java.perform(function () {
  const app = Java.use('android.app.ActivityThread').currentApplication();
  const pkg = String(app.getPackageName());
  if (!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))
    throw Error('Verification package required');
  const Modifier = Java.use('java.lang.reflect.Modifier');
  const Activity = Java.use('androidx.fragment.app.FragmentActivity');
  const Manager = Java.use('androidx.fragment.app.FragmentManager');
  function resolve(owner, name, result) {
    let named = null, matches = [];
    for (const m of owner.getDeclaredMethods()) {
      if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers()) ||
          m.getParameterTypes().length || String(m.getReturnType().getName()) !== result ||
          m.isBridge() || m.isSynthetic()) continue;
      matches.push(m);
      if (String(m.getName()) === name) named = m;
    }
    if (!named && matches.length !== 1) throw Error('Ambiguous API: ' + owner + ': ' + matches);
    const m = named || matches[0];
    console.log('PASS ' + name + ' resolves to ' + m.getName());
    return m;
  }
  const getter = resolve(Activity.class, 'getSupportFragmentManager', 'androidx.fragment.app.FragmentManager');
  const list = resolve(Manager.class, 'getFragments', 'java.util.List');
  Java.choose('androidx.fragment.app.FragmentActivity', {
    onMatch: function (a) {
      const activity = Java.retain(a);
      Java.scheduleOnMainThread(function () {
        if (activity.isFinishing()) return;
        const manager = getter.invoke(activity, Java.array('java.lang.Object', []));
        function visit(manager) {
          const rows = Java.cast(list.invoke(manager, Java.array('java.lang.Object', [])), Java.use('java.util.List'));
          for (let i = 0; i < rows.size(); i++) {
            const f = Java.cast(rows.get(i), Java.use('androidx.fragment.app.Fragment'));
            if (!f.isAdded()) continue;
            const v = f.getView();
            console.log('FRAGMENT ' + f.getClass().getName() + ' shown=' + (v && v.isShown()));
            visit(f.getChildFragmentManager());
          }
        }
        visit(manager);
        console.log('PASS active fragment traversal without deletion');
      });
    }, onComplete: function () {}
  });
});
