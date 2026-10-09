# 241 / 242 dev: スレ一覧ツールバーの未読合計

## 原因

既読数管理の板別 SharedFlow は変更のたびに Unit を通知する。
ThreadListViewModel の未読合計は、これを初期値 Unit の stateIn に変換して
combine していた。StateFlow の同値抑制により、既読数だけの変更が届かない。
ブックマーク一覧など別の入力が変わると再計算されるため、常時は再現しない。

## 修正

未読合計への入力だけを ReadCountEvents でラップし、通知ごとに異なる
identity token を渡す。初期値と元の coroutine continuation は保持する。
combine の使われていない通知引数に対する Unit キャストだけを除去する。
他の既読通知購読者・未読算出条件・永続データ・並び順には変更を加えない。
対象は 0.8.10.241 / 0.8.10.242 dev のみ。

## 検証（2026-10-09、Mi Note 10）

一時的にメモリ上の既読数を1減らし、通知・再計算値・表示用 StateFlow を比較。
その後は必ず元の値に戻した。DB を直接書き換える検証ではない。

| APK | 再計算値 | ツールバー用の値 | 復元後 |
| --- | --- | --- | --- |
| 公開時の 242 dev / 1.7.7 | 0 → 1 | 0 → 0（通知が落ちる） | 0 / 0 |
| 修正版 242 dev | 0 → 1 | 0 → 1 | 0 / 0 |
| 修正版 241 | 0 → 1 | 0 → 1 | 0 / 0 |

両版の CLI パッチ適用成功、実機起動・スレ一覧表示成功。
Java 回帰テストで連続した Unit 通知の区別、他の購読者の非変更、
continuation / suspend 戻り値の維持、例外伝播を確認。

```powershell
javac -encoding UTF-8 -d build/verification/unread-count extensions/chmate/src/main/java/app/morphe/extension/chmate/ReadCountEvents.java
java -cp build/verification/unread-count scripts/VerifyReadCountEvents.java
```

実機追跡スクリプト: `scripts/trace-unread-count.js`。
公開済み 1.7.7 のアセットやメタデータは変更していない。
