# Haiagaru for Morphe

ChMate `0.8.10.191 dev` / `0.8.10.241` / `0.8.10.243 dev` 対応のMorpheパッチです。 <br>
機能は以下を参照
https://github.com/areteruhiro/Haiagaru

## Features

* Remove ads (including margins)
* Modify User-Agent
* Remove MonaKey
* GitHubから更新できるDAT落ちスレ用検索プリセット
* パッケージ名・アプリ名・アイコン・versionCodeの変更
* Morpheで任意に有効化できるクラッシュログ保存

DAT落ちスレ用プリセットは、通常閲覧時ではなく設定画面の更新ボタンを押した時だけ、
[`presets/chmate-dat-fallen-search-urls.txt`](presets/chmate-dat-fallen-search-urls.txt) を取得します。
GitHubへ接続できない場合は、同じ取得先を収録した内蔵プリセットを使用します。
2ch.scの板一覧参照先は [`https://menu.2ch.sc/bbsmenu.html`](https://menu.2ch.sc/bbsmenu.html) です。

## インストールできない場合

ChMate `0.8.10.241`では、アプリデータを残したまま以前のChMateをアンインストールすると、再インストール時に既存のパッケージとの競合が表示される場合があります。

1. ChMateの設定や必要なデータをバックアップします。
2. 以前のChMateを、アプリデータも含めて完全にアンインストールします。
3. パッチ済みAPKをインストールします。
4. 必要に応じて、手順1のバックアップからデータを復元します。

アプリデータを削除すると、バックアップしていない設定や履歴は失われます。必ずアンインストール前にバックアップを確認してください。

パッケージ名変更版には、Shizukuを利用して旧ChMateの共有データをコピーする補助機能がありますが、chmate本来のバックアップ/復元を推奨しています。

初期状態で有効なパッチ `Change ChMate package name` では、別アプリとしてインストールするための
パッケージ名に加えて、アプリ名、PNG/WebPアイコン、versionCodeを設定できます。
アイコン・versionCodeを未指定にした項目は元の値を保持します。
新Shizuku APIを含むため、このパッチを有効にしたAPKはAndroid 7.0（API 24）以降が必要です。

備考: パッケージ名の変更により予期せぬエラーが発生する可能性がありますが、
既存のChMateとは別アプリとして扱われるため、インストール時の競合エラーを抑えられます。

任意パッチ `Save ChMate crash logs` を有効にすると、未処理例外でクラッシュした際に
`Download/Haiagaru/` へログを保存します。投稿本文、Cookieなどのアプリデータは記録しません。

情報提供: あかまつさん

## URV Managerへの追加と更新

パッチソースには次のURLを登録してください。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/master/patches-bundle.json
```

プレリリースを検証する場合のみ、次の `dev` ソースを登録してください。

```text
https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/dev/patches-bundle.json
```

パッチ本体のバージョンは `1.2.2` です。同じバージョン内で修正版を配布する場合は、
URV Managerが更新を検出できるようにJSON上の配布リビジョン（例: `1.2.2-r3`）だけを更新します。
更新が表示されない場合は、パッチソース画面から手動で更新を実行してください。

## 更新履歴

### 1.2.2-r9（プレリリース）

- ChMate `0.8.10.191 dev`／`0.8.10.241`／`0.8.10.243 dev` の全対応版でURL自動補正を有効化
- `kako.5ch.io`、`itest.5ch.io`、`2ch.sc` の自動DAT取得経路をスレッドURL起動時にも適用
- 191 devは `ResListActivity`、241／243 devは `Hilt_ResListActivity` の構造差に合わせて注入

### 1.2.2-r8（プレリリース）

- ChMate `0.8.10.243 dev` で画像添付時に発生していたメモリ不足、0除算、null関連のクラッシュを修正
- DAT落ちスレを `kako.5ch.io`、`itest.5ch.io`、`2ch.sc` から取得するプリセットを更新
- パッケージ名変更を初期状態で有効化し、アプリ名、PNG/WebPアイコン、versionCodeの変更に対応
- パッケージ名変更後のバックアップ復元に含まれる旧パッケージ参照を補正
- Shizukuを使った旧ChMate共有データのコピー補助を追加
- 広告非表示時に広告SDKと計測SDKの初期化を抑制
- 任意パッチ `Save ChMate crash logs` を追加。クラッシュ時に `Download/Haiagaru/` へログを保存
- 対応対象から ChMate `0.8.10.242 dev` を削除

### 1.2.2-r7（プレリリース）

- ChMate `0.8.10.191 dev` の添付解析用テキストから `sssp://`、HTTP(S)、制御文字形式のBEアイコントークンを入力段階で除去
- 本文側のBEアイコン描画は原文を使用するため維持

### 1.2.2-r6（プレリリース）

- ChMate `0.8.10.191 dev` のレス単体、スレ全体、表示変換の全添付経路からBEアイコンURLを除外
- `img.5ch.io/ico/marara_tya.gif` と `img.5ch.io/ico/kuma.gif` を含む `img.5ch.io/ico/` / `img.5ch.net/ico/` を対象化

### 1.2.2-r5（プレリリース）

- ChMate `0.8.10.191 dev` の添付抽出結果からBEアイコンURLを直接除外

### 1.2.2-r4（プレリリース）

- ChMate `0.8.10.191 dev` のBEアイコンを通常の添付ファイル一覧から除外

### 1.2.2

- ChMate `0.8.10.191 dev` のBEアイコン分類と表示を修正
- URV Managerが1.2.2内の修正版を検出できる配布リビジョンを追加

### 1.2.1

- Android版Morphe Managerで読み込めるDEX形式のパッチバンドルへ修正

### 1.2.0

- ChMate `0.8.10.191 dev` の画像アップロード時クラッシュを修正

### 1.1.0

- ChMate `0.8.10.191 dev` 対応
- `5ch.io` の表示・検索・書き込みに対応

## 対象

- パッケージ: `jp.co.airfront.android.a2chMate`
- バージョン: `0.8.10.191 dev`（versionCode 459、minSdk 21）
- バージョン: `0.8.10.241`（versionCode 511、minSdk 23）
- バージョン: `0.8.10.243 dev`（versionCode 513、minSdk 24）
- 元APKの署名 SHA-256:
  `7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2`

## ビルド

```powershell
.\gradlew.bat :patches:buildAndroid --no-daemon --max-workers=1
```

生成物:

```text
patches\build\libs\patches-1.2.2.mpp
```

Morphe Desktopでは `Haiagaru` を有効にして対象APKへ適用します。
APKは再署名されるため、Play版など署名が異なるChMateとはそのまま上書きできません。

## サポート
何かあればGitHubのIssueか
以下のサーバーで対応させていただきます。
お気軽にご質問等お願いします。
＊開発者自身がchmateを開かないため

[Haiagaru サポートチャンネル](https://discord.com/channels/1392057820316303362/1547235153347092572)

## 寄付

- [Amazon Gift Card](https://www.amazon.co.jp/gp/product/B004N3APGO) Send to (areteruhiro@gmail.com)
- [PayPay](https://qr.paypay.ne.jp/p2p01_RsY3yQavNdvx74da)

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
LEINsに対して寄付/ご購入してくださった皆様

<br>
フォークされる方へ
<br>
必須ではありませんが、このリポジトリのURLを貼ってくれると嬉しいです
