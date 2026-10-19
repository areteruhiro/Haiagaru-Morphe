# Haiagaru 主開発フォルダ

このフォルダが、他チャットを含むHaiagaruの主開発・統合・ビルド先です。

- 絶対パス: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current`
- OneDriveではなく、このローカルフォルダで作業する。
- 開始時に `git status --short` と `docs/integration-status.md` を確認し、他チャットの未コミット変更を保護する。
- 旧作業先や親フォルダのソースを更新せず、ここへ差分を統合する。ビルドもこのフォルダから実行する。
- 配布用パッチは `patches/build/libs/patches-<version>.mpp`。1つのバンドルに集約するが、動画ビューア等の既存の任意パッチ選択は維持する。
- 機能の実装・ビルド成功・端末確認・公開済みを区別して報告する。バージョン表記だけで公開済みと判断しない。
- 実機への開発版導入は、ユーザーの最新指示により別パッケージを標準とする。明示指示なく既存アプリを置換・アンインストールしたり、データ消去したりしない。
- APK生成・導入時は、パッケージ名と利用した署名を他チャットから特定できるよう `docs/android-installations.md` に記録する。表示名、ChMate版、端末シリアル、APKの絶対パスとSHA-256、使用MPPのSHA-256、署名証明書SHA-256、署名鍵ファイルのローカル絶対パスとエイリアス、導入日時・結果を残す。秘密鍵やパスワード自体を文書・Gitへ保存しない。次回更新前に台帳と実APKのパッケージ・署名を照合する。
- 検証用既存パッケージ: 191=`jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191`、226=`jp.co.airfront.android.a2chMate.haiagaru.responsearchive226`、242=`jp.co.airfront.android.a2chMate.haiagaru.responsearchive242`。実機に別パッケージがある場合は現状を先に照合する。
- ADB対象シリアルを必ず明示し、エミュレータ向け操作を実機へ誤適用しない。
- リリースは [Morphe patches template](https://github.com/MorpheApp/morphe-patches-template) のSemantic Release CI（`.github/workflows/release.yml`）を使用する。独自のリリーススクリプトを作成・実行せず、手動でRelease／タグ／配布物を作成・アップロードしない。
- リリース設定が必要な場合は、テンプレート由来の既存`release.yml`と`.releaserc`だけを必要最小限で調整する。`patches-bundle*.json`、`CHANGELOG.md`、`gradle.properties`などCI生成ファイルを手動編集・コミットしない。既存リリースの不具合も、原則として修正版をSemantic Releaseで新規公開する。
- リリースコミットはConventional Commits形式を使う。開発は`dev`で行い、正式版はテンプレートの手順に沿って`dev`から既定ブランチ`master`へマージする。履歴を壊すforce-pushやsquash mergeをしない。
