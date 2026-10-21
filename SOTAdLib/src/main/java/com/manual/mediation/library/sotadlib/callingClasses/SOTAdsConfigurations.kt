package com.manual.mediation.library.sotadlib.callingClasses

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobInterstitialAdSplash
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobResumeAdSplash
import com.manual.mediation.library.sotadlib.adMobAdClasses.NativeAdSize
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import com.manual.mediation.library.sotadlib.utils.PrefHelper
import com.manual.mediation.library.sotadlib.utilsGoogleAdsConsent.ConsentConfigurations

class SOTAdsConfigurations private constructor() {

    var languageScreensConfiguration: LanguageScreensConfiguration? = null
    var welcomeScreensConfiguration: WelcomeScreensConfiguration? = null
    var walkThroughScreensConfiguration: WalkThroughScreensConfiguration? = null
    private var remoteConfigData: HashMap<String, Any>? = null
    var firstOpenFlowAdIds: HashMap<String, String> = HashMap()
    private lateinit var admobResumeAdSplash: AdmobResumeAdSplash
    private lateinit var admobInterstitialAdSplash: AdmobInterstitialAdSplash

    /**
     * Checks whether a Native Ad slot is enabled in [remoteConfigData].
     * Supports both Boolean (`true`/`false`) and String (`"OFF"`, `"SMALL"`, `"MEDIUM"`, `"LARGE"`, `"MEDIA_LEFT"`).
     */
    fun isNativeAdEnabled(key: String): Boolean {
        return NativeAdSize.isEnabled(
            value = remoteConfigData?.get(key),
            isPurchased = remoteConfigData?.get("IS_PURCHASED")
        )
    }

