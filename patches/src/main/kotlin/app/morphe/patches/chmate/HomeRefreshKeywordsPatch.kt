package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal fun BytecodePatchContext.patchHomeRefreshKeywords(version:String){
    if(version!="0.8.10.242 dev")return
    val method=mutableClassDefBy("Lo/ModuleInstallRequestBuilder;").methods.single {
        it.name=="d"&&it.parameterTypes==listOf("I","I","Lo/ApiApiOptionsHasAccountOptions;",
            "Lo/SI31;","Ljava/util/List;","Ljava/util/List;")&&it.returnType=="Lo/SafeParcelableClass;"
    }
    check(method.implementation!!.registerCount-method.parameterTypes.size-1>=2){"242 HOME sorter scratch registers changed"}
    method.addInstructions(0,"""
        move-object/from16 v0, p0
        move-object/from16 v1, p6
        invoke-static {v0, v1}, Lapp/morphe/extension/chmate/HomeRefreshKeywords;->remember(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;
        move-result-object v1
        move-object/16 p6, v1
    """.trimIndent())
    for(owner in listOf("Ljp/syoboi/a2chMate/ui/home/HomeViewModel\$1\$5;","Ljp/syoboi/a2chMate/ui/home/HomeViewModel\$4\$3;")){
        val emit=mutableClassDefBy(owner).methods.single{it.name=="emit"}
        val body=emit.implementation!!.instructions.toList()
        val calls=body.indices.filter{index->
            val ref=(body[index] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass==method.definingClass&&ref.name==method.name&&ref.parameterTypes==method.parameterTypes
        }
        check(calls.size==1){"242 HOME refresh call changed: $owner"}
        val index=calls.single();val call=body[index] as RegisterRangeInstruction
        check(call.registerCount==7)
        val last=call.startRegister+6;val constant=body[index-1]
        check(constant.opcode==Opcode.CONST_4&&(constant as NarrowLiteralInstruction).narrowLiteral==0&&
            (constant as OneRegisterInstruction).registerA==last){"242 HOME refresh null argument changed: $owner"}
        emit.replaceInstruction(index-1,"invoke-static/range {v${call.startRegister} .. v${call.startRegister}}, Lapp/morphe/extension/chmate/HomeRefreshKeywords;->refresh(Ljava/lang/Object;)Ljava/util/List;")
        emit.addInstructions(index,"move-result-object v$last")
    }
    println("242 HOME refresh: retain current keywords when native refresh omits the list")
}
