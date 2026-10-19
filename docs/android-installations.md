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

## レビュー修正3件を含む191フル版（2026-10-20 生成済み・導入待ち）

- パッケージ・表示名・鍵・エイリアス・証明書SHA-256は上記full191と同一。鍵をCLIの `--keystore` で明示し、証明書一致を照合済み。
- APK: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/release-full191.apk`
- APK SHA-256: `532d3dfe48663cc384cd98221367c63990a844aba41b927c8df77cf316f3088a`（58,188,230 bytes）
- 使用MPP固定保存先: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/release-full191-input.mpp`
- MPP SHA-256: `2eeb7792c92c59bfc589309d30494e8d6cbc28c87a1cb52d28b933acf31e7d93`。dev.3公開元52c0f2eのローカルビルドで、CI生成物のバイト列とは異なる。
- 結果: 全7パッチ適用・再構築・署名成功。追加修正はZZZSearchの画面更新、小数キーボード、動画サムネイル生成設定の復元。XIG05が切断されたため、このAPKの実機更新は未実施。実機導入済みは上記の `latest-full191-separate.apk`。

## 復帰時クラッシュ修正版のエミュレータ導入（2026-10-20 JST、全て成功）

- 端末: API36 x86_64エミュレータ / `emulator-5554`。実機への導入は実施していない。
- 共通MPP: `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/resume-crash-verified-candidate.mpp`
- MPP SHA-256: `ABC2A8DF3D18B80487E430984192263AFFA4938C2EE8C2B61BB4718AF19F3A4B`
- 共通署名鍵: `C:/Users/narum/morphe-chmate-local/tools/morphe-data/morphe.keystore`、BKS、エイリアス `Morphe`。
- 証明書SHA-256: `0af4ddc1302b93481adb5c7d9657aae48d8d6733afc1e15d01129107a59c1eed`。191生成APKをapksignerで照合し、同じCLI署名設定で3版を生成。

| ChMate | パッケージ／表示名 | APK（主開発フォルダからの相対パス） | APK SHA-256 |
| --- | --- | --- | --- |
| 191 dev | `jp.co.airfront.android.a2chMate.haiagaru.full191` / Haiagaru 191 フル版 | `build/resume-verified191.apk` | `10DAF29866DE3BA1EAAC511683BB7A157F1EB3D4C0966BCE9D2FEFCA31E2E210` |
| 226 dev | `jp.co.airfront.android.a2chMate.haiagaru.resumefix226` / Haiagaru 復帰修正 226 | `build/resume-verified226.apk` | `33B264719E1C231F822587B2FC131F829892E824A463CE014EB5C22058B91463` |
| 242 dev | `jp.co.airfront.android.a2chMate.haiagaru.resumefix242` / Haiagaru 復帰修正 242 | `build/resume-verified242.apk` | `E5078E6BA9F73AF575832764E9C9E58DD1D78E7B417782EB714F38C2D013CFC7` |

絶対APKパスの共通先頭は `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/`。既存の標準パッケージは変更せず、191はエミュレータ上の同署名full191を更新、226／242は新しい検証用パッケージへ導入。各プロセスで復帰判定9項目に合格し、新しいクラッシュ記録なし。端末の全UI・通信経路の通し確認は含まない。

## 高度なNG API v2最終候補のエミュレータ導入（2026-10-20 JST）

- 端末: API36 x86_64 / emulator-5554。3版とも同署名の既存検証パッケージへ adb install -r 成功。
- 使用MPP: C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/advanced-ng-v2-verified.mpp
- MPP SHA-256: 0E0CA6D419345D393FD59499F6945B8F6C5FD00037BE6280AC3DCE6B14FFDA92
- 署名鍵: C:/Users/narum/morphe-chmate-local/tools/morphe-data/morphe.keystore（BKS）、エイリアス Morphe。既存の同署名検証APKの更新に成功。
- 証明書SHA-256: 0af4ddc1302b93481adb5c7d9657aae48d8d6733afc1e15d01129107a59c1eed

| ChMate | パッケージ／表示名 | APK絶対パス | APK SHA-256 |
| --- | --- | --- | --- |
| 191 dev | jp.co.airfront.android.a2chMate.haiagaru.full191 / Haiagaru 191 フル版 | C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/advanced-ng-v2-191.apk | C6DF18C56439ADD5B18D4DBFD137C0F1D29B7C63A3EFF74B85B245877B9E20B4 |
| 226 dev | jp.co.airfront.android.a2chMate.haiagaru.resumefix226 / Haiagaru 復帰修正 226 | C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/advanced-ng-v2-226.apk | 940E597C85B6253CB3F86F646C9076FFC6159DFD688D1004BCDF098074D5580B |
| 242 dev | jp.co.airfront.android.a2chMate.haiagaru.resumefix242 / Haiagaru 復帰修正 242 | C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current/build/advanced-ng-v2-242.apk | 7F39443BCF6BFAB41A17D4D710AEC52E1EF8277F32B607AB7D0801AEA3F41D13 |

高度なNG実コントローラーは191=22、226=22、242=15項目に合格。ZZZSearch／設定9項目に合格。実機更新と指定公開スレの通し確認は未実施。
