# Changelog

> Haiagaru for Morphe
> Format inspired by Morphe patches CHANGELOG.
> 公開済みGitHub Releasesと照合し、プレリリースを含む履歴を補完しています。
> 各版の記載は当時の変更内容です。後続版の修正や現在の動作保証とは区別してください。

## 1.8.8 (2026-10-17)

**Channel:** Prerelease

* **ChMate:** 対応対象を191 dev／226 dev／242 devに整理。241と243 devをパッチの対応一覧から除外。
* **ChMate:** Haiagaru設定を10カテゴリに分類し、項目名・説明の検索、全開閉に対応。既存の設定値・保存キーは維持。
* **ChMate:** 標準の画像アップロード履歴の︙に「新しい順（降順）」と、選択・確認付きの「サーバーから一括削除」を追加。サーバー削除は取り消せません。
* **ChMate:** 画像キャッシュサイズに4／8／16／32／64／128／256GBを追加。空き容量・標準のキャッシュ整理処理は引き続き適用。
* **ChMate:** スレタイの複数キーワード・URLで絞り込む巡回画像自動保存を追加（初期OFF）。標準巡回の本文取得に連動し、画像キャッシュ／Downloadのスレ別フォルダを選択可能。242のURL取得結合先も実行検証で補正。
* **ChMate:** Android標準のファイル選択から自作テーマを複数取り込み可能に。画像素材付きフォルダと同名ファイルの置換確認に対応。Android/dataの手動操作・全ファイルアクセス権限は不要。
* **ChMate:** 動画専用ビューアに左上下スワイプの明るさ、右上下スワイプの音量、左右ダブルタップの巻き戻し／早送りを追加（各操作は初期OFF）。YouTubeは公式埋め込みプレーヤーでのアプリ内表示に対応。システムの戻るは1回で終了。
* **ChMate:** X／Twitterの公開投稿URLから投稿者・本文・日時をレス内に追加表示する任意設定を追加（初期OFF・追加通信あり）。元DATやNG判定は変更しません。
* **ChMate:** スレ立て画面に掲示板別の編集可能なコマンド候補を追加。5ch／エッヂを自動判定し、選択内容を本文へ挿入。自動送信はしません。
* **ChMate:** ワッチョイ長押しに「下4桁で検索」を追加。226のNG編集ジェスチャー名表示と、右上の「非表示NGを表示」を隠す任意設定も追加。
* **ChMate:** エッヂ過去ログ検索の検索欄・検索結果の文字倍率を個別設定可能に。検索欄を折りたたんで結果を広く表示する設定を追加。
* **ChMate:** レス単位アーカイブの保存・一覧・オフライン閲覧を追加。従来のスレ全体保存は維持。
* **ChMate:** ローカル／MEGAのバックアップ対象にDAT、レス・スレアーカイブ、フォント・テーマ・追加設定・標準画像アップロード履歴を追加。ZIP全体の32MiB上限を撤廃し、順次読み書き。旧JSON復元も維持。
* **ChMate:** MEGA復元元の一覧選択、端末ごとの上書き、照合成功後の旧ファイル完全削除オプション（初期OFF）を追加。ファイル名・一覧日時は端末のタイムゾーンで表示。
* **ChMate:** 認証情報・通常WebView Cookie・MEGAセッションと、同一インストール専用の暗号化済みデータを別々の初期OFF項目として追加。認証情報を含むバックアップは厳重な保管が必要。Keystore秘密鍵は保存しません。
* **ChMate:** 191 devの板・スレ取得で、接続できない先頭IPの待ちを避けるTCP競合接続（約250ms後に別候補・初期ON）と、進捗通知の軽量化・任意の通信全体上限を追加。報告された停止の解消や一定の取得時間を保証するものではありません。
* **ChMate:** 詳細・初期設定・検証範囲は[1.8.7からの更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/dev/docs/release-1.8.8.md)をご覧ください。開発途中の外部板認証・登録板編集は今回の公開に含みません。

## 1.8.7 (2026-10-16)

**Channel:** Prerelease

* **ChMate:** 191 devの本文一部選択からのWeb検索を別タスクで開くよう変更。検索先がHaiagaruのタスクに残る問題への対策。
* **ChMate:** 191／226 devの設定画面右端に出る区切り線と、226 devの上部フィルタ非表示が反映されない問題を修正。
* **ChMate:** 191／226／241／242のスレタイ行間を補正。105%以上で上側の余白だけが増える問題を修正。
* **ChMate:** Imgur投稿一覧を閉じる際の通信解放によるクラッシュ、動画処理完了判定を修正。標準アップロード履歴への保存とアップロード元スレの記録・表示をそれぞれオン／オフ可能に。
* **ChMate:** 動画専用ビューアを初期未選択のパッチへ変更し、ChMateのジェスチャー設定との連携を追加。
* **ChMate:** 詳細・検証範囲・注意事項は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.7.md)をご覧ください。

## 1.8.6 (2026-10-16)

**Channel:** Prerelease

* **ChMate:** 動画専用ビューアを追加。再生・シーク、画面タップによる操作パネルの表示切替、︙から外部アプリ起動・URLコピー・再読込・回転に対応。設定でオン／オフ可能。
* **ChMate:** Imgurアルバム内のMP4取得、独立した投稿一覧・サムネイル・URLコピー・確認付き選択削除、アップロード後の処理待ち時間設定を追加（Imgurログインパッチ選択時）。
* **ChMate:** Google認証からImgurへの復帰とセッションCookie更新を改善。ジェスチャーにNG編集を追加し、設定画面の戻る動作・自動スクロールボタンの視認性を修正。
* **ChMate:** MEGA同期を初期未選択のパッチオプション化。Android 7.0以降が必要と明記し、ローカルバックアップ・復元は維持。
* **ChMate:** 詳細・検証範囲・注意事項は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.6.md)をご覧ください。

## 1.8.5 (2026-10-16)

**Channel:** Prerelease

* **ChMate:** 「書き込んだスレ」の表示数が242で10件に固定される問題を含め、191／226／241／242で件数設定を反映。0は無制限。
* **ChMate:** 書き込み履歴の保存数・無制限保存、閲覧スレ履歴の保存数と自動整理停止、最近見たスレの表示数、検索履歴の保存数を設定可能に。
* **ChMate:** 表示数と保存数は別設定。削除済み履歴の復元ではなく、無制限保存では容量・負荷増加に注意。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.5.md)をご覧ください。

## 1.8.4 (2026-10-16)

**Channel:** Prerelease

