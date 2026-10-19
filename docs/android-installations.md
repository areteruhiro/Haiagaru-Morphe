# Android導入・署名台帳

他チャットは導入前にこの台帳と実APKを照合する。秘密鍵・パスワードは記載しない。開発版は別パッケージへの導入を標準とする。

## 既存191版の照合（2026-10-20）

- 端末: XIG05 / `730eae72`
- パッケージ: `jp.co.airfront.android.a2chMate.haiagaru`
- ChMate: `0.8.10.191 dev` / versionCode `2100000000`
- 署名証明書SHA-256: `bc78368b1426256afede71c43e87582e1481a56fe2ee45021d2ccc0fe9b6e7d5`
- 使用鍵: 未照合。`build/morphe-data-xig05/imported.keystore` は候補であり、一致は未確認。
- 保存APK: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/xig05-installed-191-before-update.apk`
- APK SHA-256: `E0958D0E1D54C0E80240F8DEF784AD8CFDCC8B78D53E96B3055761F567BEF3B8`
- 元APKは端末から取得。署名証明書は `apksigner verify --min-sdk-version 24 --print-certs` で照合。今回このパッケージは更新していない。

## 最新191フル版（2026-10-20 導入成功）

- パッケージ: `jp.co.airfront.android.a2chMate.haiagaru.full191`
- 表示名: Haiagaru 191 フル版
- 対象端末: XIG05 / `730eae72`
- 署名鍵: `C:/Users/narum/morphe-chmate-local/tools/morphe-data/morphe.keystore`
- エイリアス: `Morphe`
- 署名形式: BKS。BCプロバイダーを指定したkeytoolで上記鍵の証明書とAPKの一致を確認済み。
- 署名証明書SHA-256: `0af4ddc1302b93481adb5c7d9657aae48d8d6733afc1e15d01129107a59c1eed`
- APK: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/latest-full191-separate.apk`
- APK SHA-256: `d1e3ab42f45d130bbc6c17da4e2a661e74fbb0ff160d3a6855b39276df017794`（58,188,230 bytes）
- 使用MPPの固定保存先: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/latest-full191-installed-input.mpp`（ビルド出力からコピー。再ビルドによる上書きを避けて保持）
- MPP SHA-256: `af1753a667cae3a86f45ce4f9be680821d0b18ba257a62c5cb640210755c7c3b`（5,477,981 bytes）。公開済み1.10.0-dev.2とは内容が異なるローカル開発ビルド。
- 結果: 7パッチ適用・再構築・署名成功。XIG05 `730eae72`へ `adb install -r` 成功。起動後に同パッケージのHomeActivityがforegroundとなることを確認。
- APK内のSiteCards／SiteCardMetadata／GifViewerActivity、絵文字フォント、4 ABIのVPNとTFLiteライブラリを確認。サイトカード表示・タブ切替・全機能の実機操作は未確認。
- オプション: MEGA、Imgur、動画・GIF、VPN、絵文字フォント、191画像判定修正。汎用サイトカードは本体拡張に同梱。
