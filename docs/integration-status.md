# 主開発先と統合状況

更新日: 2026-10-18（日本時間）

主開発先は `C:/Users/narum/.codex/worktrees/945d/morphe-chmate/output/haiagaru-current`。
今後は他チャットもここで作業・ビルドする。親フォルダ、`StudioProjects/Haiagaru-release-1.8.9`、`StudioProjects/Haiagaru-Morphe` は参照用の旧作業先。

## 今回の統合

1.8.15公開元と同じコミット `31422922a24e06e1bdb3b4b2c90c553cf8f108f4` を基準に、旧releaseフォルダの未コミット実装差分と追加ソースを統合。主開発先にあったメニュー順序・閲覧履歴修正も維持した。

- Xプレビュー軽量化、カード表示、返信・リポスト・いいね・リンクコピー。
- 動画の速度切替、長押し中だけ2倍速。
- エッヂの関連・次スレ候補。
- スレッドアーカイブ日時順。
- 既読スレ削除ジェスチャーの画面探索修正。
- 書き込みメニューの個別表示。
- 投稿者強調のスクロール位置補正、復元DAT行の補完に関する旧作業先の実装。
- メニュー追加項目の順序・242表示設定、全期間の閲覧履歴に時刻不明行を含める補正。
- 親フォルダのワッチョイ上／下4桁検索画面と検索ロジック。標準ツールバー設定に検索2項目を登録し、既存の下4桁検索経路にも接続。板チェック・折り畳み・カレンダー・全対応板選択・再開・ZZZSearchを維持。
- 旧Haiagaru-MorpheフォルダのKyodemoテーマ属性補正。

配布物は単一のMPPバンドルにまとめる。動画ビューア等の既存のパッチ選択は維持する。開発時の内部識別は `1.8.16-dev.1`、配布候補は `1.8.16`。今回、正式・プレ両方の取得先を同じ1.8.16へ更新する。公開完了はGitHubのRelease・配布物・各メタデータを再取得して別途確認する。

## 検証の扱い

1.8.16配布候補: `:patches:build :patches:buildAndroid` 成功、Android用classes.dex入りMPPを確認。履歴内検索13ケース、人気レス7ケース、アーカイブ並び20ケース、関連スレ15ケースが成功。191／226／242元APKへの適用成功と初期versionCode=2100000000、226／242で動画／MEGA／Imgur任意パッチ込みの適用成功を確認。端末未接続のため実画面での通し確認・Morphe更新表示は未確認。

配布候補MPPのSHA-256: `DCA6ADEB29B122FCB2FB56500B898C5A0E7172BD09D24EB096DE058FB2651DA4`。

追加: 4桁検索の履歴・キャッシュ保持制限を任意指定（0は無制限）。MEGA／ローカル共通ZIPへ履歴・保存結果／ページキャッシュの個別カテゴリを追加（初期オフ）。MPPビルドとdiffチェック成功。専用Androidテスト `scripts/VerifyWacchoiStorage.java` はコンパイル・DEX化済みだが、実行直前にADB端末接続が切れたため実機実行・ZIP復元は未確認。アプリへのインストール・公開は行っていない。

追加: ワッチョイ上／下4桁検索履歴・条件・結果をファイルへ保存し、保存結果表示と再検索を分離。履歴保存切替・履歴／ページキャッシュ個別削除を追加。ページキャッシュをファイル領域へ移行。MPPビルド成功。Androidテストでキャッシュ利用による検索・結果保存と、別プロセスで履歴を再読込できることを確認。アプリ画面での履歴選択・削除は未確認、追加版の実機インストールは未実施。

追加: 上／下4桁の検索をActivity非依存ワーカーと通知付きフォアグラウンドサービスへ接続（設定初期オフ）。上4桁のワッチョイ長押し項目・個別表示設定を追加。Android app_process上のキャッシュ専用テストで、画面なしの検索完了、投稿者・スレ名・本文の保持、リスナー切断後の結果保持を確認。通知許可・サービス起動・画面再接続を含む実通信の通し確認は未実施。操作・制約は [バックグラウンド検索](wacchoi-background-search.md) を参照。

