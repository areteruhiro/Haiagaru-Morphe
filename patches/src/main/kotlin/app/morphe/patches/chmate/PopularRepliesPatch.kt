package app.morphe.patches.chmate

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.findFreeRegister

internal fun BytecodePatchContext.patchPopularReplies(version: String) {
    val extension = "Lapp/morphe/extension/chmate/PopularReplies;"
    if(version == "0.8.10.191 dev") {
        val method=mutableClassDefBy("Lo/pa\$21;").methods.single {
            it.name=="e" && it.parameterTypes.isEmpty() && it.returnType=="V"
        }
        val original=method.implementation!!.instructions.first()
        val reg=method.findFreeRegister(0)
        method.addInstructionsWithLabels(0,"""
            iget-object v$reg, p0, Lo/pa${'$'}21;->d:Lo/pa;
            invoke-static/range {v$reg .. v$reg}, $extension->interceptLegacy(Ljava/lang/Object;)Z
            move-result v$reg
            if-eqz v$reg, :original_popular
            return-void
        """.trimIndent(), ExternalLabel("original_popular",original))
    } else if(version == "0.8.10.226 dev") {
        val method=mutableClassDefBy("Lo/listener\$setContentView\$ComponentActivity;").methods.single {
            it.name=="invoke" && it.parameterTypes==listOf("Ljava/lang/Object;") && it.returnType=="Ljava/lang/Object;"
        }
        val original=method.implementation!!.instructions.first()
        val reg=method.findFreeRegister(0)
        method.addInstructionsWithLabels(0,"""
            invoke-static/range {p0 .. p1}, $extension->interceptCallback(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v$reg
            if-eqz v$reg, :original_popular
            sget-object v$reg, Lo/Ff11;->c:Lo/Ff11;
            return-object v$reg
        """.trimIndent(),ExternalLabel("original_popular",original))
    } else if(version == "0.8.10.242 dev") {
        val method=mutableClassDefBy("Ljp/syoboi/a2chMate/ui/reslist/ResListFragmentViewModel;").methods.single {
            it.name=="b" && it.parameterTypes==listOf("Lo/isAtLeastLollipop;") && it.returnType=="V"
        }
        val original=method.implementation!!.instructions.first()
        val reg=method.findFreeRegister(0)
        method.addInstructionsWithLabels(0,"""
            invoke-static/range {p0 .. p1}, $extension->interceptModel(Ljava/lang/Object;Ljava/lang/Object;)Z
            move-result v$reg
            if-eqz v$reg, :original_popular
            return-void
        """.trimIndent(),ExternalLabel("original_popular",original))
    }
    println("Popular direct-reply popup: $version")
}
