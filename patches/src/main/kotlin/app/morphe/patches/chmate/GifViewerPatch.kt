package app.morphe.patches.chmate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Direct links and native image thumbnails both open the GIF player. */
internal val gifViewerRoutingPatch = bytecodePatch {
    dependsOn(haiagaruPatch)
    execute {
        val method = mutableClassDefBy("Ljp/syoboi/a2chMate/activity/ImagesViewer2Activity;").methods.single {
            it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;") && it.returnType == "V"
        }
        val instructions = method.implementation!!.instructions.toList()
        val superIndex = instructions.indexOfFirst {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            (it.opcode == Opcode.INVOKE_SUPER || it.opcode == Opcode.INVOKE_SUPER_RANGE) && ref?.name == "onCreate"
        }
        check(superIndex >= 0 && method.implementation!!.registerCount > 2) { "Native image viewer entry changed" }
        method.addInstructionsWithLabels(superIndex + 1, """
            invoke-static/range {p0 .. p0}, Lapp/morphe/extension/chmate/GifViewerActivity;->redirectNative(Landroid/app/Activity;)Z
            move-result v0
            if-eqz v0, :native_images
            return-void
        """.trimIndent(), ExternalLabel("native_images", instructions[superIndex + 1]))
    }
}
