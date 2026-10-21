package com.manual.mediation.library.sotadlib.activities

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
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
import com.manual.mediation.library.sotadlib.interfaces.LanguageInterface
import com.manual.mediation.library.sotadlib.objects.StatusBarColor
import com.manual.mediation.library.sotadlib.utils.hideSystemUIUpdated
import com.manual.mediation.library.sotadlib.utils.setStatusBarColor

class LanguageScreenOne : AppCompatBaseActivity(), LanguageInterface {

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var recyclerView: RecyclerView
    private var sotAdsConfigurations: SOTAdsConfigurations? = null
    private var tracker: CommonEventTracker? = null
    private var comeFrom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sotAdsConfigurations = SOTAdsManager.getConfigurations()
        tracker = sotAdsConfigurations?.languageScreensConfiguration?.eventTracker
        supportActionBar?.hide()
        hideSystemUIUpdated()
        setContentView(R.layout.language_screen_one)
        comeFrom = intent.getStringExtra("comeFrom")
        Log.d("comeFrom", "comeFromLanguage1:${intent.getStringExtra("comeFrom")} ")
        tracker?.logEvent(
            this,
            "language1_scr"
        )
        Log.i("SOTStartTestActivity", "language1_scr")
        onBackPressedDispatcher.addCallback(this) {
            /**Disable backPress until Home**/
        }
        recyclerView = findViewById(R.id.recyclerViewLanguage)
        recyclerView.layoutManager = LinearLayoutManager(this)

        Log.i("LanguageScreenOne", "Language: onCreate")

        LanguageScreensConfiguration.languageInstance?.let { config ->
            config.setLanguageInterface(this)
            Log.d("fontColor", "config.fontColor:${config.headingColor} ")
            config.headingColor?.let {
                findViewById<TextView>(R.id.txtSelectKeyboard).setTextColor(it)
                findViewById<TextView>(R.id.txtAllLanguages).setTextColor(it)
            }

            config.theme?.let {
                val rootView = findViewById<View>(R.id.root_view)
                rootView.setBackgroundColor(it)
            }
            config.statusBarColor?.let {
                setStatusBarColor(it)
                StatusBarColor.setColor(it)
            }
            config.languageList?.let {
                if (config.selectedDrawable != null && config.unSelectedDrawable != null) {
                    if (config.selectedRadio != null && config.unSelectedRadio != null) {
                        languageAdapter = LanguageAdapter(
                            ctx = this,
                            languages = config.languageList!!,
                            selectedDrawable = config.selectedDrawable!!,
                            unSelectedDrawable = config.unSelectedDrawable!!,
                            selectedRadio = config.selectedRadio!!,
                            unSelectedRadio = config.unSelectedRadio!!,
                            onItemClickListener = {
                                tracker?.logEvent(
                                    this,
                                    "language1_scr_tap_language"
                                )
                                config.showLanguageTwoScreen()
                            },
                            fontColor = config.fontColor!!
                        )

                        recyclerView.adapter = languageAdapter
                    }
                }
            }
        }
    }

    override fun showLanguageTwoScreen() {
        if (!isFinishing && !isDestroyed) {
            try {
                val intent = Intent(this, LanguageScreenDup::class.java)
                if (intent.resolveActivity(packageManager) != null) {
                    Log.d("comeFrom", "comeFrom:$comeFrom ")
                    if (comeFrom != null) {
                        intent.putExtra("comeFrom", comeFrom)
                        startActivity(
                            intent,
                            ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
                        )
                        finish()
                    } else {
                        startActivity(intent, ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle())
                        finish()
                    }
                } else {
                    Log.e("LanguageScreenOne", "No activity found to handle intent")
                }
            } catch (e: Exception) {
                Log.e("LanguageScreenOne", "Error starting activity", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val nativeLanguage1Enabled = sotAdsConfigurations?.isNativeAdEnabled("NATIVE_LANGUAGE_1") == true
        if (nativeLanguage1Enabled) {
            showAdmobLanguageScreenOneNatives()
        } else {
            findViewById<CardView>(R.id.nativeAdContainerAd)?.visibility = View.GONE
        }
    }

    private fun showAdmobLanguageScreenOneNatives() {
        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_LANGUAGE_1")?.let { adId ->
            AdmobNativeAdManager.requestAd(
                mContext = this,
                adId = adId,
                adName = "NATIVE_LANGUAGE_1",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_LANGUAGE_1") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_LANGUAGE_1"),
                populateView = true,
                requestAgain = false,
                adContainer = findViewById(R.id.nativeAdContainerAd),
                onAdFailed = {
                    findViewById<CardView>(R.id.nativeAdContainerAd)?.visibility = View.GONE
                    Log.i("LanguageScreenOne", "Language: onAdFailed()")
                },
                onAdLoaded = {
                    tracker?.logEvent(
                        this,
                        "sot_language_0ne_adShown"
                    )
                    Log.i("LanguageScreenOne", "Language: onAdLoaded()")
                }
            )
        } ?: Log.w("LanguageScreenOne", "ADMOB_NATIVE_LANGUAGE_1 ad ID is missing.")
    }
}