* **ChMate:** 初期未選択の「Imgurログインアップロード（開発中）」パッチを追加。アプリ内Webログインでアカウント送信し、失敗時に匿名送信へ切り替えない方式に。
* **ChMate:** 191 devの下4桁NG登録で通常タップが長押し用メニューに吸収される経路を追加修正。
* **ChMate:** 詳細・非公式Web方式の制約は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.4.md)をご覧ください。

## 1.8.3 (2026-10-16)

**Channel:** Prerelease

* **ChMate:** フィルタ切替時の表示位置維持を設定可能に（初期OFF）。抽出対象から外れた場合は近いレスへ移動。
* **ChMate:** NG対象板を「全板共通」「選択した板のみ」「選択した板以外すべて」から複数チェックで指定可能に。
* **ChMate:** 191 devの下4桁NG通常タップを修正。新しい複数板・除外板形式は旧パッチと互換性に注意。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.3.md)をご覧ください。

## 1.8.2 (2026-10-15)

**Channel:** Prerelease

* **ChMate:** ワッチョイ下4桁NGの通常タップで、文字列／正規表現・連鎖・非表示・登録板を選べる追加画面へ。長押しはコピーして標準NGName編集へ移動。
* **ChMate:** 226／242などの長押し経路と191のメニューレジスタ処理を修正。既存正規表現への追記とメニュー順のNG優先設定を追加。
* **ChMate:** NGName統合候補が多いと確認ボタンが画面外へ押し出される問題を修正。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.8.2.md)をご覧ください。

## 1.8.1 (2026-10-15)

**Channel:** Prerelease

* **ChMate:** 文字列登録されたワッチョイが下4桁NGName統合の候補に出ない問題と、保存する検索方式フラグを修正。
* **ChMate:** スレ一覧下部のNGグループを非表示にする設定、241のUPLIFT投稿・スレ作成経路の署名依存計算と0除算への補正を追加。
* **ChMate:** 191の標準ZIP復元で画面再表示時の再展開・処理中断を改善し、空き容量・DB検査・取り込み失敗時の保持を追加。報告された18GBバックアップの移行完了は未検証。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.8.1)をご覧ください。

## 1.8.0 (2026-10-14)

**Channel:** Prerelease

* **ChMate:** 登録NGの検索・板別表示・ワッチョイ下4桁統合と、下4桁をパイプ付きでコピーしてNG編集する機能を追加。
* **ChMate:** 自動リロードの秒数表示、画像回転ボタン位置・オン／オフ、過去ログからのタブレット表示切替、Haiagaru設定の言語選択を追加。
* **ChMate:** Imgurサムネイル重複、X画像URL、242のTalk投稿・サムネイル、191タブレットのツールバー設定、アイコン変更パスを修正。
* **ChMate:** 初期未選択の画像取得専用VPNを追加（実験用・検証中）。全経路の通信漏れ検証は未完了で、IP保護・匿名性は保証しません。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.8.0)をご覧ください。

## 1.7.91 (2026-10-09)

**Channel:** Prerelease

* **ChMate:** 191／226／241／242で下4桁NG登録のクリック通知を補正。
* **ChMate:** フィルタの「画像」長押しによる画像一覧にも、解決済みのImgur・X・Instagram添付画像を追加。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/releases/1.7.91.md)をご覧ください。

## 1.7.9 (2026-10-09)

**Channel:** Prerelease

* **ChMate:** 書き込み画面の文字サイズ・任意サイズ指定と、投稿用端末情報へのHaiagaruバージョン追加に対応。
* **ChMate:** 241／242の自動リロードを現在位置からのスクロール・末尾待機へ変更し、秒数指定を追加。
* **ChMate:** 標準Web表示への切替、不完全URLの添付取得、HTTPS化による動画サムネイル失敗、巨大画像の表示ずれ、191のCookie確認フォームを修正。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/releases/1.7.9.md)をご覧ください。

## 1.7.8 (2026-10-09)

**Channel:** Prerelease

* **ChMate:** 241／242 devで、未読スレを読んで一覧に戻っても左下の未読合計が残る問題を修正。
* **ChMate:** 既読変更の通知に追従して合計を再計算します。未読数を一律に0へ変更する処理ではありません。
* **ChMate:** 両バージョンで実機検証済み。詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.7.8.md)をご覧ください。

## 1.7.7 (2026-10-08)

**Channel:** Prerelease

* **ChMate:** Instagram／X／Imgurの添付メディア取得、スレタイの行間設定、ワッチョイ末尾4文字でのNGName登録を追加。
* **ChMate:** 191／226の画像回転ボタンとエッヂの必死チェッカーメニューを改善。
* **ChMate:** 詳細は[更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.7.7.md)をご覧ください。

## 1.7.6 (2026-10-08)

**Channel:** Prerelease

* **ChMate:** 191 dev／226 devのBEリンク補正で、レス内に同じドメインのURLが複数あると補正が効かない条件を修正。
* **ChMate:** 直前の見出しと次行のURLを対応させ、下線の範囲とリンク先を補正します。
* **ChMate:** 詳細は[更新内容](release-notes-1.7.6.md)をご覧ください。

## 1.7.5 (2026-10-08)

**Channel:** Prerelease

* **ChMate:** 226 dev／241／242 devで、見出しの直後から次の見出しまでをまとめて選択・解除する処理を修正。範囲外の選択は維持します。
* **ChMate:** 191 dev／226 devのBE表示補正で、URLの下線位置とリンク先が前の行の「2ch.net」にずれる問題を修正。
* **ChMate:** Android向けMPP。安定版は1.7.1のままです。詳細は[更新内容](release-notes-1.7.5.md)をご覧ください。

## 1.7.2 (2026-10-07)

**Channel:** Prerelease

* **ChMate:** 過去ログ検索の条件・結果・ページ・スクロール位置を保存し、検索結果からスレを開いた後の戻る動線を追加。
* **ChMate:** 241／242の板見出しによるまとめて選択、スレツールバーの過去ログ・未読0操作を追加。
* **ChMate:** 191／226のBEリンク・タブレット検索遷移、226の外部ブラウザ候補を補正。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.7.2)をご覧ください。

## 1.7.1 (2026-10-06)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** 専用必死チェッカービュワーをOFFにした場合、ChMate内WebViewへ固定せずAndroidの通常URL処理に戻すよう修正
* **ChMate:** ChMate 191 devでTalkの旧形式スレURLから書き込みフォームを開けない問題を修正
* **ChMate:** Talk読み込み中だけ変更する通信種別を読み込み後に復元し、書き込み先判定への影響を防止
* **ChMate:** エッヂ記者IDを含むスレタイの記号処理を修正

### 📝 Notes

* **ChMate:** Android向けMorphe MPPを配布します。
* **ChMate:** 詳細は[1.7.1の更新内容](release-notes-1.7.1.md)をご覧ください。