    /**
     * Resolves the Native Ad size for [key] from [remoteConfigData].
     * Looks up:
     * 1. `"${key}_SIZE"` (e.g., `"NATIVE_LANGUAGE_1_SIZE"`)
     * 2. `"NATIVE_SIZE"` (global FOF native size override)
     * 3. `[key]` itself (if passed as `"SMALL"`, `"MEDIUM"`, `"LARGE"`, `"MEDIA_LEFT"`)
     */
    fun getNativeAdSize(key: String): Any? {
        return remoteConfigData?.get("${key}_SIZE")
            ?: remoteConfigData?.get("NATIVE_SIZE")
            ?: remoteConfigData?.get(key)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun setRemoteConfigData(activityContext: Activity, myRemoteConfigData: HashMap<String, Any>) {
        this.remoteConfigData = myRemoteConfigData
        this.remoteConfigData?.forEach { value ->
            Log.i(
                "RemoteConfigFetches",
                "SOTAdsConfigurations : setRemoteConfigData() " + value.key + " : " + value.value
            )
        }

        if (NetworkCheck.isNetworkAvailable(activityContext) &&
            myRemoteConfigData["IS_PURCHASED"] as? Boolean == false
        ) {
            if (!PrefHelper(activityContext).getBooleanDefault("StartScreens", default = false)) {
                if (isNativeAdEnabled("NATIVE_LANGUAGE_1")) {
                    loadAdmobLanguageScreenOneNatives(activityContext)
                }
                if (isNativeAdEnabled("NATIVE_LANGUAGE_2")) {
                    loadAdmobLanguageScreenDupNatives(activityContext)
                }
            }
            when (myRemoteConfigData["RESUME_INTER_SPLASH"]?.toString()?.trim()?.uppercase()) {
                "RESUME" -> {
                    showAdMobResumeAdSplash(activityContext)
                }
                "INTERSTITIAL" -> {
                    showAdMobInterstitialAdSplash(activityContext)
                }
                else -> {
                    proceedNext(activityContext)
                }
            }
        } else {
            proceedNext(activityContext)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadNativeAds(activityContext: Activity) {
        if (!PrefHelper(activityContext).getBooleanDefault("StartScreens", default = false)) {
            if (isNativeAdEnabled("NATIVE_LANGUAGE_1")) {
                loadAdmobLanguageScreenOneNatives(activityContext)
            }
            if (isNativeAdEnabled("NATIVE_LANGUAGE_2")) {
                loadAdmobLanguageScreenDupNatives(activityContext)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadAdmobLanguageScreenOneNatives(mContext: Activity) {
        val adId = firstOpenFlowAdIds.getOrDefault("ADMOB_NATIVE_LANGUAGE_1", "")
        if (adId.isEmpty()) return

        AdmobNativeAdManager.requestAd(
            mContext = mContext,
            adId = adId,
            adName = "NATIVE_LANGUAGE_1",
            isMedia = true,
            isMediumAd = true,
            remoteConfig = remoteConfigData?.get("NATIVE_LANGUAGE_1") ?: true,
            nativeSize = getNativeAdSize("NATIVE_LANGUAGE_1"),
            populateView = false
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadAdmobLanguageScreenDupNatives(mContext: Activity) {
        val adId = firstOpenFlowAdIds.getOrDefault("ADMOB_NATIVE_LANGUAGE_2", "")
        if (adId.isEmpty()) return

        AdmobNativeAdManager.requestAd(
            mContext = mContext,
            adId = adId,
            adName = "NATIVE_LANGUAGE_2",
            isMedia = true,
            isMediumAd = true,
            remoteConfig = remoteConfigData?.get("NATIVE_LANGUAGE_2") ?: true,
            nativeSize = getNativeAdSize("NATIVE_LANGUAGE_2"),
            populateView = false
        )
    }

    private fun showAdMobResumeAdSplash(activityContext: Activity) {
        val adId = firstOpenFlowAdIds["ADMOB_SPLASH_RESUME"] ?: ""
        if (adId.isEmpty()) {
            proceedNext(activityContext)
            return
        }
        admobResumeAdSplash = AdmobResumeAdSplash(
            activityContext,
            adId,
            onAdDismissed = { proceedNext(activityContext) },
            onAdFailed = { proceedNext(activityContext) },
            onAdTimeout = { proceedNext(activityContext) },
            onAdShowed = {}
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun showAdMobInterstitialAdSplash(activityContext: Activity) {
        val adId = firstOpenFlowAdIds.getOrDefault("ADMOB_SPLASH_INTERSTITIAL", "")
        if (adId.isEmpty()) {
            Log.e("SOT_ADS_TAG", "Key ADMOB_SPLASH_INTERSTITIAL is missing! Skipping ad.")
            proceedNext(activityContext)
            return
        }

        admobInterstitialAdSplash = AdmobInterstitialAdSplash(
            activityContext,
            adId,
            onAdDismissed = { proceedNext(activityContext) },
            onAdFailed = { proceedNext(activityContext) },
            onAdTimeout = { proceedNext(activityContext) },
            onAdShowed = {}
        )
    }

    fun proceedNext(activityContext: Activity) {
        if (PrefHelper(activityContext).getBooleanDefault("StartScreens", default = false)) {
            SOTAdsManager.notifyFlowFinished()
        } else {
            startLanguageScreenConfiguration()
        }
    }

    fun getRemoteConfigData(): HashMap<String, Any>? {
        return this.remoteConfigData
    }

    private fun startLanguageScreenConfiguration() {
        Log.i("LanguageScreens", "SOTAdsConfigurations : startLanguageScreenConfiguration() ")
        this.languageScreensConfiguration?.languageInitializationSetup() ?: run { }
    }

    fun startWelcomeScreenConfiguration() {
        Log.i("WelcomeScreens", "SOTAdsConfigurations : startWelcomeScreenConfiguration() ")
        SOTAdsManager.notifyReConfigureBuilders()
        this.welcomeScreensConfiguration?.welcomeInitializationSetup() ?: run { }
    }

    fun startWalkThroughConfiguration() {
        Log.i("WalkThroughScreens", "SOTAdsConfigurations : startWalkThroughConfiguration() ")
        this.walkThroughScreensConfiguration?.walkThroughInitializationSetup() ?: run { }
    }

    class Builder {
        private lateinit var consentConfigurations: ConsentConfigurations
        private var languageScreensConfiguration: LanguageScreensConfiguration? = null
        private var welcomeScreensConfiguration: WelcomeScreensConfiguration? = null
        private var walkThroughScreensConfiguration: WalkThroughScreensConfiguration? = null
        private var firstOpenFlowAdIds: HashMap<String, String> = HashMap()

        fun setFirstOpenFlowAdIds(firstOpenFlowAdIds: HashMap<String, String>) = apply {
            this.firstOpenFlowAdIds = firstOpenFlowAdIds
            this.firstOpenFlowAdIds.forEach { value ->
                Log.i("SOT_ADS_TAG", "setFirstOpenFlowAdIds : " + value.key + " : " + value.value)
            }
        }

        fun setConsentConfig(consentConfig: ConsentConfigurations) = apply {
            this.consentConfigurations = consentConfig
        }

        fun setLanguageScreenConfiguration(languageScreensConfiguration: LanguageScreensConfiguration) = apply {
            this.languageScreensConfiguration = languageScreensConfiguration
        }

        fun setWelcomeScreenConfiguration(welcomeScreensConfiguration: WelcomeScreensConfiguration) = apply {
            this.welcomeScreensConfiguration = welcomeScreensConfiguration
        }

        fun setWalkThroughScreenConfiguration(walkThroughScreensConfiguration: WalkThroughScreensConfiguration) = apply {
            this.walkThroughScreensConfiguration = walkThroughScreensConfiguration
        }

        fun build(): SOTAdsConfigurations {
            if (!::consentConfigurations.isInitialized) {
                throw IllegalStateException("ConsentConfigurations must be provided")
            }
            val sotAdsConfigurations = SOTAdsConfigurations()
            sotAdsConfigurations.firstOpenFlowAdIds = firstOpenFlowAdIds
            sotAdsConfigurations.welcomeScreensConfiguration = welcomeScreensConfiguration
            sotAdsConfigurations.languageScreensConfiguration = languageScreensConfiguration
            sotAdsConfigurations.walkThroughScreensConfiguration = walkThroughScreensConfiguration
            return sotAdsConfigurations
        }
    }
}
