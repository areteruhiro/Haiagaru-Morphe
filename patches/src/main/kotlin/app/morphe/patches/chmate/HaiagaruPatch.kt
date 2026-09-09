package app.morphe.patches.chmate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.findFreeRegister
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

private const val EXTENSION = "Lapp/morphe/extension/chmate/Haiagaru;"

private val compatibility = Compatibility(
    name = "ChMate",
    packageName = "jp.co.airfront.android.a2chMate",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x607D8B,
    signatures = setOf(
        "7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2"
    ),
    targets = listOf(
        AppTarget(
            version = "0.8.10.241",
            minSdk = 23
        ),
        AppTarget(
            version = "0.8.10.242 dev",
            minSdk = 23
        ),
        AppTarget(
            version = "0.8.10.243 dev",
            minSdk = 24
        )
    )
)

private object ApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/syoboi/a2chMate/RoidonApp;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList()
)

private object SettingsOnResumeFingerprint : Fingerprint(
    definingClass = "Ljp/syoboi/a2chMate/activity/SettingActivity;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList()
)

private object SettingsOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/syoboi/a2chMate/activity/SettingActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

private object HiltSettingsOnCreateFingerprint : Fingerprint(
    definingClass = "Ljp/syoboi/a2chMate/activity/Hilt_SettingActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

private object HomeOnViewCreatedFingerprint : Fingerprint(
    definingClass = "Ljp/syoboi/a2chMate/ui/home/HomeFragment;",
    name = "onViewCreated",
    returnType = "V",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;")
)

private data class ChMateProfile(
    val providerClass: String,
    val cookieClearMethod: String,
    val signatureClass: String,
    val signatureMethod: String,
    val signatureDelegateField: String,
    val signatureDelegateType: String,
    val signatureDelegateMethod: String,
    val signatureSuperType: String,
    val patchSignatureWrapper: Boolean,
    val signatureDirectWrapperBypass: Boolean,
    val viewModelFactoryClass: String,
    val viewModelDispatchField: String,
    val viewModelTrapKind: ViewModelTrapKind,
    val settingsWindowFeatureDivideTrap: Boolean,
    val homeAdClass: String,
    val homeAdLoadMethod: String,
)

private enum class ViewModelTrapKind {
    NONE,
    DIVIDE_BY_ZERO,
    FAILURE_BRANCH,
}

private fun profileFor(versionName: String) = when (versionName) {
    "0.8.10.241" -> ChMateProfile(
        providerClass = "Lo/Kjv22;",
        cookieClearMethod = "e",
        signatureClass = "Lo/getWebView${'$'}3;",
        signatureMethod = "a",
        signatureDelegateField = "a",
        signatureDelegateType = "Lo/getWebView${'$'}write;",
        signatureDelegateMethod = "a",
        signatureSuperType = "Lo/getWebView${'$'}IconCompatParcelizer;",
        patchSignatureWrapper = true,
        signatureDirectWrapperBypass = true,
        viewModelFactoryClass =
            "Lo/getBorderWidth${'$'}r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw${'$'}RemoteActionCompatParcelizer;",
        viewModelDispatchField = "c",
        viewModelTrapKind = ViewModelTrapKind.FAILURE_BRANCH,
        settingsWindowFeatureDivideTrap = true,
        homeAdClass = "Lo/setUseHandlerThreadForCallbacks;",
        homeAdLoadMethod = "e",
    )
    "0.8.10.242 dev" -> ChMateProfile(
        providerClass = "Lo/isConnected;",
        cookieClearMethod = "e",
        signatureClass = "Lo/TTRewardExpressVideoActivity${'$'}5;",
        signatureMethod = "c",
        signatureDelegateField = "a",
        signatureDelegateType = "Lo/TTRewardExpressVideoActivity${'$'}read;",
        signatureDelegateMethod = "c",
        signatureSuperType =
            "Lo/TTRewardExpressVideoActivity${'$'}RemoteActionCompatParcelizer;",
        patchSignatureWrapper = true,
        signatureDirectWrapperBypass = true,
        viewModelFactoryClass =
            "Lo/onInterstitialDismissed${'$'}_init_lambda2${'$'}ComponentActivity;",
        viewModelDispatchField = "e",
        viewModelTrapKind = ViewModelTrapKind.FAILURE_BRANCH,
        settingsWindowFeatureDivideTrap = false,
        homeAdClass = "Lo/zzbgb;",
        homeAdLoadMethod = "d",
    )
    "0.8.10.243 dev" -> ChMateProfile(
        providerClass = "Lo/zzbvh;",
        cookieClearMethod = "a",
        signatureClass = "Lo/SafeParcelableReserved${'$'}4;",
        signatureMethod = "a",
        signatureDelegateField = "b",
        signatureDelegateType = "Lo/SafeParcelableReserved${'$'}RemoteActionCompatParcelizer;",
        signatureDelegateMethod = "a",
        signatureSuperType = "Lo/SafeParcelableReserved${'$'}IconCompatParcelizer;",
        patchSignatureWrapper = true,
        signatureDirectWrapperBypass = false,
        viewModelFactoryClass = "Lo/hasData${'$'}_init_lambda2${'$'}write;",
        viewModelDispatchField = "d",
        viewModelTrapKind = ViewModelTrapKind.DIVIDE_BY_ZERO,
        settingsWindowFeatureDivideTrap = false,
        homeAdClass = "Lo/zzexb;",
        homeAdLoadMethod = "c",
    )
    else -> error("Unsupported ChMate version: $versionName")
}

@Suppress("unused")
val haiagaruPatch = bytecodePatch(
    name = "Haiagaru",
    description = "Ports the Haiagaru ChMate module, including its in-app settings.",
) {
    compatibleWith(compatibility)
    extendWith("extensions/chmate.mpe")

    execute {
        val profile = profileFor(packageMetadata.versionName)

        mutableClassDefBy(profile.providerClass).methods.single { method ->
            method.name == "onCreate"
                && method.returnType == "Z"
                && method.parameters.isEmpty()
        }.addInstruction(
            0,
            "invoke-static { }, $EXTENSION->installSignatureSpoof()V"
        )

        ApplicationOnCreateFingerprint.method.addBeforeEveryReturn(
            "invoke-static/range { p0 .. p0 }, $EXTENSION->onApplicationCreate(Landroid/app/Application;)V"
        )
        SettingsOnResumeFingerprint.method.addBeforeEveryReturn(
            "invoke-static/range { p0 .. p0 }, $EXTENSION->onSettingsResume(Landroid/app/Activity;)V"
        )
        // ChMate 0.8.10.242 reuses the p1 register later in onViewCreated. Inject while
        // p1 is still guaranteed to contain the Fragment root; the extension posts its
        // scans to the view queue, so child views are inspected after construction.
        HomeOnViewCreatedFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p1 .. p1 }, $EXTENSION->hideHomeBanner(Landroid/view/View;)V"
        )
        mutableClassDefBy(
            "Lcom/franmontiel/persistentcookiejar/persistence/SharedPrefsCookiePersistor;"
        ).methods.single { method ->
            method.name == profile.cookieClearMethod
                && method.returnType == "V"
                && method.parameters.isEmpty()
        }.addBeforeEveryReturn(
            "invoke-static { }, $EXTENSION->removeMonaKey()V"
        )

        // ChMate performs initialization and a signature check from this provider before
        // Application.onCreate. Preserve the provider and all initialization, and convert only
        // the check's numeric RuntimeException rejection into the same delegate return used by
        // its successful path.
        if (profile.patchSignatureWrapper) {
            mutableClassDefBy(profile.signatureClass).methods.single { method ->
                method.name == profile.signatureMethod
                    && method.returnType == "Ljava/lang/Object;"
                    && method.parameters.isEmpty()
            }.ignoreSignatureRejection(profile)
            if (profile.signatureDirectWrapperBypass) {
                mutableClassDefBy(profile.signatureSuperType).methods.single { method ->
                    method.name == profile.signatureDelegateMethod
                        && method.returnType == "Ljava/lang/Object;"
                        && method.parameters.isEmpty()
                }.bypassSignatureFailureBranches()
            }
        }

        mutableClassDefBy(profile.viewModelFactoryClass).methods.single { method ->
            method.name == "get"
                && method.returnType == "Ljava/lang/Object;"
                && method.parameters.isEmpty()
        }.bypassTamperTrap(profile)
        if (profile.viewModelTrapKind != ViewModelTrapKind.NONE) {
            SettingsOnCreateFingerprint.method.bypassSettingsTamperTrap(profile)
            HiltSettingsOnCreateFingerprint.method.bypassHiltSettingsTamperTrap(profile)
        }
        patchDistributedIntegrityComparisons()

        listOf(
            "Lcom/amazon/device/ads/DTBAdRequest;",
            "Lcom/unity3d/mediation/banner/LevelPlayBannerAdView;"
        ).forEach { classType ->
            mutableClassDefBy(classType).methods
                .filter { it.name == "loadAd" && it.returnType == "V" }
                .forEach { it.addHideAdsGuard() }
        }

        mutableClassDefBy("Lcom/unity3d/mediation/banner/LevelPlayBannerAdView;")
            .methods
            .filter { it.name == "<init>" }
            .forEach {
                it.addBeforeEveryReturn(
                    "invoke-static/range { p0 .. p0 }, $EXTENSION->hideAdView(Landroid/view/View;)V"
                )
            }

        // The exact class is version-specific, but each target was matched by the same
        // FrameLayout/ad-placement/load-method structure instead of by its obfuscated name.
        mutableClassDefBy(profile.homeAdClass).methods.forEach { method ->
            when {
                method.name == "<init>" -> method.addBeforeEveryReturn(
                    "invoke-static/range { p0 .. p0 }, $EXTENSION->hideAdView(Landroid/view/View;)V"
                )
                method.name == profile.homeAdLoadMethod
                    && method.returnType == "V"
                    && method.parameters.isEmpty() ->
                    method.addHideAdsViewGuard()
            }
        }

        patchSetTextCalls()
    }
}