## 1.7.0 (2026-10-05)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** 191／226のBEアイコンとリンク下線のずれを修正

### ✨ New Features

* **ChMate:** MEGA／ローカルのバックアップ・復元、項目選択、起動時の同期と復元前の変更確認を追加
* **ChMate:** 191／226／241／242に外部TXTによる本文の文字列置換を追加（初期状態OFF）

### ♻️ Improvements

* **ChMate:** hissi.org／Kyodemo専用ビュワー、外部板・ワッチョイ検索、コピー・配色・ジェスチャーを改善
* **ChMate:** エッヂ過去ログ検索、ツールバーのフィルター、未読・新着バッジ、投稿・起動互換処理を改善

### 📝 Notes

* **ChMate:** 1.5.2以降の先行版で追加・改善した機能をまとめた正式版です。
* **ChMate:** 詳細は[1.7.0の更新内容](release-notes-1.7.0.md)をご覧ください。
* **ChMate:** 更新時は元のChMate APKにパッチを適用し、事前にバックアップしてください。

## 1.6.11 (2026-10-05)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 任意の有効なパッケージ名でパッチできるよう検証条件を修正。アプリ名を変更すると起動時に`ClassNotFoundException`になる問題も修正
* **ChMate:** 191 devのフィルター行を隠した際に残る余白を抑制し、フィルター画面の「画像」長押しから画像一覧を開けるよう改善
* **ChMate:** 必死チェッカー／Kyodemoのインライン表示でスタイルが適用されないケースを修正

### ♻️ Improvements

* **ChMate:** クラス名復号に使うパッケージ名の文字・長さだけを純正ChMateの値に固定し、通常のパッケージ名参照への影響を限定

### 📝 Notes

* **ChMate:** ChMate 0.8.10.191 devでMorphe適用とXIG05での起動を確認しています。更新時は元のAPKへパッチを適用してください。

## 1.6.10 (2026-10-05)

**Channel:** Prerelease

* **ChMate:** 191 devでスレを開くとVerifyErrorでクラッシュする問題を修正。広告行補正がフィルタ命令を書き換える処理を改め、元の戻り値・引数を保持。
* **ChMate:** 226 devでも元の型チェックを保持することを検証。1.6.9の新着バッジ更新修正は維持。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.6.10)をご覧ください。

## 1.6.9 (2026-10-04)

**Channel:** Prerelease

* **ChMate:** 191／226／241／242で「未読をすべて0にする」後も残る新着レス数バッジを補正。DBだけでなくメモリ上の既読数も更新。
* **ChMate:** スレ一覧の画面再生成を避け、一覧が空になる問題への対策を維持。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.6.9)をご覧ください。

## 1.6.8 (2026-10-04)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** パッケージ名変更時はChMate本来のパッケージ名を先頭に含む値だけを受け付け、起動時のクラス名復号に起因するクラッシュを防ぎます。

### ♻️ Improvements

* **ChMate:** 「未読をすべて0にする」実行後に画面を再生成せず、開いているスレ一覧の状態を保つようにしました。

## 1.6.7 (2026-10-04)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 242 devで上部フィルタ行の高さを更新する際、未接続ComposeViewを直接計測してクラッシュする問題を修正します。行の高さを更新して親リストへ再レイアウトを依頼します。

## 1.6.6 (2026-10-04)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 5ch.io移行前のBEアイコンURL（`img.5ch.net`／`img.2ch.net`）を正規化し、レス表示で同じアイコンが重複しないよう補正します。

### ✨ New Features

* **ChMate:** 必死チェッカー専用ビュワー上部の「更新」「URL」「全レス」「本文」「日付」「ID/ﾜｯﾁｮｲ」「分析」「文字」「配色」を個別に表示・非表示に設定できます。初期状態ではすべて表示します。
* **ChMate:** 板のスレ一覧ツールバーにも「未読をすべて0にする」を追加します。

### ♻️ Improvements

* **ChMate:** 191／226／241／242で、上部フィルタを隠した後にListViewへ残る古い行高を更新し、設定を戻した場合は元の高さを復元します。
* **ChMate:** エッヂ過去ログ専用ビュワーに、専用ビュワーと同じ左右スワイプ設定を適用します。先頭ページで「戻る」操作をした場合はビュワーを閉じます。
* **ChMate:** エッヂのワッチョイ検索で `L20 abcd-EFGH` のような階層表記を検索文字列に含めます。
* **ChMate:** 「既読スレを上に」の対象を、すべての既読スレ／新着レスがある既読スレから選べるようにします。短いスレ一覧の上詰め表示もレイアウト後に位置を調整します。

## 1.6.5 (2026-10-04)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 板のスレ一覧に「既読スレを上に」を追加（191 dev／226 dev／241／242 dev）。241では履歴IDが0の既読スレも正しく並び替えるよう修正
* **ChMate:** 226 dev／241でワッチョイ長押し時に選択中のレスから検索文字列を取得する処理を修正
* **ChMate:** UPLIFTのログイン・書き込みに影響する再署名後の計算や分岐を修正（226 dev／241／242 dev／243 dev）
* **ChMate:** 191 devの自動NG画像判定クラッシュ向けに、パッチ適用時に無効化できる修正オプションを追加
* **ChMate:** 必死チェッカー専用ビューアでKyodemoの投稿／分析を切り替え可能に。エッヂ過去ログのエラー時は診断コードを表示し、`Download/Haiagaru`にログを保存
* **ChMate:** クラッシュログ保存パッチを初期状態で有効化（Morpheで無効化できます）

### ✨ New Features

* **ChMate:** 「未読をすべて0にする」をツールバーに追加。実行前に確認画面を表示し、レスや履歴は削除しません
* **ChMate:** レス数の少ないスレを上詰めで表示する設定を追加（241／242 dev／243 dev）
* **ChMate:** ツールバーのフィルタをチェックボックス式に変更し、ダイアログを開いたまま複数の項目を切り替え可能に。設定で元の上部フィルタ行だけを非表示にできます

## 1.6.4 (2026-10-02)

**Channel:** Preview

### 🐛 Bug Fixes

* **ChMate:** 242 devの画像添付でアップロード用一時ファイル名の計算がゼロ除算になる問題を修正
* **ChMate:** 226 devのスレ起動クラッシュとツールバーのフィルタを開けない問題を修正

### 🚀 Updated App Support

* **ChMate:** 191 devのエッヂ次スレ判定で、スレタイ末尾の記者IDを比較対象から除外

### 📝 Notes

* **ChMate:** 226 devのスレ起動は修正版で確認済みです。画像添付、フィルタ操作、エッヂ次スレの自動お気に入り追加は実機での最終確認が必要です。利用前のバックアップを推奨します。

