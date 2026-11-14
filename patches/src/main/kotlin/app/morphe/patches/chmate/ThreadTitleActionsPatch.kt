package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

internal fun BytecodePatchContext.patchThreadTitleActions(version: String){
    val type=when(version){
        "0.8.10.191 dev"->"Lo/q1aExternalSyntheticLambda2;"
        "0.8.10.226 dev"->"Lo/getDislikeManager;"
        "0.8.10.242 dev"->"Lo/zzbbk;"
        else->return
    }
    val owner=mutableClassDefBy(type)
    if(version!="0.8.10.191 dev"){
        val modern=version=="0.8.10.242 dev"
        val renderer=mutableClassDefBy(if(modern)"Lo/zzbbs;" else "Lo/getTtAdContainer;")
        val sites=renderer.methods.flatMap { method ->
            method.implementation?.instructions?.mapIndexedNotNull { i,insn ->
                val ref=(insn as? ReferenceInstruction)?.reference as? MethodReference
                val register=when{
                    modern&&insn.opcode==Opcode.INVOKE_STATIC_RANGE&&ref?.definingClass=="Lo/zaco;"&&ref.name=="c"&&
                        ref.parameterTypes.firstOrNull()=="Ljava/util/List;" -> (insn as RegisterRangeInstruction).startRegister
                    !modern&&insn.opcode==Opcode.INVOKE_DIRECT&&ref?.definingClass=="Lo/TTCeilingLandingPageActivity3;"&&
                        ref.name=="<init>"&&ref.parameterTypes.lastOrNull()=="Ljava/util/List;" -> (insn as FiveRegisterInstruction).registerG
                    else->null
                }
                register?.let { Triple(method,i,it) }
            }.orEmpty()
        }
        check(sites.size==1) { "$version thread title menu list anchor changed" }
        val (method,index,list)=sites.single()
        method.addInstructionsWithLabels(index,"""
            invoke-static/range {v$list .. v$list}, Lapp/morphe/extension/chmate/ThreadTitleActions;->appendNativeGroups(Ljava/util/List;)Ljava/util/List;
            move-result-object v$list
        """.trimIndent())
    }
    val parameters=listOf("Landroid/view/LayoutInflater;","Landroid/view/ViewGroup;","Landroid/os/Bundle;")
    val original=owner.methods.singleOrNull { it.name=="onCreateView"&&it.parameterTypes.map(CharSequence::toString)==parameters }
    if(original!=null){
        original.implementation!!.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_OBJECT }.asReversed().forEach { (i,insn)->
            val r=(insn as OneRegisterInstruction).registerA
            // Save the result in now-dead parameters; p0 remains the exact dialog fragment.
            original.addInstructionsWithLabels(i,"""
                move-object/16 p1, v$r
                invoke-static/range {p0 .. p1}, Lapp/morphe/extension/chmate/ThreadTitleActions;->wrap(Ljava/lang/Object;Landroid/view/View;)Landroid/view/View;
                move-result-object v$r
            """.trimIndent())
        }
    }else{
        val wrapper=MutableMethod(ImmutableMethod(type,"onCreateView",parameters.map { ImmutableMethodParameter(it,emptySet(),null) },
            "Landroid/view/View;",1,emptySet(),emptySet(),ImmutableMethodImplementation(4,emptyList(),emptyList(),emptyList())))
        wrapper.addInstructions(0,"""
            invoke-super/range {p0 .. p3}, ${owner.superclass}->onCreateView(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;)Landroid/view/View;
            move-result-object p1
            invoke-static/range {p0 .. p1}, Lapp/morphe/extension/chmate/ThreadTitleActions;->wrap(Ljava/lang/Object;Landroid/view/View;)Landroid/view/View;
            move-result-object p1
            return-object p1
        """.trimIndent());owner.methods.add(wrapper)
    }
}
