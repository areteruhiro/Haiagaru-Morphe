# 更新履歴の配置・版別設定と標準の外部板分類（2026-11-13）

## 設定画面

Haiagaru設定トップの検索欄に続き、「Haiagaru更新履歴」を共有・読み込み・最低限構成のボタンより上へ配置。カテゴリ一覧の旧項目と基本設定内の重複ボタンは撤去し、ネイティブの設定タブにも共有より前に同じ入口を追加しました。

一括の「機能のオン・オフを表示／閉じる」ボタンは削除。バージョンの開閉対象を本文と設定のコンテナに変更し、ReleaseFeatureOptionsに記録した安定版に対応する既存スイッチ・設定ボタンを表示します。1.25.0には追加・拡充した画像／NG／履歴／検索／エッヂ等、1.26.0にはオルグ板を配置。従来の保存先・初期値・変更時処理は維持します。投稿後自動監視など既存機能の拡充も1.25.0内に含め、各機能が1.25.0で初めて実装されたと断定しない見出しにしています。

開発版も表示する場合、安定版がある版の設定は安定版だけに配置し、安定版の公開前は同じ版の最新devへ配置します。今後の機能追加ではOptionへ公開先の安定版を登録し、そのバージョンは後続リリースでも変更しません。CHANGELOGからの同梱・GitHub取得・安定版／dev切替は既存の自動生成経路です。

## エッヂ・オルグ板がその他に分類された原因

SiteBoardGateway.sitePatternがHTTPSのみを認めていました。一方、標準の板分類はBoardIDの正規化URL（http://server/board/）をproviderへ渡します。191原本のBBSUrlInfo$BoardIDのcフィールドとMediationServiceImplaの分類呼出しを照合しました。そのためBBSMENUから板を取り込めても標準providerに一致せず、その他に入っていました。

direct providerの分類をSiteBoardPatternsへ切り出し、HTTP／HTTPS双方へ一致させました。エッヂcc／org、オルグ板kenmo.org／00c.orgを対象とし、類似ドメイン・認証付きURL・スレURLは除外。爆サイ・ふたば・あにまんと同じ既存のSiteNativeBoards provider経路でサイト別項目を作ります。任意の外部板を分割するexternalBoardSiteGroupsを強制ONにする処理は追加していません。

取得先はHTTPSのまま、板ID・ログ・カテゴリ・お気に入り・使用する設定を変更しません。新規native hookとレジスター変更はありません。新版適用後はChMateを再起動して板一覧を再構築してください。

## 確認

- ReleaseFeatureOptionsTest: 231項目成功（実CHANGELOG、安定版／dev、版の対応、重複なし、全22コントロールの保持）。
- ReleaseHistoryTextTest: 10項目成功。
- SiteBoardPatternsTest: 49項目成功（旧条件のHTTP分類漏れ、HTTP／HTTPSとドメイン境界）。
- 通常Lint／R8 buildAndroidはbuild/history-site-20261113-build.logへ記録。
- 利用者の検証省略指示を継続し、実機への導入・画面操作は行いません。ホスト判定を実機の表示結果と同一視しません。

Morphe用の累積差分は既存.releaserc生成元で追加20／変更修正34項目。今回の2項目を新しい版へ追記し、1.27.1の3項目はその版に固定しました。公開はdev→masterの既存Semantic Release CIを使用します。