追加: 人気レスを直接返信数で判定するポップアップを実装（初期オフ、独立閾値初期3）。判定7ケースとMPPビルドを確認。191／242元APKへのパッチ適用を確認し、226はブリッジのObjectシグネチャへ補正後にフック適用を確認。実画面の位置・既読数維持は未確認。操作は [人気レス](popular-replies.md) を参照。

追加: Haiagaru設定ダイアログ内でChMate標準のジェスチャー設定を使えるよう接続。戻る／閉じる／先頭／末尾を設定画面の動作に割り当て、タップや通常スクロールを残す。1.8.16 MPPビルド・191元APKへの適用・XIG05上の同一署名更新・起動を確認。ジェスチャー操作自体の実機通し確認は未実施。

追加: ZZZSearchに「スレ名だけ」「書き込み内容だけ」の検索ボタンを追加し、どちらも4文字なしで実行できるようにした。条件の組み合わせも可能。Hissi日付／板検索とエッヂDAT検索は4桁を使うため、上4桁（ハイフン前）／下4桁（ハイフン後）の指定は維持。MPPビルド・191元APKへの適用・XIG05への更新・起動を確認。検索サービス上の結果は未確認。

追加: エッヂ現行スレのワッチョイ検索元をeddibb.ccのsubject/DATからKyodemoのエッヂ板・スレHTMLへ変更。板ページ送り、投稿内の `wc` 情報、50レス単位の続きページを巡回する。4桁検索時に残っていた任意の不正値エラー表示もZZZSearch開始時に消去する。`:patches:build :patches:buildAndroid` 成功、191元APK適用成功、XIG05（730eae72）の `jp.co.airfront.android.a2chMate.haiagaru` を同一署名で上書き・起動確認。検索結果を含む画面上の通し確認は未実施。

統合版の `:patches:build` と `:extensions:chmate:assembleRelease` が成功。
関連スレ判定15件、アーカイブ並び替え20件、DAT補正6件（実機由来ログで360→361行、バイト数不変）、コンパイル済み削除ジェスチャーAPI探索4件、書き込みメニュー表示切替、上／下4桁・板全選択・ページ継続・公開インデックス照合を確認した。

生成物: `patches/build/libs/patches-1.8.16-dev.1.mpp`

初回統合MPPのSHA-256: `6A625AB10F0CA217ADD5F56BCE2CF1CD330B266F9219B42CE83BD3F143C3EE1D`（その後の追加実装を含む再ビルドとは異なる）。

初回統合版191をXIG05の `jp.co.airfront.android.a2chMate.haiagaru.xpreviewfix191` へ同じ署名で上書きし、起動を確認。全機能の通し確認は未実施。公開操作も行っていない。

その後、ZZZSearchのアプリ内表示を追加。検索文字列を引き継ぎ、戻る・外部表示へ対応。JavaScript描画を維持するWebView方式で、追加版の端末確認・インストールは未実施。詳細は [ZZZSearch](zzzsearch-internal.md) を参照。

ビルド成功と、パッチの191／226／242への適用成功、端末上での動作確認を区別する。過去チャットでの個別検証を、統合APKの通し確認済みとは報告しない。

次の報告は引き続き調査中で、今回の統合だけで解決済みにしない。

- あぼーん／レス番号アンカーのタップが反応しない。
- Talkスレへ「進む」で戻る最初の操作が効かない。

旧作業先のCookie・キー・端末データ・APK・ビルドキャッシュは統合しない。元の未コミット変更は保全する。