## 1.6.3 (2026-10-02)

**Channel:** Preview

### 🐛 Bug Fixes

* **ChMate:** エッヂ過去ログ検索をツールバーから開く際、Activityを一時的に取得できない場合もアプリ内で起動するよう修正

### ♻️ Improvements

* **ChMate:** MEGAログインに必要な暗号化プロバイダをAndroid向けMPPに登録
* **ChMate:** 191 devのTalk認証キャッシュが1日後に失効しないよう変更

### 📝 Notes

* **ChMate:** MEGAログインとTalkのゼロ除算は実機での解消を未確認です。利用前のバックアップを推奨します。

## 1.6.2 (2026-10-02)

**Channel:** Preview

### ✨ New Features

* **ChMate:** 191 devのフィルタ行を非表示にしてもツールバーのフィルタを開けるよう改善
* **ChMate:** 191 dev／226 dev／241／242 dev／243 devで、設定OFFでもフィルタをツールバー設定に追加可能に変更
* **ChMate:** MEGAバックアップでCookieを個別に選択可能に（初期OFF。WebViewのCookieは対象外）
* **ChMate:** MEGAを使わず、端末のファイルへバックアップ・ファイルから復元する機能を追加

### ♻️ Improvements

* **ChMate:** 専用ビュワーで「戻る」スワイプ時に履歴がなければビュワーを閉じるよう変更

### 📝 Notes

* **ChMate:** フィルタ操作、バックアップ・復元は実機で未検証です。Cookieを含むローカルバックアップは暗号化されません。

## 1.6.1 (2026-10-02)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ID中の `+` が検索欄で空白に変わる問題と、エッヂ過去ログ検索画面がステータスバーに重なる問題を修正
* **ChMate:** 191 devのTalk投稿確認フォームで、改行を含む本文が失われる場合を修正

### ✨ New Features

* **ChMate:** スレの4種類のフィルタをツールバーの「フィルタ」ボタンにまとめる設定を追加（191 dev／242 devでは旧フィルタ行も非表示）
* **ChMate:** 必死チェッカー専用ビュワーに更新ボタンと、向きを選べる履歴スワイプ設定を追加

### ♻️ Improvements

* **ChMate:** ID・ﾜｯﾁｮｲの長押しメニューを拡張し、Kyodemo検索でﾜｯﾁｮｲ全体を引き継ぐよう改善

## 1.6.0 (2026-10-01)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 必死チェッカー専用ビューアーを戻る操作で閉じた後、遅延したレスコピー処理がクラッシュを起こさないよう修正
* **ChMate:** MEGA同期のAndroid実行時クラッシュを修正

### ✨ New Features

* **ChMate:** エッヂ過去ログ検索を板カテゴリ一覧・板のスレ一覧のツールバーにも追加。ツールバー用画像を選択可能
* **ChMate:** ID／ﾜｯﾁｮｲ検索でIDの引き継ぎと表記ゆれへの対応を改善

### 🚀 Updated App Support

* **ChMate:** ChMate 0.8.10.242 devへの対応を追加

## 1.5.9 (2026-09-30)

**Channel:** Pre-release

### ✨ New Features

* **ChMate:** MEGA同期に必要なHTTPエンジン登録を追加し、コルーチン実行環境をChMate本体から分離

## 1.5.8 (2026-09-30)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 191 dev／226 devの古いBEアイコンが改行をまたいで二重に描かれる問題を修正
* **ChMate:** Talk通信経路とエッヂの記者ID付きスレタイをコピーした際の表示を修正

### ✨ New Features

* **ChMate:** エッヂ過去ログをChMate内の検索画面から探せるようにし、ホームツールバーの編集項目にも追加
* **ChMate:** KyodemoのID／ﾜｯﾁｮｲ検索画面を追加（設定で有効化）。検索結果のレス・スレURLを元の掲示板で開く動作も改善
* **ChMate:** Hissiの暗色テーマで本文を読みやすくし、AA表示用フォントを追加

### ♻️ Improvements

* **ChMate:** パッチ時の全クラス探索を対象クラス指定に変更し、特に226 dev／241／243 devの適用負荷を軽減
* **ChMate:** 191 dev／226 dev／241／243 devへのパッチ適用を確認

## 1.5.7 (2026-09-28)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 双方向同期では既存の設定・履歴・NG・書き込みメモを保持し、不足分だけを追加

### ✨ New Features

* **ChMate:** MEGA同期に「MEGA → この端末」「この端末 → MEGA」の片方向同期を追加
* **ChMate:** 自動同期の間隔を分・時間・日で指定可能化。アプリ起動時だけ実行
* **ChMate:** 復元前の変更候補を表示し、表示の省略も設定可能

### ♻️ Improvements

* **ChMate:** MEGA側のバックアップが前回適用分より新しい場合だけ復元

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devに共通対応

## 1.5.6 (2026-09-28)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 古いレスのBEアイコンURLを現在の配信先に補正し、アイコンが表示されない問題を修正

### ♻️ Improvements

* **ChMate:** ID検索の自動選択でBBSPINKをhissi.orgへ接続し、両方表示モードでは板ごとに前回の検索先を記憶
* **ChMate:** hissi.orgのダークモードで文字と表を読みやすくし、スマホ縦持ち時の順位表・レス本文の横幅を調整
* **ChMate:** 専用ビューアを無効にしてもID長押しの検索メニューを残し、hissi.orgの結果をChMate内のWebViewで表示

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devに共通対応

## 1.5.5 (2026-09-28)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ダークモード端末でHissi／Kyodemoのレス本文が見にくくなる問題を修正
* **ChMate:** 外部板投稿時の絵文字結合子・異体字セレクタ補正を継続適用

### ✨ New Features

* **ChMate:** 「全レス」と「本文」のコピー内容を分離し、レス長押しからレス番号・名前・ID・本文・レスURL・ヘッダー＋本文・選択を個別にコピー可能化

### ♻️ Improvements

* **ChMate:** 専用ビューアを無効にした場合も、Hissi結果をChMate内のWebViewで開くよう改善

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devに共通対応

## 1.5.4 (2026-09-28)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 外部板投稿時の絵文字結合子・異体字セレクタ補正を追加

### ✨ New Features

* **ChMate:** Hissi／Kyodemo専用ビューアのレス長押しから、レス番号・名前・ID・本文・レスURL・ヘッダー＋本文を個別にコピー可能化

### ♻️ Improvements

* **ChMate:** 「選択」からレス本文の任意範囲を選択してコピー可能化

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devに共通対応

## 1.5.3 (2026-09-27)

