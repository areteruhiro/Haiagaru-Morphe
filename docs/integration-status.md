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

2026-10-19 長押しワッチョイ検索・DAT設定説明: 「探す・過去ログ」に検索メニュー表示スイッチを追加。明示変更時はID75の表示を優先し、既存のnative非表示設定からも戻せる。上／下4桁と他項目の標準チェックは維持。長いTextView説明を入力欄ラベルへ押し込めず、短いラベル＋選択可能な全文説明へ分離し、経路入力を最低6行表示。設定検索でも説明を対象にする。:patches:assemble :patches:buildAndroid 成功（build/settings-wacchoi-help-fix.log）。ビルドには進行中の読み位置保持変更も含まれ、今回の変更後のAPK適用・端末確認・公開は未実施。

2026-10-19 最新設定修正版の実機導入: ユーザー指示でXIG05（730eae72）の191別アプリHaiagaru 設定修正版（jp.co.airfront.android.a2chMate.haiagaru.composefix191）へ、settings-wacchoi-help191.apkをinstall -rで更新。生成APKの実パッケージと前回検証APKの署名SHA256一致を確認して導入、Success。元Haiagaruの別パッケージは変更・消去していない。最新ローカルMPP patches-1.10.0-dev.2.mppにはワッチョイ長押し検索表示スイッチ・DAT説明全文表示と進行中の読み位置保持変更を含む。設定Activityを起動してforeground（PID2063）を確認し、同PIDのcrash bufferは空。長押し操作と各設定変更の通し確認は未実施。

2026-10-19 ワッチョイ全文検索の両検索先切替（451d6f4）: 5chの全文検索で、IDチェッカーの検索先固定設定にかかわらずKyodemo／Hissiの切替を表示。Kyodemoの直接結果表示とHissiの日別一覧走査を区別して案内し、検索条件・元の板・ﾜｯﾁｮｲW等の元表記を引き継ぐ。HissiからKyodemoへ戻る操作を追加し、日付・通信上限等のHissi入力条件を再切替時に復元。Compose画面名・検索ボタン・全文入力欄を整理し、全文モードの上側／下側選択を非表示。実行中は切替を無効化。最終 :patches:assemble :patches:buildAndroid 成功（build/full-wacchoi-provider-ui-final.log、7m8s）。ローカルMPPはpatches/build/libs/patches-1.10.0-dev.2.mpp（公開済みdev.2と内容は異なる）。ビルドには並行作業中の読み位置・バックアップ等の変更も含まれる。今回のAPK適用・実機操作確認・インストール・公開は未実施。

2026-10-19 投稿検索の設定トグル即時反映: SearchOptionsSheet内でSharedPreferencesを直接読むだけではComposeが変更を追跡できず、親画面のみ再描画され、設定シートのチェック表示が閉じ直すまで古い状態で残っていた。検索利用・バックグラウンド・履歴保存・エッヂ表示・ZZZSearch表示の5設定を共通のCompose状態スナップショットへ変更し、保存変更リスナーで更新。設定シートに同じスナップショットを渡し、各トグル行に設定キーを付与。設定シートのチェックと検索先表示が同じ値を参照する。:extensions:chmate:compileReleaseKotlin 成功（build/search-options-immediate-compile.log、56s）。今回のMPP再生成・実機操作確認・インストール・公開は未実施。

2026-10-19 全文ワッチョイ検索の両方向切替: Kyodemo結果画面とHissi検索画面に現在の検索先と切替を表示。元の板・全文・ﾜｯﾁｮｲW等の表記、Hissiの日付・通信上限等のフォームを引き継ぐ。全文モードでは上側／下側選択を非表示にし、Hissiは日別一覧の全文照合で検索する。固定のIDチェッカー設定でも全文画面の切替は表示。最終ビルド `build/full-wacchoi-provider-ui-final.log` 成功（7m8s）。端末での切替操作・インストール・公開は未実施。

2026-10-19 GIF専用再生を追加: 動画専用ビューアの任意パッチにGifViewerActivityと、ImagesViewer2Activityで最初に選択したGIFの入口を追加。外部ACTION_VIEWの直接GIF URLにも対応。Android標準Movieによるアニメーション、再生／一時停止、バックグラウンド停止、再読み込み、回転、URLコピー、外部表示、DownloadManagerでのGIF形式保存を実装。動画URL判定や動画サムネイル抽出にはGIFを混ぜない。URL UyaGPfQ.gifはHTTP200・image/gif・4,370,061bytesを確認。`:patches:assemble :patches:buildAndroid` 成功（build/gif-viewer-support.log、3m38s）。生成MPPは開発中の他の未コミット変更も含む。端末再生・APK適用・インストール・公開は未実施。

