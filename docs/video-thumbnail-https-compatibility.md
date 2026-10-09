# HTTPS切り替えと動画サムネイル

`video.twimg.com` の動画はHTTPSで取得するが、生成した先頭フレームと仮サムネイルは
ChMateの画像ダウンローダーへ `http://127.0.0.1:<port>/...` で渡す。
この端末内HTTP配信をHTTPSへ書き換えると、HTTP専用サーバーとのTLS接続が失敗する。

`VideoThumbnailServer` 起動時に実際の待受ポートを `HttpsTransport` に登録し、
**127.0.0.1とそのポートの組み合わせだけ**を変換対象から除外する。
外部のHTTP、別のローカルポート、既存HTTPS、証明書検証の扱いは変更しない。

java.netの両openConnection経路とopenStream、各版のOkHttp URLビルダーに同じ例外を適用する。
OkHttpフックはscheme・hostのnullチェックとportフィールドを検証し、
小さいSDKビルダーで作業レジスタとp0が重なっても、元の処理へ戻る前にp0を復元する。

再現URL（報告スレの277レス目）:
https://video.twimg.com/ext_tw_video/2104908394507411457/pu/vid/avc1/720x960/x6cfbDt22DoPXS7l.mp4

`scripts/VerifyHttpsTransport.java` は外部通信を行わず、HTTPS切り替えONでの
端末内サムネイル取得、別ホスト・別ポートの除外拒否、既存のURL変換を検証する。

## 検証結果

- 上記テスト42項目成功。
- 191 dev／226 dev／241／242 devへのパッチ適用・APK生成成功。
- Mi Note 10の242 devでHTTPS ONの端末内サムネイル取得がHTTP 200、PNG先頭バイトも確認。
- 実機でOkHttpビルダーの端末内HTTP維持・外部HTTPS化を確認。
- p0が作業レジスタと重なる3レジスタSDKビルダーも、両分岐で正しく動作。
- 報告された外部動画URLへの実機HTTPS HEADリクエストは200。
- 検証用に変えたHTTPSフラグはRAM上だけで、終了時に元へ復元。ユーザー設定は変更していない。
