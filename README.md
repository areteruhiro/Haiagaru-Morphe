
◆9qrWgYJJCo
◆8LNH56sLs6
# Haiagaru for Morphe

ChMate `0.8.10.191 dev` / `0.8.10.226 dev` / `0.8.10.242 dev` 対応のMorpheパッチです。 <br>

**0.8.10.241／0.8.10.243 devは対応対象外です。** 191 dev／226 dev／242 devをご利用ください。

ChMateに追加設定・閲覧・メディア・バックアップ機能を組み込むパッチです。
従来の機能紹介は [Haiagaru](https://github.com/areteruhiro/Haiagaru) も参照してください。

最新正式版: [1.7.1](https://github.com/areteruhiro/Haiagaru-Morphe/releases/latest) ／ 最新プレ版: [1.8.11](https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.8.11)

以下の機能紹介にはプレ版で追加した機能も含みます。正式版1.7.1ですべての機能が使えるわけではありません。

更新履歴: [CHANGELOG.md](CHANGELOG.md)  

1.8.11: [アーカイブの元スレURL確認・コピー](docs/archive-original-url.md)。アーカイブ一覧またはHaiagaru設定の「過去ログ・エッヂ検索」から確認でき、元URL情報もスレアーカイブと一緒にMEGA／ローカルへバックアップできます。

1.8.11: [コピー履歴・分類](docs/clipboard-history.md)。Haiagaru設定の「履歴・保存件数」または書き込み画面の︙から、ChMate内でコピーした本文を検索・再コピー・カテゴリ分けできます。記録は初期オフ、最新100件まで。他アプリのコピーは収集しません。
正式版 `1.7.1` の概要: [1.7.1の更新内容](release-notes-1.7.1.md)

プレ版 `1.8.11` の概要: [1.8.10からの更新内容](docs/release-1.8.11.md)

## 主な機能

* Remove ads (including margins)
* 画像キャッシュサイズを最大256GBまで選択（4／8／16／32／64／128／256GBを追加、現在の設定は維持）。[詳細](docs/image-cache-size.md)
* Modify User-Agent
* 画像を含むHTTP通信のHTTPS切り替え（Haiagaru設定でON/OFF、初期値OFF）
* Remove MonaKey
* GitHubから更新できるDAT落ちスレ用検索プリセット
* 自動DAT取得経路の並べ替えと任意HTTPS経路の追加
* SS速報VIPの過去ログdat取得と、DAT経路変数の部分取り出し（`{$key:0:4}` など）。191／226／242 dev対応。[設定・書式](docs/dat-routes.md)
* 自動DAT取得のON/OFF切り替え
* 古いDAT・過去ログの改行保持と`.io` URL直接起動時の自動DAT取得
* Talkの現行・旧形式板URLからの板一覧／スレ取得と書き込み互換処理
* モバイル回線固定時の投稿で失効したNetwork IDを再利用しない接続更新（Haiagaru設定でON/OFF、初期値ON）
* ChMate `0.8.10.191 dev`／`0.8.10.226 dev` のスレ内広告行の非表示
* 5ch.io板が外部板扱いと5ch扱いで重複した場合の内部板一覧一括整理
* パッケージ名・アプリ名・アイコン・versionCodeの変更
* クラッシュログ保存（Morpheで無効化可能、初期値ON）
* レス本文の直接選択・コピー: 本文長押しで一部を選択し、Android標準メニューからコピーできます。Haiagaru設定、またはスレ内ツールバー設定に追加した「長押しコピー」でオン／オフ可能（初期オフ）。191／226／242 dev対応。[使い方](docs/direct-body-selection.md)
* 画像／動画のアップロード先切り替え: Imgur、画像専用imgbox（10MB）、imgpile（API不要100MB／API方式97MB）、動画専用Sendvidを選択可能。標準添付の成功結果を本文へ挿入し標準履歴へ記録します。imgbox／imgpileの保存済み管理情報から選択・確認付きの一括削除も追加。権限・画像単位のフォーム・成功応答を確認できない場合は停止します。手動Web画面は別操作です（開発中・実送信／実削除成功は未確認）。[使い方・制限](docs/upload-hosts.md)
* 専用ビュワーの追加　<オフに出来ないとの報告があります>
* 動画専用ビューアの左右上下スワイプ（明るさ／音量）・左右ダブルタップの相対シーク設定、およびYouTube公式プレーヤーのアプリ内表示。[操作と制限](docs/video-viewer-controls.md)
* エッヂ過去ログビューアーで開けない場合はエラーコードを表示し、`Download/Haiagaru` に診断ログを保存
* ChMate `0.8.10.191 dev`／`0.8.10.226 dev` のChMate+互換設定
  * 単発ID表示を省略
  * コピペ省略2　<191でコピペ省略が機能していないとの報告があります>
  * 荒らし省略
* エッヂ板でスレタイ末尾に記者IDを表示（初期値ON、Haiagaru設定から切り替え）
* 端末内JavaScriptで複雑なNG条件を作れる「高度なNGルール」
* [5chのIDなしレスを非表示](docs/hidden-id-ng.md)（初期オフ、ID付きレスのあるスレ内のみ。191／226／242 dev）
* NGワード・NG ID・NG名前などの登録上限をHaiagaru設定から変更（0で無制限、191 dev／226 dev／242 dev共通）

## 最近の追加機能

### 1.8.11：コピー・投稿者強調・動画プレビューなど

レス保存メニューの欠落を修正し、投稿者の強調、ツールバーの長押しコピー、動画の時系列分割サムネイル、X全文・折りたたみ・キャッシュ、アップロード先切替を追加しました。閲覧履歴の板別表示、5分／10分巡回、アーカイブ元URL、高度なNGのバックアップにも対応します。

詳細・操作方法・検証範囲は[1.8.10との差分](docs/release-1.8.11.md)をご覧ください。動画分割の実リモート取得、imgpile／Sendvidの実送信とimgboxの実一括削除は未確認です。

### 1.8.10：複数の保存用スレ・226のタブ選択補正

- **保存用スレ**：レス長押しの「このレスをアーカイブに保存」から、名前付きの保存先を複数作成できます。「過去レスを見る」で保存用スレを選ぶと、レスを続けて閲覧できます。掲示板への投稿は行いません。従来の保存レスも保持します。[詳細](docs/response-archive.md)
- **バックアップ**：保存用スレの名前・保存レスは、ローカル／MEGAバックアップの「レスのアーカイブ・保存用スレ」に含まれます。MEGAは導入時に対応パッチの選択が必要です。
- **226 devのタブ選択補正**：タブレットモードのタブ一覧更新中に、反映途中の選択位置を書き戻さないよう変更しました。[検証範囲](docs/tablet-tabs-226.md)

### 1.8.9：外部板の認証・登録板編集

- **外部板の認証支援**：書き込み画面の「︙ → 外部板の認証ページ」、または「Haiagaru設定 → 詳細 → 外部板の認証ページを設定・開く」から認証ページをアプリ内で開けます。認証を要求するエラーを検出した際も案内します。コード入力後に「認証を反映して戻る」を押すと、その板のHTTPS通信へCookieを連携します。
- **登録した外部板の名前・URL編集**：「ChMate設定 → 掲示板／BBSMENU → 登録した外部板の名前・URLを編集」から変更できます。同じ板の改名・移転向けで、ブックマーク・ログ・NGの識別情報は維持します。保存後はChMateを終了・再起動してください。
- **パッケージ名変更の修正**：追加した設定画面の起動先も変更後のパッケージ名へ揃えます。

認証は利用者自身で操作してください。二重投稿防止のため自動再投稿は行いません。Cookie以外の認証方式など、対応できないサイトがあります。実サイトの認証完了・投稿成功は未検証です。板移転時に別ホストへ認証情報をコピーすることはありません。

詳細：[外部板認証](docs/external-board-auth.md) ／ [板名・URL編集](docs/external-board-editor.md) ／ [1.8.9の更新内容](docs/release-1.8.9.md)

### 1.8.8：設定・巡回画像・テーマ・バックアップなど

- **設定を整理**：Haiagaru設定を10カテゴリへ分類。項目名・説明の検索と全開閉に対応し、既存の設定値は維持します。
- **巡回時の画像自動保存（初期OFF）**：ChMate標準の巡回対象を、複数のスレタイキーワード・スレURLで絞り込み可能。未表示のスレでも本文取得に合わせて画像を保存します。保存先はChMate画像キャッシュ／`Download/Haiagaru/スレごとのフォルダ`を選択でき、保存済み画像の重複取得を避けます。
- **自作テーマの取り込み**：Android標準のファイル選択からテーマを複数取り込み可能。画像素材付きフォルダと同名ファイルの置換確認にも対応し、Android/dataの手動操作は不要です。
- **画像キャッシュ上限を拡張**：4／8／16／32／64／128／256GBを追加。空き容量と標準のキャッシュ整理処理は引き続き適用されます。
- **標準画像アップロード履歴を拡張**：右上の︙に「新しい順（降順）」と、選択・確認付きの「サーバーから一括削除」を追加。サーバー上の削除は取り消せません。
- **動画操作を追加**：左右上下スワイプの明るさ・音量調整、左右ダブルタップの巻き戻し・早送りを個別設定可能（初期OFF）。YouTubeは公式埋め込みプレーヤーでアプリ内表示。システムの戻る1回でプレーヤーを終了します。
- **X／Twitter本文プレビュー（初期OFF）**：公開投稿URLから投稿者・本文・日時をレス内に追加表示。全文／冒頭のみの切替、タップで開閉する折りたたみ表示も設定可能です。取得済み本文は端末内に最大7日・128投稿／16MiBまでキャッシュし、再起動後やオフラインでも再利用します（1.8.11）。設定からキャッシュを削除できます。初回取得などでは追加通信を行います。投稿の編集・削除が即時反映されない場合があります。元DATやNG判定は変更しません。
- **投稿者の強調（初期OFF）**：スレ主・ID・ﾜｯﾁｮｲ・ﾜｯﾁｮｲ下4桁・トリップから条件を選び、背景色と太字で表示。レス長押しから登録し、スレ／板／全板の範囲指定、登録ごとのオン／オフ・削除ができます。詳しくは[投稿者の強調](docs/poster-highlight.md)をご覧ください。
- **本文の一部を直接コピー（1.8.11・初期OFF）**：Haiagaru設定 →「スレ・スレ一覧の表示」→「レス本文を直接選択してコピー」をオンにすると、本文長押しから範囲を調整してコピーできます。従来のレスメニューはレス番号・ヘッダーから操作してください。191／226／242 dev対応。
- **閲覧履歴を板ごとに表示（1.8.11）**：通常画面の履歴だけを最近閲覧した板からまとめ、板内も新しい順に表示します。区切り行は追加しません。初期対象は2日前の午前0時以降で、日数・全期間・オン／オフを設定できます。ポップアップと保存データは変更しません。[使い方](docs/browsing-history.md)
- **内部動画ビューアから保存（1.8.11）**：︙の「動画を保存」で直接MP4を`Download/Haiagaru`へ保存。画面を閉じても取得は続きます。YouTube公式プレイヤーは対象外です。[詳細](docs/video-viewer.md)
- **スレ立てコマンド候補**：5ch／エッヂに応じた編集可能な候補を選び、本文へ挿入できます。ワッチョイ候補は初期3行で、自由入力の改行と同じ内容の再追加にも対応（1.8.11）。自動送信はしません。
- **NG操作を拡張**：ワッチョイ長押しの「下4桁で検索」、226 devの「NG編集」ジェスチャー名表示、右上の「非表示NGを表示」を隠す任意設定を追加。
- **エッヂ過去ログ検索の表示調整**：検索欄・検索結果の文字倍率を個別設定でき、検索欄の折りたたみで結果を広く表示できます。
- **レス単位アーカイブ**：レスごとの保存・一覧・オフライン閲覧を追加。従来のスレ全体保存も維持します。
- **バックアップを拡充**：ローカル／MEGAへDAT、レス・スレアーカイブ、フォント・テーマ・追加設定・標準画像アップロード履歴などを保存。ZIP全体の32MiB上限を撤廃し、旧JSON復元も維持します。
- **MEGAバックアップの管理**：端末名・作成日時の一覧から復元元を選択可能。端末ごとの上書きと、照合成功後の旧ファイル完全削除オプション（初期OFF）を追加。日時は端末のタイムゾーンで表示します。
- **認証情報の任意バックアップ**：認証情報・通常WebView Cookie・MEGAセッションと、同一インストール専用の暗号化済みデータを別々の初期OFF項目として追加。Keystore秘密鍵は保存しません。
- **191 devの通信改善**：TCP競合接続（約250ms後に別候補へ接続を開始・初期ON）と進捗通知の軽量化、任意の通信全体上限を追加。特定の取得時間や、すべての通信停止の解消を保証するものではありません。

巡回画像保存は「Haiagaru設定 → 画像・動画・X／Twitter → 巡回時の画像自動保存（スレタイ・URL指定）」にあります。ChMate標準の自動巡回と「本文(dat)もダウンロード」も有効にしてください。巡回対象を増やす機能ではなく、動画は対象外です。通信量・保存容量が増える点にご注意ください。

1.8.11で、ChMate標準の自動巡回間隔に5分・10分を追加しています（191／226／242 dev）。Androidの省電力制御や通信条件により実行が遅れる場合があります。詳細は[自動巡回の間隔](docs/patrol-intervals.md)をご覧ください。

自作テーマは「Haiagaru設定 → スレ・スレ一覧の表示 → 自作テーマをファイル／フォルダから取り込む」から追加できます。

認証情報入りバックアップは第三者に渡さず厳重に保管してください。端末固有の暗号化済みデータは他端末・再インストール先で利用できるとは限りません。MEGA同期は任意パッチでAndroid 7.0以降が必要です。ローカルバックアップはMEGAなしでも利用できます。

詳細：[1.8.8の更新内容・検証範囲](docs/release-1.8.8.md)

## 機能別ドキュメント

- [ID検索と専用ビュワー](docs/id-search-viewer.md)
- [エッヂの記者ID表示](docs/edge-reporter-id.md)
- [HTTP通信のHTTPS切り替え](docs/https-switching.md)
- [MEGAバックアップ](docs/mega-backup.md)
- [ChMate+互換機能](docs/chmate-plus-compat.md)
- [高度なNGルール](docs/advanced-ng.md)
- [本文の文字列置換（外部TXT）](docs/replace-str.md)
- [Haiagaru設定のカテゴリ・検索](docs/settings-organization.md)
- [巡回時の画像自動保存・フィルタリング](docs/patrol-images.md)
- [自作テーマの取り込み](docs/custom-theme-import.md)
- [画像キャッシュサイズ](docs/image-cache-size.md)
- [標準画像アップロード履歴・一括削除](docs/native-upload-history.md)
- [動画ビューアの操作](docs/video-viewer-controls.md)
- [X／Twitter本文プレビュー](docs/x-post-preview.md)
- [スレ立てコマンド](docs/thread-commands.md)
- [ワッチョイ下4桁検索](docs/wacchoi-suffix-search.md)
- [非表示NGメニューの表示設定](docs/hidden-ng-menu.md)
- [エッヂ過去ログ検索の表示調整](docs/edge-archive-display.md)
- [レス単位アーカイブ](docs/response-archive.md)
- [191 devの通信改善・検証範囲](docs/chmate191-update-performance.md)
- [外部板の認証ページ](docs/external-board-auth.md)
- [登録した外部板の名前・URL編集](docs/external-board-editor.md)
- Cookieエラー、ワッチョイ検索、画像一覧、高度なNGワードについては [よくある質問](docs/faq.md) を参照してください。


### DAT落ちスレの自動取得

DAT落ちスレ用プリセットは、通常閲覧時ではなく設定画面の更新ボタンを押した時だけ、
[`presets/chmate-dat-fallen-search-urls.txt`](presets/chmate-dat-fallen-search-urls.txt) を取得します。
GitHubへ接続できない場合は、基本3経路を収録した内蔵プリセットを使用します。
2ch.scの板一覧参照先は [`https://menu.2ch.sc/bbsmenu.html`](https://menu.2ch.sc/bbsmenu.html) です。

Haiagaru設定の「自動DAT取得経路」は、上の行から順に試行します。
初期状態では、元スレと同じサーバーの `5ch.io` DAT、`kako.5ch.io`、
`itest.5ch.io`、同じサーバーの `2ch.sc` DATの順です。
行を並べ替えると優先順を変更でき、1行追加すると任意のHTTPS経路も追加できます。

各行は `auto|`、`dat|`、`kako|`、`itest|` のいずれかにURLを続けます。
URLでは `{$server}`、`{$bbs}`、`{$key}`、`{$rand}` を使用できます。
空行と `#` で始まる行は無視され、無効な設定しかない場合は初期経路へ戻ります。

## 既知の不具合

- **タブレット用モードはOFFにすることを推奨します。**
  - タブレット用モードでは、エッヂ過去ログなどのツールバー機能に不具合が発生する場合があります。

- 書き込み長押しメニューの **「エッジ過去ログ」**、**「未読を0にする」** は現在機能していません。

### スレ内の検索長押しで「URLを開けるアプリがありません」と表示される場合

Androidの「標準アプリとして設定」から、以下の項目をOFFにしてください。

- `itest.5ch.io`
- `*.5ch.io`

この設定をOFFにすると、書き込みボタンを長押しした際に外部ブラウザで開けるようになります。

設定例: https://i.imgur.com/lmYxXZD.jpeg

なお、「標準アプリとして設定」内の項目をOFFにしても、Haiagaru内で `*.5ch.io` のURLを開いた場合は、Haiagaru内で正常に開かれます。

情報提供: (ﾜｯﾁｮｲ 0de3-uUZJ)

## インストールできない場合

### パッチ適用時にエラーが発生して適用できない

Morpheでパッチを適用しようとした際にエラーが発生する場合は、Morphe右下の設定から以下のように変更してください。

`Morphe右下 → 別プロセスで実行 → 1024MB`

設定変更後、もう一度パッチ適用をお試しください。

以前のChMateをアプリデータを残してアンインストールした場合など、既存のパッケージとの競合が表示されることがあります。まずパッケージ名・署名の違いを確認してください。データを消す操作は最終手段です（過去の241での報告。現在241は対応対象外）。

1. ChMateの設定や必要なデータをバックアップします。
2. 以前のChMateを、アプリデータも含めて完全にアンインストールします。
3. パッチ済みAPKをインストールします。
4. 必要に応じて、手順1のバックアップからデータを復元します。

アプリデータを削除すると、バックアップしていない設定や履歴は失われます。必ずアンインストール前にバックアップを確認してください。

初期状態で有効なパッチ `Change ChMate package name` では、別アプリとしてインストールするための
パッケージ名に加えて、アプリ名、PNG/WebPアイコン、versionCodeを設定できます。
アイコン・versionCodeを未指定にした項目は元の値を保持します。

標準設定ではShizukuを組み込みません。旧ChMateの共有データをShizukuでコピーする場合だけ、
任意パッチ `Migrate ChMate data with Shizuku` を追加で有効にしてください。
この任意パッチを有効にしたAPKはAndroid 7.0（API 24）以降が必要です。
データ移行にはchmate本来のバックアップ/復元を推奨しています。

備考: パッケージ名の変更により予期せぬエラーが発生する可能性がありますが、
既存のChMateとは別アプリとして扱われるため、インストール時の競合エラーを抑えられます。

任意パッチ `Save ChMate crash logs` を有効にすると、未処理例外でクラッシュした際に
`Download/Haiagaru/` へログを保存します。投稿本文、Cookieなどのアプリデータは記録しません。

情報提供: あかまつさん

## URV Manager / Morphe Managerへの追加と更新

### Morpheへリポジトリとして追加

Morpheのパッチソースには、次のGitHubリポジトリURLをそのまま登録できます。

```text
https://github.com/areteruhiro/Haiagaru-Morphe/
```

[MorpheへHaiagaruを追加](https://morphe.software/add-source?github=areteruhiro/Haiagaru-Morphe&name=Haiagaru)

通常版URLは正式版 `1.7.1`、プレ版用URLは `1.8.9` を取得します。
配布物はAndroid拡張を内包したMPPです。

現在の正式版（1.7.1）を取得するパッチソースです。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/master/patches-bundle.json
```

プレ版 `1.8.9` を取得するパッチソースです。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/master/patches-bundle-pre.json
```

通常版とプレリリース版は別々に更新します。同じバージョン内で修正版を配布する場合は、
URV Manager / Morphe Managerが更新を検出できるようにJSON上の配布リビジョンを更新します。
更新が表示されない場合は、パッチソース画面から手動で更新を実行してください。
Morpheで「プレリリース」を有効にしたリポジトリソースは、`dev/patches-bundle.json`を参照する場合もあります。この取得先も1.8.9に揃えています。GitHubの配信キャッシュにより反映に数分かかる場合があります。
変更履歴で「このリリースは空です」と出る場合は、Morphe Managerも最新版へ更新してください。

## 更新履歴

各バージョンの変更内容は [CHANGELOG.md](CHANGELOG.md) を参照してください。

## 対象

- パッケージ: `jp.co.airfront.android.a2chMate`
- バージョン: `0.8.10.191 dev`（versionCode 459、minSdk 21）
- バージョン: `0.8.10.226 dev`（versionCode 494、minSdk 23）
- バージョン: `0.8.10.242 dev`（versionCode 512、minSdk 23）
- 元APKの署名 SHA-256:
  `7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2`

## ビルド

```powershell
.\gradlew.bat :patches:buildAndroid --no-daemon --max-workers=1
```

生成物:

```text
patches\build\libs\patches-1.8.9.mpp
```

公開には`:patches:buildAndroid`で生成したMPPを使用し、`classes.dex`が含まれることを確認してください。

191 devの自動NG画像判定でTFLiteがクラッシュする場合は、パッチ適用時の
「191の自動NG画像判定クラッシュを修正」（`imageNgTfliteFix`）を有効にしてください。
既定では有効です。`false`を指定すると元APKのTFLiteライブラリを維持します。
このオプションは191 dev以外には影響しません。

Morphe Desktopでは `Haiagaru` を有効にして対象APKへ適用します。
APKは再署名されるため、Play版など署名が異なるChMateとはそのまま上書きできません。

## サポート
何かあればGitHubのIssueか
以下のサーバーで対応させていただきます。
お気軽にご質問等お願いします。
~~＊開発者自身がchmateを開かないため~~

[Haiagaru サポートチャンネル](https://discord.gg/ypTzS4VWaz)

## 寄付

開発の継続を応援していただける場合は、よろしければGitHubのStarだけでもお願いします。励みになります。
さらにご支援いただける場合は、以下から寄付を受け付けています。

- [Amazon Gift Card](https://www.amazon.co.jp/gp/product/B004N3APGO) Send to (areteruhiro@gmail.com)
- [PayPay](https://qr.paypay.ne.jp/p2p01_Cc1k5WqxClWy8HCG)

### 謝辞
yujirox 様 <br>
ﾜｯﾁｮｲ 8f01-uDul 様 <br>
toya0717 様<br>
MUMEI 様<br>
タケヨシキ様<br>
無名 様<br>
ご支援ありがとうございます。

皆様からのご報告・検証・ご支援が開発の励みになっています。

## 構成

- `patches/src/main/kotlin/app/morphe/patches/chmate/HaiagaruPatch.kt`
  - 対象メソッドの特定とバイトコードパッチ
- `extensions/chmate/`
  - ChMate内で動く設定UIと移植機能

ベースのビルドシステムとパッチ形式は
[Morphe patches](https://github.com/MorpheApp/morphe-patches) を使用しています。


## Credit

Original Tsubonofuta is developed by AioiLight. \
https://github.com/AioiLight/Tsubonofuta

Forked from Tsubonofuta (Modify), developed by nonnonstop. \
https://github.com/nonnonstop/Tsubonofuta

Forked from Binnosoko
https://github.com/Chipppppppppp/Binnosoko

Contribution <br>

LEINs Contribution<br>
LEINsに対して寄付/ご購入してくださった皆様

<br>
フォークされる方へ
<br>
必須ではありませんが、このリポジトリのURLを貼ってくれると嬉しいです
