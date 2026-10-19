package app.morphe.patches.chmate

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val POSTER_CHAIN = "Lapp/morphe/extension/chmate/PosterIdChainNg;"

/** Insert before the native chain cache write, not after the final rendering flags were computed. */
internal fun BytecodePatchContext.patchPosterIdChainNg() {
    val profile = when (packageMetadata.versionName) {
        "0.8.10.191 dev" -> listOf("Lo/m9ExternalSyntheticLambda1;", "SE_", "Lo/processAdDisplayErrorPostbackForUserError;", "Landroid/util/SparseIntArray;", "put", "c")
        "0.8.10.226 dev" -> listOf("Lo/listener;", "d", "Lo/BouncyCastleSocketAdapterCompanion;", "Lo/getUpstreamReader;", "c", "b")
        "0.8.10.241" -> listOf("Lo/SignInButton;", "a", "Lo/setDislikeWidth;", "Lo/ComponentModela;", "b", "")
        "0.8.10.242 dev" -> listOf("Lo/zzaW;", "c", "Lo/KeJ11;", "Lo/hLnYhp;", "c", "")
        "0.8.10.243 dev" -> listOf("Lo/zzdpd;", "e", "Lo/zzabv;", "Lo/requiresSignIn;", "e", "")
        else -> error("Unsupported poster ID chain profile: ${packageMetadata.versionName}")
    }
    val owner = mutableClassDefBy(profile[0])
    val method = owner.methods.single {
        it.name == profile[1] && it.returnType == "I" &&
            it.parameterTypes.take(2) == listOf(profile[2], "Z")
    }
    val writes = method.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        if (reference?.definingClass != profile[3] || reference.name != profile[4]
            || reference.parameterTypes.map(CharSequence::toString) != listOf("I", "I")) null
        else {
            val state = when (instruction) {
                is FiveRegisterInstruction -> {
                    check(instruction.registerCount == 3) { "Poster ID chain write argument count changed" }
                    instruction.registerE
                }
                is RegisterRangeInstruction -> {
                    check(instruction.registerCount == 3) { "Poster ID chain write argument count changed" }
                    instruction.startRegister + 2
                }
                else -> error("Unsupported poster ID chain write instruction")
            }
            index to state
        }
    }
    check(writes.size == 1) { "Poster ID chain cache write changed: ${packageMetadata.versionName}" }
    val (index, state) = writes.single()
    // p2 is the ancestor-walk Boolean; the walk has finished and this parameter is dead here.
    method.addInstructionsWithLabels(index, """
        move/16 p2, v$state
        invoke-static/range {p0 .. p2}, $POSTER_CHAIN->merge(Ljava/lang/Object;Ljava/lang/Object;I)I
        move-result v$state
    """)
    if (profile[5].isNotEmpty()) {
        val rebuild = owner.methods.single {
            it.name == profile[5] && it.returnType == "V" && it.parameterTypes == listOf("Z")
        }
        rebuild.addInstructionsWithLabels(0, """
            invoke-static/range {p0 .. p0}, $POSTER_CHAIN->reset(Ljava/lang/Object;)V
        """)
    }
}
