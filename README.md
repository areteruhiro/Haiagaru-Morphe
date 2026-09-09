# Haiagaru for Morphe

ChMate `0.8.10.241` / `0.8.10.242 dev` / `0.8.10.243 dev` 対応のMorpheパッチです。
Haiagaru のXposed機能と設定画面を、Xposedなしで動作する
Morphe拡張へ移植しています。収録パッチは `Haiagaru` の1件だけです。

## 対象

- パッケージ: `jp.co.airfront.android.a2chMate`
- バージョン: `0.8.10.241`（versionCode 511、minSdk 23）
- バージョン: `0.8.10.242 dev`（versionCode 512、minSdk 23）
- バージョン: `0.8.10.243 dev`（versionCode 513、minSdk 24）
- 元APKの署名 SHA-256:
  `7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2`

## 移植した機能

- 広告の非表示
  - `0.8.10.241` の同一構造クラス `o.setUseHandlerThreadForCallbacks` を直接対象化
  - `0.8.10.243 dev` の上部広告クラス `o.zzexb` を直接対象化
  - `0.8.10.242 dev` の同一構造クラス `o.zzbgb` も直接対象化
- User-Agentの変更
- Cookie削除時のMonaKey削除
- `5ch.net` 表示の `5ch.io` 置換（`chtoio`）
- ChMate設定画面内の `Haiagaru` ボタンと全設定項目
- Morpheで再署名されたAPK向けの、ChMate固有署名検査互換処理

難読化名はバージョンごとに固定せず、継承元、フィールド構成、メソッド形状、
命令パターンを照合して各バージョンの対応先を選択しています。

設定値はChMate内の
`io.github.areteruhiro.chmate.haiagaru.ui-config` に保存されます。

## ビルド

```powershell
.\gradlew.bat :patches:jar --no-daemon --max-workers=1
```

生成物:

```text
patches\build\libs\patches-1.0.0.mpp
```

Morphe Desktopでは `Haiagaru` を有効にして対象APKへ適用します。
APKは再署名されるため、Play版など署名が異なるChMateとはそのまま上書きできません。

## サポート

[Haiagaru サポートチャンネル](https://discord.com/channels/1392057820316303362/1547235153347092572)

## 構成

- `patches/src/main/kotlin/app/morphe/patches/chmate/HaiagaruPatch.kt`
  - 対象メソッドの特定とバイトコードパッチ
- `extensions/chmate/`
  - ChMate内で動く設定UIと移植機能

ベースのビルドシステムとパッチ形式は
[Morphe patches](https://github.com/MorpheApp/morphe-patches) を使用しています。