private fun MutableMethod.bypassHiltSettingsTamperTrap(profile: ChMateProfile) {
    val instructions = implementation?.instructions
        ?: error("ChMate Hilt settings onCreate has no implementation")
    if (profile.signatureDirectWrapperBypass) {
        val failureBranchIndex = instructions.indexOfLast { it.opcode == Opcode.IF_NE }
            .takeIf { it >= 0 }
            ?: error("ChMate Hilt settings signature branch was not found")
        replaceInstruction(failureBranchIndex, "nop")
        return
    }

    val rejectionConstructorIndex = instructions.indexOfFirst { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@indexOfFirst false
        reference.definingClass == "Ljava/lang/RuntimeException;"
            && reference.name == "<init>"
            && reference.parameterTypes.map(CharSequence::toString) ==
            listOf("Ljava/lang/String;")
    }.takeIf { it >= 0 }
        ?: error("ChMate Hilt settings signature rejection was not found")
    val failureBranchIndex = instructions.subList(0, rejectionConstructorIndex)
        .indexOfLast { it.opcode == Opcode.IF_NE }
        .takeIf { it >= 0 }
        ?: error("ChMate Hilt settings signature branch was not found")

    replaceInstruction(failureBranchIndex, "nop")
}

private fun MutableMethod.bypassSettingsTamperTrap(profile: ChMateProfile) {
    val instructions = implementation?.instructions
        ?: error("ChMate settings onCreate has no implementation")
    if (profile.settingsWindowFeatureDivideTrap) {
        // 0.8.10.241 derives FEATURE_NO_TITLE through an integrity-dependent divisor.
        // Re-signing can make that divisor zero, so retain the normal value directly.
        val requestWindowFeatureIndex = instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@indexOfFirst false
            reference.definingClass == "Landroid/app/Activity;"
                && reference.name == "requestWindowFeature"
                && reference.parameterTypes.map(CharSequence::toString) == listOf("I")
        }.takeIf { it >= 0 }
            ?: error("ChMate settings requestWindowFeature call was not found")
        val divideIndex = instructions.subList(0, requestWindowFeatureIndex)
            .indexOfLast { it.opcode == Opcode.DIV_INT_2ADDR }
            .takeIf { it >= 0 }
            ?: error("ChMate settings window feature divide trap was not found")
        val featureRegister = (instructions[divideIndex] as TwoRegisterInstruction).registerA
        replaceInstruction(
            divideIndex,
            "const/4 v$featureRegister, 0x1"
        )
    }

    val failureBranchIndex = if (profile.signatureDirectWrapperBypass) {
        instructions.indexOfLast { it.opcode == Opcode.IF_NE }
            .takeIf { it >= 0 }
            ?: error("ChMate settings tamper branch was not found")
    } else {
        val trapIndex = instructions.indices.firstOrNull { index ->
            index + 7 < instructions.size
                && instructions[index].opcode == Opcode.NEW_ARRAY
                && instructions[index + 1].opcode == Opcode.ADD_INT_LIT8
                && instructions[index + 2].opcode == Opcode.APUT
                && instructions[index + 3].opcode == Opcode.MUL_INT_2ADDR
                && instructions[index + 4].opcode == Opcode.CONST_4
                && instructions[index + 5].opcode == Opcode.REM_INT_2ADDR
                && instructions[index + 6].opcode == Opcode.SUB_INT_2ADDR
                && instructions[index + 7].opcode == Opcode.AGET
        } ?: error("ChMate settings tamper trap was not found")
        instructions.subList(0, trapIndex)
            .indexOfLast { it.opcode == Opcode.IF_NE }
            .takeIf { it >= 0 }
            ?: error("ChMate settings tamper branch was not found")
    }

    // Falling through this branch executes ChMate's complete normal initialization path,
    // including the Object[] state later consumed by the real settings setup.
    replaceInstruction(failureBranchIndex, "nop")

    // The final obfuscated calculation supplies only the fallback for the preferenceXml
    // intent extra. Re-signing turns its denominator into zero; an actual supplied extra
    // remains authoritative, while zero means no preselected settings page.
    val preferenceDefaultDivideIndex = instructions.indexOfLast {
        it.opcode == Opcode.DIV_INT_2ADDR
    }.takeIf { it >= 0 }
        ?: error("ChMate settings preferenceXml fallback was not found")
    val preferenceDefaultRegister =
        (instructions[preferenceDefaultDivideIndex] as TwoRegisterInstruction).registerA
    replaceInstruction(
        preferenceDefaultDivideIndex,
        "const/4 v$preferenceDefaultRegister, 0x0"
    )
}