2026-10-18 追加実装: 四文字固定・必須チェックを解除。Hissiはワッチョイ部分一致、エッヂはKyodemoの投稿ID（`ID:/GNQXmAUz` の `/` を保持）・スレ名・各レス本文をAND照合し、IDなしの本文単独検索にも対応。エッヂ開始ではZZZSearchへ切り替えない。現行／過去ログを選択でき、板ページ送り・50レスごとの続き・走査上限からの再開を通常／バックグラウンド両方に実装。検索条件を履歴へ保存する。ZZZSearch関連／エッヂ関連の個別表示スイッチを追加（初期オン、検索画面を開き直して反映）。本文抽出の入れ子、投稿者名・フッター除外、ID先頭スラッシュ、任意ID、複数語ANDのローカル確認が成功。最新版ビルドと実機更新の結果は以下に追記する。
最新版の :patches:build :patches:buildAndroid と191元APKへの適用が成功。Kyodemoの実際の続きページから50レスの本文・次ページリンクを読めることを確認。XIG05（730eae72）の既存Haiagaruへ同一パッケージ・同一署名で上書きインストール成功、プロセス起動確認（24559）。実機の検索画面で入力から結果表示・表示スイッチ切替までの通し確認は未実施。
2026-10-18 404修正: Kyodemo過去ログ一覧の先頭2件（1792331885、1792331852）が404 / Dat not found、3件目（1792331851）が200となることを再現。通常／バックグラウンドの両検索で、スレ本文・続きページの404を未収録として数え、次スレへ継続するよう修正。板一覧の404や403・429・サーバーエラーの停止は維持。
404修正版のMPPビルド・191元APKへの適用が成功。同一署名を照合してXIG05（730eae72）の既存Haiagaruへ上書きインストールし、起動プロセス（12367）を確認。検索画面からの通し確認は未実施。
2026-10-19 走査上限緩和: 検索画面の初期値を20から100へ、入力可能な最大値を1,000から10,000へ変更。Hissiは一覧ページ数、エッヂはスレ件数。通常／バックグラウンドの両方でエッヂの板一覧を1ページずつ取得・走査するよう変更し、大きな上限でも全一覧の先読み待ちを避ける。停止・続きから再開を維持。
走査上限緩和版のMPPビルド・191元APKへの適用が成功。同一署名でXIG05（730eae72）へ上書きインストール・起動を確認（22766）。10,000スレ／ページ全件の実機通し走査は未実施。
2026-10-19: ユーザー指示で1.9.0公開を保留。先にエッヂ検索の走査上限0＝無制限、正の整数の10,000件制限撤廃を追加。スレ名入力時はKyodemoのbs/kt/fr/to/k付き板検索URLから一覧を取得し、サイトの検索語付き次ページリンクを追う。Shift_JIS（Windows-31J）で日本語・特殊文字をエンコード。指定test URLのHTTP200と検索語付き次ページを確認、URL生成・日本語／特殊文字・現行／過去・空スレ名のローカル確認に成功。
無制限・スレ名検索URL版のMPPビルドと191元APKへの適用が成功。同一署名でXIG05（730eae72）へ上書きインストール成功。1.9.0のRelease作成・公開・配布メタデータ更新は行っていない。全件走査の実機通し確認は未実施。
2026-10-19: ユーザー指定でエッヂも1〜10,000スレへ戻し、0＝無制限を廃止。初期値100、スレ名のKyodemo一覧検索と検索語付きページ送りは維持。バックグラウンド起動引数も1〜10,000へ制限。1.9.0公開は引き続き保留。

2026-10-19 1.9.0候補: 通信回数上限を追加（開始／再開ごとのページ取得試行、0＝無制限、キャッシュ再利用を除外）。履歴へ保存、通常／バックグラウンド両対応。エッヂのレス続きページ位置も保持。境界・超過防止・再開リセット・無制限の単体確認に成功。ユーザー指示により公開手続きを再開し、1.7.1／1.8.15からの比較案内を新規作成。バージョンを1.9.0へ更新し、Android用classes.dex入りMPPビルド成功。候補SHA-256: 84D0B334F7EBB82E5266A932F99127D0A16B747D7C70E7FC8683AF63DAE9B587。

1.9.0最終候補確認: 191元APKの標準パッチ、226／242元APKの動画・MEGA・Imgurログイン任意パッチ込みの適用が成功。3版のパッケージ名・versionCode=2100000000を照合。通信上限／検索URLと人気レス7・関連スレ15・直接動画URL21・HOMEキーワード保持19の確認が成功。XIG05の既存191へ同一署名で1.9.0候補を上書き・起動。全機能・実画面の通し確認とは区別。
2026-10-19 1.9.0公開完了: タグ1.9.0（ソースdcb8a0e）、GitHub Releaseはdraft=false・prerelease=false・Latest=1.9.0。Android MPP同一内容の2名称、JSON3点、SHA256SUMSの計6配布物を登録。GitHubの6配布物digestをローカルと照合し、公開MPP再ダウンロードのSHA-256も84D0B334F7EBB82E5266A932F99127D0A16B747D7C70E7FC8683AF63DAE9B587と一致。master/main/devの正式・プレJSON計6取得先が1.9.0であることをGitHub APIから再確認。masterの利用者別案内2点とRelease本文リンクを確認。191候補をXIG05へ上書き・起動済み（20727）。Morphe端末UIでの更新表示と全機能の通し確認は未実施。公開URL: https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.9.0

