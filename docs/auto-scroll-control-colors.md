# 自動スクロールの速度ボタンのテーマ色（1.8.6プレリリース）

226 dev / 241 / 242 dev の自動スクロール中に表示される「−／＋」が、
暗い背景でも黒くなる問題への修正。191 dev の既存表示は変更しない。

## 修正

ネイティブComposeアイコンの既定tintを無効化し、ChMateの現在のテーマの
`textPrimaryColor`を明示的に渡す。ARGBをsRGBのCompose Color（上位32ビット）へ変換。
固定の白色ではないため、明るいテーマではそのテーマの文字色になる。
速度・操作・長押しリピート・自動リロード・レイアウトのコードは変更しない。

| ChMate | アイコン | 現在のテーマ取得 | textPrimaryColor |
| --- | --- | --- | --- |
| 226 dev | `o.getChannel` | `o.addConnectException.b(Context)` | `bv` |
| 241 | `o.QWA11` | `o.getPageMargin.a(Context)` | `bs` |
| 242 dev | `o.AdChoicesView` | `o.onTextChanged.a(Context)` | `bq` |

適用時にはUp/Downの2箇所、テーマJSONのキーとフィールドの対応、
アイコンAPIの引数と既定マスクを照合し、不一致なら失敗する。

## 検証

- `:patches:buildAndroid`成功。
- 226 / 241 / 242の全APK生成成功。
- `VerifyAutoScrollColors`が全3版で成功。最終DEXの2アイコン、テーマフィールド、
  Colorのビット配置、既定tint無効化を確認。
- 分離アプリの`frida-auto-scroll-colors-fixture.js`で、各版の実ネイティブアイコンを
  灰色・黒の背景に表示。テーマオブジェクトの文字色を一時的に`#e0e0e0`へ設定し、
  合計4描画／版とスクリーンショットを確認。設定ファイルの書き換え・掲示板投稿はない。
  通常のスレ画面での速度調整・長押しの通し試験は未実施。
- 検証初期にFridaの全体deoptimizeを使用した242はANRになったため終了。
  この操作を外し、新しいプロセスでの226 / 241 / 242の描画が成功した。
  検証用Activity取得の起動タイミング、Javaコールバック戻り値の誤りも検証コード側で修正。
- ログは`build/verification/verify-auto-scroll-colors.log`、
  `auto-scroll-render226.log`、`auto-scroll-render241-clean.log`、`auto-scroll-render242-final-pass.log`。
  画像は`build/verification/auto-scroll-colors{226,241,242}.png`。
- ローカルMPPのみ更新。公開バンドル・バージョン・リリースは変更していない。