2026-10-19 バックアップ・新ダイアログ（公開前）: ローカル／MEGAの入口を別のCompose画面へ分離し、保存対象の設定も分離（旧選択を初回継承）。MEGA復元の初期選択をバックアップのcategoriesへ変更し、注意事項から戻った場合の選択保持も修正。履歴DBの既存行へ復元するときもnativeのflag（BBSThread.rating）とbookmark_orderを反映する。既読・表示位置・新しいタイトルは端末側を維持し、双方向同期では端末側の★を残す。191標準復元にHaiagaru形式ZIPが渡った場合はsnapshot.jsonを検証して専用復元へ振り分け、空のDBは作らない。報告された実バックアップ自体は未取得で、互換性の確認は専用データによる。
ローカル手動／自動の保存は書き込み、通常ファイルのfsync、ストリーム終了、保存先のSHA-256読み直し照合まで行ってから完了にする。進行中・完了・失敗の通知、未許可時の通知許可ボタン、MEGA操作通知を追加。クラウド提供元のサーバー転送完了を保証するものではない。長い案内はGitHubへ接続。
HaiagaruDialogをCompose／Material 3 Expressiveで追加。バックアップの復元項目・確認・エラー・進行表示、MEGAログイン、自動バックアップ、設定の再起動確認、IDなしNG、投稿者強調、アーカイブ元URLを移行。既存の入力検証、ボタン差し替え、dismiss/cancel、表示中の保存先・状態更新を維持。描画関数名ContentがComposeView.Contentと衝突して再帰する不具合をAPI36エミュレータで検出し、DialogContentへ変更して修正。再ビルド成功（build/expressive-dialog-recursion-fix-build.log）。固定候補build/expressive-dialog-fixed.mpp SHA256 F84C4BB81E8EB9D970D3E37CA274A43FBA7FD598B288A45CAD5330F10DAA5769。
検証: 旧実機730eae72の別アプリで★復元、既読維持、双方向の★維持、categories、CookieのみZIP、従来JSON、保存先読み直しの7項目成功。API36の専用Java fixtureでは保存不一致の失敗判定とnative ZIP振り分けも含め9項目成功（build/backup-final-emulator-fixture.log）。新実機d5497008（Mi Note 10／Android 11）にcomposefix191を別パッケージで導入し画面起動確認。新ダイアログの再帰修正前の版を先に入れたため、その後の修正版更新と表示結果を追記する。既存haiagaru／haiagarutestは変更していない。公開操作は未実施。

2026-10-19 ダイアログ修正版の導入: 固定候補F84C4BB81E8EB9D970D3E37CA274A43FBA7FD598B288A45CAD5330F10DAA5769から191／226／242すべてのパッチ適用成功（build/expressive-fixed{191,226,242}-patch.log）。191の実パッケージjp.co.airfront.android.a2chMate.haiagaru.composefix191をaapt2で照合し、d5497008へ上書き成功・バックアップ画面起動成功。API36エミュレータでも同じAPKを導入し、新自動バックアップダイアログの描画成功をスクリーンショットとUI階層で確認（build/fixed-auto.png、build/fixed-auto.xml）。プロセス10097のクラッシュバッファは空。バックアップの9項目fixtureは成功済み。全ダイアログの操作確認、実ユーザーの1.8.15バックアップの読込、226／242の修正版画面確認、公開は未実施。旧実機730eae72に入れた版は新ダイアログ移行前で、現在は切断されている。