2026-10-19 1.9.1候補: エッヂの照合対象を投稿ID／ワッチョイ上4桁／下4桁から選択できるよう追加。Kyodemo投稿ヘッダーのwcから抽出し、本文引用を除外。IDとワッチョイを結果に併記し、通常／バックグラウンド・履歴／再開へモードを引き継ぐ。合成ケースと実際の50レスを合わせた64項目の解析確認は成功。通知から検索画面へ戻るIntentへ検索条件を保持し、Activity再接続・再作成時に復元。画面再作成では編集中の条件を優先し、復元後の板選択も保持。通知からの復元操作を含む実画面の通し確認は未実施。ユーザー指示で今回の実機検証・インストールを省略して公開へ進む。

1.9.1公開候補のAndroid用MPPビルド成功。created_atは3配布JSONすべてタイムゾーンなしのYYYY-MM-DDTHH:mm:ss形式（Zなし）。MPP SHA-256: B03F58C96174CDE8090D290F55E96F441D37FB9290A9B715A53DB15381436517。

2026-10-19 Morpheメタデータ修正: 公開JSONのcreated_atに`Z`が付いた版があり、Morphe側のタイムゾーンなし日時形式と不一致だった。master/main/devの正式・プレ・版別JSONを確認・修正し、Raw URLを再取得。現在の正式／プレは1.9.2・created_atにZなし、参照MPPはHTTP 200を確認。Raw配信の最大キャッシュ時間は300秒で、更新直後に旧本文を返すことも確認。Morpheアプリ内の更新表示はADB未接続のため未確認。

2026-10-19 リリース手順: Morphe patches template由来のSemantic Release CIを導入。`master`を正式、`dev`をプレリリースとして設定し、既存のタグ・MPP名に合わせて`.releaserc`のtag／download URLを構成。READMEに生成用PATCHESマーカーがないためテンプレートのREADME自動生成ステップは含めず、既存の手書き案内を維持。手動メタデータ公開スクリプトを廃止し、今後の生成JSON／CHANGELOG／バージョン更新／ReleaseはCIへ一本化。Morpheアプリからの更新確認とGitHub Actionsの実行は未実施。

2026-10-19 1.9.1公開完了: タグ1.9.1のソース3755909、GitHub Releaseはdraft=false・prerelease=false・Latest=1.9.1。Android MPP2名称・JSON3点・SHA256SUMSの6配布物を登録し、全配布物のGitHub digestをローカルと照合。公開MPPを再ダウンロードしてSHA-256 B03F58C96174CDE8090D290F55E96F441D37FB9290A9B715A53DB15381436517と一致。最終MPPの191／226／242元APKへのパッチ適用成功。master/main/devの正式・プレJSON計6取得先を公開raw URLから再取得し、version=1.9.1・download_url・created_at=2026-10-18T16:01:28（Zなし）を確認。公開操作案内も取得成功。並行して行われた1.9.0の日時修正をdevへマージし、master/mainでも保持。1.8.16のversioned JSONをmaster/main/devとRelease添付から取得し、全4点が既に2026-10-18T20:00:00（Zなし）であることを確認、追加変更は不要。ユーザー指示により今回の実機検証・インストール・Morphe端末表示確認を省略。公開URL: https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.9.1

2026-10-19 1.9.2候補: DAT落ち非表示はnative DROPフラグのみを参照し、設定変更時も板一覧向け再描画しか行っていなかった。未更新フラグを公式5chの過去ログ格納済みバナーで非同期補完し、HOME表示コピーと191／226の元リスト復元を更新。404・現行一覧不掲載・通信エラーだけでは非表示にしない。報告スレ1792155688はDAT404・現行一覧不掲載・公式HTML過去ログ表示を確認。実HTMLを含む判定21項目、API36エミュレータで191／226各51・242で17項目のネイティブ接続確認に成功。3版で報告スレのフラグ0からの確認、オフ復元、元モデル・flags不変、191／226の36回切替、242のAdapterData更新を確認。Android用MPPビルドと3版への適用成功、DEX接続・ポップアップ非接続も確認。署名SHA256は既存と一致。実機と実ユーザーの大量履歴を含む全画面確認は未実施。候補MPP SHA256: 9358D67880D2D07538DA27C714DFE0E8E205DDC02C3190C502B6B1EAA9D53129。created_atは3JSONともZなし。