private fun MutableMethod.bypassTamperTrap(profile: ChMateProfile) {
    val instructions = implementation?.instructions
        ?: error("ChMate ViewModel factory has no implementation")
    val dispatchIndex = instructions.indexOfFirst { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            ?: return@indexOfFirst false
        instruction.opcode == Opcode.IGET
            && reference.definingClass == profile.viewModelFactoryClass
            && reference.name == profile.viewModelDispatchField
            && reference.type == "I"
    }.takeIf { it > 0 }
        ?: error("ChMate ViewModel factory dispatch was not found")

    when (profile.viewModelTrapKind) {
        ViewModelTrapKind.NONE -> Unit
        ViewModelTrapKind.DIVIDE_BY_ZERO -> {
            val divideIndex = instructions.subList(0, dispatchIndex)
                .indexOfLast { it.opcode == Opcode.DIV_INT_2ADDR }
                .takeIf { it >= 0 }
                ?: error("ChMate ViewModel factory divide trap was not found")
            addInstructionsWithLabels(
                divideIndex,
                "goto/32 :haiagaru_dispatch",
                ExternalLabel("haiagaru_dispatch", instructions[dispatchIndex])
            )
        }
        ViewModelTrapKind.FAILURE_BRANCH -> {
            val failureBranchIndex = instructions.subList(0, dispatchIndex)
                // 0.8.10.242 compares two values produced by its integrity state and
                // sends inequality to the RuntimeException(String) block. The normal
                // fall-through immediately loads the factory discriminator and switches.
                .indexOfLast { it.opcode == Opcode.IF_NE }
                .takeIf { it >= 0 }
                ?: error("ChMate ViewModel factory failure branch was not found")
            replaceInstruction(failureBranchIndex, "nop")
        }
    }
}

