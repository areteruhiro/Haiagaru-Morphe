package app.morphe.patches.chmate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** 191 native ZIP restore only. Every anchor is checked against the original DEX. */
internal fun BytecodePatchContext.patchNativeRestore191() {
    val helper = "Lapp/morphe/extension/chmate/NativeRestore191;"
    val fragment = mutableClassDefBy("Lo/setFormat;")
    val create = fragment.methods.single { it.name == "onCreate" }
    val stagingCall = create.implementation!!.instructions.toList().mapIndexedNotNull { i, op ->
        val ref = (op as? ReferenceInstruction)?.reference as? MethodReference
        if (ref?.definingClass == "Landroid/content/Context;" && ref.name == "getCacheDir")
            i to when (op) {
                is FiveRegisterInstruction -> op.registerC
                is RegisterRangeInstruction -> op.startRegister
                else -> error("Unknown 191 staging receiver")
            } else null
    }.single()
    create.replaceInstruction(stagingCall.first,
        "invoke-static/range {v${stagingCall.second} .. v${stagingCall.second}}, $helper->stagingRoot(Landroid/content/Context;)Ljava/io/File;")
    create.implementation!!.instructions.toList().mapIndexedNotNull { i, op ->
        i.takeIf { op.opcode == Opcode.RETURN_VOID }
    }.asReversed().forEach { i ->
        create.addInstructionsWithLabels(i, "invoke-static/range {p0 .. p0}, $helper->retain(Ljava/lang/Object;)V")
    }
    val start = fragment.methods.single { it.name == "onStart" }
    check(start.implementation!!.instructions.first().opcode == Opcode.INVOKE_SUPER)
    start.addInstructionsWithLabels(1, """
        invoke-static/range {p0 .. p0}, $helper->beginPreview(Ljava/lang/Object;)Z
        move-result v0
        if-nez v0, :start_preview
        return-void
        :start_preview
        nop
    """.trimIndent())
    // Keep both the running worker and its reference across screen-off/resume.
    val stop = fragment.methods.single { it.name == "onStop" }
    var interruptions = 0
    var clears = 0
    stop.implementation!!.instructions.toList().forEachIndexed { i, op ->
        val ref = (op as? ReferenceInstruction)?.reference
        if (ref is MethodReference && ref.definingClass == "Ljava/lang/Thread;" && ref.name == "interrupt") {
            stop.replaceInstruction(i, "nop")
            interruptions++
        }
        if (ref is FieldReference && op.opcode == Opcode.IPUT_OBJECT && ref.name == "c"
            && ref.definingClass == fragment.type && ref.type == "Ljava/lang/Thread;") {
            stop.replaceInstruction(i, "nop")
            clears++
        }
    }
    check(interruptions == 1 && clears == 1) { "191 restore onStop changed" }

    val preview = mutableClassDefBy("Lo/setFormat\$3;").methods.single { it.name == "run" }
    var ops = preview.implementation!!.instructions.toList()
    val deletion = ops.indices.single { i ->
        val ref = (ops[i] as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Lo/r0ExternalSyntheticLambda27;" && ref.name == "b"
            && ref.parameterTypes.map(CharSequence::toString) == listOf("Ljava/io/File;")
    }
    val deletionCall = ops[deletion] as FiveRegisterInstruction
    check(deletionCall.registerC == 5)
    preview.replaceInstruction(deletion, "invoke-static {v5, v2}, $helper->clearUnpreparedDirectory(Ljava/io/File;Ljava/lang/Object;)V")
    ops = preview.implementation!!.instructions.toList()
    val dbLiteral = ops.indices.single { i ->
        ((ops[i] as? ReferenceInstruction)?.reference as? StringReference)?.string == "databases/roidon.sqlite"
    }
    val prepared = (0 until dbLiteral).last { ops[it].opcode == Opcode.NEW_INSTANCE }
    val zipLiteral = ops.indices.single { i ->
        ((ops[i] as? ReferenceInstruction)?.reference as? StringReference)?.string == "backup.zip"
    }
    val zipCtor = (zipLiteral + 1 until dbLiteral).first { i ->
        val ref = (ops[i] as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Ljava/io/File;" && ref.name == "<init>"
    }
    preview.addInstructionsWithLabels(zipCtor + 1, """
        invoke-static {v2}, $helper->reusePreview(Ljava/lang/Object;)Z
        move-result v6
        if-nez v6, :prepared_database
    """.trimIndent(), ExternalLabel("prepared_database", ops[prepared]))
    // Cookie-only Haiagaru archives contain snapshot.json, not roidon.sqlite.
    // Never feed their container to SQLite or install a fabricated empty DB.
    val prepareDatabase = preview.implementation!!.instructions.toList().indexOf(ops[prepared])
    check(prepareDatabase >= 0)
    preview.addInstructionsWithLabels(prepareDatabase, """
        invoke-static {v2}, $helper->routeHaiagaruBackup(Ljava/lang/Object;)Z
        move-result v6
        if-eqz v6, :native_database
        return-void
        :native_database
        nop
    """.trimIndent())
    preview.implementation!!.instructions.toList().mapIndexedNotNull { i, op ->
        i.takeIf { op.opcode == Opcode.RETURN_VOID }
    }.asReversed().forEach { i ->
        preview.addInstructionsWithLabels(i, """
            iget-object v0, p0, Lo/setFormat${'$'}3;->e:Lo/setFormat;
            invoke-static {v0}, $helper->finishPreview(Ljava/lang/Object;)V
        """.trimIndent())
    }
    // Preserve the real SQLite error instead of incorrectly claiming a wrong ZIP.
    ops = preview.implementation!!.instructions.toList()
    val wrongFile = ops.indices.single { i ->
        ((ops[i] as? ReferenceInstruction)?.reference as? StringReference)?.string
            ?.contains("選択されたファイルはChMate") == true
    }
    check(ops[wrongFile - 1].opcode == Opcode.NEW_INSTANCE)
    preview.replaceInstruction(wrongFile - 1, "move-exception v0")
    preview.replaceInstruction(wrongFile, "invoke-static {v0}, $helper->previewError(Ljava/lang/Throwable;)Ljava/lang/String;")
    preview.addInstructionsWithLabels(wrongFile + 1, """
        move-result-object v2
        new-instance v0, Ljava/lang/Exception;
    """.trimIndent())

    val restore = mutableClassDefBy("Lo/setFormat\$2;").methods.single { it.name == "run" }
    var checks = 0
    restore.implementation!!.instructions.toList().forEachIndexed { i, op ->
        val ref = (op as? ReferenceInstruction)?.reference as? MethodReference
        if (ref?.name == "interrupted" && ref.parameterTypes.isEmpty() && ref.returnType == "Z") {
            restore.replaceInstruction(i, "invoke-static {}, $helper->isInterrupted()Z")
            checks++
        }
    }
    check(checks == 3) { "191 restore interruption checks changed ($checks)" }
    // Ignore rapid duplicate restore clicks before copying/destructive native steps.
    restore.addInstructionsWithLabels(0, """
        invoke-static/range {p0 .. p0}, $helper->beginRestore(Ljava/lang/Object;)Z
        move-result v0
        if-nez v0, :restore_once
        return-void
        :restore_once
        nop
    """.trimIndent())

    val application = mutableClassDefBy("Lo/lo;")
    val pending = application.methods.single { method -> method.implementation?.instructions?.any {
        ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "rdp"
    } == true }
    ops = pending.implementation!!.instructions.toList()
    val copy = ops.indices.single { i ->
        val ref = (ops[i] as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Lo/r0ExternalSyntheticLambda27;" && ref.name == "c"
            && ref.parameterTypes.map(CharSequence::toString) == listOf("Ljava/io/File;", "Ljava/io/File;")
    }
    val copyCall = ops[copy] as FiveRegisterInstruction
    check(copyCall.registerC == 4 && copyCall.registerD == 2)
    val cleanup = (copy + 1 until ops.size).first { i ->
        val ref = (ops[i] as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Lo/r0ExternalSyntheticLambda27;" && ref.name == "b"
            && ref.parameterTypes.map(CharSequence::toString) == listOf("Ljava/io/File;")
    }
    val cache = (copy + 1 until cleanup).single { i ->
        ((ops[i] as? ReferenceInstruction)?.reference as? MethodReference)?.name == "getCacheDir"
    }
    val cacheReceiver = when (val op = ops[cache]) {
        is FiveRegisterInstruction -> op.registerC
        is RegisterRangeInstruction -> op.startRegister
        else -> error("Unknown 191 cleanup receiver")
    }
    pending.replaceInstruction(cache,
        "invoke-static/range {v$cacheReceiver .. v$cacheReceiver}, $helper->stagingRoot(Landroid/content/Context;)Ljava/io/File;")
    check(ops[copy + 1].opcode != Opcode.MOVE_RESULT)
    pending.replaceInstruction(copy, "invoke-static {v4, v2}, $helper->installDatabase(Ljava/io/File;Ljava/io/File;)Z")
    pending.addInstructionsWithLabels(copy + 1, """
        move-result v7
        if-eqz v7, :preserve_restore_files
    """.trimIndent(), ExternalLabel("preserve_restore_files", ops[cleanup + 1]))
}
