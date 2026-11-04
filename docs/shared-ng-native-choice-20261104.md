# 共有NG除外の標準チェック一覧

dev.8でも操作できないとの追加報告。以前の直接Composeダイアログ試験で成功していた事実は保持するが、利用者環境での解消の証拠ではない。実機はADB未接続。UAが見える画面と利用版は非同期質問中。

共有NG除外の2入口（レスに一致する条件/全受信条件）だけandroid.app.AlertDialogと実ListView/CheckedTextViewへ変更し、Composeのdetached model変換経路を使用しない。ChoiceListで条件ID集合を正とし、bind時にもcheckedを反映。clearChoices後は集合からnative choice stateを再構築。検索と保存、保存時の他条件保持は維持。リスト高さは画面高の1/3を120〜320dpに制限しIME resize。共通Composeダイアログは変更しない。

通常Lint build2m21s、64tasks成功。MPP build/shared-ng-native-choice.mpp。3版適用/既知旧hashと署名照合install-r成功。各版の検証親メニューを実タップし、本番SharedNgResponseExclusion.handle→showResponseExclusionsへ進め、チェック右端/文字タップのchecked表示OFF→ON→OFFと保存が成功。12条件の実検索/末尾条件選択/隠れ選択保持/保存後に全受信条件画面へ開き直してチェック表示保持も成功。検索model15/UA21各版成功。合成ルールはメモリのみ、DB/DAT改変/通信/投稿/共有NG送信なし。設定/staticを復元しAVD表示/network復元・データ保持停止。対象app新規crashなし。ログbuild/shared-ng-native-choice-<version>-<glyph|text|workflow|search|ua>.logとsummary。実スレのnative長押しUIからの起点ではなく検証用親メニューであることは区別する。

初回試験のFrida setTitleはString/CharSequence overload不一致で止まった。明示Java StringとCharSequence配列で修正して全検証を完了。製品クラッシュとは区別する。

UAテンプレートはdev.7から14（標準）/15/16。4.0.3は旧出荷設定との完全一致移行文字列としてだけ残り、選択肢にはない。今回3版の本番設定カテゴリでも21項目を再確認。正式1.22.1には未反映。版不明のため利用者が古い配布物を使っていると断定しない。

この候補は未公開。公開は別途実施していない。


2026-11-04 利用者が「検証用としてプレスリリースして」と指示。既存の静的修正方針を維持し、未確認項目を冒頭へ明記して既存Semantic Release CIからプレ公開へ進む。最終端末導入・ZIP往復・速度試験は追加実施せず、先行共有NG候補の試験結果とは区別する。
