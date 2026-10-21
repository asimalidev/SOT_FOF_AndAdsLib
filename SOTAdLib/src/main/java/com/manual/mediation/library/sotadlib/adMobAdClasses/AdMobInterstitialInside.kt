package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.utils.AdLoadingDialog
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

@SuppressLint("StaticFieldLeak")
object AdMobInterstitialInside : CoroutineScope by MainScope() {
    private const val adShowingDelayTime = 1000
    private var isShowDialog = true

    private var mContextAdmob: Context? = null
    var interstitialAdMobHashMap: HashMap<String, InterstitialAd> = HashMap()

    // Track callbacks for delayed loading
    private var pendingShowCallback: (() -> Unit)? = null
    private var pendingClosedCallback: (() -> Unit)? = null
    private var isAdLoading = false

    private var isWaitDialogShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun checkAndLoadAdMobInterstitial(
        context: Context?,
        nameFragment: String,
        adId: String,
        onAdLoadedCallAdmob: (() -> Unit)? = null
    ) {
        mContextAdmob = context
        if (NetworkCheck.isNetworkAvailable(mContextAdmob)) {
            if (!interstitialAdMobHashMap.containsKey(nameFragment) && !isAdLoading) {
                loadAdmobInterstitial(nameFragment, adId, onAdLoadedCallAdmob)
            } else {
                onAdLoadedCallAdmob?.invoke()
            }
        }
    }

    private fun loadAdmobInterstitial(
        nameFragment: String,
        adId: String,
        onAdLoadedCallAdmob: (() -> Unit)?
    ) {
        if (adId.isEmpty()) {
            isAdLoading = false
            return
        }
        isAdLoading = true
        Log.i("SOT_ADS_TAG", "Requesting AdMob Next-Gen Interstitial: $nameFragment")

        val adRequestInterstitial = AdRequest.Builder(adId).build()
        InterstitialAd.load(
            adRequestInterstitial,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.i("SOT_ADS_TAG", "AdMob Next-Gen Interstitial Loaded: $nameFragment")
                    mainHandler.post {
                        isAdLoading = false
                        interstitialAdMobHashMap[nameFragment] = interstitialAd

                        if (pendingShowCallback != null && pendingClosedCallback != null) {
                            val showCallback = pendingShowCallback!!
                            val closedCallback = pendingClosedCallback!!

                            pendingShowCallback = null
                            pendingClosedCallback = null

                            showAdmobInterstitial(showCallback, nameFragment, closedCallback)
                        } else {
                            onAdLoadedCallAdmob?.invoke()
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        "SOT_ADS_TAG",
                        "AdMob Next-Gen Interstitial Failed: $nameFragment. Error: ${adError.message}"
                    )
                    mainHandler.post {
                        isAdLoading = false

                        pendingClosedCallback?.let {
                            dismissWaitDialog()
                            it.invoke()
                            pendingShowCallback = null
                            pendingClosedCallback = null
                        }
                    }
                }
            }
        )
    }

    fun showIfAvailableOrLoadAdMobInterstitial(
        context: Context?,
        nameFragment: String,
        adId: String,
        onAdClosedCallBackAdmob: () -> Unit,
        onAdShowedCallBackAdmob: () -> Unit
    ) {
        mContextAdmob = context
        isShowDialog = true

        if (interstitialAdMobHashMap.containsKey(nameFragment)) {
            showAdmobInterstitial(onAdShowedCallBackAdmob, nameFragment, onAdClosedCallBackAdmob)
        } else {
            Log.i("SOT_ADS_TAG", "Ad not available. Requesting on-demand and waiting: $nameFragment")
            showWaitDialog()

            pendingShowCallback = onAdShowedCallBackAdmob
            pendingClosedCallback = onAdClosedCallBackAdmob

            checkAndLoadAdMobInterstitial(context, nameFragment, adId, null)

            // 5-Second Safety Timeout
            mainHandler.postDelayed({
                if (pendingClosedCallback != null) {
                    Log.e("SOT_ADS_TAG", "Ad Load Timeout: $nameFragment")
                    dismissWaitDialog()
                    pendingClosedCallback?.invoke()

                    pendingShowCallback = null
                    pendingClosedCallback = null
                }
            }, 5000)
        }
    }

    private fun showAdmobInterstitial(
        onAdShowedCallBackAdmob: () -> Unit,
        nameFragment: String,
        onAdClosedCallBackAdmob: (() -> Unit)? = null
    ) {
        showWaitDialog()
        try {
            mainHandler.postDelayed({
                dismissWaitDialog()
                val interstitialAd = interstitialAdMobHashMap[nameFragment]
                if (interstitialAd != null && mContextAdmob is Activity) {
                    interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            Log.i("SOT_ADS_TAG", "AdMob Interstitial Dismissed: $nameFragment")
                            mainHandler.post {
                                interstitialAdMobHashMap.remove(nameFragment)
                                onAdClosedCallBackAdmob?.invoke()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            Log.e(
                                "SOT_ADS_TAG",
                                "Failed to Show AdMob: $nameFragment. Error: ${fullScreenContentError.message}"
                            )
                            mainHandler.post {
                                interstitialAdMobHashMap.remove(nameFragment)
                                onAdClosedCallBackAdmob?.invoke()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.i("SOT_ADS_TAG", "AdMob Interstitial Shown: $nameFragment")
                            mainHandler.post {
                                onAdShowedCallBackAdmob.invoke()
                            }
                        }
                    }
                    interstitialAd.show(mContextAdmob as Activity)
                } else {
                    onAdClosedCallBackAdmob?.invoke()
                }
            }, adShowingDelayTime.toLong())
        } catch (e: Exception) {
            dismissWaitDialog()
            onAdClosedCallBackAdmob?.invoke()
            Log.e("SOT_ADS_TAG", "Error showing AdMob Interstitial: ${e.message}")
        }
    }

    private fun showWaitDialog() {
        if (isShowDialog && mContextAdmob is Activity && !isWaitDialogShowing) {
            val activity = mContextAdmob as Activity
            if (!activity.isFinishing && !activity.isDestroyed) {
                isWaitDialogShowing = true
                val view = activity.layoutInflater.inflate(R.layout.dialog_adloading, null, false)
                AdLoadingDialog.setContentView(activity, view = view, isCancelable = false)
                    .showDialogInterstitial()
            }
        }
    }

    private fun dismissWaitDialog() {
        if (mContextAdmob is Activity && isWaitDialogShowing) {
            isWaitDialogShowing = false
            val activity = mContextAdmob as Activity
            if (!activity.isFinishing && !activity.isDestroyed) {
                AdLoadingDialog.dismissDialog(activity)
            }
        }
    }
}