2026-10-19 1.9.2公開完了: タグ1.9.2（ソース0897ff8）、Latest=1.9.2・draft=false・prerelease=false。Android MPP2名称・JSON3点・SHA256SUMSの6配布物を登録し、GitHub digestとローカルを全件照合。公開MPPを再取得し、Android classes.dex入りでSHA-256 9358D67880D2D07538DA27C714DFE0E8E205DDC02C3190C502B6B1EAA9D53129と一致。master/main/devの正式・プレ取得先計6URLを、クエリなしの実URLから再取得してversion=1.9.2・created_at=2026-10-18T16:55:56（Zなし）を確認。更新後のmain/devの正式JSONは一時旧版のRawキャッシュを返したが、切替後の全6URLを確認済み。操作案内も公開取得成功。公開スクリプトはPowerShellのISO日時自動変換で正常な文字列を誤拒否する問題をConvertFrom-Json -DateKind Stringで修正し、Zなし受理／Z付き拒否を確認（dff1f51）。API36エミュレータで191／226各51・242で17のネイティブテストを再確認、3版プロセスの起動を確認後に検証用エミュレータを終了。設定はテスト前の値へ復元、DAT／IDX／履歴DBは変更していない。実機と実ユーザーの大量履歴・全画面の通し確認は未実施。公開URL: https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.9.2

2026-10-19 1.9.3修正: 追加ツールバー項目のコンストラクターでnative選択フラグを上書きしていた接続を廃止し、191 setButtons／226・242 setToolbarStateの描画時だけ全体表示設定を適用する。標準のチェック・コピー・設定保存はnativeのまま。ローカル自動バックアップは保存先設定を同期保存し、開いたダイアログへ通知、URIの日本語表示と同じフォルダ再選択時の管理情報保持を修正。ローカル復元はバックアップ内のcategoriesを初期選択し、注意事項から戻る際も選択を保持。Android向け1.9.3候補ビルドと191／226／242元APKへの適用成功。今回の端末での操作確認・追加テストは未実施。並行作業のSemantic Release導入を保持して公開へ接続し、既存master/main/dev取得先への生成JSONミラーをCIに追加。Morphe公式bundle生成器のcreated_atがZなしになることと、Gradle publishの依存にbuildAndroidが含まれることを確認済み。

2026-10-19 コピペ省略2: nativeフィルタがスレ読込時に組み立てられるため、設定切替を再起動確認へ接続し、標準SharedPreferencesの保存完了確認も追加。191の標準判定（先行レス番号と本文照合）・OFF分岐は維持。端末の省略表示は未確認。公開処理は独自ミラーを廃止し、テンプレート既存設定で正式・プレJSONを生成、masterからdev/mainへ戻す構成へ変更。初回の別履歴統合のみ過去featの再昇格を抑止し、公開後に通常のminor判定へ戻す。

2026-10-19 1.9.3公開完了: Semantic Release CI 39195451170成功。公式Latest=1.9.3、draft=false/prerelease=false。公開MPPを再取得してGitHub digestとSHA-256 630268B4FB3512C6D17A0C3740286B4352296E4E0E7A951862CE1E81DC5168A8の一致、Android classes.dex入りを確認。master/main/devの正式・プレJSON計6公開URLすべて1.9.3、created_at=2026-10-19T03:37:30（Zなし）、参照MPPの公開取得成功を確認。コピペ反映修正版のローカルAndroidビルド・191適用成功。今回の端末操作は未確認。初回履歴統合用のfeat抑止は解除し、以後は通常のminor昇格へ復帰。公開URL: https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.9.3

