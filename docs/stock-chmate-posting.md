# 通常版ChMateへエッヂの書き込みを渡す

Haiagaruのエッヂ書き込み画面のメニューに「通常版ChMateで書き込む」を追加する。
本文と、新規スレ作成時のスレタイを、通常版の公開Activityへ明示Intentで渡す。
通常版の画面で利用者が送信し、通常版に保存されたCookie・エッヂレベルを使う。
root、Shizuku、プロキシ、CA追加は不要。

## 操作

1. 同じAndroidユーザーに通常版ChMateをインストールし、エッヂの認証を済ませる。
2. Haiagaruでエッヂの書き込み画面を開き、本文と必要なスレタイを入力する。
3. メニューの「通常版ChMateで書き込む」→「通常版へ渡す」を選ぶ。
4. 通常版の投稿先・本文・名前・メール欄を確認し、通常版の画面で送信する。
5. Haiagaruへ戻ってスレを更新する。投稿済みの本文を二重送信しないよう下書きを確認する。

名前・メールは通常版の設定を使う。書き込み履歴・自分の書き込みマーク・Cookie更新も通常版側で管理され、Haiagaruへ自動同期しない。
通常版の結果通知を受け取る公開契約は未確認なので、Haiagaru側で成功扱い・自動再送・下書き削除をしない。
渡す前にネイティブ下書き保存処理を呼び、通常版を開けない場合にも本文を保持する。

## 連携仕様

- パッケージ固定: `jp.co.airfront.android.a2chMate`
- Activity固定: `jp.syoboi.a2chMate.activity.ResEditActivity`
- action: `android.intent.action.SENDTO`
- data: ネイティブ編集モデルから得た `https://bbs.eddibb.cc/liveedge/` または同ホストの `/test/read.cgi/liveedge/{正のスレID}/`
- `Intent.EXTRA_TEXT`: 現在の本文。改行・Unicode・引用をそのまま渡す。
- `Intent.EXTRA_SUBJECT`: スレ作成時の現在のスレタイのみ。
- 連携用のbooleanマーカーを追加。パッケージ変更版の自己画面遷移補正は、マーカー・action・通常版の固定Component・正規化済みエッヂURLがすべて一致する場合に限り、このIntentを通常版へ保持する。
- 通常版の起動はApplication context + `FLAG_ACTIVITY_NEW_TASK` を使用。ChMate独自ActivityによるSENDTOの内部編集画面への取り込みを避け、通常版のタスクで確認・送信する。
- 本文は128 Ki文字、スレタイは改行なし1024文字まで。超過時に切り詰めず停止する。
- URIのuserinfo・別ホスト・別ポート・query・fragmentを拒否。本文内URLから投稿先を決定しない。
- アプリ未導入・無効化・起動拒否時は、別アプリへフォールバックせず下書きを保持して案内する。

通常版248のManifestでActivityが`exported=true`、エディター側でdata・EXTRA_TEXT・EXTRA_SUBJECTを読み取ることを確認。
端末のresolve-activityでも同Activityを確認済み。元の通常版APKや保存領域を変更する処理はない。
Haiagaru側は既存の191/226/241/242用の編集画面フックを利用し、ネイティブ下書き保存処理の先頭のタイトル読み取りに対応するgetterを生成する。

以前のエッヂCookie移行・受信UI、ローカル受信サーバー、取り込み専用の永続化ブリッジ・テストは削除した。
通常版248はHTTPSを使うため、従来のHTTP受信方式ではCookieを取得できなかった。
旧テスト版で通常版のProxyを変更した場合は、通常版の設定で元に戻す必要がある。

## 検証

`scripts/StockChmatePostingRulesTest.java` は模擬本文・URLで対象の限定、スレURLの正規化、スレ立てと返信の区別、空本文・タイトル・IPCサイズ制限を確認する。
GitHub Actionsの `Verify stock ChMate posting` で上記テストとMPP生成を行う。
公開掲示板への投稿は検証に使用しない。
