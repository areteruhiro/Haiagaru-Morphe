# 投稿確認ページと軽量版の5ch.io対応（2026-10-26）

ローカルの主開発先で実装。公開・pushなし。

## 投稿確認

krsw等のサーバーが返した計算認証/CAPTCHAのHTMLを、一時メモリーに保持し元のHTTPS originと文字コードでWebViewへ表示する。元フォーム/hidden値/スクリプトを保持し、ユーザーがサイトのボタンを操作する。計算開始・CAPTCHA解答・投稿は自動実行しない。HTMLは最大64KiB/4ページ/5分、Intentや設定には本文を保存しない。Cookieは既存native jarから同一originだけ引き継ぐ。

送信許可は初期OFF、異originへのmain-frame POSTは停止。元の書き込みがサイト側で送信される場合があるため、画面と遷移確認で案内する。

実機スクリーンショットで「ERROR: ブラウザ変ですよん」「フォーム情報が正しく読めないです。」を確認。先行説明では表示崩れと取り違えたが、サイトのエラー本文だった。原因となる経路として、CAPTCHAのサードパーティCookie許可後にweb.reloadでbbs.cgiへGETし、フォームのないページへ進む不備を特定。Cookie許可と再表示ボタンは共通reloadPageへ統一し、確認モードでは捕捉したHTMLを再表示する。POST再送はしない。

上部操作欄25%制限でチェックが隠れる問題も対処。認証画面だけ最大55%とし、長い説明とCookie設定を開閉式へ整理、戻る/再表示ボタンの幅を均等化する。他のブラウザ画面の配分は変更しない。

## 軽量版

「Haiagaru 広告だけの超軽量構成（検証用）」へnet→io互換を常時有効で追加。保存済みmenu.5ch.netをHTTPSのmenu.5ch.ioへ変換し、板一覧URL1未設定時の初期値をhttps://menu.5ch.io/bbsmenu.htmlにする。保存済みの別メニューは書換えない。191/226のURLモデル入出力と5ch固定endpoint/ドメイン判定は既存の共通変換を使う。241/242/243は元のio対応を使いメニュー経路を補う。通常版のchtoio設定と保存値は変更しない。

軽量版へ通常版の設定・Compose・検索・計算認証機能は持ち込まない。

## 検証

- host: 投稿確認HTML/有効期限/板隔離/文字コード39ケース、外部板URL判定70ケース成功。
- 実サイトの認証完了・投稿成功は未確認。Cloudflare等のWebView対応も保証しない。
- 新しい端末/候補のビルド・導入・実機試験結果は完了後に追記。

軽量版191: 通常Lint付きAndroid build成功（build/browser-lite-io-build-retry.log、6m43s）。完成APKで軽量runtime31クラス/外部hook299参照解決・ioメニュー初期URL・通常runtime非混入を確認。Mi Note10の既存adsonly191へ署名一致install-r成功、cold起動、View/メニュー/スレURL/別メニュー保持の10チェック成功。226/241/242/243の今回候補適用は未実施。台帳参照。

投稿確認最終候補: 通常Lint付きbuild成功（build/board-form-reload-final-build.log、3m42s）、191APK適用/再構築/署名成功。Mi Note10/Android11/serial d5497008のsettingsfix191へ既存署名一致install-r成功、起動Status ok。先に表示されたChMate初回規約画面をUI操作して開始。Android11ではテストのJava.choose(Activity)がActivityを拾わずHome unavailableになったため、ActivityThreadのActivityClientRecord列挙へ試験コードを修正した。本番コードの認証失敗ではない。

実機オフラインfixture全12チェック成功（build/board-form-reload-fixture.log）: HTTPS origin、開いただけでは計算/送信しない、Worker利用/手動ボタン実行、Shift_JIS、計算完了でも投稿しない、DOM確認、実WebViewのfetch POST未許可時遮断、明示許可/異origin防止、設定維持、再表示で元フォーム復元、終了時メモリー破棄。実CAPTCHA/掲示板への送信はfixtureに含まない。以前のcleanup失敗は実画面とfixtureのActivity選択が混在する試験コードを修正後に解消。

ユーザーは指定krsw/1771588017とrid/1729422449へ本文「てすや」の実投稿検証を指示。指定krswを検証アプリで開くADB am startコマンドは自動承認審査にblocked by policyで拒否、理由詳細なし。別経路で同操作を再試行せず、ユーザーへ実機でスレを開くよう依頼。実サイトの計算認証完了・CAPTCHA完了・投稿成功は現時点で未確認、ゴール未完了。CAPTCHAの人間確認はユーザーによる操作が必要。
