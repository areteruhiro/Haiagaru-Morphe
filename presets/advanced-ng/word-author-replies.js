// Haiagaru 1.11.0+: 「レス本文」に設定し、「NGワード」を自分の条件へ置き換えます。
// 標準NGWordの登録内容を自動で読むスクリプトではありません。
function (text, options) {
  return {
    match: text.includes("NGワード"),
    samePoster: true,
    chainReplies: true
  };
}
