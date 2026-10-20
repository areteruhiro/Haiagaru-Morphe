# 1.11.0適用後のクラッシュとフィルターチェック更新

2026-10-20、API36 x86_64の `emulator-5554` で確認。

## 再現と原因

- 191 dev: Android板の「ChMate用Morpheパッチ『Haiagaru』Part2」（1792102288）を開くと、`o.m9ExternalSyntheticLambda1.getView()` のVerifyError。広告行フックがフィルター拡張のView返却を元の描画呼び出しと誤認し、Viewへ再利用済みの位置引数を整数として渡していた。
- 226 dev: 同じスレの3レスにある画像サムネイルを開くと、`ImagesViewer2Activity.onCreate()` のVerifyError。動画ビューア任意パッチのGIF振り分けが、生存中のローカルレジスタv0（Activity参照）をBooleanで破壊していた。
- 226 dev: レスのフィルターダイアログで画像を選択しても、チェック表示が更新されない。件数更新が固定長のArrayAdapterリストをclearし、`UnsupportedOperationException`で後続のチェック更新を中断していた。加えて、Composeの遅延行から通常のBooleanArrayを参照しており、変更を直接監視できていなかった。

## 修正

- 191/226それぞれの元レス描画メソッドを型・名前・引数で識別し、元の位置引数が整数の間に広告行を処理する。
- GIF振り分けの結果には、super.onCreate後に使われないBundle引数を使用し、元のローカルレジスタを維持する。
- ダイアログのチェック表示をSnapshotStateListへ接続し、コールバック用BooleanArrayとも同期する。既定ArrayAdapterには更新可能なリストを渡す。
- ネイティブのフィルター選択が非同期で反映されるため、ダイアログ表示中だけ100ms間隔で選択変更を確認する。変更した項目だけ描画へ反映し、閉じた際はコールバックを解除する。
- タップしたチェック値は非同期処理中も保持し、古いネイティブ値で戻さない。ネイティブ値が一致したら保留を解除する。

## 検証

- `scripts/VerifyLegacyAdAdapter.java` は壊れた191 APKを拒否し、修正後191/226 APKを受理。元のView返却、アサーション、位置引数を検査。
- `scripts/VerifyResumeReloadHooks.java` で191/226の復帰処理における元命令・引数・分岐の保持を確認。
- 修正後191で指定スレの本文を表示。修正後226で同じスレの画像を標準のImagesViewer2Activityで表示。両操作後の新規crash bufferは空。
- 226の実スレからフィルターを開き、同じダイアログ内で画像チェックのオフ→オンを確認。件数更新の例外も解消。画面記録は `build/filter-off-proof226.png` と `build/filter-on-proof226.png`。
- 最終MPPは `build/crash-filter-immediate-final.mpp`（SHA-256 `C48D7EA3D9960820BAB00C5BD449CB13701E18D6563A78D823352C2089AEFCA5`）。191/226とも既存検証アプリへ上書き済み。

作業中の他チャットによる投稿者ID連鎖NGの修正は保持。今回の修正版はローカル検証用で、Release公開・バージョン更新は行っていない。
