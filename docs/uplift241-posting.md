# ChMate 241 UPLIFT投稿・スレッド作成の補正

## 原因と過去の修正

`f12bf715` で追加された241向けUPLIFT修正は、トークン期限の除算と
ログイン成功時の診断メッセージのsubstring位置を対象としていた。
しかし、`Lo/VLj;` の以下の署名依存比較が残っていた。

- `c(ComponentActivity): String`：トークン取得前。比較不一致で巨大配列の確保や不正indexを使用する分岐へ進む。
- `c(ComponentActivity, getPaint, long): String`：実際のログイン前。比較不一致で意図的なnull throwへ進む。

191／226の `patchUpliftIntegrityComparison` と同じ、int配列を2つ読む直後の
IF_NEだけを241にも補正する。対象シグネチャごとに比較が1箇所であることを検証する。
242のクラス名やレジスタ番号は流用しない。

さらに、以前の診断メッセージ修正はsubstringの直前で引数を2へ戻すだけだった。
その前に実行される `7120 / 署名依存値` が0除算になる余地が残っていたため、
除算命令そのものを `const/4 ..., 2` に置換する。分子と出力レジスタも照合する。

## 対象経路

241の `VLj` には、上記トークン取得関数への呼び出しが2箇所ある。
通常の5ch投稿リクエストを構築する経路と、投稿URLの `${sid}` を埋める経路である。
書き込み・スレッド作成とも、元のChMateの投稿処理・サーバー側認証は維持する。
UPLIFTの実ログイン `setDownloadListener.c(String,String,String)`、成功トークン保存、
認証失敗時の処理を置換しない。Talk用の認証ラッパーとは別の修正。

## 検証

- `:patches:buildAndroid` 成功。
- 原版241に `VerifyUplift241` を実行すると、残存比較の検出で失敗する（修正前の再現）。
- 修正版を241へ適用し、生成APKのDEXで2箇所の比較、期限の除算、診断用の除算、
  共有呼び出しと本来のログイン呼び出しを検査する。

```powershell
javac -cp C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar -d build/verification scripts/VerifyUplift241.java
java -cp 'build/verification;C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar' VerifyUplift241 build/verification/chmate241-uplift-fixed.apk
```

UPLIFTアカウントでの実投稿・実スレッド作成は未確認。
端末データの削除、アカウント情報の記録、コミット・公開は行っていない。
