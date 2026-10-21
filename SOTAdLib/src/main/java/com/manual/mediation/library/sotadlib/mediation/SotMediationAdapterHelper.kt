package com.manual.mediation.library.sotadlib.mediation

import android.content.Context
import android.util.Log
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.ads.mediation.pangle.PangleMediationAdapter
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory

/**
 * Easy Mediation Adapter helper for GMA Next-Gen SDK.
 * Handles Pangle, Mintegral, and AppLovin consent & initialization automatically
 * when AdMob / UMP consent is gathered, and provides manual overrides if needed.
 */
object SotMediationAdapterHelper {

    private const val TAG = "SOT_MEDIATION"

    /**
     * Configures consent and initializes mediation SDKs (Pangle, Mintegral, AppLovin).
     * Called automatically by [com.manual.mediation.library.sotadlib.utilsGoogleAdsConsent.ConsentConfigurations]
     * after UMP consent is gathered, or can be invoked directly.
     */
    @JvmStatic
    @JvmOverloads
    fun configureMediationAdapters(
        context: Context,
        hasConsent: Boolean = true,
        doNotSell: Boolean = false,
        mintegralAppId: String? = null,
        mintegralAppKey: String? = null,
        pangleAppId: String? = null
    ) {
        val appContext = context.applicationContext
        configurePangle(hasConsent, pangleAppId)
        configureMintegral(appContext, hasConsent, doNotSell, mintegralAppId, mintegralAppKey)
        configureAppLovin(appContext, hasConsent, doNotSell)
    }

    /**
     * Configures Pangle Mediation Adapter consent settings.
     */
    @JvmStatic
    @JvmOverloads
    fun configurePangle(
        hasConsent: Boolean = true,
        pangleAppId: String? = null
    ) {
        try {
            PangleMediationAdapter.setPAConsent(
                if (hasConsent) {
                    PAGConstant.PAGPAConsentType.PAG_PA_CONSENT_TYPE_CONSENT
                } else {
                    PAGConstant.PAGPAConsentType.PAG_PA_CONSENT_TYPE_NO_CONSENT
                }
            )
            Log.d(TAG, "Pangle adapter configured (hasConsent=$hasConsent, appId=${pangleAppId ?: "from AdMob UI"})")
        } catch (t: Throwable) {
            Log.w(TAG, "Pangle adapter configuration skipped or failed: ${t.message}")
        }
    }

    /**
     * Configures Mintegral Mediation Adapter consent and optional manual SDK initialization.
     */
    @JvmStatic
    @JvmOverloads
    fun configureMintegral(
        context: Context,
        hasConsent: Boolean = true,
        doNotSell: Boolean = false,
        appId: String? = null,
        appKey: String? = null
    ) {
        try {
            val sdk = MBridgeSDKFactory.getMBridgeSDK()
            sdk.setConsentStatus(
                context,
                if (hasConsent) MBridgeConstans.IS_SWITCH_ON else MBridgeConstans.IS_SWITCH_OFF
            )
            sdk.setDoNotTrackStatus(doNotSell)

            if (!appId.isNullOrBlank() && !appKey.isNullOrBlank()) {
                val map = sdk.getMBConfigurationMap(appId, appKey)
                sdk.init(map, context)
                Log.d(TAG, "Mintegral SDK initialized with appId=$appId")
            } else {
                Log.d(TAG, "Mintegral consent configured (hasConsent=$hasConsent, doNotSell=$doNotSell)")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Mintegral adapter configuration skipped or failed: ${t.message}")
        }
    }

    /**
     * Configures AppLovin Mediation Adapter privacy settings.
     */
    @JvmStatic
    @JvmOverloads
    fun configureAppLovin(
        context: Context,
        hasConsent: Boolean = true,
        doNotSell: Boolean = false
    ) {
        try {
            AppLovinPrivacySettings.setHasUserConsent(hasConsent)
            AppLovinPrivacySettings.setDoNotSell(doNotSell)
            AppLovinSdk.getInstance(context)
            Log.d(TAG, "AppLovin adapter configured (hasConsent=$hasConsent, doNotSell=$doNotSell)")
        } catch (t: Throwable) {
            Log.w(TAG, "AppLovin adapter configuration skipped or failed: ${t.message}")
        }
    }

    /**
     * Logs the initialization status of all GMA Next-Gen mediation adapters (Pangle, Mintegral, AppLovin, AdMob).
     */
    @JvmStatic
    fun logAdapterStatuses(initializationStatus: InitializationStatus) {
        logAdapterInitializationStatus(initializationStatus)
    }

    @JvmStatic
    fun logAdapterInitializationStatus(initializationStatus: InitializationStatus) {
        try {
            val statusMap: Map<String, AdapterStatus> = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                Log.d(
                    TAG,
                    "Adapter [$adapterClass] -> state=${status.initializationState}, " +
                        "latency=${status.latency}ms, desc=${status.description}"
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to log adapter status: ${t.message}")
        }
    }
}
