
◆9qrWgYJJCo
◆sZdvPTT7TE
 # Haiagaru for Morphe

ChMate `0.8.10.191 dev` / `0.8.10.226 dev` / `0.8.10.241` / `0.8.10.242 dev` 対応のMorpheパッチです。 <br>

**0.8.10.243 devは非推奨です。一部実装していない機能があります。** パッチ適用は引き続き可能ですが、利用する機能に応じて191 dev／226 dev／241／242 devをご検討ください。

機能は以下を参照
https://github.com/areteruhiro/Haiagaru

最新版: [Latest](https://github.com/areteruhiro/Haiagaru-Morphe/releases/latest)

更新履歴: [CHANGELOG.md](CHANGELOG.md)  
最新版 `1.7.0` の概要: [1.7.0の更新内容](release-notes-1.7.0.md)

## Features

* Remove ads (including margins)
* Modify User-Agent
* 画像を含むHTTP通信のHTTPS切り替え（Haiagaru設定でON/OFF、初期値OFF）
* Remove MonaKey
* GitHubから更新できるDAT落ちスレ用検索プリセット
* 自動DAT取得経路の並べ替えと任意HTTPS経路の追加
* 自動DAT取得のON/OFF切り替え
* 古いDAT・過去ログの改行保持と`.io` URL直接起動時の自動DAT取得
* Talkの現行・旧形式板URLからの板一覧／スレ取得と書き込み互換処理
* モバイル回線固定時の投稿で失効したNetwork IDを再利用しない接続更新（Haiagaru設定でON/OFF、初期値ON）
* ChMate `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.243 dev` のスレ内広告行の非表示
* 5ch.io板が外部板扱いと5ch扱いで重複した場合の内部板一覧一括整理
* パッケージ名・アプリ名・アイコン・versionCodeの変更
* クラッシュログ保存（Morpheで無効化可能、初期値ON）
* エッヂ過去ログビューアーで開けない場合はエラーコードを表示し、`Download/Haiagaru` に診断ログを保存
* ChMate `0.8.10.191 dev`／`0.8.10.226 dev` のChMate+互換設定
  * 単発ID表示を省略
  * コピペ省略2
  * 荒らし省略
* 191 dev／226 dev／241／243 devのエッヂ板でスレタイ末尾に記者IDを表示（初期値ON、Haiagaru設定から切り替え）
* 端末内JavaScriptで複雑なNG条件を作れる「高度なNGルール」
* NGワード・NG ID・NG名前などの登録上限をHaiagaru設定から変更（0で無制限、4対応版共通）

### ID検索と専用ビュワー

IDを長押ししたときの検索先は、Haiagaru設定の「ID長押しの必死チェッカー」で選べます。
自動では5ch系の板にhissi.org、対応する外部板にKyodemoを使います。片方への固定や、
検索画面で切り替える設定も選べます。Kyodemoの検索結果は元の板・スレURLへ戻してChMateで開きます。
KyodemoへのID検索は、同サイトの「ID/ﾜｯﾁｮｲ」検索経路を使用します。専用ビュワーの
「ID/ﾜｯﾁｮｲ」ボタンから任意のID・ﾜｯﾁｮｲも検索できます。Haiagaru設定の
「KyodemoのID/ﾜｯﾁｮｲ検索を専用表示」をONにすると、検索結果をレス／スレ／名前の
タブで切り替え、レスのコピー操作を利用できます。初期値はOFFで、従来の表示を維持します。
ワッチョイは前半4文字に省略せず、選択したレスのラベル・全文・大小文字・記号を保持して検索します。
固定の桁数や英数字への制限は設けません。通常はShift_JIS、表現できない文字を含む場合はUTF-8で
URLエンコードし、`+`、`/`、`&` 等も検索文字として渡します。検索結果の有無やUTF-8検索への対応は
Kyodemo側の仕様・取得状況に依存します。括弧・空白等のヘッダー区切り、制御文字・入力長の上限は維持します。
同じ前半を持つ別の投稿が結果に混ざることがあります。

パッチ適用時のオプション「必死チェッカー専用ビュワー」は初期値ONです。OFFにすると、
必死チェッカー／Kyodemoの専用画面へのURL変換を省き、ChMateが元から使う動作を維持します。
エッヂ過去ログの専用画面はこの設定とは独立して利用できます。
ONの場合はhissi.orgとKyodemoの結果をChMate内で表示し、URLコピー、表示中テキストのコピー、
日付選択、文字サイズ変更、端末設定／ライト／ダーク／AMOLEDブラックの表示を利用できます。
操作ボタンは丸みのあるチップ型にまとめ、日付・文字サイズ・配色を画面上からすぐ変更できます。
「全レスをコピー」は表示ページのレス本文をまとめてクリップボードへ保存します。
各レスの長押しでは、レス番号・名前・ID・本文・レスのURL・ヘッダー＋本文を個別にコピーできます。
「選択」を開くと、レス中の必要な文字だけを範囲選択できます。
専用WebViewでは広告用のサブリソース、iframe、動的に追加される広告要素も遮断します。
設定画面のテーマ・文字サイズ・全画面表示は次回起動時にも保持されます。

検索先ごとに必要な通信のため、hissi.orgはHTTPを使う場合があります。これは専用ビュワーを
有効にしたAPKのみに平文通信を許可する設定です。KyodemoはHTTPSで開きます。

エッヂの過去ログは、Haiagaru設定の「エッヂの過去ログを検索」または
\`https://eddiarchive3rd.boy.jp/\` を「アプリで開く」から起動できます。
また、ChMate標準のホーム／板カテゴリ一覧／板のスレ一覧ツールバー設定に「エッジ過去ログ」項目を追加します。
必要な場合はツールバー編集画面から有効化・配置してください（初期状態では追加項目は無効で、押すとエッヂ過去ログ検索が開きます）。
標準のアーカイブ画像を表示します。別の画像を使う場合は、パッチ適用時の「エッジ過去ログのツールバー画像」にPNG/WebPファイルの絶対パスを指定してください。
ChMate内の検索画面でキーワード、AND/OR、除外語、曖昧検索、レス数、並び順、期間を指定できます。
検索結果のスレをタップすると、元の \`bbs.eddibb.cc\` スレをChMateで開きます。
検索結果は過去ログサイトから取得します。サイトの形式が変わって結果を表示できなくなった場合は、
「元サイトの表示に切り替える」から従来のWeb表示を利用できます。

ID検索ビューはAndroid標準のActivityとWebView、エッヂ検索はネイティブUIで構成しており、参照先アプリのAPKやソースコードは
同梱していません。画面機能の要件整理には[Desperate-checker-droidの公開説明](https://github.com/Kdroidwin/Desperate-checker-droid-by-kdroidwin)
を参照しています。
* ID長押しの検索先をhissi.org／Kyodemoから選択し、ChMate内の専用ビュワーで開く（4対応版共通）

外部板への書き込みでは、絵文字の表示に必要な結合子・異体字セレクタを送信時のコピーで補正します。
編集画面や投稿履歴の元データは変更しません。


### エッヂの記者ID表示

対応済みの全バージョンで、エッヂ板（`bbs.eddibb.cc/liveedge`）の板一覧取得時に、通常の
`subject.txt`ではなく記者ID付きの`subject-metadent.txt`を使用します。
対象URL以外は変更せず、HTTP/HTTPS、標準ポート、クエリ、フラグメントを保持します。
不要な場合はHaiagaru設定の「エッヂのスレタイ末尾に記者IDを表示」をOFFにしてください。

取得した記者IDはエッヂのスレッド番号に紐づけて端末内に保存します。
履歴タイトルの保存・読み込み時に補完するため、DAT落ち後や再起動後も取得済みの記者IDからNG登録できます。
導入後に一度エッヂの板一覧を更新してください。既にDAT落ちしており記者IDを一度も取得していないスレッドのIDは復元できません。

記者ID付きスレッドからNGThread追加を開くと、191 dev・226 dev・241・243 devの全対応版で
「記者IDだけをNG」を選択できます。191 devでは標準のNGThread編集画面へこの操作を追加します。
スレタイを手作業で削除する必要はなく、記者ID部分（`[xxxxxxxx★]`）を通常のNGThreadとして保存します。
登録内容の確認・削除は従来のNGThread設定で行えます。


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

## HTTP通信のHTTPS切り替え

ChMate設定 → Haiagaru →「HTTP通信をHTTPSへ切り替える（画像を含む）」をONにして保存します。
設定変更後は既存の設定と同様にアプリが再起動します。初期値はOFFです。

対応済みの191 dev・226 dev・241・243 devで、OkHttpのURL生成とJava標準の
`URL.openConnection` / `URL.openStream`を通るHTTP通信をHTTPSへ切り替えます。
掲示板だけでなく、画像取得や固定URLも対象です。`chtoio`とは独立した設定です。
ホスト、パス、クエリを保持し、ポート80はHTTPSの標準ポートへ切り替えます。
80以外の明示ポートは保持します。既にHTTPSのURLは変更しません。

証明書・ホスト名の検証は無効にせず、HTTPS失敗時にHTTPへ戻す再試行も追加しません。
HTTPS非対応の接続先が読み込めない場合は、この設定をOFFにしてください。
WebView内部のサブリソースやネイティブライブラリ独自の通信を含む、
全ソケットのHTTP遮断を保証する機能ではありません。
242 devは現在のHaiagaru対応一覧に含まれず、この変更で対応版を追加していません。

## MEGAバックアップ（開発中）

ChMate設定 → Haiagaru →「MEGAにバックアップ・復元」から、
「お気に入り・閲覧履歴」「NG設定」「ChMate・Haiagaruの設定」
「書き込み履歴（postDataList.json）」「書き込みメモ（kakikomi.txt）」を個別に選べます。
バックアップ時は選択項目だけを保存し、復元時にも対象を再確認します。
「MEGA → この端末」「この端末 → MEGA」の片方向同期と、双方の不足分だけを追加する
双方向同期を選択できます。復元前には変更候補を表示でき、設定から表示を省略できます。
双方向同期では既存の設定・履歴・NG・書き込みメモを消さず、不足している値だけを追加します。
自動同期は初期OFFで、分・時間・日単位の間隔を指定できます。自動同期はバックグラウンドでは
実行せず、アプリ起動時に前回の同期から指定間隔が経過していれば実行します。復元先では
MEGA側のバックアップが前回適用したものより新しい場合だけ復元します。

利用者自身のMEGAアカウントでログインします。Google CloudやOAuth Playgroundは不要です。
パスワードと2段階認証コードは保存せず、ログイン後のセッションだけをAndroid Keystoreで
暗号化して端末内に保存します。セッションを含むスクリーンショットやログは共有しないでください。
MEGA同期にはAndroid 7以降が必要です（ChMate本体の対応範囲は変更しません）。

MEGA上に`Haiagaru`フォルダを作成し、選択した項目のスナップショットを保存します。
別の端末からは同じMEGAアカウントでログインして復元できます。MEGAの暗号化通信を
使用しますが、ChMateの認証情報やCookieはスナップショットに含めません。
この試験機能は[MEGA用Kotlinライブラリ](https://github.com/acarlsen/kmp-mega)を使用します。
Cookie・ログイン情報・OAuth情報は設定同期の対象外です。

## インストールできない場合

ChMate `0.8.10.241`では、アプリデータを残したまま以前のChMateをアンインストールすると、再インストール時に既存のパッケージとの競合が表示される場合があります。

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

通常版URLとプレ版用URLは、どちらも正式版 `1.7.0` を取得します。
配布物はAndroid拡張を内包したMPPです。

現在の正式版（1.7.0）を取得するパッチソースです。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/master/patches-bundle.json
```

プレ版用のパッチソースです。今回は正式版1.7.0に揃えています。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/master/patches-bundle-pre.json
```

通常版とプレリリース版は別々に更新します。同じバージョン内で修正版を配布する場合は、
URV Manager / Morphe Managerが更新を検出できるようにJSON上の配布リビジョンを更新します。
更新が表示されない場合は、パッチソース画面から手動で更新を実行してください。

## 更新履歴

各バージョンの変更内容は [CHANGELOG.md](CHANGELOG.md) を参照してください。

## 対象

- パッケージ: `jp.co.airfront.android.a2chMate`
- バージョン: `0.8.10.191 dev`（versionCode 459、minSdk 21）
- バージョン: `0.8.10.226 dev`（versionCode 494、minSdk 23）
- バージョン: `0.8.10.241`（versionCode 511、minSdk 23）
- バージョン: `0.8.10.242 dev`（versionCode 512、minSdk 23）
- バージョン: `0.8.10.243 dev`（versionCode 513、minSdk 24）
- 元APKの署名 SHA-256:
  `7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2`

## ビルド

```powershell
.\gradlew.bat :patches:buildAndroid --no-daemon --max-workers=1
```

生成物:

```text
patches\build\libs\patches-1.7.0.mpp
```

公開には`:patches:buildAndroid`で生成したMPPを使用し、`classes.dex`が含まれることを確認してください。

191 devの自動NG画像判定でTFLiteがクラッシュする場合は、パッチ適用時の
「191の自動NG画像判定クラッシュを修正」（`imageNgTfliteFix`）を有効にしてください。
既定では有効です。`false`を指定すると元APKのTFLiteライブラリを維持します。
このオプションは191 dev以外には影響しません。

Morphe Desktopでは `Haiagaru` を有効にして対象APKへ適用します。
APKは再署名されるため、Play版など署名が異なるChMateとはそのまま上書きできません。

### ChMate+互換機能

191／226／243 devでは、Haiagaru設定から「単発ID表示を省略」「コピペ省略2」
「荒らし省略」を切り替えられます。設定を保存するとアプリが再起動します。
191では荒らし・コピペ2の判定処理にも有効化の修正を適用し、各設定がOFFの場合は
判定処理を登録しません。243は元の判定処理と設定条件を使用します。

この検査は191の2箇所の登録制限の除去、設定OFFの分岐の維持、
243の判定処理の維持を確認します。実際のレスの省略表示は別途実機で確認してください。

## サポート
何かあればGitHubのIssueか
以下のサーバーで対応させていただきます。
お気軽にご質問等お願いします。
＊開発者自身がchmateを開かないため

[Haiagaru サポートチャンネル](https://discord.gg/ypTzS4VWaz)

## 寄付

開発の継続を応援していただける場合は、よろしければGitHubのStarだけでもお願いします。励みになります。
さらにご支援いただける場合は、以下から寄付を受け付けています。

- [Amazon Gift Card](https://www.amazon.co.jp/gp/product/B004N3APGO) Send to (areteruhiro@gmail.com)
- [PayPay](https://qr.paypay.ne.jp/p2p01_Cc1k5WqxClWy8HCG)

### 謝辞

ﾜｯﾁｮｲ 8f01-uDul 様、UPLIFT利用料のご支援をありがとうございます。

ご寄付いただいた toya0717 様、MUMEI 様、無名 様、本当にありがとうございます。
皆様からのご報告・検証・ご支援が開発の励みになっています。

## 構成

- `patches/src/main/kotlin/app/morphe/patches/chmate/HaiagaruPatch.kt`
  - 対象メソッドの特定とバイトコードパッチ
- `extensions/chmate/`
  - ChMate内で動く設定UIと移植機能

ベースのビルドシステムとパッチ形式は
[Morphe patches](https://github.com/MorpheApp/morphe-patches) を使用しています。


### 高度なNGルール

ChMate設定 → Haiagaru →「高度なNGルール（条件・スクリプト）」から設定します。
初期状態はOFFで、既存のChMate NG設定には変更を加えません。

「NG条件を追加」には、スレタイ／レス本文のキーワード、正規表現、エッヂの記者ID、
自由記述JavaScriptのひな形があります。対象を全板または指定した板URLに限定でき、
直近に読み込んだデータで保存前に判定件数をテストできます。

判定関数は `function (text, options) { return true または false; }` の形で記述します。
`text` はスレタイまたはレス本文です。`options` には板URL、スレッド番号、レス数、勢い、
記者ID、レス番号、レスIDなど、取得できた項目だけが入ります。

スクリプトはJava/Android APIへアクセスできない制限付きインタプリタで実行し、
1ルール250ms、1回の一覧判定500ms、ルール32件などの上限を設けています。
エラーや上限超過時はその判定をNGにせず、設定画面へ理由を表示します。
設定と判定対象は外部へ送信しません。

スレ一覧とレス本文の判定は `0.8.10.191 dev`／`0.8.10.226 dev`／`0.8.10.241`／
`0.8.10.243 dev` に対応します。レス本文の一致結果は各バージョンの標準NGWordフラグへ統合します。

この機能はGPLv3の派生リポジトリ
[`testuser0123-web/Haiagaru-Morphe`](https://github.com/testuser0123-web/Haiagaru-Morphe)
の設計を参考に、現行コード構成と複数バージョン向け共通フックへ書き直したものです。
参考実装の作者アカウント: [`testuser0123-web`](https://github.com/testuser0123-web)
詳細な著作権・ライセンス表示は [`LICENSE`](LICENSE) と [`NOTICE`](NOTICE) を参照してください。


### 本文の文字列置換（外部TXT）

191 dev／226 dev／241／242 dev向け。243 devは対象外です。
Haiagaru設定 →「本文の文字列置換（外部TXT）」でON/OFF、サンプルの作成、
TXTの選択、再読み込みを行います。初期状態はOFFです。

サンプルは `Download/Haiagaru/ReplaceStr.txt` に作成します。既存のファイルは上書きしません。
外部のテキストエディタでUTF-8・タブ区切りに編集し、再読み込み後にスレを開き直してください。
ファイルを置き換えた場合やパッケージ名を変更した場合は、ファイル選択画面から再選択してください。

```text
; 次の列間は実際のタブ文字です。
<ex2>https://example■.com/	https://example.com/	msg
```

通常置換は「検索文字［TAB］置換後の文字」。`<ex>`（省略時も同じ）は大小文字を区別せず、
`<ex2>` は区別します。置換後を空にすると削除します。ルールは上から順に適用します。
`;`、`'`、`//` で始まる行はコメントです。

[JaneXenoのReplaceStr.txt仕様](https://w.atwiki.jp/janexeno/pages/76.html)を参考にした独立実装です。
完全互換ではありません。対象は本文のみ（`msg`、省略、`all`も本文のみ）。
名前・日時・スレタイ・正規表現・URL/タイトル条件は未対応で、指定した場合はエラーになります。
ファイルは128KB、ルール256件まで。不正なTXTや読み取りエラー時は置換せず表示します。

画面表示用の本文コピーをリンク認識前に置換するため、置換後のURLをリンクとして認識します。
保存DAT、投稿本文、元のNG判定は変更しません。全体の `■` を一律削除するようなルールは
URL以外にも適用されるため、なるべくURLを含む具体的な文字列を指定してください。
掲示板の投稿規約に従って利用してください。

## Credit

Original Tsubonofuta is developed by AioiLight. \
https://github.com/AioiLight/Tsubonofuta

Forked from Tsubonofuta (Modify), developed by nonnonstop. \
https://github.com/nonnonstop/Tsubonofuta

Forked from Binnosoko
https://github.com/Chipppppppppp/Binnosoko

Contribution <br>
Haiagaru Contribution<br>
yujirox 様 <br>

LEINs Contribution<br>
LEINsに対して寄付/ご購入してくださった皆様

<br>
フォークされる方へ
<br>
必須ではありませんが、このリポジトリのURLを貼ってくれると嬉しいです
