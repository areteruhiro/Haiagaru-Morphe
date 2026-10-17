package app.morphe.patches.chmate

import org.w3c.dom.Document
import org.w3c.dom.Element

/** Extend only the native automatic-patrol interval list. */
object PatrolIntervalOptions {
    private val oldValues = listOf("0", "15", "30", "60", "180", "360", "1440")
    private val oldLabels = listOf("しない", "15分", "30分", "1時間", "3時間", "6時間", "24時間")
    private fun arrays(doc: Document): List<Element> = doc.getElementsByTagName("string-array").let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }
    private fun items(array: Element): List<String> = array.getElementsByTagName("item").let { nodes ->
        (0 until nodes.length).map { nodes.item(it).textContent.trim().removePrefix("\\") }
    }
    private fun insert(array: Element, values: List<String>) {
        val anchor = array.getElementsByTagName("item").item(1)
        values.forEach { value -> array.insertBefore(array.ownerDocument.createElement("item").apply { textContent = value }, anchor) }
    }
    @JvmStatic fun expand(doc: Document): List<String> {
        val all = arrays(doc)
        val labels = all.single { items(it) == oldLabels }
        val values = all.single { items(it) == oldValues }
        val names = listOf(labels.getAttribute("name"), values.getAttribute("name"))
        expandOverrides(doc, names)
        return names
    }
    @JvmStatic fun expandOverrides(doc: Document, names: List<String>) {
        val changes = arrays(doc).filter { it.getAttribute("name") in names }
        check(changes.all { items(it).size == 7 }) { "Unexpected patrol interval options" }
        check(changes.filter { it.getAttribute("name") == names[1] }.all { items(it) == oldValues })
        changes.forEach {
            if (it.getAttribute("name") == names[1]) {
                val prefix = if (it.getElementsByTagName("item").item(0).textContent.trim().startsWith("\\")) "\\" else ""
                insert(it, listOf(prefix + "5", prefix + "10"))
            } else insert(it, if (items(it).any { label -> label.contains("分") }) listOf("5分", "10分") else listOf("5 min", "10 min"))
        }
    }
}