private fun MutableMethod.ignoreSignatureRejection(profile: ChMateProfile) {
    if (profile.signatureDirectWrapperBypass) {
        // 0.8.10.242 encodes rejection as IF_NE -> null throw in both wrapper layers.
        // Keep their complete initialization and delegate calls, but force the normal path.
        bypassSignatureFailureBranches()
        return
    }

    val instructions = implementation?.instructions
        ?: error("ChMate signature check has no implementation")
    val rejectionConstructorIndex = instructions.indexOfFirst { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            ?: return@indexOfFirst false
        reference.definingClass == "Ljava/lang/RuntimeException;"
            && reference.name == "<init>"
            && reference.parameterTypes.map(CharSequence::toString) ==
            listOf("Ljava/lang/String;")
    }.takeIf { it >= 0 }
        ?: error("ChMate signature rejection constructor was not found")
    val throwOffset = instructions.drop(rejectionConstructorIndex)
        .indexOfFirst { it.opcode == Opcode.THROW }
        .takeIf { it >= 0 }
        ?: error("ChMate signature rejection throw was not found")
    val rejectionThrowIndex = rejectionConstructorIndex + throwOffset

    addInstructionsWithLabels(
        rejectionThrowIndex,
        """
            move-object/from16 v0, p0
            iget-object v0, v0, ${profile.signatureClass}->${profile.signatureDelegateField}:${profile.signatureDelegateType}
            invoke-virtual { v0 }, ${profile.signatureDelegateType}->${profile.signatureDelegateMethod}()Ljava/lang/Object;
            move-result-object v0
            return-object v0
        """
    )
}

