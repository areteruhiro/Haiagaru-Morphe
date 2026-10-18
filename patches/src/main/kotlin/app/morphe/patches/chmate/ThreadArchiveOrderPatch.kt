package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.Opcode

internal fun BytecodePatchContext.patchThreadArchiveOrder(version: String) {
    val extension="Lapp/morphe/extension/chmate/ThreadArchiveOrder;"
    val type=when(version){
        "0.8.10.191 dev"->"Lo/r8lambdaYrwts3V6791pyBc7u3YcjNqd0NI;"
        "0.8.10.226 dev"->"Lo/AndroidLogHandler;"
        "0.8.10.242 dev"->"Lo/setAdCreativeClickListener;"
        else->return
    }
    val owner=mutableClassDefBy(type)
    val ctor=owner.methods.single{it.name=="<init>" && it.parameterTypes==listOf("Landroid/content/Context;","Ljava/io/File;")}
    ctor.implementation!!.instructions.withIndex().filter{it.value.opcode==Opcode.RETURN_VOID}.map{it.index}.asReversed().forEach{
        ctor.addInstruction(it,"invoke-static/range {p0 .. p0}, $extension->sort(Ljava/lang/Object;)V")
    }
    if(version!="0.8.10.226 dev"){
        val comparator=if(version=="0.8.10.191 dev")mutableClassDefBy("Lo/m6d\$2;") else owner
        val compare=if(version=="0.8.10.191 dev")comparator.methods.single{it.name=="compare"}
            else owner.methods.single{it.name=="b" && it.returnType=="I" && it.parameterTypes.size==2}
        val native=MutableMethod(ImmutableMethod(comparator.type,"haiagaruNativeArchiveCompare",compare.parameters,compare.returnType,
            compare.accessFlags,compare.annotations,compare.hiddenApiRestrictions,compare.implementation))
        val wrapper=MutableMethod(ImmutableMethod(comparator.type,compare.name,compare.parameters,compare.returnType,
            compare.accessFlags,compare.annotations,compare.hiddenApiRestrictions,
            ImmutableMethodImplementation(if(version=="0.8.10.191 dev")4 else 3,emptyList(),emptyList(),emptyList())))
        val params=if(version=="0.8.10.191 dev")"p1, p2" else "p0, p1"
        val originalCall=if(version=="0.8.10.191 dev")"invoke-virtual {p0, p1, p2}, $native" else "invoke-static {p0, p1}, $native"
        wrapper.addInstructions(0,"""
            invoke-static {}, $extension->enabled()Z
            move-result v0
            if-eqz v0, :native_order
            invoke-static {$params}, $extension->compare(Ljava/lang/Object;Ljava/lang/Object;)I
            move-result v0
            return v0
            :native_order
            $originalCall
            move-result v0
            return v0
        """.trimIndent())
        comparator.methods.remove(compare);comparator.methods.add(native);comparator.methods.add(wrapper)
    }
    println("Thread archive chronological sorting: $version")
}