2026-10-19 Compose設定・投稿検索UI: 設定一覧と全8カテゴリの項目、ワッチョイ／エッヂ検索、日付選択、保持上限、アイコン画像編集をCompose＋Material 3 Expressiveへ変更。元の保存キー・検証・再起動確認を既存リスナーで維持し、AndroidViewは使用しない。読む／探す・過去ログ／書き込む／画像・動画／NG／履歴／バックアップ／アプリ・通信へ再分類。全体の追加機能一覧、ツールバー表示の二重指定、エッヂ検索の本体ショートカット、長押しコピーの重複オンオフを廃止。ツールバーは標準選択フラグだけを使い、旧グローバル非表示・強制表示指定を参照しない。検索の有効化・通知・履歴保存・検索先表示・保持上限は検索画面内へ集約。検索オフでも入口を残し、停止を要求、保存データは保持。検索先の明示選択、条件／検索先復元、全板条件復元時の一覧準備、入力エラー表示と詳細欄展開に対応。
UI用0x7dリソース専用APKを整列して同梱し、API30以上のResourcesLoader／API21-29の独立AssetManagerへ接続。AndroidXライセンス・NOTICE同梱。最終 :patches:assemble :patches:buildAndroid 成功（build/compose-release-candidate.log）。最終MPP SHA256 D34A50A4A0439E6ED1707BE0E74A23A99093E07A4FC86E20412F5B5982BAE3C6。191標準、226/242動画・MEGA・Imgur任意パッチ込み適用が全て成功（build/compose-ui/final/）。3 APK内のUIリソース521258 bytes・SHA256 D7E466842A37321063F3F913500231E2A21927F957139B2BD1C3264C2704F527が元資材と一致。MPEは単一DEX・63636 method references／11053 classes。追加テスト・実機／エミュレータ操作・インストールは未実施。今回の変更はローカルdevのみで、push・公式Release・CI生成バージョン／メタデータ更新は行っていない。公開済み1.9.3とは別の開発候補。操作案内: docs/compose-settings.md。

2026-10-19 設定検索からのオン／オフ: 検索結果の設定行へnativeのチェック状態と有効状態のスナップショットを渡し、既存の保存リスナーを通した変更をComposeの再描画へ反映。検索結果に安定した行キーを付け、同じ検索語の設定変更時は200msの入力待ちと検索中表示への切替を省略。検索結果から直接変更・自動保存できる案内を追加。ツールバー等の専用画面へ移した設定は重複追加しない。:patches:assemble :patches:buildAndroid 成功（build/settings-search-toggle-build.log）。ローカルMPP SHA256: 5BDEF34B06EEFA5D390C3A2E6CE7605D07ECED0B946F4991D07832A78D72A0D1。今回の追加テスト・APK適用・実機／エミュレータ操作・インストール・push・公開は未実施。

2026-10-19 5chの「ﾜｯﾁｮｲで検索」からHissi切替: full-SLIP起動時にhissiTargetをnullへ固定しており、「hissi.orgで開く」で未対応板のToastになっていた。Hissiを選ぶ場合は既存の日別投稿者一覧検索へ元の板と上下両方のSLIPを渡す処理を追加。検索先Hissi固定、両方表示で前回Hissi選択の場合にも対応。以前使ったエッヂ／ZZZSearchの選択や初期android板への置換を防ぎ、実行中バックグラウンド検索の条件で初期入力を上書きしない。開始操作までは走査しない。:patches:assemble :patches:buildAndroid 成功（build/hissi-full-wacchoi-build.log）。ローカルMPP SHA256: 3D0AE5455FB9A688880CB12F9E144CA26554915E13F38BE496EDF7D230B6051F。追加テスト・APK適用・端末操作・インストール・push・公開は未実施。

2026-10-19 BEログインメニューへの画像投稿項目混入: 242 dev／Haiagaru 1.9.2での報告に対応。native ResEditFragment.d(Menu)はBE専用XML（0x7f0d0001）にも呼ばれる一方、従来フックはメニューを区別せず追加していた。191／226／241／242元APKの通常・ダイアログ両方のメニューXMLからversion／画像アップロード履歴のIDを確認し、両項目を持つメニューだけを拡張する共通判定を追加。Imgur投稿一覧とpreparePostingMenu経由のアップロード先・コピー履歴・外部板認証がBE側へ混入しないようにした。native BE項目は変更しない。:patches:assemble :patches:buildAndroid 成功（build/be-menu-upload-fix-build.log）。ローカルMPP SHA256: 532F40B235B246860423BC0AC0E6F680369436CBAABA157F9FF892DB04680355。追加テスト・APK適用・端末操作・インストール・push・公開は未実施。

