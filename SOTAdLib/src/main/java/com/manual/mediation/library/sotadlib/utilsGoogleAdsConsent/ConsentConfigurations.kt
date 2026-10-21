package com.manual.mediation.library.sotadlib.utilsGoogleAdsConsent

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.manual.mediation.library.sotadlib.mediation.SotMediationAdapterHelper
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ConsentConfigurations private constructor(
    private val activityContext: Activity,
    private val applicationContext: Application,
    private val admobAppId: String,
    private val testDeviceHashedIdList: ArrayList<String>,
    private val onConsentGathered: () -> Unit,
    private val mintegralAppKey: String,
    private val mintegralAppId: String,
    private val unityGameId: String,
    private val unityTestMode: Boolean
) {

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val isCallbackInvoked = AtomicBoolean(false)
    private val slowInternetHandler = Handler(Looper.getMainLooper())

    init {
        consentInitializationSetup()
    }

    private fun consentInitializationSetup() {
        Log.i("ConsentMessage", "ConsentConfigurations: consentInitializationSetup called")

        // Timeout fallback
        slowInternetHandler.postDelayed({ safeInvokeCallback() }, 15000)

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(activityContext)
        googleMobileAdsConsentManager.gatherConsent(
            activity = activityContext,
            testDeviceHashedIdList = testDeviceHashedIdList,
            removeSlowInternetCallBack = {
                Log.i("ConsentMessage", "ConsentConfigurations: removeSlowInternetCallBack")
                slowInternetHandler.removeCallbacksAndMessages(null)
            },
            errorMakingRequest = {
                Log.i("ConsentMessage", "ConsentConfigurations: errorMakingRequest")
                initializeMobileAdsSdk()
            },
            onConsentGatheringCompleteListener = { error ->
                if (googleMobileAdsConsentManager.canRequestAds) {
                    initializeMobileAdsSdk()
                } else if (error != null) {
                    Log.e("ConsentMessage", "ConsentConfigurations: error:: ${error.message}")
                    initializeMobileAdsSdk()
                }
            }
        )
    }

    private fun initializeMobileAdsSdk() {
        slowInternetHandler.removeCallbacksAndMessages(null)

        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            Log.i("ConsentMessage", "initializeMobileAdsSdk() already called")
            safeInvokeCallback()
            return
        }

        if (NetworkCheck.isNetworkAvailable(activityContext)) {
            activityContext.getSharedPreferences("ConsentMessage", MODE_PRIVATE)
                .edit().putBoolean("FirstTime", true).apply()

            // Configure Pangle, Mintegral, and AppLovin mediation consent
            SotMediationAdapterHelper.configureMediationAdapters(
                context = activityContext,
                hasConsent = true,
                mintegralAppId = mintegralAppId,
                mintegralAppKey = mintegralAppKey
            )

            val resolvedAppId = resolveAdMobAppId(activityContext, admobAppId)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val initConfig = InitializationConfig.Builder(resolvedAppId).build()
                    MobileAds.initialize(activityContext.applicationContext, initConfig) { status ->
                        SotMediationAdapterHelper.logAdapterStatuses(status)
                        if (testDeviceHashedIdList.isNotEmpty()) {
                            MobileAds.setRequestConfiguration(
                                RequestConfiguration.Builder()
                                    .setTestDeviceIds(testDeviceHashedIdList)
                                    .build()
                            )
                        }
                        safeInvokeCallbackOnMain()
                    }
                } catch (e: Exception) {
                    Log.e("ConsentMessage", "Failed to initialize GMA Next-Gen SDK: ${e.message}")
                    safeInvokeCallbackOnMain()
                }
            }
        } else {
            safeInvokeCallbackOnMain()
        }
    }

    private fun resolveAdMobAppId(context: Context, explicitAppId: String): String {
        if (explicitAppId.isNotBlank()) return explicitAppId

        // 1. Check AndroidManifest meta-data com.google.android.gms.ads.APPLICATION_ID
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val manifestId = appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
            if (!manifestId.isNullOrBlank()) return manifestId
        } catch (_: Exception) {
        }

        // 2. Check string resource admob_app_id
        try {
            val resId = context.resources.getIdentifier("admob_app_id", "string", context.packageName)
            if (resId != 0) {
                val resString = context.getString(resId)
                if (resString.isNotBlank()) return resString
            }
        } catch (_: Exception) {
        }

        // 3. Fallback sample AdMob App ID
        return "ca-app-pub-3940256099942544~3347511713"
    }

    private fun safeInvokeCallbackOnMain() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            safeInvokeCallback()
        } else {
            Handler(Looper.getMainLooper()).post {
                safeInvokeCallback()
            }
        }
    }

    private fun safeInvokeCallback() {
        if (!isCallbackInvoked.getAndSet(true)) {
            onConsentGathered.invoke()
        }
    }

    class Builder {
        private lateinit var activityContext: Activity
        private lateinit var applicationContext: Application
        private var admobAppId: String = ""
        private var appKey: String = ""
        private var appId: String = ""
        private var gameId: String = ""
        private var testMode: Boolean = true
        private var testDeviceHashedIdList: ArrayList<String> = ArrayList()
        private lateinit var onConsentGathered: () -> Unit

        fun setApplicationContext(applicationContext: Application) = apply {
            this.applicationContext = applicationContext
        }

        fun setAdMobAppId(admobAppId: String) = apply {
            this.admobAppId = admobAppId
        }

        fun setMintegralInitializationId(appKey: String, appId: String) = apply {
            this.appKey = appKey
            this.appId = appId
        }

        fun setPangleInitializationId(appId: String) = apply {
            // Optional Pangle appId override (AdMob Mediation UI also provides this automatically)
        }

        fun setUnityInitializationId(gameId: String, testMode: Boolean) = apply {
            this.gameId = gameId
            this.testMode = testMode
        }

        fun setActivityContext(activity: Activity) = apply {
            this.activityContext = activity
        }

        fun setTestDeviceHashedIdList(ids: ArrayList<String>) = apply {
            this.testDeviceHashedIdList = ids
        }

        fun setOnConsentGatheredCallback(callback: () -> Unit) = apply {
            this.onConsentGathered = callback
        }

        fun build(): ConsentConfigurations {
            require(::activityContext.isInitialized) { "Activity context must be provided" }
            require(::applicationContext.isInitialized) { "Application context must be provided" }
            require(::onConsentGathered.isInitialized) { "OnConsentGathered callback must be provided" }

            return ConsentConfigurations(
                activityContext = activityContext,
                applicationContext = applicationContext,
                admobAppId = admobAppId,
                testDeviceHashedIdList = testDeviceHashedIdList,
                onConsentGathered = onConsentGathered,
                mintegralAppKey = appKey,
                mintegralAppId = appId,
                unityGameId = gameId,
                unityTestMode = testMode
            )
        }
    }
}
