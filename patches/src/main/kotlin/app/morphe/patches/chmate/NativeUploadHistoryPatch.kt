package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val HISTORY = "Lapp/morphe/extension/chmate/NativeUploadHistory;"

internal fun BytecodePatchContext.patchNativeUploadHistory(version: String) {
    if (version == "0.8.10.191 dev") {
        val fragment = mutableClassDefBy("Lo/onRewardedInterstitialAdHidden;")
        val create = fragment.methods.single { it.name == "Ww_" && it.parameterTypes.size == 2 }
        val returns = create.implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }
        returns.reversed().forEach { (index, _) ->
            create.addInstruction(index, "invoke-static/range {p0 .. p1}, $HISTORY->addMenu(Ljava/lang/Object;Landroid/view/Menu;)V")
        }
        val select = fragment.methods.single { it.name == "VQ_" && it.parameterTypes == listOf("Landroid/view/MenuItem;") }
        check(select.implementation!!.registerCount >= 3)
        select.addInstructionsWithLabels(0, """
            invoke-static/range {p0 .. p1}, $HISTORY->handle(Ljava/lang/Object;Landroid/view/MenuItem;)Z
            move-result v0
            if-eqz v0, :native
            return v0
        """.trimIndent(), ExternalLabel("native", select.implementation!!.instructions.first()))
        val notify = mutableClassDefBy("Lo/onRewardedInterstitialAdClicked;").methods.single { it.name == "notifyDataSetChanged" }
        notify.implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.reversed().forEach { (i, _) ->
            notify.addInstruction(i, "invoke-static/range {p0 .. p0}, $HISTORY->sortAdapter(Ljava/lang/Object;)V")
        }
    } else if (version == "0.8.10.226 dev" || version == "0.8.10.242 dev") {
        val v242 = version == "0.8.10.242 dev"
        val popup = mutableClassDefBy(if (v242) "Lo/zzble;" else "Lo/bindIconData;")
            .methods.single { it.name == "invoke" }
        val body = popup.implementation!!.instructions.toList()
        val rendererOwner = if (v242) "Lo/getWrapperDetectionLastPing;" else "Lo/convertBooleanToFlag;"
        val renderIndices = body.indices.filter {
            val ref = (body[it] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == rendererOwner && ref.name == "d" && ref.parameterTypes.size == 12
        }
        check(renderIndices.size == 3) { "$version native upload overflow changed" }
        // Both verified native lambdas retain model in v1 and composer in v15 here.
        popup.addInstruction(renderIndices.last() + 1,
            "invoke-static {v1, v15}, $HISTORY->composeMenu(Ljava/lang/Object;Ljava/lang/Object;)V")

        val titleOwner = mutableClassDefBy(if (v242) "Lo/zzbjz;" else "Lo/kUGNk;")
        val title = titleOwner.methods.single { it.name == (if (v242) "e" else "b") && it.parameterTypes.size == 2 }
        val titleBody = title.implementation!!.instructions.toList()
        val lookups = titleBody.indices.filter {
            val ref = (titleBody[it] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.returnType == "Ljava/lang/String;" && ref.parameterTypes.firstOrNull() == "I"
        }
        check(lookups.size == 1) { "$version native upload title changed" }
        val result = titleBody[lookups.single() + 1] as OneRegisterInstruction
        title.addInstruction(lookups.single() + 2,
            "invoke-static/range {v${result.registerA} .. v${result.registerA}}, $HISTORY->title(Ljava/lang/String;)Ljava/lang/String;")
        title.addInstruction(lookups.single() + 3, "move-result-object v${result.registerA}")

        val load = mutableClassDefBy(if (v242)
            "Ljp/syoboi/chmate2/feature/screen/uploadlist/UploadListViewModel\$read;"
            else "Lo/RFEndCardBackUpLayout2\$write;").methods.single { it.name == "invokeSuspend" }
        val loadBody = load.implementation!!.instructions.toList()
        val clone = loadBody.indices.single {
            val ref = (loadBody[it] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Ljava/util/ArrayList;" && ref.name == "<init>"
                && ref.parameterTypes == listOf("Ljava/util/Collection;")
        }
        val listRegister = when (val inst = loadBody[clone]) {
            is FiveRegisterInstruction -> inst.registerC
            is RegisterRangeInstruction -> inst.startRegister
            else -> error("Unexpected upload display copy")
        }
        load.addInstruction(clone + 1,
            "invoke-static/range {v$listRegister .. v$listRegister}, $HISTORY->sort(Ljava/util/List;)V")
    } else return
    println("Native upload history overflow/sort/bulk delete: $version")
}