**Channel:** Pre-release

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev`の標準「必死チェッカーもどき」メニューを、ChMate内の画面で開くよう変更
* **ChMate:** 既存のメニュー設定を維持し、5ch.ioの板でもメニューを表示

## 1.5.2 (2026-09-27)

**Channel:** Release / Pre-release

### 🐛 Bug Fixes

* **ChMate:** Android向けMPPを配布し、Morphe Managerで`Patch bundle is missing dex entries`となる問題を修正しました。

### ♻️ Improvements

* **ChMate:** パッチの機能は1.5.1と同じです。版番号を更新し、登録済みの1.5.1から更新できるようにしました。

## 1.5.1 (2026-09-27)

**Channel:** Release / Pre-release

### 🐛 Bug Fixes

* **ChMate:** 過去ログのレス番号付きURL（例: `/test/read.cgi/android/1744849408/3`）を、DAT取得後も保持して指定レスへ移動できるよう修正
* **ChMate:** `itest.5ch.io` への過去ログURL補正時に、レス番号・クエリ・フラグメントを維持
* **ChMate:** TalkのDAT変換でゼロ幅接合子（ZWJ）を含む複合絵文字のバリエーションセレクタを保持
* **ChMate:** Talk／過去ログのDAT取得完了時、タブレットの既存ペインを再利用して検索結果・スクロール位置・未読状態を保持
* **ChMate:** 226版では検証エラーを避けるため、レス行アダプタ経由で安全に適用

### ✨ New Features

* **ChMate:** Talkの名前・本文に含まれる端末未対応絵文字を同梱フォントで補完し、画面更新・行再利用後も再適用
* **ChMate:** 絵文字適用範囲を`missing`（未対応のみ）／`all`（全絵文字）／`off`から選択可能
* **ChMate:** パッチ時に任意のTTF/OTF絵文字フォントを指定可能

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev`に共通適用

### 📝 Notes

* **ChMate:** 過去ログURLを開いたときに先頭へ戻る場合があった問題と、タブレットモードでDAT取得後に開いていたタブが再読み込みされる問題を修正しました。
* **ChMate:** また、Talkの複合絵文字が崩れるケースを修正しています。プレリリースのため、既存の設定やバックアップを確認したうえでお試しください。

## 1.5.0 (2026-09-27)

**Channel:** Prerelease

* **ChMate:** 過去ログのレス番号付きURLと、itest補正時のレス番号・クエリ・フラグメントを保持。
* **ChMate:** Talkの複合絵文字を保持し、Talk／過去ログ取得完了時にタブレットの既存ペイン・検索結果・スクロール位置を維持。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.5.0)をご覧ください。

## 1.4.11 (2026-09-26)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 既読のEdgeスレッドで履歴を削除しなくても、NGThreadから記者IDを登録できるよう修正

### ♻️ Improvements

* **ChMate:** スレッド番号に加えて履歴タイトルの記者ID情報も保存し、NGThread操作時に復元

### 🚀 Updated App Support

* **ChMate:** ChMate 0.8.10.191 dev / 0.8.10.226 dev / 0.8.10.241 / 0.8.10.243 devに共通対応

## 1.4.10 (2026-09-25)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMateの「本文内の5chスレURLにスレ立て日を表示」でURLを板名・日付へ置換する削除範囲を補正
* **ChMate:** `hニュー速(嫌儲)/2026-09-14 23:29:15` のようにURL先頭の`h`が残る表示を修正
* **ChMate:** 1.4.9までのBEアイコン、投稿、Talk、広告、URL補正を継承

### ♻️ Improvements

* **ChMate:** NGワード・NG ID・NG名前などの保存上限を、Haiagaru設定から0（無制限）〜100000件で指定できるように変更

## 1.4.9 (2026-09-25)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 1.4.8で残っていた「本文内の5chスレURLにスレ立て日を表示」との併用時のアイコン二重表示・URL位置ずれを修正

### ♻️ Improvements

* **ChMate:** ChMateが内部で使う制御文字付き`img.5ch.io/ico/...gif`表記もBEアイコン描画経路へ正規化

## 1.4.8 (2026-09-24)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 「本文内の5chスレURLにスレ立て日を表示」と併用した際のBEアイコン二重表示、URL認識ずれ、画像添付化を修正
* **ChMate:** 1.4.7までの投稿、Talk、広告、URL補正、BEアイコン修正を継承

### ♻️ Improvements

* **ChMate:** `https://img.5ch.io/ico/...gif`、プロトコル相対URL、`sssp://`形式のBEアイコンを既存のアイコン描画経路へ正規化

## 1.4.7 (2026-09-24)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** BEアイコンと「本文内の5chスレURLにスレ立て日を表示」を併用した際の、アイコンの二重表示やURL認識のずれを修正
* **ChMate:** 191 devのスレ立て確認処理で本文などの入力内容が失われる問題を修正
* **ChMate:** 191 dev／226 devのモバイル回線固定投稿で、`getAllNetworks()`由来の失効Network／SocketFactoryを使わず、投稿ごとに新しいセルラーNetworkを要求するよう修正
* **ChMate:** Android 16でNetworkが投稿中に切り替わる場合は、別のNetworkを再要求してから接続を再試行するよう修正

### ✨ New Features

* **ChMate:** Haiagaru設定に「投稿時にモバイル回線を再取得する」を追加（初期値ON、OFFでChMate本来の接続選択へ戻す）
* **ChMate:** 対応バージョン：ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev`

## 1.4.6 (2026-09-24)

**Channel:** Prerelease

* **ChMate:** premium形式のBEアイコンを既存の板アイコン描画経路へ統合し、通常の画像サムネイルとは区別。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.4.6)をご覧ください。

## 1.4.5 (2026-09-24)

**Channel:** Prerelease

* **ChMate:** Talkの板・スレURLをAndroidの「アプリで開く」の候補へ追加。任意の受け付けURLを指定するパッチオプションを追加。
* **ChMate:** URLの受け付け設定であり、ChMateが解析できないサイトの閲覧対応を追加するものではありません。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.4.5)をご覧ください。

## 1.4.4 (2026-09-24)

**Channel:** Prerelease

* **ChMate:** 新規インストール時に板一覧がまちBBSだけになる問題を修正。BBSMENU未設定時に5ch.ioを初期値として使用し、利用者の設定済みURLは維持。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.4.4)をご覧ください。

## 1.4.3 (2026-09-24)

**Channel:** Prerelease

* **ChMate:** 板一覧更新時に保存済みの旧menu.5ch.netをmenu.5ch.ioへ補正。
* **ChMate:** 詳細は[公開時の更新内容](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.4.3)をご覧ください。

## 1.4.2 (2026-09-24)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** Talk 以外の掲示板で、スレを初めて開いたときに本文が白くなり表示されない問題を修正
* **ChMate:** Talk の投稿認証時刻を正しい単位で扱うよう修正

### ✨ New Features

* **ChMate:** Talkスレ内の広告予約枠による大きな空欄を、対応バージョン共通のView構造判定で削除

### ♻️ Improvements

* **ChMate:** Talk DAT 変換時に絵文字バリエーションセレクタを適切に処理

### 🚀 Updated App Support

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev`向けMPPを更新

