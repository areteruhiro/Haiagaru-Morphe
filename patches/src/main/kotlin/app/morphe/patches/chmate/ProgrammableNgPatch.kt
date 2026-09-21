package app.morphe.patches.chmate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val PROGRAMMABLE_NG = "Lapp/morphe/extension/chmate/ProgrammableNgController;"

/** Exact legacy hooks. Structural checks abort instead of patching a similar-looking method. */
internal fun BytecodePatchContext.patchProgrammableNg191() {
    check(packageMetadata.versionName == "0.8.10.191 dev")
    val titleFilter = mutableClassDefBy("Lo/r8lambdaGCnF6WpW_bFarRe7yCX2B6KzQ;").methods.single {
        it.name == "c" && it.returnType == "Ljava/util/ArrayList;" &&
            it.parameters.map(CharSequence::toString) == listOf(
                "Ljava/util/ArrayList;", "Lo/mgExternalSyntheticLambda0;", "Ljava/util/ArrayList;"
            )
    }
    val titleReturns = titleFilter.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
        if (instruction.opcode == Opcode.RETURN_OBJECT)
            index to (instruction as OneRegisterInstruction).registerA else null
    }
    check(titleReturns.size == 1) { "191 programmable NG title hook changed" }
    titleReturns.asReversed().forEach { (index, register) ->
        titleFilter.replaceInstruction(index, "move-object/16 p1, v$register")
        titleFilter.addInstructionsWithLabels(index + 1, """
            invoke-static/range {p0 .. p3}, $PROGRAMMABLE_NG->filterLegacyThreadRows(Ljava/lang/Object;Ljava/util/ArrayList;Ljava/lang/Object;Ljava/util/ArrayList;)Ljava/util/ArrayList;
            move-result-object v$register
            return-object v$register
        """)
    }

    val adapterOwner = "Lo/m9ExternalSyntheticLambda1;"
    val adapter = mutableClassDefBy(adapterOwner)
    val rebuild = adapter.methods.single {
        it.name == "c" && it.returnType == "V" && it.parameters.map(CharSequence::toString) == listOf("Z")
    }
    check(rebuild.implementation!!.instructions.any {
        val reference = (it as? ReferenceInstruction)?.reference as? FieldReference
        reference?.definingClass == adapterOwner && reference.name == "K"
    }) { "191 programmable NG response model anchor changed" }
    rebuild.addInstructionsWithLabels(0, """
        invoke-static/range {p0 .. p0}, $PROGRAMMABLE_NG->prepareLegacyResponses(Ljava/lang/Object;)V
    """)

    val responseFilter = adapter.methods.single {
        it.name == "SE_" && it.returnType == "I" &&
            it.parameters.map(CharSequence::toString) == listOf(
                "Lo/processAdDisplayErrorPostbackForUserError;", "Z", "Landroid/util/SparseIntArray;"
            )
    }
    val responseReturns = responseFilter.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
        if (instruction.opcode == Opcode.RETURN)
            index to (instruction as OneRegisterInstruction).registerA else null
    }
    check(responseReturns.size == 2) { "191 programmable NG response hook changed" }
    responseReturns.asReversed().forEach { (index, register) ->
        responseFilter.replaceInstruction(index, "move/16 p2, v$register")
        responseFilter.addInstructionsWithLabels(index + 1, """
            invoke-static/range {p0 .. p2}, $PROGRAMMABLE_NG->mergeLegacyResponseFlags(Ljava/lang/Object;Ljava/lang/Object;I)I
            move-result v$register
            return v$register
        """)
    }
}
