# 242 Talkサムネイルのクリック補正

報告URL: https://talk.jp/boards/operation/1736158664/49

49レス目の画像URL:

- https://i.imgur.com/6GuDq9K.jpeg
- https://i.imgur.com/RMMv1zJ.jpeg

242のレス行 `zzbhe.onClick` は、表示中の子サムネイルの位置を、レスモデル `KeJ11.i()` が返す配列へそのまま適用します。配列と表示がずれた場合、範囲外例外を捕捉して何も開かず終了します。

補正は、この配列を取得した直後に入れます。押されたサムネイルのURLが同じ位置にある場合は元配列を維持し、違う場合に限り現在の子サムネイルのURL順へ合わせます。元配列にある動画等の型付き項目は同じオブジェクトを保持します。元配列にないURLを補完するのはHTTP(S)の画像ファイルだけです。

読み込み完了・NG画像保護・画像ビューアの起動方法・一覧表示設定は元の処理を通します。Talk以外も同じ242の行クリック処理を使用しますが、配列にずれがない場合は変更しません。

`java scripts/VerifyThumbnailClickAttachments.java` で通常経路、範囲外、順序違い、型付きメディア維持、不明な行のフォールバックを検証します。

## 2026-10-14: 無反応の実原因をエミュレータで確認

242のTalkスレの画像サムネイルで無反応を再現。Fridaで、ロード済み・選択index0・
表示画像1件・URL配列1件・ビューア起動呼び出し済みを確認した。
配列不一致はこの再現の原因ではなかった。

画像Fragment `o.VN10` 自体は追加済みだったが、親の
`androidx.fragment.app.FragmentContainerView` が `GONE` / layout height0になっていた。
Talk余白除去の `collapseTalkBlankRows` が、起動時に空のビューア用Fragment領域を
「大きい子なしViewGroup」として広告余白扱いしていた。
Fridaで親領域の高さをMATCH_PARENT、visibilityをVISIBLEへ戻すと画像が表示された。

修正はFragmentContainerView（その派生クラスを含む）とActivity content rootを保護する。
空行検出から除外し、広告の親領域への遡及・empty判定・最終collapseにも安全チェックを入れる。
通常の広告枠は引き続き非表示にし、画像ビューアは元のChMate実装のまま使用する。

`scripts/ad-container/VerifyAdContainerSafety.java` は画面領域保護と通常広告枠の非保護を検証。
恒久修正版 `build/verification/image-rotation/chmate242-talk-thumbnail-fixed.apk` を
同じパッケージへ上書きインストール。Fridaを終了し、アプリ再起動から検証した。

- Talkアニメスレ690の画像サムネイルから `1/1` の画像ビューアが表示。
- 戻る→再タップでも画像表示が成功。
- 報告URLの49レス目をChMate内検索から開き、1枚目のタップで `1/2`・1080x1863、
  2枚目のタップで `2/2`・1080x1847 の画像が表示。
- AndroidRuntimeの新規クラッシュ出力なし。
- MPPビルド、画面領域保護テスト、既存URL配列テスト、APKの画像クリックフック検証が成功。
- 先に修正したTalk投稿フック（認証4・投稿5・integrity return4）もAPK内に残存。

検証環境はAPI36 rootエミュレータ。物理端末は未接続のため未確認。