2026-10-19 折り畳み端末の自動表示切替を現在のHaiagaruへ統合: 旧作業フォルダBinnosoko.javaとbuild/verification/fold-layout/verification.md、probe/test-fold-layout.pyを参照。旧記録には191／226／242の411dp⇔686dp模擬幅変更の実行結果があり、単純recreateでは191が切り替わらず新しいHomeActivityが必要なことを確認。今回FoldLayout.javaを新設し、Application起動・構成変更・Activityのサイズ変更／復帰を監視。画面幅600dp以上でenableTabletHome2=true、未満でfalseへ変更し、新しい本体HOMEから表示中スレ／取得できた板URLを再表示。ReadingPositionに切替専用の論理レス番号・オフセット引継ぎと構成変更直前のアンカー保持を追加。投稿・設定・メディア画面、閲覧画面上のダイアログ／入力中は切替を保留。連続サイズ変更は350ms待ち、同じ表示区分では切替しない。設定は「スレを読む→画面と操作→画面幅でスマホ／タブレット表示を自動切替」、初期オフ。オフ後は現在の本体手動モードを維持し、自動変更しない。操作・仕様・旧解析の範囲はdocs/fold-layout.mdへ記載。
最終 :patches:assemble :patches:buildAndroid 成功（build/fold-layout-final.log、4m34s）。MPPはpatches/build/libs/patches-1.10.0-dev.2.mppで、公開済みdev.2と内容は異なり、他チャットの未コミット作業も含む。旧解析の模擬切替結果は新APKの確認結果ではない。今回の追加テスト・APK適用・実機／エミュレータの開閉・タブ／読み位置の実行確認・インストール・公開は未実施。

2026-10-19 Imgur投稿一覧: Haiagaru設定の書き込む／画像・動画の投稿に「メニューに Imgur投稿一覧 を表示」を追加。haiagaru_imgur_web/showUploadsMenu、初期true。書き込みメニュー生成とクリック処理が同じ設定を参照し、OFFなら既存の追加項目も除去する。任意Imgurパッチ導入条件を維持し、設定内・ログイン画面の入口は残す。ImgurUploadsActivityをJava/native ViewからKotlin/Compose/Material 3 Expressiveへ移行。カード、サムネイル・日付・画像/動画/処理中表示、URLコピー、元スレリンク、ページ送り、全選択、選択件数と削除アクションの下部表示、確認と結果ダイアログ、読込中・空・失敗・再読込を実装。既存uploadPage/deleteUploadsを使用し、アカウント確認・削除対象検証・無自動再試行を維持。サムネイルは既存のサイズ制限／Cookieなし・リダイレクトなしローダーへCompose用コールバックを追加。:extensions:chmate:compileReleaseKotlin :extensions:chmate:compileReleaseJavaWithJavac 成功（build/imgur-uploads-compose-compile2.log、51s）。今回のテスト実行・MPP再生成・端末インストール・実画面確認・公開は未実施。

2026-10-19 ★スレの初期表示: ツールバー項目追加時のFavoriteThreads.TABの初期選択をtrueからfalseへ変更。191の旧モデルと226／242のToolbarButtonSpecの両経路で未チェックにする。保存済みの本体ツールバー選択は書き換えない。この変更時点ではビルド・テスト・端末更新・公開は未実施（後述の全UI移行MPPビルドに含む）。

2026-10-20 全UI移行（公開前）: 独自画面・ダイアログを49ファイルの一覧で整理（docs/compose-ui-migration.md）。NG管理、コピー／検索履歴、レス保存、外部板編集・認証、アップロード管理、置換／テーマ、巡回画像、メディア操作をCompose／Material 3 Expressiveへ移行。既存Viewは保存・検証のモデルとして残し、表示はComposeの入力・選択・一覧へ変換。共通ダイアログに複数選択、長押し、フッター、独自ボタン処理、adapter更新通知を接続。エッヂ過去ログの表示位置をComposeスクロールへ引継ぎ、非表示WebViewの後片付けをmain Handlerへ変更。WebView／VideoView等の描画、OS画面、本体へ注入する入口は既存部品を使用。Javaソースの標準ダイアログ生成と直接setContentViewは残存なし。全画面の動作保証を意味しない。
拡張がDEX参照上限を超えたためsyncExtensionでR8の全DEXに別名を付け、chmate.mpe／chmate-2.mpe以降として保持。Patcher extendWithAllと全DEX共通のAndroidX／Kotlin隔離表を使用し、分割間の型・文字列参照も同じ名前へ変更。重複除外によるコード欠落を回避。
Kotlin／JavaコンパイルとAndroid MPP生成成功（build/ui-migration-package-build.log、4分3秒）。通常LintはKotlin FIRの内部例外でModelUi.kt／HaiagaruUi.kt解析に失敗し、成功した生成コマンドではlintVitalAnalyzeRelease／lintVitalReportRelease／lintVitalReleaseを除外。恒久的なLint無効化設定は追加していない。固定成果物build/ui-migration-compose.mpp SHA256 F614F6A9CC3B93376C1A68B28F89368AD9E391B6BB7741E4472D26857C80B75D。Android classes.dex、拡張DEX2本とComposeリソースAPKの同梱を確認。Imgur一覧／表示設定と★スレ初期未選択、他チャットの未コミット変更も含む。公開済みdev.2とローカルMPPの内容は異なる。今回の追加テスト実行・APK適用・全画面操作確認・実機インストール・公開は未実施。

