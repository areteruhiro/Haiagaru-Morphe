# Haiagaru 1.9.2

DAT落ちスレを非表示の修正版です。ネイティブのDAT落ちフラグが未更新でも、公式5chの過去ログ格納済み表示を確認して補完します。設定切替・判定完了時にHOMEの未読／履歴／スレ一覧を再描画します。

- 設定をオフにすれば一覧を復元。DAT・IDX・履歴は削除しません。
- 通信失敗・404・現行一覧からの欠落だけでDAT落ちとは判定しません。初回は確認後に非表示へ反映します。
- 配布JSONの `created_at` は **Zなし** の形式です。

- [今回の修正・操作・確認範囲](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.9.2.md)
- **正式版1.7.1をご利用の方:** [累積の追加機能・変更点・注意事項](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.9.0-from-1.7.1.md)
- **プレ版1.8.15をご利用の方:** [累積の差分・操作案内・確認範囲](https://github.com/areteruhiro/Haiagaru-Morphe/blob/master/docs/release-1.9.0.md)

対応はChMate **191 dev／226 dev／242 dev**。元APKへ再パッチして更新してください。versionCode初期値2,100,000,000と241／243 dev非対応は従来どおりです。

確認済み: Android MPPビルド、3版への適用、エミュレータ上の191／226各51項目・242で17項目。報告スレのフラグ未更新時の確認と表示復元を含みます。実機・全画面の通し確認は未実施です。

`haiagaru_patches-1.9.2.mpp` と `patches-1.9.2.mpp` は同一のAndroid用MPPです。正式・プレ双方の取得先を1.9.2へ更新します。
