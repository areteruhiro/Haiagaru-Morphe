package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/** Correct only ChMate's patrol request after the generic builder's 15-minute clamp.
 * WorkManager uses one-shot JobScheduler latency for these periodic WorkSpecs;
 * its constraints, cancellation, persistence and retry/backoff remain unchanged.
 */
internal fun BytecodePatchContext.patchPatrolInterval(version: String) {
    val config = when (version) {
        "0.8.10.191 dev" -> listOf("Lo/mdExternalSyntheticLambda0;", "Lo/a7aExternalSyntheticLambda4\$IconCompatParcelizer;", "a", "m", "g")
        "0.8.10.226 dev" -> listOf("Lo/settings;", "Lo/isAdLoaded\$ComponentActivity;", "d", "k", "j")
        "0.8.10.242 dev" -> listOf("Lo/TTRewardVideoActivity8;", "Lo/CloseGuard\$RemoteActionCompatParcelizer;", "c", "m", "f")
        else -> error("Unsupported patrol interval version: $version")
    }
    val owner = mutableClassDefBy(config[0])
    val scheduler = owner.methods.single { it.name == "e" && it.parameterTypes == listOf("Landroid/content/Context;") && it.returnType == "V" }
    val builder = mutableClassDefBy(config[1])
    val base = mutableClassDefBy(builder.superclass!!)
    val spec = base.fields.single { it.name == config[2] }
    val specClass = mutableClassDefBy(spec.type)
    listOf(config[3], config[4]).forEach { name -> check(specClass.fields.single { it.name == name }.type == "J") }
    val constructor = builder.methods.single { it.name == "<init>" && it.parameterTypes == listOf("Ljava/lang/Class;", "J", "Ljava/util/concurrent/TimeUnit;") }
    val written = constructor.implementation!!.instructions.mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
    listOf(config[3], config[4]).forEach { name -> check(written.any { it.definingClass == spec.type && it.name == name }) }
    val helper = MutableMethod(ImmutableMethod(owner.type, "haiagaruPatrolInterval",
        listOf(ImmutableMethodParameter(builder.type, emptySet(), null), ImmutableMethodParameter("J", emptySet(), null)),
        "V", 0x9, emptySet(), emptySet(), ImmutableMethodImplementation(6, emptyList(), emptyList(), emptyList())))
    helper.addInstructions(0, """
        const-wide/16 v0, 0x5
        cmp-long v2, p1, v0
        if-eqz v2, :apply
        const-wide/16 v0, 0xa
        cmp-long v2, p1, v0
        if-nez v2, :done
        :apply
        const-wide/32 v0, 0xea60
        mul-long/2addr p1, v0
        iget-object v2, p0, ${base.type}->${spec.name}:${spec.type}
        iput-wide p1, v2, ${spec.type}->${config[3]}:J
        iput-wide p1, v2, ${spec.type}->${config[4]}:J
        :done
        return-void
    """.trimIndent())
    owner.methods.add(helper)
    val sites = scheduler.implementation!!.instructions.withIndex().filter {
        val ref = (it.value as? ReferenceInstruction)?.reference as? MethodReference
        ref?.definingClass == builder.type && ref.name == "<init>"
    }
    check(sites.size == 1) { "Patrol scheduling constructor changed: $version" }
    val (index, instruction) = sites.single()
    val call = instruction as FiveRegisterInstruction
    check(call.registerCount == 5 && call.registerF == call.registerE + 1)
    scheduler.addInstructions(index + 1, "invoke-static {v${call.registerC}, v${call.registerE}, v${call.registerF}}, ${owner.type}->haiagaruPatrolInterval(${builder.type}J)V")
    println("Native patrol intervals: $version, 5/10 minutes scoped to autocheck-auto")
}