2026-10-20 HOMEのDAT落ち非表示修正（公開前）: 191のHOMEアダプターは実表示リスト`j`を使う一方、従来処理は`g`／`i`だけを絞り込んでいた。さらにnativeの再構築と検索結果公開が実表示リストを後から置換していたため、未読・履歴・スレへDAT落ちが再表示されていた。191の`g`／`i`／`j`、226の`g`／`d`、242の既存AdapterData経路へ対応し、native通知直前にも現在の順序と検索結果を保った表示専用フィルターを適用。元リスト、DROPフラグ、DAT、履歴DBは変更しない。XIG05の191実データでは40行中9行のDAT落ちを確認し、修正版の実表示31行・遅い再公開後も0行、検索順序・元データ維持・「既読スレを上に」と同じチェック欄書式を含む確認に成功。修正版APKの実機インストールは未実施。

同じ表示専用処理をHOMEの「下書き／送信済み」へ拡張し、「DAT落ちスレの下書きを非表示」と「DAT落ちスレの送信済み・書き込み先を非表示」を独立設定として追加。初期値は両方オフ。本文や送信履歴を削除せず、オフに戻すと再表示する。新規作成中やDAT落ちを確認できない行は残す。API36エミュレータの191実プロセスへnative型を接続したfixtureで、4通りの設定、下書き／送信済み／書き込み先の選別、オフ復元、元データ維持の14項目に成功。226／242の投稿行を含む実画面操作は未確認。

2026-10-20 投稿者IDからの連鎖NG: PosterIdChainNg／PosterIdChainNgPatchを追加。標準NGWordの同じルールで連鎖(8)と投稿者IDもNG(256)が有効な場合に、同一IDの別レスもネイティブ連鎖状態表へ登録。高度なNGの有効化は不要。191／226の再構築時はキャッシュを破棄し、241／242／243はNGエンジン単位で分離。130件の判定テスト、5版APKへの本番フック単体適用・レジスター／モデル／NGWord一覧検査・NGクラスDEX出力、Java／Kotlinコンパイル、:patches:buildAndroid成功。実機表示は未確認。現行バンドルの配布対象191／226／242は維持し、241／243は今回のフック単体互換性のみ確認。

2026-10-20 任意資材によるMPP軽量化（公開前）: 追加資材を選択した場合だけ生成APKへ格納するよう変更。Noto Color Emoji（TTF）、191 dev画像判定修正（4 ABIのZIP／対応APK）、画像VPN（AAR）をMorpheのファイル選択で指定し、SHA-256を検証してから組み込む。未選択時は端末標準絵文字・本体の画像判定を使い、VPN任意パッチはチェックを外す。3資材のGitHub直接URLをMorpheのオプション説明へ掲載。画像判定ライブラリはGitHub Rawから取得できる `optional-assets/haiagaru-image-ng.zip` として生成し、Semantic ReleaseのAssetsにも添付する設定。初回ユーザー向け手順とGitHub配布元・ソース・ライセンスをdocs/optional-assets.mdへ記載。:patches:buildAndroid成功（2026-10-20、初回3分48秒・リンク更新後39秒）。別配布ZIPは7,196,692 bytes（SHA-256 66A7EAA6EF0FD05F036FC7C8232CFA4821D1B98CE969002E8919C4A8DAC4DEBA）。同日生成MPPは5,090,395 bytesで、NotoColorEmoji.ttfと4 ABIのTFLite .so、VPN JNI .soが含まれないことを確認。ZIPを含む変更はローカルのみで、GitHub公開・Morphe適用・APK生成・実機確認は未実施。

2026-10-20 最新191フル版を別パッケージで実機導入: Downloads/chamate/chmate191の未改変191 APKへ最新MPP（SHA256 af1753a667cae3a86f45ce4f9be680821d0b18ba257a62c5cb640210755c7c3b）から7パッチ適用・再構築・署名成功。汎用サイトカード・GIF・MEGA・Imgur・VPN・絵文字フォント・画像判定修正を同梱。XIG05 730eae72へjp.co.airfront.android.a2chMate.haiagaru.full191として導入成功、HomeActivityのforegroundを確認。全機能の操作確認は未実施。署名鍵と証明書、APKハッシュをdocs/android-installations.mdへ保存し、今後は別パッケージを標準にするルールをAGENTS.mdへ追加。