## 1.4.1 (2026-09-24)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** Talkの過去ログDAT変換で、MS932外の絵文字・複合絵文字・外字を保持

### ✨ New Features

* **ChMate:** Talkスレ内の広告予約枠による大きな空欄を、対応バージョン共通のView構造判定で削除

### ♻️ Improvements

* **ChMate:** 広告枠の判定が下部バーや書き込みボタンを巻き込まないよう対象を大きな子要素なしコンテナに限定

### 🚀 Updated App Support

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev`向けMPPを更新

## 1.4.0 (2026-09-23)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.226 dev`でモバイル回線固定の書き込み時に、失効直後のnetwork IDを再利用して`Binding socket to network ... failed: EPERM`になる問題を修正

### ♻️ Improvements

* **ChMate:** モバイル回線固定の投稿では毎回Androidの`requestNetwork()`で有効なセルラー回線を取得し、投稿完了まで`NetworkCallback`を維持するよう変更

## 1.3.9 (2026-09-22)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** `1.3.7`／`1.3.8`からアプリデータを残して更新した際、古いTalk書き込みキーが残って`divide by zero`になる問題を修正
* **ChMate:** Talkスレ更新時にDAT全体とIDXを作り直さず、不足したレスだけを追記して「自分の書き込み」印を保持

### ♻️ Improvements

* **ChMate:** ChMate本体の同じversionNameではなくAPKの更新時刻を使用し、パッチ済みAPKの更新ごとにTalk書き込みセッションを一度だけ安全に再生成
* **ChMate:** Talkテストスレへ実投稿し、別画面へ移動後に再度開いても投稿レスと自分の書き込み印が残ることを実機で確認

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devの全対応APKへパッチできることを確認

## 1.3.8 (2026-09-22)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`でTalk板一覧のURLから`/boards/`が失われ、板一覧を取得できなくなる回帰を修正
* **ChMate:** ChMate `0.8.10.191 dev`のNGThread編集画面へ「記者IDだけをNG」を復旧
* **ChMate:** Talk投稿直後に読み取りAPIが一世代古い場合、画面遷移後に投稿レスが消えないよう新しいローカルDATを保持

### ♻️ Improvements

* **ChMate:** 読み取りAPIが追いついた後は通常どおり新しいDATへ更新されることを実機で確認

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devの全対応APKへパッチできることを確認

## 1.3.7 (2026-09-22)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** Talkの書き込み確認画面を戻る操作で閉じた場合、次回投稿用のセッションだけを安全に再生成するよう修正
* **ChMate:** Talkへの投稿直後、読み取りAPIの反映が遅れていてもローカルの新しいレスを短いDATで上書きしないよう修正
* **ChMate:** ChMate `0.8.10.226 dev`で、Talk投稿前の生成キー処理に発生する`NullPointerException`対策を追加
* **ChMate:** ChMate `0.8.10.241`で、Talkキー保持オブジェクトと投稿処理の署名依存経路を保護

### ✨ New Features

* **ChMate:** ChMate `0.8.10.191 dev`で、過去のパッチや確認画面のキャンセルにより不整合になったTalk書き込みキーを、アプリデータを削除せず修復する処理を追加

### 🚀 Updated App Support

* **ChMate:** 191 dev／226 dev／241／243 devの全対応APKへ静的にパッチできることと、生成APKの署名を確認

## 1.3.6 (2026-09-21)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.241`でTalkスレ取得時に数値エラーが発生する問題を修正し、Talk APIの応答をDATへ変換する経路を追加
* **ChMate:** ChMate `0.8.10.241`のTalk投稿で署名依存トラップにより`222`などの数値エラーが発生する問題を修正
* **ChMate:** ChMate `0.8.10.191 dev`のTalk認証状態が更新された場合に発生する`divide by zero`／`NullPointerException`への補正を強化
* **ChMate:** 「投稿前の本文チェックを無効化」をHaiagaru設定へ追加し、226 dev／241で空欄ではない投稿が誤判定される問題を回避可能に変更
* **ChMate:** 高度なNGのスレ一覧フィルターで、ARTが`Object`と具体型の不一致を検出する問題を修正

### ✨ New Features

* **ChMate:** ChMate `0.8.10.241`を実機へ導入し、Talkスレの閲覧と投稿を確認

## 1.3.5 (2026-09-21)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** 1.3.4の機能・修正を継承し、参考実装の作者 [`testuser0123-web`](https://github.com/testuser0123-web) を明記

### ♻️ Improvements

* **ChMate:** 判定結果を各バージョンの標準NGWordフラグへ統合し、既存のNG処理と表示設定を維持

### 🚀 Updated App Support

* **ChMate:** 「高度なNGルール」のレス本文判定を191 dev／226 dev／241／243 devの全対応バージョンへ拡張

## 1.3.4 (2026-09-21)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate標準の書き込み履歴について、保持件数をHaiagaru設定から0〜10000件で変更可能に追加
* **ChMate:** ChMate `0.8.10.191 dev`でNGThread追加画面を開けない問題を修正
* **ChMate:** ChMate `0.8.10.191 dev`のTalk投稿で署名依存キャッシュが更新された後に発生する`divide by zero`を、一度だけ正規化して再試行するよう修正
* **ChMate:** ChMate `0.8.10.241`／Android 17で設定画面を開く際の`divide by zero`を修正

### ✨ New Features

* **ChMate:** 端末内だけで動作する「高度なNGルール」を追加し、スレタイ・レス本文のキーワード、正規表現、記者ID、JavaScript条件を設定可能に変更
* **ChMate:** スレ一覧の高度なNG判定を191 dev／226 dev／241／243 devへ対応（レス本文は191 devで対応）
* **ChMate:** GPLv3派生実装とMozilla Rhino（MPL-2.0）のライセンス表示を追加

### ♻️ Improvements

* **ChMate:** 高度なNG設定画面から内部向けの「対象: 0.8.10.191 dev」表記を削除し、操作説明と状態表示を整理