private fun MutableMethod.bypassSignatureFailureBranches() {
    val branchIndexes = implementation?.instructions
        ?.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode == Opcode.IF_NE) index else null
        }
        .orEmpty()
    if (branchIndexes.isEmpty()) {
        error("ChMate signature failure branches were not found")
    }
    branchIndexes.asReversed().forEach { replaceInstruction(it, "nop") }
}

private fun MutableMethod.addHideAdsViewGuard() {
    val freeRegister = findFreeRegister(0)
    addInstructionsWithLabels(
        0,
        """
            invoke-static/range { p0 .. p0 }, $EXTENSION->hideAdView(Landroid/view/View;)V
            invoke-static { }, $EXTENSION->shouldHideAds()Z
            move-result v$freeRegister
            if-eqz v$freeRegister, :show_ads
            return-void
            :show_ads
            nop
        """
    )
}

private fun MutableMethod.addHideAdsGuard() {
    val freeRegister = findFreeRegister(0)
    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $EXTENSION->shouldHideAds()Z
            move-result v$freeRegister
            if-eqz v$freeRegister, :show_ads
            return-void
            :show_ads
            nop
        """
    )
}

private fun MutableMethod.addBeforeEveryReturn(instruction: String) {
    implementation?.instructions
        ?.mapIndexedNotNull { index, value ->
            if (value.opcode == Opcode.RETURN_VOID) index else null
        }
        ?.asReversed()
        ?.forEach { addInstruction(it, instruction) }
}

/**
 * ChMate repeats its certificate-derived comparison inside many screen ViewModel
 * constructors. The names and surrounding arithmetic change between builds, but the
 * comparison is structurally stable: two int values are read from the obfuscator's
 * Object[] state and IF_NE jumps to a decoy exception block. Keep the real constructor
 * body by forcing the equality fall-through.
 */
private fun app.morphe.patcher.patch.BytecodePatchContext.patchDistributedIntegrityComparisons() {
    classDefForEach { classDef ->
        if (!classDef.type.startsWith("Ljp/syoboi/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }
        classDef.methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach
            val matches = instructions.indices.filter { index ->
                if (instructions[index].opcode != Opcode.IF_NE) return@filter false
                val window = instructions.subList(maxOf(0, index - 12), index)
                window.count { it.opcode == Opcode.AGET_OBJECT } >= 2
                    && window.count { it.opcode == Opcode.CHECK_CAST } >= 2
                    && window.count { it.opcode == Opcode.AGET } >= 2
            }
            if (matches.isEmpty()) return@forEach

            val mutableMethod = mutableClass.findMutableMethodOf(method)
            matches.asReversed().forEach { mutableMethod.replaceInstruction(it, "nop") }
        }
    }
}

private fun app.morphe.patcher.patch.BytecodePatchContext.patchSetTextCalls() {
    classDefForEach { classDef ->
        if (classDef.type.startsWith("Lapp/morphe/extension/chmate/")) {
            return@classDefForEach
        }

        val mutableClass by lazy { mutableClassDefBy(classDef) }
        classDef.methods.forEach { method ->
            val matches = method.implementation?.instructions
                ?.mapIndexedNotNull { index, instruction ->
                    val reference = (instruction as? ReferenceInstruction)
                        ?.reference as? MethodReference
                        ?: return@mapIndexedNotNull null
                    if (reference.name != "setText"
                        || reference.parameterTypes.firstOrNull() != "Ljava/lang/CharSequence;"
                    ) {
                        return@mapIndexedNotNull null
                    }

                    val argumentRegister = when (instruction) {
                        is FiveRegisterInstruction -> instruction.registerD
                        is RegisterRangeInstruction -> instruction.startRegister + 1
                        else -> return@mapIndexedNotNull null
                    }
                    index to argumentRegister
                }
                ?.toList()
                .orEmpty()

            if (matches.isEmpty()) return@forEach
            val mutableMethod = mutableClass.findMutableMethodOf(method)
            matches.asReversed().forEach { (index, register) ->
                mutableMethod.addInstructionsWithLabels(
                    index,
                    """
                        invoke-static/range { v$register .. v$register }, $EXTENSION->replace5chDomain(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$register
                    """
                )
            }
        }
    }
}
