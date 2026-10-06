
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

詳細は [ID検索と専用ビュワー](docs/id-search-viewer.md) を参照してください。


### エッヂの記者ID表示

詳細は [エッヂの記者ID表示](docs/edge-reporter-id.md) を参照してください。


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

## HTTP通信のHTTPS切り替え

詳細は [HTTP通信のHTTPS切り替え](docs/https-switching.md) を参照してください。


## MEGAバックアップ（開発中）

詳細は [MEGAバックアップ](docs/mega-backup.md) を参照してください。


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
