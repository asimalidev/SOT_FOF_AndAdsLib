package com.manual.mediation.library.sotadlib.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.adapters.LanguageAdapter
import com.manual.mediation.library.sotadlib.callingClasses.LanguageScreensConfiguration
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.interfaces.CommonEventTracker
import com.manual.mediation.library.sotadlib.utils.hideSystemUIUpdated
import com.manual.mediation.library.sotadlib.utils.setStatusBarColor

class LanguageScreenDup : AppCompatBaseActivity() {
    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var imvDone: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private var sotAdsConfigurations: SOTAdsConfigurations? = null
    private var tracker: CommonEventTracker? = null
    private var comeFrom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        sotAdsConfigurations = SOTAdsManager.getConfigurations()
        hideSystemUIUpdated()
        comeFrom = intent.getStringExtra("comeFrom")
        setContentView(R.layout.language_screen_dup)
        tracker = sotAdsConfigurations?.languageScreensConfiguration?.eventTracker
        tracker?.logEvent(
            this,
            "language2_scr"
        )
        Log.i("SOTStartTestActivity", "language2_scr")
        imvDone = findViewById(R.id.imvDone)
        progressBar = findViewById(R.id.progressBar)
        imvDone.visibility = View.INVISIBLE
        val delayStr = sotAdsConfigurations?.getRemoteConfigData()?.get("DELAY_TO_SHOW_LANGUAGE_DONE")?.toString()

        Log.i("SOT_ADS_TAG", "DELAY_TO_SHOW_LANGUAGE_DONE: $delayStr")

        val timeDelayMillis = delayStr?.toLongOrNull() ?: 3000L

        if (timeDelayMillis in 1000L..10000L) {
            imvDone.postDelayed({
                imvDone.visibility = View.VISIBLE
                progressBar.visibility = View.INVISIBLE
            }, timeDelayMillis)
        } else {
            imvDone.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
        recyclerView = findViewById(R.id.recyclerViewLanguage)
        recyclerView.layoutManager = LinearLayoutManager(this)
        onBackPressedDispatcher.addCallback(this) {
            /**Disable backPress until Home**/
        }

        LanguageScreensConfiguration.languageInstance?.let { config ->
            Log.d("fontColor", "config.tow:${config.fontColor} ")

            config.headingColor?.let {
                findViewById<TextView>(R.id.txtSelectKeyboard).setTextColor(it)
                findViewById<TextView>(R.id.txtAllLanguages).setTextColor(it)
            }
            config.tickSelector?.let {
                findViewById<AppCompatButton>(R.id.imvDone).background = it
            }

            config.nextButtonText?.let { customText ->
                findViewById<AppCompatButton>(R.id.imvDone).text = customText
            }
            config.theme?.let {
                val rootView = findViewById<View>(R.id.root_view)
                rootView.setBackgroundColor(it)
                setStatusBarColor(it)
            }
            config.statusBarColor?.let {
                setStatusBarColor(it)
            }
            config.languageList?.let { languageList ->
                config.selectedDrawable?.let { selectedDrawable ->
                    config.unSelectedDrawable?.let { unSelectedDrawable ->
                        config.selectedRadio?.let { selectedRadio ->
                            config.unSelectedRadio?.let { unSelectedRadio ->
                                languageAdapter = LanguageAdapter(
                                    ctx = this,
                                    languages = languageList,
                                    selectedDrawable = selectedDrawable,
                                    unSelectedDrawable = unSelectedDrawable,
                                    selectedRadio = selectedRadio,
                                    unSelectedRadio = unSelectedRadio,
                                    onItemClickListener = { _ ->
                                        tracker?.logEvent(
                                            this,
                                            "language2_scr_tap_language"
                                        )
                                    },
                                    fontColor = config.fontColor!!
                                )
                                recyclerView.adapter = languageAdapter
                            }
                        }
                    }
                }
            }
        }

        imvDone.setOnClickListener {
            tracker?.logEvent(
                this,
                "language2_scr_tap_next"
            )
            Log.i("SOTStartTestActivity", "language2_scr_tap_language")
            intent?.let { incomingIntent ->
                Log.d("comeFrom", "intent:$incomingIntent")

                val comeFrom = incomingIntent.getStringExtra("comeFrom")

                if (comeFrom == "AppSettings") {
                    finish()
                } else {
                    SOTAdsManager.showWelcomeScreen()
                    finish()
                }
            }
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_SURVEY_1") == true && comeFrom == null) {
            loadAdmobSurveyOneNatives()
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_SURVEY_2") == true && comeFrom == null) {
            loadAdmobSurveyDupNatives()
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                intent?.let {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onResume() {
        super.onResume()

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_LANGUAGE_2") == true) {
            findViewById<CardView>(R.id.nativeAdContainerAd).visibility = View.VISIBLE
            showAdmobLanguageScreenDupNatives()
        } else {
            findViewById<CardView>(R.id.nativeAdContainerAd)?.visibility = View.GONE
        }
    }

    private fun showAdmobLanguageScreenDupNatives() {
        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_LANGUAGE_2")?.let { adId ->
            AdmobNativeAdManager.requestAd(
                mContext = this,
                adId = adId,
                adName = "NATIVE_LANGUAGE_2",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_LANGUAGE_2") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_LANGUAGE_2"),
                populateView = true,
                requestAgain = false,
                adContainer = findViewById(R.id.nativeAdContainerAd),
                onAdFailed = {
                    tracker?.logEvent(
                        this,
                        "sot_language_two_onAdFailed"
                    )
                    findViewById<CardView>(R.id.nativeAdContainerAd)?.visibility = View.GONE
                    Log.i("SOT_ADS_TAG", "LanguageScreenDup: Admob onAdFailed()")
                },
                onAdLoaded = {
                    tracker?.logEvent(
                        this,
                        "sot_language_two_onAdLoaded"
                    )
                    Log.i("SOT_ADS_TAG", "LanguageScreenDup: Admob onAdLoaded()")
                }
            )
        } ?: Log.w("SOT_ADS_TAG", "ADMOB_NATIVE_LANGUAGE_2 ad ID is missing.")
    }

    private fun loadAdmobSurveyOneNatives() {
        val adId = sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_SURVEY_1")
        if (!adId.isNullOrEmpty()) {
            AdmobNativeAdManager.requestAd(
                mContext = this,
                adId = adId,
                adName = "NATIVE_SURVEY_1",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_SURVEY_1") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_SURVEY_1"),
                populateView = false
            )
        } else {
            Log.w("SOT_ADS_TAG", "ADMOB_NATIVE_SURVEY_1 ad ID is missing.")
        }
    }

    private fun loadAdmobSurveyDupNatives() {
        val adId = sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_SURVEY_2")
        if (!adId.isNullOrEmpty()) {
            AdmobNativeAdManager.requestAd(
                mContext = this,
                adId = adId,
                adName = "NATIVE_SURVEY_2",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_SURVEY_2") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_SURVEY_2"),
                populateView = false
            )
        } else {
            Log.e("SOT_ADS_TAG", "Admob ad ID not found for NATIVE_SURVEY_2")
        }
    }
}
