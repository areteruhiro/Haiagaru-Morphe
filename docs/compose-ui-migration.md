# Haiagaru UI移行一覧

Haiagaru独自の設定・追加ツールをCompose／Material 3 Expressiveへ統一するためのソース調査です。
本体ChMateのスレ一覧・レス本文・標準設定や、Androidのファイル／権限選択画面は別の所有画面です。

## 今回の移行

- NG管理、高度なNG、対象板の選択、ワッチョイNG。
- 検索履歴、保存結果内検索、エッヂ過去ログ、投稿検索の結果・コピー・日付選択。
- コピー履歴と分類、レス保存・保存用スレ・アーカイブの管理。
- 投稿コマンド、板設定、外部板編集・認証。
- アップロード先設定、Imgurログイン、標準／外部サービスのアップロード履歴・削除管理。
- 置換TXT、テーマ取り込み、巡回画像保存、ツールバーアイコンの確認。
- 動画・GIF・YouTubeの操作ボタン／メニューと動画再生コントロール。

一覧のクリック・長押し・複数選択・全選択・フッター、検証後のボタン差し替えを共通ダイアログから既存の処理へ渡します。
保存先・一覧・状態などの非同期更新は画面表示中にモデルを観測して反映します。
モデルのフォームViewは画面へ貼らず、Composeのフィールド・選択・ボタンとして描画します。

## 配布構成

Composeの追加後は拡張コードが単一DEXの参照数上限を超えるため、R8が出力した全DEXを
`extensions/chmate.mpe`、`extensions/chmate-2.mpe`以降として同梱します。
Morphe Patcherの`extendWithAll`で全ファイルを読み込み、全DEXで共有する型名変換表で
AndroidX／Kotlinのクラス名・参照・クラス名文字列を隔離します。
重複ファイルを除外してコードを欠落させる処理は使用しません。

## プラットフォーム部品を使う箇所

- WebView：認証・JavaScriptで描画される検索サイト、公式YouTube埋め込み。
- VideoView／GIF描画：再生・デコード・既存のタッチ操作。周囲の操作はComposeです。
- Androidの文書選択・権限確認：OS画面です。
- ChMate標準画面内のHaiagaru入口ボタンや、レスに挿入するショートカット：本体Viewへ接続します。
- OpenUrlActivity：描画しないURL転送専用です。

## ファイル別

| ファイル | 移行先 |
|---|---|
| `ArchiveOrigins.java` | Composeダイアログ |
| `ClipboardHistory.java` | Composeダイアログ |
| `DeleteReadThreads.java` | Composeダイアログ |
| `EddiArchiveSearchUi.java` | Compose画面 |
| `EdgeReporterHistory.java` | Composeダイアログ |
| `ExternalBoardAuth.java` | Composeダイアログ |
| `ExternalBoardAuthActivity.java` | Compose画面 |
| `ExternalBoardEditorActivity.java` | Compose画面 |
| `ExtraQuickFilters.java` | Composeダイアログ |
| `GifViewerActivity.java` | Compose画面 |
| `Haiagaru.java` | Composeダイアログ |
| `HaiagaruLocalBackupActivity.java` | Composeダイアログ |
| `HaiagaruMegaSync.java` | Composeダイアログ |
| `HiddenIdNg.java` | Composeダイアログ |
| `HissiMenuActivity.java` | Compose画面 |
| `HissiWacchoiSearchActivity.java` | Composeダイアログ |
| `ImgurWebLoginActivity.java` | Compose画面 |
| `LocalAutoBackup.java` | Composeダイアログ |
| `MediaUploadHosts.java` | Composeダイアログ |
| `MediaUploadManagement.java` | Composeダイアログ |
| `MediaUploadWebActivity.java` | Compose画面 |
| `NativeRestore191.java` | Composeダイアログ |
| `NativeUploadHistory.java` | Composeダイアログ |
| `NgBoardScopePicker.java` | Composeダイアログ |
| `NgManagement.java` | Composeダイアログ |
| `PatrolImages.java` | Composeダイアログ |
| `PopularReplies.java` | Composeダイアログ |
| `PosterHighlight.java` | Composeダイアログ |
| `ProgrammableNgDialog.java` | Composeダイアログ |
| `QuickFilterToolbar.java` | Composeダイアログ |
| `ReadThreadsFirst.java` | Composeダイアログ |
| `RelatedThreads.java` | Composeダイアログ |
| `ReplacementSettingsActivity.java` | Compose画面 |
| `ResponseArchive.java` | Composeダイアログ |
| `ResponseMenuSettings.java` | Composeダイアログ |
| `SettingsRestart.java` | Composeダイアログ |
| `ThemeImportActivity.java` | Compose画面 |
| `ThreadArchiveOrder.java` | Composeダイアログ |
| `ThreadCommands.java` | Composeダイアログ |
| `ThreadCommandSettings.java` | Composeダイアログ |
| `ToolbarIconsActivity.java` | Composeダイアログ |
| `VideoDownloads.java` | Composeダイアログ |
| `VideoViewerActivity.java` | Compose画面 |
| `VideoViewerSettings.java` | Composeダイアログ |
| `WacchoiHistory.java` | Composeダイアログ |
| `WacchoiStorageLimits.java` | Composeダイアログ |
| `WacchoiSuffixNg.java` | Composeダイアログ |
| `XPostPreview.java` | Composeダイアログ |
| `YouTubeViewerActivity.java` | Compose画面 |

## 確認範囲

2026-10-20: Kotlin／JavaコンパイルとAndroid用MPP生成が成功しました。
`build/ui-migration-package-build.log`（4分3秒）、固定成果物は`build/ui-migration-compose.mpp`です。
SHA-256: `F614F6A9CC3B93376C1A68B28F89368AD9E391B6BB7741E4472D26857C80B75D`。
Android用classes.dex、拡張のchmate.mpe／chmate-2.mpe、Compose用リソースAPKの同梱を確認しました。

通常ビルドのLintはKotlin FIR解析の内部例外で失敗しました（ModelUi.kt／HaiagaruUi.kt）。
配布物生成はlintVitalAnalyzeRelease／lintVitalReportRelease／lintVitalReleaseをコマンド引数で除外しています。
プロジェクトのLint無効化設定は追加しておらず、Lint成功とは扱いません。

Javaソースの標準AlertDialog.Builder／ProgressDialog／DatePickerDialog生成と直接setContentView呼び出しは残っていません。
ソースの移行と、各画面の入力・通信・長押し・キーボード・回転を端末で通し確認した結果は区別します。
今回の全画面操作確認・実機更新・公開はまだ行っていません。