## 1.3.3 (2026-09-21)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** エッヂの記者IDをスレ履歴へ保持し、既読スレや再起動後でも取得済みの記者IDをNGThreadへ登録できるように修正
* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`で、5ch.io移行後も必死チェッカーのメニューを利用できるように修正
* **ChMate:** ChMate `0.8.10.243 dev`のTalk投稿時に発生する署名依存の整合性エラーを修正
* **ChMate:** 226 devへ修正版を実機導入し、上部余白のみが消えてフィルターボタンが維持されることを確認

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.243 dev`のスレ内広告行を非表示化
* **ChMate:** タブレット表示で上部に残っていた広告予約枠を、191の旧View構造と226／241／243のCompose構造ごとに除去

## 1.3.2 (2026-09-17)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`で広告削除をONにすると、取得済みスレの検索結果画面にある検索条件のチェックボックスが消える問題を修正
* **ChMate:** 修正版191 APKを実機にインストールし、ユーザーによる表示確認済み

### ♻️ Improvements

* **ChMate:** 「正規表現」「大文字小文字を区別しない」「ヘッダー」「本文」「取得済みのスレ」「アーカイブ」を表示したまま、特定済みの広告Viewを非表示にするよう変更

## 1.3.1 (2026-09-16)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** 1.3.0のTalk対応、タブレット再取得ループ修正、広告行非表示を継承

### ✨ New Features

* **ChMate:** エッヂの `subject.txt` を記者ID付き `subject-metadent.txt` へ切り替える設定を追加（初期値ON）
* **ChMate:** アプリ内のHTTP通信（画像取得を含む）をHTTPSへ切り替える設定を追加（初期値OFF）

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev` のエッヂ板で、記者ID付きスレタイを表示

## 1.3.0 (2026-09-15)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** Talk投稿時に動的生成クラスの署名依存比較が不一致となり、`NullPointerException`で失敗する問題を修正
* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`のタブレットモードで、取得済みTalk DATを再取得し続ける問題を修正
* **ChMate:** タブレットモードで過去ログの自動取得に失敗した際、失敗通知と再取得が無限に繰り返される問題を修正
* **ChMate:** 運用情報板・裏社会板を含むTalk板URLの補正、既存の自動DAT取得、URL補正、設定、パッケージカスタマイズを収録

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`の1レス目と2レス目の間に残る広告行を非表示化

### 🚀 Updated App Support

* **ChMate:** ChMate `0.8.10.226 dev`で、Talkの現行スレをTalk APIからDATへ変換して閲覧できるように対応

## 1.2.4.r1 (2026-09-15)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`のタブレット表示で、板一覧からTalkスレを開くとDAT落ち扱いになる問題を修正

### ♻️ Improvements

* **ChMate:** タブレット内遷移でもTalkスレを判定し、通常表示と同じTalk APIからDATキャッシュを生成する経路へ統一
* **ChMate:** `5ch.net`からの自動DAT取得設定に依存せず、Talkスレは常にTalk専用処理へ渡すように変更

## 1.2.4 (2026-09-15)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** `1.2.3.r5`のTalk旧形式URL・板一覧・スレ取得修正を収録
* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.243 dev`のタブレット二画面表示で、画面内遷移が通常のスレActivityを経由せず自動DAT取得を回避していた問題を修正

### ♻️ Improvements

* **ChMate:** DAT変換完了後は通常のスレ表示Activityを経由して再表示し、タブレット側にも取得結果を反映

## 1.2.3.r5 (2026-09-15)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`で、旧形式のTalk板URLから運用情報板や裏社会板を開くと404になる問題を修正
* **ChMate:** `talk.jp/{板}/subject.txt`などの板情報を2ch互換配信先へ補正

### ♻️ Improvements

* **ChMate:** `talk.jp/{板}/{スレID}`、`talk.jp/test/read.cgi/{板}/{スレID}`、`talk.jp/boards/{板}/{スレID}`を同じTalk API取得経路で扱うように変更
* **ChMate:** Androidエミュレーター上で運用情報板・裏社会板の一覧表示と、両形式のスレ取得を確認

## 1.2.3.r4 (2026-09-15)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`の荒らし省略・コピペ省略2について、設定がONでも判定処理が登録されない内部条件を修正

### ✨ New Features

* **ChMate:** ChMate `0.8.10.243 dev`のHaiagaru設定に、単発ID表示の省略・コピペ省略2・荒らし省略を追加

### ♻️ Improvements

* **ChMate:** 個別の報告レスが実機で省略されることは未確認

## 1.2.3.r3 (2026-09-15)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.243 dev` で、Talkの現行スレを開くと `divide by zero` またはDAT落ちになる問題を修正

### ♻️ Improvements

* **ChMate:** Talk APIのレスをChMateのDATキャッシュへ変換し、署名変更後に不安定になるTalk専用取得処理より先に読み込むように変更
* **ChMate:** TalkのURL、板情報、書き込み処理はChMate本来の経路を維持

## 1.2.3.r2 (2026-09-14)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.226 dev` の設定画面復元時に `o.setImageAssetsFolder.<init>` の署名依存デコイで `divide by zero` が発生する問題を修正
* **ChMate:** 226実機で起動後に `SettingActivity` を開き、同クラッシュが再発しないことを確認
* **ChMate:** 広告View非表示処理で、View復元中の例外がChMate本体のクラッシュへ波及しないように保護

## 1.2.3.r1 (2026-09-14)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate+互換設定のチェック状態だけが変わり、実機能が有効にならない問題を修正

### ♻️ Improvements

* **ChMate:** 1.2.2-r15で検証した変更を、1.2.3系の最初の検証版として再公開
* **ChMate:** Haiagaru設定ボタンが反応せず設定画面を開けない端末への互換処理を収録

## 1.2.3 (2026-09-14)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** Android 16を含む一部端末でHaiagaru設定ボタンを押しても設定画面が開かない問題を修正
* **ChMate:** Haiagaru設定からChMate+互換機能を有効にしても、実際の動作へ反映されない問題を修正
* **ChMate:** ChMate 0.8.10.191 devの単一ID省略設定に残っていた有効化判定を補正

### 🚀 Updated App Support

* **ChMate:** ChMate 0.8.10.191 dev、0.8.10.226 dev、0.8.10.241、0.8.10.243 devに対応

## 1.2.2-r15 (2026-09-14)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev` のChMate+互換設定で、チェックはONになるが実際の機能が有効にならない問題を修正
* **ChMate:** ChMate `0.8.10.191 dev` の `単発ID表示を省略` で、保存済み設定がONのときに追加の有効化ゲートで無効化されないように修正

### ♻️ Improvements

* **ChMate:** `master` / `main` のパッチバンドル混在を解消するため、配布対象を `master` に統一
* **ChMate:** `コピペ省略2` をONにした場合はChMate本体側の親設定 `copipeNg` もONにするように変更
* **ChMate:** `荒らし省略` をONにした場合はChMate本体側の親設定 `copipeNgAR` もONにするように変更

