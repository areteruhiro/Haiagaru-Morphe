package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableTryBlock
import com.android.tools.smali.dexlib2.immutable.ImmutableExceptionHandler
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

/** ViewPager can synchronously select a different position during notifyDataSetChanged. */
internal fun BytecodePatchContext.patchTabletTabSelection226() {
    patchTabletClosedTabSelection226()
    val pane = "Lo/QWA;"
    val owner = mutableClassDefBy("Lo/QWA\$5\$5\$5;")
    val emit = owner.methods.single { it.name == "emit" && it.returnType == "Ljava/lang/Object;" }
    check(emit.parameterTypes.map(CharSequence::toString) == listOf("Ljava/lang/Object;", "Lo/bea12;"))
    val refs = emit.implementation!!.instructions.mapNotNull {
        (it as? ReferenceInstruction)?.reference as? MethodReference
    }
    check(refs.count { it.name == "notifyDataSetChanged" } == 2)
    check(refs.count { it.definingClass == pane && it.name == "d" &&
        it.parameterTypes.map(CharSequence::toString) == listOf(pane, "Z") } == 2)
    val flag = mutableClassDefBy(pane).methods.single {
        it.name == "h" && it.returnType == "Z" && it.parameterTypes == listOf(pane)
    }
    check(flag.accessFlags and 0x8 != 0)
    check(owner.fields.single { it.name == "c" }.type == pane)
    owner.methods.remove(emit)
    val original = MutableMethod(ImmutableMethod(owner.type, "haiagaruApplyPaneState", emit.parameters,
        emit.returnType, emit.accessFlags, emit.annotations, emit.hiddenApiRestrictions, emit.implementation))
    owner.methods.add(original)
    val wrapper = MutableMethod(ImmutableMethod(owner.type, emit.name, emit.parameters,
        emit.returnType, emit.accessFlags, emit.annotations, emit.hiddenApiRestrictions,
        ImmutableMethodImplementation(7, emptyList(), emptyList(), emptyList()))).apply {
        addInstructions(0, """
            iget-object v0, p0, ${owner.type}->c:$pane
            invoke-static {v0}, $pane->h($pane)Z
            move-result v1
            const/4 v2, 0x1
            invoke-static {v0, v2}, $pane->d(${pane}Z)V
            invoke-virtual {p0, p1, p2}, $original
            move-result-object v2
            invoke-static {v0, v1}, $pane->d(${pane}Z)V
            return-object v2
            move-exception v3
            invoke-static {v0, v1}, $pane->d(${pane}Z)V
            throw v3
        """.trimIndent())
    }
    // addInstructions transfers instructions only, not smali .catchall directives.
    // Preserve the finally region explicitly in the resulting DEX implementation.
    val body = wrapper.implementation!!.instructions.toList()
    fun offset(index: Int) = body.take(index).sumOf { it.codeUnits }
    val guarded = body.indexOfFirst {
        val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
        ref?.name == original.name && ref.definingClass == owner.type
    }
    val handler = body.indexOfFirst { it.opcode == com.android.tools.smali.dexlib2.Opcode.MOVE_EXCEPTION }
    check(guarded >= 0 && handler > guarded)
    owner.methods.add(MutableMethod(ImmutableMethod(owner.type, emit.name, emit.parameters,
        emit.returnType, emit.accessFlags, emit.annotations, emit.hiddenApiRestrictions,
        ImmutableMethodImplementation(7, body, listOf(ImmutableTryBlock(offset(guarded),
            offset(guarded + 2) - offset(guarded), listOf(ImmutableExceptionHandler(null, offset(handler))))), emptyList()))))
    println("ChMate 226 tablet tabs: suppress selection feedback throughout pane-state application")
}

/** Keep the remaining neighbour selected in the same state emission as page removal. */
private fun BytecodePatchContext.patchTabletClosedTabSelection226() {
    val remove = mutableClassDefBy("Lo/GNk;").methods.single {
        it.name == "d" && it.returnType == "V" && it.parameterTypes == listOf("I")
    }
    val instructions = remove.implementation!!.instructions.toList()
    val removeCall = instructions.withIndex().single { (_, instruction) ->
        val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == "Ljava/util/List;" && ref.name == "remove" &&
            ref.parameterTypes.map(CharSequence::toString) == listOf("I")
    }
    val listRegister = (removeCall.value as FiveRegisterInstruction).registerC
    val cleared = instructions.withIndex().single { (_, instruction) ->
        instruction.opcode == com.android.tools.smali.dexlib2.Opcode.CONST_4 &&
            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == -1
    }
    check(cleared.index > removeCall.index) { "226 closed-tab selection anchor moved" }
    val selectionRegister = (cleared.value as OneRegisterInstruction).registerA
    check(listRegister != selectionRegister && remove.implementation!!.registerCount < 16)
    // This branch is reached only when the removed tab was selected. Selecting the
    // same index chooses its right neighbour, or its left neighbour at the end.
    // An empty pane still correctly receives -1; unselected-tab removal is unchanged.
    remove.replaceInstruction(cleared.index,
        "invoke-interface {v$listRegister}, Ljava/util/List;->size()I")
    remove.addInstructions(cleared.index + 1, """
        move-result v$selectionRegister
        add-int/lit8 v$selectionRegister, v$selectionRegister, -0x1
        invoke-static {p1, v$selectionRegister}, Ljava/lang/Math;->min(II)I
        move-result v$selectionRegister
    """.trimIndent())
    println("ChMate 226 tablet tabs: select remaining neighbour atomically when closing selected tab")
}
