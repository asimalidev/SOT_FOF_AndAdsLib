package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.manual.mediation.library.sotadlib.BuildConfig
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.utils.AdLoadingDialog
import com.manual.mediation.library.sotadlib.utils.NetworkCheck

class AdmobInterstitialAdSplash(
    activity: Activity? = null,
    val adId: String,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailed: (() -> Unit)? = null,
    onAdTimeout: (() -> Unit)? = null,
    onAdShowed: (() -> Unit)? = null
) {
    private var interstitialAd: InterstitialAd? = null
    private var currentActivity: Activity? = activity
    private var isShowingAd = false
    private var isShowingDialog = false
    private val timeoutHandler = Handler(Looper.getMainLooper())

    private val timeoutRunnable = Runnable {
        if (interstitialAd == null) {
            onAdTimeout?.invoke()
            dismissWaitDialog()
            Log.i("SOT_ADS_TAG", "Admob: Interstitial : Timeout()")
            currentActivity?.let {
                if (BuildConfig.DEBUG) {
                    Toast.makeText(it, "Admob: Interstitial : Timeout", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    init {
        currentActivity?.let {
            fetchAd(onAdDismissed, onAdFailed, onAdShowed)
        }
    }

    private fun fetchAd(
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        onAdShowed: (() -> Unit)? = null
    ) {
        if (isAdAvailable()) return

        if (!NetworkCheck.isNetworkAvailable(currentActivity)) {
            onAdFailed?.invoke()
            return
        }

        currentActivity?.let {
            if (BuildConfig.DEBUG) {
                Toast.makeText(currentActivity, "Admob: Interstitial : Request", Toast.LENGTH_SHORT).show()
            }
        }

        val adRequest = AdRequest.Builder(adId).build()
        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.i("SOT_ADS_TAG", "Admob Next-Gen: Interstitial : onAdLoaded()")
                    timeoutHandler.post {
                        interstitialAd = ad
                        showAdIfAvailable(onAdDismissed, onAdFailed, onAdShowed)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i("SOT_ADS_TAG", "Admob Next-Gen: Interstitial : onAdFailedToLoad(${adError.message})")
                    timeoutHandler.post {
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissWaitDialog()
                        onAdFailed?.invoke()
                        currentActivity?.let {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(
                                    currentActivity,
                                    "Admob: Interstitial : Failed To Load",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        )
        timeoutHandler.postDelayed(timeoutRunnable, 20000)
    }

    private fun showAdIfAvailable(
        onAdDismissed: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null,
        onAdShowed: (() -> Unit)? = null
    ) {
        if (!isShowingAd && isAdAvailable()) {
            interstitialAd?.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    Log.i("SOT_ADS_TAG", "Admob: Interstitial : onAdDismissed()")
                    timeoutHandler.post {
                        isShowingAd = false
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissWaitDialog()
                        interstitialAd = null
                        onAdDismissed?.invoke()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    Log.i("SOT_ADS_TAG", "Admob: Interstitial : onAdFailedToShow()")
                    timeoutHandler.post {
                        isShowingAd = false
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissWaitDialog()
                        interstitialAd = null
                        onAdFailed?.invoke()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    Log.i("SOT_ADS_TAG", "Admob: Interstitial : onAdShowed()")
                    timeoutHandler.post {
                        isShowingAd = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        dismissWaitDialog()
                        onAdShowed?.invoke()
                    }
                }
            }

            currentActivity?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed && interstitialAd != null) {
                    showWaitDialog()

                    Handler(Looper.getMainLooper()).postDelayed({
                        currentActivity?.let { safeActivity ->
                            if (!safeActivity.isFinishing && !safeActivity.isDestroyed && interstitialAd != null) {
                                interstitialAd?.show(safeActivity)
                            } else {
                                dismissWaitDialog()
                            }
                        }
                    }, 500)
                }
            }
        }
    }

    private fun isAdAvailable(): Boolean {
        return interstitialAd != null
    }

    private fun showWaitDialog() {
        currentActivity?.let {
            if (!it.isFinishing && !it.isDestroyed) {
                val view = it.layoutInflater.inflate(R.layout.dialog_adloading, null, false)
                isShowingDialog = true
                AdLoadingDialog.setContentView(it, view = view, isCancelable = false).showDialogInterstitial()
            }
        }
    }

    private fun dismissWaitDialog() {
        if (isShowingDialog) {
            currentActivity?.let {
                if (!it.isFinishing && !it.isDestroyed) {
                    AdLoadingDialog.dismissDialog(it)
                }
            }
            isShowingDialog = false
        }
    }
}
