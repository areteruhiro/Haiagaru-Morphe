# スレ一覧最下部のNGグループを非表示

Haiagaru設定に「スレ一覧の最下部にあるNGグループを非表示」を追加。
標準はOFF（従来どおり）。ONで `NG (件数)` の見出しと、その展開内容を表示しない。
設定保存後は画面を再起動して一覧を再構築する。OFFで元の表示へ戻る。
NG登録、元のスレ一覧、過去ログは削除・変更しない。

## 実装

各版の一覧行を構築する関数の第2引数（NG表示用のリスト）だけを、ON時にnullへ置換。
全版とも元のコードでnullが「NGグループなし」として扱われることを確認した。
行を高さ0にする方法ではないため、空行やタップ領域を残さない。
展開状態に関係なく、見出しとNG本文行の生成をまとめて抑制する。

| ChMate | 一覧構築クラス |
| --- | --- |
| 191 dev | `o.m8a` |
| 226 dev | `o.getWriteBytesTotal$RemoteActionCompatParcelizer` |
| 241 | `ThreadListViewModel$RemoteActionCompatParcelizer` |
| 242 dev | `ThreadListViewModel$ComponentActivity` |

フックは正確な引数型とstaticフラグで1箇所に限定。
191では戻り値を元のArrayList型へcastする。通常スレ、キーワード見出し、並び順、
既読優先機能は元の処理を維持する。

## 検証

`VerifyNgThreadGroup`：ON/OFF、元リストの同一性・内容維持、再表示、null、空リスト。
`VerifyNgThreadGroupHooks`：生成APKのDEXを読み、正しいクラスの先頭でp1だけを
読み書きするフックが1箇所あることを版ごとに検査。

MPPビルド、4版への適用、生成APK4版のフック検査がすべて成功。
241のUPLIFT修正の回帰検査も成功。実機での画面確認・公開は未実施。
