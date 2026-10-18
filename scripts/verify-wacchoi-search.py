"""Exercise the real search Activity without sending posts or touching ChMate data."""
import json
import pathlib
import sys
import time

sys.path.insert(0, str(pathlib.Path('output/frida-runtime').resolve()))
import frida
import frida_tools

device = frida.get_device('emulator-5554', timeout=10)
session = device.attach('app.morphe.extension.chmate')
bridge = pathlib.Path(frida_tools.__file__).parent / 'bridges' / 'java.js'
source = bridge.read_text(encoding='utf-8') + '\nconst Java = bridge;\n' + r'''
rpc.exports = {
  check: function (mode) {
    return new Promise(function(resolve, reject) {
      Java.perform(function() {
        Java.scheduleOnMainThread(function() {
          try {
            const Search = Java.use('app.morphe.extension.chmate.HissiWacchoiSearchActivity');
            const records = Java.use('android.app.ActivityThread').currentActivityThread()
              .mActivities.value.values().toArray();
            let act = null;
            for (const record of records) {
              const r = Java.cast(record, Java.use('android.app.ActivityThread$ActivityClientRecord'));
              if (String(r.activity.value.getClass().getName()).endsWith('HissiWacchoiSearchActivity'))
                act = Java.cast(r.activity.value, Search);
            }
            if (!act) throw Error('Search Activity not running');
            function text(field, value) {
              Java.cast(act[field].value, Java.use('android.widget.TextView')).setText
                .overload('java.lang.CharSequence').call(act[field].value,
                  Java.use('java.lang.String').$new(value));
            }
            if (mode === 'toggleBoard') {
              const choices = act.boardChoices.value;
              let target = null;
              for (let i = 0; i < choices.getChildCount(); i++) {
                const child = Java.cast(choices.getChildAt(i), Java.use('android.widget.CheckBox'));
                if (String(child.getTag()) === 'android') { target = child; break; }
              }
              if (!target) throw Error('Android checkbox missing');
              target.performClick();
            } else if (mode === 'foldout') {
              const initial = act.boardSection.value.getVisibility();
              act.boardToggle.value.performClick();
              const opened = act.boardSection.value.getVisibility();
              act.boardToggle.value.performClick();
              const restored = act.boardSection.value.getVisibility();
              if (initial !== 8 || opened !== 0 || restored !== initial)
                throw Error('Board foldout state mismatch: ' + [initial, opened, restored]);
            } else if (mode === 'multiBoard') {
              const entries = act.availableBoards.value.toArray();
              const Board = Java.use('app.morphe.extension.chmate.HissiWacchoiSearchActivity$BoardEntry');
              let other = null;
              for (const entry of entries) {
                const board = Java.cast(entry, Board);
                if (String(board.code.value) !== 'android') { other = String(board.code.value); break; }
              }
              if (!other) throw Error('Second board unavailable');
              const before = act.selectedBoards.value.size();
              text('boardFilter', other);
              let target = null;
              for (let i = 0; i < act.boardChoices.value.getChildCount(); i++) {
                const child = Java.cast(act.boardChoices.value.getChildAt(i), Java.use('android.widget.CheckBox'));
                if (String(child.getTag()) === other) { target = child; break; }
              }
              if (!target) throw Error('Filtered board checkbox missing: ' + other);
              target.performClick();
              const selected = act.selectedBoards.value.size();
              target.performClick();
              const restored = act.selectedBoards.value.size();
              if (selected !== before + 1 || restored !== before)
                throw Error('Board selection count mismatch: ' + [before, selected, restored]);
            } else if (mode === 'calendar') {
              act.fromInput.value.performClick();
            } else if (mode === 'startLower' || mode === 'startUpper') {
              if (act.job.value && !act.job.value.isDone()) throw Error('Search still running');
              text('boardFilter', 'android'); text('fromInput', '20261016');
              text('toInput', '20261016'); text('pageLimitInput', '1');
              text('fourInput', mode === 'startUpper' ? 'Sdff' : 'Q8ld');
              Java.cast(act.selectedBoards.value, Java.use('java.util.Set'))
                .add(Java.use('java.lang.String').$new('android'));
              const radio = act.halfGroup.value;
              radio.check(radio.getChildAt(mode === 'startUpper' ? 0 : 1).getId());
              const method = Search.class.getDeclaredMethod('startSearch', Java.array('java.lang.Class', []));
              method.setAccessible(true); method.invoke(act, Java.array('java.lang.Object', []));
            } else if (mode === 'resume' || mode === 'stop') {
              act[mode === 'resume' ? 'resumeButton' : 'stopButton'].value.performClick();
            }
            const results = [];
            const layout = act.resultsLayout.value;
            for (let i = 0; i < layout.getChildCount(); i++) {
              results.push(String(Java.cast(layout.getChildAt(i), Java.use('android.widget.TextView')).getText()));
            }
            resolve({mode, done: !act.job.value || act.job.value.isDone(),
              status: String(act.statusView.value.getText()), results,
              canResume: act.resumeButton.value.isEnabled(),
              boardCount: act.availableBoards.value.size(),
              selectedBoardCount: act.selectedBoards.value.size(),
              boardSectionVisibility: act.boardSection.value.getVisibility(),
              boardToggleText: String(act.boardToggle.value.getText()),
              androidBoardSelected: act.selectedBoards.value.contains(Java.use('java.lang.String').$new('android')),
              startDateFocusable: act.fromInput.value.isFocusable(),
              startDateClickable: act.fromInput.value.isClickable(),
              toolbarShortcutMode: String(act.getIntent().getStringExtra('app.morphe.extension.chmate.WACCHOI_HALF'))});
          } catch (e) {
            const detail = String(e.stack || e);
            send({verificationError: detail});
            reject(new Error(detail));
          }
        });
      });
    });
  }
};
'''
script = session.create_script(source)
script.on('message', lambda m, _: print(json.dumps(m, ensure_ascii=False), flush=True))
script.load()
mode = sys.argv[1] if len(sys.argv) > 1 else 'inspect'
try:
    print(json.dumps(script.exports_sync.check(mode), ensure_ascii=False), flush=True)
    if mode.startswith('start') or mode == 'resume':
        deadline = time.monotonic() + 100
        while time.monotonic() < deadline:
            time.sleep(5)
            state = script.exports_sync.check('inspect')
            print(json.dumps(state, ensure_ascii=False), flush=True)
            if state['done']:
                break
finally:
    session.detach()
