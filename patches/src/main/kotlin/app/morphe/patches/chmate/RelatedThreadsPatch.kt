package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import com.android.tools.smali.dexlib2.Opcode

internal fun BytecodePatchContext.patchRelatedThreads(version: String) {
    val type=when(version){
        "0.8.10.191 dev"->"Lo/r8lambdaGCnF6WpW_bFarRe7yCX2B6KzQ;"
        "0.8.10.226 dev"->"Lo/Yhp5;"
        "0.8.10.242 dev"->"Ljp/syoboi/a2chMate/ui/threadlist/ThreadListFragment;"
        else->return
    }
    val method=mutableClassDefBy(type).methods.single {
        it.name=="onViewCreated" && it.parameterTypes==listOf("Landroid/view/View;","Landroid/os/Bundle;")
    }
    val exits=method.implementation!!.instructions.withIndex().filter{it.value.opcode==Opcode.RETURN_VOID}.map{it.index}
    check(exits.isNotEmpty()) { "Next-thread search view exits changed: $version" }
    exits.asReversed().forEach{method.addInstruction(it,
        "invoke-static/range {p0 .. p0}, Lapp/morphe/extension/chmate/RelatedThreads;->onSearch(Ljava/lang/Object;)V")}
    println("Related next-thread candidates: $version -> $type")
}