2026-10-20 dev.2以降の統合確認: 別チャットのレビュー3件を現在ソースでも確認し、ZZZSearchでCompose更新コールバックを解除しない修正、動画サムネイル生成スイッチの再配置、小数入力のKeyboardType.Decimal引継ぎを追加。Kotlin/Java・R8・Android MPP生成成功（build/pre-release-integration-build.log、3分21秒）。Lintの既知のFIR内部例外のためローカル最終ビルドではLint3工程を除外。今回導入した191フル版はこの3件追加前の固定MPPによるもの。公開差分・操作・未確認範囲はdocs/release-1.10.0-dev.3.mdへ整理。

2026-10-20 02:14 JST 1.10.0-dev.3プレリリース公開: 統合コミット52c0f2eから公式テンプレート由来Semantic Release CI（run 39291750955）が成功し、1.10.0-dev.3を公開。MPP 5,477,765 bytes / SHA256 b799ad2745d407e748b048c2700fc0808bd6e6c7c6872ff2b4911c7714c1468c、画像判定ZIP 7,196,692 bytes / SHA256 66a7eaa6ef0fd05f036fc7c8232cfa4821d1b98ce969002e8919c4a8dac4deba、bundle JSON2本の添付とprovenance成功を確認。ローカル版からレビュー修正3件込みの191フルAPKも生成・署名成功したが、XIG05切断により再更新は未実施。APKと使用鍵の照合結果・固定MPPをdocs/android-installations.mdへ記録。

2026-10-20 スレ表示時クラッシュ修正・正式版準備: 実機XIG05のfull191からo.pa.onResumeのVerifyError（0x107、v0 Boolean→Object）を取得。API36 emulator-5554の修正前191でもFragment生成で同一例外を再現。再読込抑制を独立したstaticメソッドへ移し、呼出元レジスタを一切上書きしない方式へ変更。191の初回空リスト読込d(ZZ)から自動復帰e(I,Object)への接続へ修正。226のd(I,Object)、242のe(I,Object)も同じ方式へ揃え、226の設定入口を追加。元APK／生成APKの3版比較でレジスタ数・引数・分岐・他のnative命令・初回読込の保持に合格（scripts/VerifyResumeReloadHooks.java、build/verify-resume-hooks.txt、verify-resume226-hooks.txt）。
最終ビルド成功（build/crash-resume-final-build.log、3分32秒。既知のLint FIR問題のためローカルではLint3工程を除外）。固定MPP build/resume-crash-verified-candidate.mpp SHA256 ABC2A8DF3D18B80487E430984192263AFFA4938C2EE8C2B61BB4718AF19F3A4B。191は全7パッチ・任意資材込み、226／242はMEGA・Imgur・動画を含め、3版の適用・再構築・署名に成功。エミュレータのfull191／resumefix226／resumefix242でnative Fragmentの生成と復帰ポリシー各9項目、計27項目に合格（scripts/frida-resume-reload-regression.js、build/emulator-resume{191,226,242}-regression.txt）。初回・同一Activity内の復帰・Activity自体の復帰・無効化・スマホActivityをfixtureで確認。設定値は各fixtureの最後に復元。対象アプリの新規crash bufferなし。実機への修正版導入、公開スレの本文表示・タブ切替による通信回数計測は未実施。ユーザー指定の5ch／jpnknのHTMLは読取取得したが、ADBから公開URLを起動する操作が自動承認でblocked by policyとなったため、その通し確認に成功したとは扱わない。
標準NGWordの投稿者ID連鎖は統合済み実装を維持。高度なNG APIは単一レスのみを入力にするため、動かない自動連鎖JavaScriptは配布せず、同じNGWordの連鎖＋投稿者ID設定で実現する登録例と5レスの判定表をdocs/ngword-poster-chain-example.mdへ追加。docs/advanced-ng.mdから案内。正式版docs/release-1.10.0.md、README、使い方、復帰時再読込の案内を更新。公開は既存release.yml/.releasercによるSemantic Releaseを使用し、CI生成メタデータを手編集しない。
