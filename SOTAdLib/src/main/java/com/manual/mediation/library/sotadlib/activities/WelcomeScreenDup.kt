package com.manual.mediation.library.sotadlib.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.callingClasses.WelcomeScreensConfiguration
import com.manual.mediation.library.sotadlib.interfaces.WelcomeDupInterface
import com.manual.mediation.library.sotadlib.objects.StatusBarColor
import com.manual.mediation.library.sotadlib.utils.hideSystemUIUpdated
import com.manual.mediation.library.sotadlib.utils.setStatusBarColor

class WelcomeScreenDup : AppCompatBaseActivity(), WelcomeDupInterface {

    private var myView: View? = null
    private var sotAdsConfigurations: SOTAdsConfigurations? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUIUpdated()
        onBackPressedDispatcher.addCallback(this) {
            /**Disable backPress until Home**/
        }
        setStatusBarColor(StatusBarColor.getColor())
        sotAdsConfigurations = SOTAdsManager.getConfigurations()

        WelcomeScreensConfiguration.welcomeInstance?.let { config ->
            config.setWelcomeDupInterface(this)
            myView = config.view
            myView?.parent?.let { parent ->
                if (parent is ViewGroup) {
                    parent.removeView(myView)
                }
            }
            setContentView(myView)
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_2") == true) {
            loadAdmobWTTwoNatives()
        }
    }

    override fun onResume() {
        super.onResume()
        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_SURVEY_2") == true) {
            showAdmobLanguageScreenOneNatives()
        } else {
            myView?.let {
                myView?.findViewById<CardView>(R.id.nativeAdContainerAdmob)?.visibility = View.GONE
                myView?.findViewById<CardView>(R.id.nativeAdContainerMintegral)?.visibility = View.GONE
            }
        }
    }

    private fun showAdmobLanguageScreenOneNatives() {
        myView?.let {
            myView?.findViewById<CardView>(R.id.nativeAdContainerMintegral)?.visibility = View.GONE
            myView?.findViewById<CardView>(R.id.nativeAdContainerAdmob)?.visibility = View.VISIBLE
            sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_SURVEY_2")?.let { adId ->
                AdmobNativeAdManager.requestAd(
                    mContext = this,
                    adId = adId,
                    adName = "NATIVE_SURVEY_2",
                    isMedia = true,
                    isMediumAd = true,
                    remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_SURVEY_2") ?: true,
                    nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_SURVEY_2"),
                    populateView = true,
                    requestAgain = false,
                    adContainer = myView?.findViewById(R.id.nativeAdContainerAdmob),
                    onAdFailed = {
                        myView?.findViewById<CardView>(R.id.nativeAdContainerAdmob)?.visibility = View.GONE
                        Log.i("SOT_ADS_TAG", "WelcomeScreenDup: Admob: onAdFailed()")
                    },
                    onAdLoaded = {
                        Log.i("SOT_ADS_TAG", "WelcomeScreenDup: Admob: onAdLoaded()")
                    }
                )
            } ?: Log.w("WelcomeScreenDup", "ADMOB_NATIVE_SURVEY_2 ad ID is missing.")
        }
    }

    override fun endWelcomeTwoScreen() {
        finish()
    }

    private fun loadAdmobWTTwoNatives() {
        val adId = sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_2")
        if (!adId.isNullOrEmpty()) {
            AdmobNativeAdManager.requestAd(
                mContext = this,
                adId = adId,
                adName = "WALKTHROUGH_2",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_WALKTHROUGH_2") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_WALKTHROUGH_2"),
                populateView = false
            )
        } else {
            Log.e("SOT_ADS_TAG", "Admob ad ID not found for ADMOB_NATIVE_WALKTHROUGH_2")
        }
    }
}
