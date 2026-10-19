package app.morphe.extension.chmate

import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.PopupMenu

/** Existing menu actions are displayed in the shared expressive action dialog. */
class HaiagaruPopupMenu(context: Context, anchor: View) {
    private val model = PopupMenu(context, anchor)
    private val owner = context
    private var click: PopupMenu.OnMenuItemClickListener? = null
    private var dismiss: OnDismissListener? = null
    fun interface OnDismissListener { fun onDismiss(menu: HaiagaruPopupMenu) }
    fun getMenu(): Menu = model.menu
    fun setOnMenuItemClickListener(listener: PopupMenu.OnMenuItemClickListener?) { click = listener }
    fun setOnDismissListener(listener: OnDismissListener?) { dismiss = listener }
    fun show() {
        val menu = model.menu
        val rows = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible && it.isEnabled }
        HaiagaruDialog.Builder(owner).setTitle("操作を選択")
            .setItems(rows.map { (if (it.isChecked) "✓ " else "") + it.title }.toTypedArray()) { _, index ->
                click?.onMenuItemClick(rows[index])
            }.setNegativeButton("閉じる", null)
            .setOnDismissListener { dismiss?.onDismiss(this) }.show()
    }
}
