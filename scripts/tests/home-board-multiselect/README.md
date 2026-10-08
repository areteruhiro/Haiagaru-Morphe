# Board-group selection regression test

From the repository root, with JDK 21 on PATH:

```powershell
javac -d build/verification/board-select-test/classes scripts/tests/home-board-multiselect/android/util/Log.java scripts/tests/home-board-multiselect/android/widget/ListView.java extensions/chmate/src/main/java/app/morphe/extension/chmate/HomeBoardMultiSelect.java scripts/tests/home-board-multiselect/BoardSelectionTest.java
java -cp build/verification/board-select-test/classes o.BoardSelectionTest
```

The fixtures mirror the verified 241/242 board-header and bookmark-ID fields.
They check full selection, deselection, partial selection, preservation of other
selections, and non-interference with normal board/thread taps. UI IDs deliberately
differ from bookmark IDs. This JVM test does not replace APK application or device
testing. The Android logger stub is test-only and must not be packaged in the app.
