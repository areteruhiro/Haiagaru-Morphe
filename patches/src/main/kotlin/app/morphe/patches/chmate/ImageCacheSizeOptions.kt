package app.morphe.patches.chmate

import org.w3c.dom.Document
import org.w3c.dom.Element

/** Extend only ChMate's disk-cache preference, not bitmap memory or HTTP caches. */
object ImageCacheSizeOptions {
    private val originalLabels = listOf("300MB", "500MB", "1GB", "2GB")
    private val originalValues = listOf("300", "500", "1024", "2048")
    private val extraGiB = listOf(4, 8, 16, 32, 64, 128, 256)

    private fun arrays(document: Document): List<Element> {
        val nodes = document.getElementsByTagName("string-array")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun items(array: Element): List<String> {
        val nodes = array.getElementsByTagName("item")
        return (0 until nodes.length).map {
            val value = nodes.item(it).textContent.trim()
            // Morphe sanitizes numeric string-array items as \300 before patch execution.
            if (value.matches(Regex("\\\\[0-9]+"))) value.substring(1) else value
        }
    }

    private fun append(array: Element, values: List<String>) {
        values.forEach { value ->
            array.appendChild(array.ownerDocument.createElement("item").apply { textContent = value })
        }
    }

    private fun numericOptions(array: Element): List<String> {
        val escaped = array.getElementsByTagName("item").item(0).textContent.trim().startsWith("\\")
        return extraGiB.map { (if (escaped) "\\" else "") + (it * 1024).toString() }
    }

    @JvmStatic
    fun expand(document: Document): List<String> {
        val all = arrays(document)
        // Resolve both before modifying either: unknown layouts fail without partial changes.
        val labels = all.single { items(it) == originalLabels }
        val values = all.single { items(it) == originalValues }
        append(labels, extraGiB.map { "${it}GB" })
        append(values, numericOptions(values))
        return listOf(labels.getAttribute("name"), values.getAttribute("name"))
    }

    /** Keep locale/configuration overrides the same length as the default arrays. */
    @JvmStatic
    fun expandOverrides(document: Document, names: List<String>) {
        val changes = arrays(document).filter { it.getAttribute("name") in names }
        check(changes.all { items(it).size == 4 }) { "Unexpected image-cache override options" }
        check(changes.filter { it.getAttribute("name") == names[1] }.all {
            items(it) == originalValues
        }) { "Unexpected image-cache values" }
        changes.forEach {
            val isValue = it.getAttribute("name") == names[1]
            append(it, if (isValue) numericOptions(it) else extraGiB.map { gib -> "${gib}GB" })
        }
    }
}
