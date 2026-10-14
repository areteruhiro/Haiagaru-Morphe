# 242 Talk投稿NPEの再現確認

2026-10-14、root対応API 36 (`emulator-5558`) の `HaiagaruTalk242` で確認。
通常ログには出ない、投稿画面が捕捉して表示するNullPointerException。
Fridaで既存の例外オブジェクトのstackを読み取り、その後ユーザーの投稿許可を受けて
リトライで同じstackを再確認した。投稿成功は確認していない。

```text
o.success.d(:406)
o.success.b(:47)
java.lang.reflect.Method.invoke(Native Method)
o.PAGSdk71.d(:61)
o.PAGSdk71.d(:31)
o.getTopCountDown.b(:477)
o.getBackImage.a(:299)
o.attachInfo.b(:63)
ResEditFragment$write$RemoteActionCompatParcelizer.invokeSuspend(:1484)
```

`o.success` はInMemoryDexClassLoaderからロードされる。
認証メソッドは `b(o.zzcvr, String, String, long): String`。
NPEはその内部の `d(String): String` で発生。
`PAGSdk71.d(getBackImage)` の認証リフレクション呼び出しは修正前の242パッチの
`patchModernTalkPostIntegrity("Lo/getTopCountDown;")` の対象ではない。
このフックは投稿側の戻り値を使わない呼び出しに限定される。

過去の242でのUPLIFT認証NPE (`getAdLogo`) の修正とは別経路。
241には `patchIoTalkAuthentication()` / `invokeIoTalkAuth()` があるが、
それを242の難読化クラス名へそのまま適用してよいとはまだ確認していない。
生成DEXをロード直前に取得し、署名依存の比較不一致による `throw null` と確認した。

追跡スクリプト: `scripts/frida-talk242-npe.js`。引数・本文・token・Cookieは記録しない。
## 修正

- 認証4箇所・投稿トークン5箇所の反射呼び出しを互換ラッパーへ置換。
- 242の生成クラス `o.success` の呼び出し中のみ、ThreadLocalで補正を有効化。
- `o.GoogleApiSettingsBuilder.d` の配列 `[0]実値 / [2]期待値 / [3]演算seed` を補正。
- 生成キャッシュ `o.GNk4.e` を毎回無効化し、ランダムseedになるwarm経路を避ける。
- 242用seed5704は、CPU時間の境界値と乱数100003件で除数1594・substring位置12を確認。
- 正確な認証シグネチャだけ秒をミリ秒へ戻す。生成側が再び1000で割るため。
- 243の直接digest呼び出しは既知の生成クラスの `c(String)` に限定。
  対象外の1引数Stringメソッドは通常の反射呼び出しを維持する。

## 検証

JVMの242/243回帰テスト、MPPビルド、242への適用が成功。
APK内の認証4箇所・投稿5箇所・integrity return4箇所をDEXから検証。
初回APKの投稿確認で、対象外の呼び出しが243向け処理へ入ることを検出し、上記限定を追加。
修正版 `chmate242-talk-fixed-v2.apk` を同じパッケージに上書きインストールし、
Fridaを切断した状態でTalkの書き込み確認画面を経て投稿成功。
対象スレ `https://talk.jp/boards/operation/1736158664/` のレス数が613から614へ増加し、
末尾にテスト本文が表示されることをUIから確認。NPEおよびクラッシュログの新規出力なし。
実行環境はroot対応API36エミュレータであり、物理端末・連続投稿は未確認。