## 1.2.2-r14 (2026-09-14)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** Haiagaru設定ボタンをアプリ内のポップアップ表示へ変更し、Android 16 / Samsung系端末でボタンが表示されても設定画面が開けない問題を修正
* **ChMate:** ChMate+設定、DAT経路、プリセット更新、Shizuku移行、重複板整理の各設定セクションを個別に保護し、追加機能側の失敗で基本設定画面全体が開けなくなる問題を回避
* **ChMate:** DAT取得、URL自動補正、投稿、板整理、パッケージ名・アプリ名・アイコン・versionCode変更機能を維持

### ✨ New Features

* **ChMate:** 診断用に追加していた設定ボタンのログ保存処理を正式版から削除
* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev` のHaiagaru設定画面にChMate+互換設定を追加

## 1.2.2-r13 (2026-09-13)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** URV Managerが解釈できるタイムゾーンなしの`LocalDateTime`形式へ`created\\\_at`を修正

### ✨ New Features

* **ChMate:** 対応バージョン一覧とパッチバンドルの配布情報を更新

## 1.2.2-r11 (2026-09-13)

**Channel:** Stable

### 🐛 Bug Fixes

* **ChMate:** `.io` URLを直接開いた古いスレッドも自動DAT取得の対象に修正
* **ChMate:** 自動DAT取得開始時の「過去ログを取得しています」表示を抑制し、既存DATがある場合はChMate本来の処理へ移行
* **ChMate:** kako HTMLとitest JSONの本文変換で、`<br>`・段落タグ・元改行を保持

### ✨ New Features

* **ChMate:** ChMate内部の`roidon.sqlite`を直接編集し、外部板扱い／5ch扱いの5ch.io板を選択して一括削除できる機能を追加
* **ChMate:** ChMate `0.8.10.191 dev` のAndroid「URLをアプリで開く」対象へ、`\\\*.5ch.io` と `itest.5ch.io` を追加

### ♻️ Improvements

* **ChMate:** Haiagaru設定画面から、自動DAT取得をON/OFF可能に変更
* **ChMate:** 削除後はChMateの再起動で板一覧を更新

### 🚀 Updated App Support

* **ChMate:** 対応対象にChMate `0.8.10.226 dev`（versionCode 494）を追加

## 1.2.2-r10 (2026-09-13)

**Channel:** Pre-release

### ✨ New Features

* **ChMate:** 自動DAT取得で現役サーバーのDATを最初に確認する経路を追加
* **ChMate:** DAT・kako HTML・itest JSONの形式指定と、任意HTTPS URLテンプレートの追加に対応
* **ChMate:** GitHub配布プリセットへ、`.net`／`.io`対応の板別 `2ch.sc` 14経路を追加

### ♻️ Improvements

* **ChMate:** `kako`、`itest`、`2ch.sc` を含む取得経路を1行単位で並べ替え可能に変更

## 1.2.2-r9 (2026-09-12)

**Channel:** Stable

### ♻️ Improvements

* **ChMate:** `kako.5ch.io`、`itest.5ch.io`、`2ch.sc` の自動DAT取得経路をスレッドURL起動時にも適用
* **ChMate:** 191 devは `ResListActivity`、241／243 devは `Hilt\\\_ResListActivity` の構造差に合わせて注入

### 🚀 Updated App Support

* **ChMate:** ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／`0.8.10.243 dev` の全対応版でURL自動補正を有効化

## 1.2.2-r8 (2026-09-12)

**Channel:** Pre-release

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.243 dev` で画像添付時に発生していたメモリ不足、0除算、null関連のクラッシュを修正
* **ChMate:** パッケージ名変更後のバックアップ復元に含まれる旧パッケージ参照を補正
* **ChMate:** 広告非表示時に広告SDKと計測SDKの初期化を抑制
* **ChMate:** 任意パッチ `Save ChMate crash logs` を追加。クラッシュ時に `Download/Haiagaru/` へログを保存

### ✨ New Features

* **ChMate:** パッケージ名変更を初期状態で有効化し、アプリ名、PNG/WebPアイコン、versionCodeの変更に対応
* **ChMate:** Shizukuを使った旧ChMate共有データのコピー補助を追加

### ♻️ Improvements

* **ChMate:** DAT落ちスレを `kako.5ch.io`、`itest.5ch.io`、`2ch.sc` から取得するプリセットを更新

### 🚀 Updated App Support

* **ChMate:** 対応対象から ChMate `0.8.10.242 dev` を削除

## 1.2.2-r7 (2026-09-12)

**Channel:** Pre-release

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev` の添付解析用テキストから `sssp://`、HTTP(S)、制御文字形式のBEアイコントークンを入力段階で除去
* **ChMate:** 本文側のBEアイコン描画は原文を使用するため維持

## 1.2.2-r6 (2026-09-12)

**Channel:** Pre-release

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev` のレス単体、スレ全体、表示変換の全添付経路からBEアイコンURLを除外
* **ChMate:** `img.5ch.io/ico/marara\\\_tya.gif` と `img.5ch.io/ico/kuma.gif` を含む `img.5ch.io/ico/` / `img.5ch.net/ico/` を対象化

## 1.2.2-r5 (2026-09-12)

**Channel:** Pre-release

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev` の添付抽出結果からBEアイコンURLを直接除外

## 1.2.2-r4 (2026-09-12)

**Channel:** Pre-release

### ♻️ Improvements

* **ChMate:** ChMate `0.8.10.191 dev` のBEアイコンを通常の添付ファイル一覧から除外

## 1.2.2 (2026-09-11)

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev` のBEアイコン分類と表示を修正
* **ChMate:** URV Managerが1.2.2内の修正版を検出できる配布リビジョンを追加

## 1.2.1 (2026-09-10)

### 🐛 Bug Fixes

* **ChMate:** Android版Morphe Managerで読み込めるDEX形式のパッチバンドルへ修正

## 1.2.0 (2026-09-10)

### 🐛 Bug Fixes

* **ChMate:** ChMate `0.8.10.191 dev` の画像アップロード時クラッシュを修正

## 1.1.0 (2026-09-10)

### ✨ New Features

* **ChMate:** ChMate `0.8.10.191 dev` 対応
* **ChMate:** `5ch.io` の表示・検索・書き込みに対応

## 1.0 (2026-09-09)

**Channel:** Stable

* **ChMate:** 初回公開。公開ページには更新内容の本文がないため、個別の機能差分は補記していません。
* **ChMate:** [公開ページ](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.0)