2026-10-19 プレリリース1.10.0-dev.1公開完了: ユーザー指示によりdevへCompose UIと設定検索・Hissi切替・BEメニュー修正をpushし、既存Semantic Release CI 39239787523が成功。CI生成タグ／コミット545cd31、draft=false・prerelease=true。公開MPPとJSON2点を再ダウンロードし、GitHub digestとの全件一致を確認。MPP SHA256: 1C9AAFA3FDEA0DB9894D23E9FEF23179D4D245BD2A5FA5458E48AE6CD2415511。MPP本体のclasses.dex、Compose用リソースAPK（521258 bytes）、extensions/chmate.mpe（DEX magic 035）を確認。devの正式名／プレ名JSON両方の公開URLが1.10.0-dev.1・created_at=2026-10-19T10:47:24（Zなし）・公開MPP参照であることを確認。master/mainの正式JSONと公式Latestは1.9.3を維持。タグ内の操作案内も公開取得成功。今回の実機／エミュレータ操作・インストール・端末Morpheの更新表示は未確認。公開URL: https://github.com/areteruhiro/Haiagaru-Morphe/releases/tag/1.10.0-dev.1

2026-10-19 設定起動クラッシュ修正: XIG05（730eae72）の元HaiagaruからMain dispatcher未検出を確認。MPEにMETA-INF/servicesがない一方、Coroutines consumer rulesがFastServiceLoaderを無効化していたため、Android直接読み込みを維持しfactory名文字列も難読化へ追従させた。この段階の別191でEmptySemanticsElementのNoSuchMethodErrorを確認。R8のkeepで名前が残るAndroidX／Kotlinクラス533個を、MPEマージ前に専用名前空間へ移す型・クラス名文字列の書換えを追加。native側の同名クラスは変更しない。
最終 :patches:assemble :patches:buildAndroid 成功（build/compose-isolation-fix.log）。修正版ローカルMPP SHA256: 47D258E5765E0A3B2CB581ACA50E6749D8A75EEE3E168D3996D1DB32DCAD6FBC。191標準、226／242は動画・MEGA・Imgur任意パッチ込み適用成功。ユーザー指示で最初に191別アプリ（jp.co.airfront.android.a2chMate.haiagaru.composefix191、表示名Haiagaru 設定修正版）をインストールし、設定一覧の描画とforegroundを確認。その後226／242を別パッケージcomposefix226／composefix242へ導入、226の一覧・スレを読む、242の設定・バックアップ画面を確認。最終PID191=16815／226=13432／242=1957のcrash bufferはいずれも空。最後に191の設定一覧へ戻し、再表示成功。UI階層の証跡はbuild/compose-fix191-final-ui.xml、compose-fix226-category.xml、compose-fix242-ui.xml。全機能の通し確認・Morphe端末内でのパッチ適用は未確認。
注意: 現行バンドルの互換対象は191／226／242のみ。241生成時に全パッチが非対応でスキップされたことをインストール前に見落とし、標準パッケージjp.co.airfront.android.a2chMateを241へ更新してしまった（lastUpdateTime=2026-10-19 20:45:11 JST）。データ消去は実施していない。ユーザーへ説明済み。元Haiagaruの別パッケージは変更していない。241は検証成功に数えない。以後226／242はaapt2の実パッケージ名を確認してから導入した。
`n2026-10-19 長押しワッチョイ検索・DAT設定説明: 「探す・過去ログ」に検索メニュー表示スイッチを追加。明示変更時はID75の表示を優先し、既存のnative非表示設定からも戻せる。上／下4桁と他項目の標準チェックは維持。長いTextView説明を入力欄ラベルへ押し込めず、短いラベル＋選択可能な全文説明へ分離し、経路入力を最低6行表示。設定検索でも説明を対象にする。:patches:assemble :patches:buildAndroid 成功（build/settings-wacchoi-help-fix.log）。ビルドには進行中の読み位置保持変更も含まれ、今回の変更後のAPK適用・端末確認・公開は未実施。
