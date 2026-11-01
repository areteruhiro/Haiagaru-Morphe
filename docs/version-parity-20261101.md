# 191・226・242の対応差の確認

対象は現在の検証用ChMate 0.8.10.191 dev / 226 dev / 242 dev。版の分岐には難読化名・UI構造の違いを吸収するものと、その版固有の不具合対策があるため、分岐の削除自体を目的にしない。

| 項目 | 対応・今回の判断 |
| --- | --- |
| 巡回失敗の板URL | 242純正イベントへの追記に加え、3版共通の「巡回エラーログ（板URL）」を履歴カテゴリへ追加。191/226には同じevent inbox処理がないため、workerの板取得呼び出しだけを包み、失敗を記録して元の例外をそのまま再throw。 |
| Web検索をChMateと別タスクで起動 | 191の専用ランチャーに加え、3版共通の既存Intent境界でACTION_WEB_SEARCHへNEW_TASKを追加。検索語・宛先・既存flagsを維持。通常のURL起動は変更しない。242のnative for-result経路でも適用。191の検索アプリ不在時fallbackは維持。 |
| 投稿ポップアップとIME | 226/242に適用。191は同じ不具合の報告対象ではなく、旧popupの構造も異なるため、既存の動作を維持。 |
| 板一覧の失敗キャッシュ | 3版対応済み。native HTTP504を成功の空一覧として扱わず、通常の再取得へ戻す。 |
| 設定UI・位置保持・色見本 | 共通実装、3版で確認済み。 |
| ID・名前・ワッチョイ検索のON/OFF | 共通実装、3版で確認済み。初期OFF、既存選択維持。 |
| Imgurアルバムのサムネ順 | 共通実装、3版で確認済み。 |
| 全スレメディア移動と戻る競合 | 設定された戻る方向の優先は共通。242の標準画像Fragmentによる上下終了だけ追加でnativeへ渡す。191/226と専用ビューアは従来の上下メディア移動を維持。 |
| 画像ビューア終了時のFragmentManagerクラッシュ | 242のVN10.GのFragment終了処理に対する対策。191/226は同じFragment経路を持たないため適用しない。 |
| スレ一覧のキーワード順 | 242のID200でグループ処理を飛ばすnative条件だけ補正。旧版へ242のcoroutineを移植するものではない。 |
| サムネ再bind/クリック経路 | 242の新しいCharSequence投影・再bind経路への補正。旧版では既存のgetter/adapterフックが同じメディア機能を担当。 |
| 191の通信詰まり対策 | 旧java.net/socket経路用の対策を含む。新しい通信経路へ旧版のsocket置換をそのまま入れない。共通通信設定と191固有のパッチを区別する。 |

## 共通の巡回エラーログ

「Haiagaru設定 → 履歴・コピーした記事 → 巡回エラーログ（板URL）」。最新100件を端末内だけに保存。板URL・時刻・例外クラス名のみで、Cookie・query・fragment・認証値・例外messageは保存しない。正常な巡回では書き込まない。1巡回の同じ板は重複を抑制し、履歴は明示的に消去可能。

242の純正イベント表示は維持。エラー履歴の検証データは試験後に元のpreferencesを復元する。通常NG・DAT・巡回対象・認証・通信方式を変更しない。

## 検証

各行の「共通」は関連コードの対応を表し、すべての既存機能を端末上で再実行したという意味ではない。今回の追加は共通巡回エラー履歴、191/226の板取得エラー捕捉、全版のWeb検索起動フラグ。

- 最終通常LintVital/R8 build成功2m31s。MPP `build/version-parity-final.mpp` SHA256 `a7fca9a480f08b0339fd37136c165bdff6a6de2579c9ad8282663d895c420dda`。
- emulator-5580へ既存署名を照合して3版更新成功。native191/226:各18項目、242:20項目、各album7/search38/Web検索6項目成功。エラー履歴の時刻・URL・例外種類、例外message除外、上限100、巡回sessionクリアを確認。試験preferencesは元へ復元。
- 191/226は実workerの新wrapperへnull readerと合成板URLを渡し、ネットワークへ進まず元のnative例外を返しながらURLを記録することを確認。実通信によるエラー発生からの巡回一巡の試験ではない。
- `VerifyLegacyPatrolErrors`でworkerの元register/引数/全命令/分岐先保持、元Exceptionの再throw、catchが板取得呼び出しだけに限定されることを確認。242は既存native UI hook検査成功。
- 全版で設定ホーム→履歴カテゴリ→巡回エラーログを実タップし、ダイアログの表示と閉じる操作を確認。`scripts/patrol-error-history-ui.py`、`build/patrol-error-history-{191,226,242}.png`。
- host NativeReaderRules609項目成功。host-only stubはAndroid保存処理を置き換えるもので、実際の保存処理は上記のAndroid runtime試験で検証。
- 最終エミュレータcrash buffer空。`build/version-parity-final-{191,226,242}-suite.json`。新たな実投稿・認証操作・NG登録はなし。
- 初回226試験はnativeがhttp URLを返すことを考慮せずhttps一致を要求して失敗。nativeのURLを変えず、host/pathとhttp/httpsの有効性を検査するよう試験側を修正して全版再実行。初期の命令検査でwrapperのJavaオブジェクト同一性を使っていた不備も名前判定へ修正。UI hierarchyの一時nullは再取得で確認。失敗を成功扱いしない。
- XIG05更新時は730eae72がADBから消えており失敗。今回の修正は実機へ未導入、直前のreader-ui-scroll版を保持。接続後は同じ3パッケージを署名照合install-rで更新する。公開/配布feedは未変更。
