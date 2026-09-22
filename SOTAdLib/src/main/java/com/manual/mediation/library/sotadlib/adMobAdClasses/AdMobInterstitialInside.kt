package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.data.InterstitialMaster.interstitialAdMobHashMap
import com.manual.mediation.library.sotadlib.utils.AdLoadingDialog
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

//@SuppressLint("StaticFieldLeak")
//object AdMobInterstitialInside : CoroutineScope by MainScope() {
//    private const val adShowingDelayTime = 1500
//    private var isShowDialog = true
//
//    private var mContextAdmob: Context? = null
//    private var onAdClosedCallBackAdmob: (() -> Unit)? = null
//    private var onAdLoadedCallBackAdmob: (() -> Unit)? = null
//
//    fun checkAndLoadAdMobInterstitial(
//        context: Context?,
//        nameFragment: String,
//        adId: String,
//        onAdLoadedCallAdmob: (() -> Unit)? = null
//    ) {
//        mContextAdmob = context
//        onAdLoadedCallBackAdmob = onAdLoadedCallAdmob
//
//        if (NetworkCheck.isNetworkAvailable(mContextAdmob)) {
//            if (!interstitialAdMobHashMap.containsKey(nameFragment)) {
//                loadAdmobInterstitial(nameFragment, adId)
//            }
//        } else {
//            return
//        }
//    }
//
//    private fun loadAdmobInterstitial(nameFragment: String, adId: String) {
//        Log.i("SOT_ADS_TAG", "Requesting AdMob Interstitial: $nameFragment")
//        if (!interstitialAdMobHashMap.containsKey(nameFragment)) {
//            val adRequestInterstitial = AdRequest.Builder().build()
//            InterstitialAd.load(
//                mContextAdmob!!,
//                adId,
//                adRequestInterstitial,
//                object : InterstitialAdLoadCallback() {
//                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                        Log.i("SOT_ADS_TAG", "AdMob Interstitial Loaded: $nameFragment")
//                        interstitialAdMobHashMap[nameFragment] = interstitialAd
//                        onAdLoadedCallBackAdmob?.invoke()
//                        onAdLoadedCallBackAdmob = null
//                    }
//
//                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                        Log.e(
//                            "SOT_ADS_TAG",
//                            "AdMob Interstitial Failed to Load: $nameFragment. Error: ${loadAdError.message}"
//                        )
//                    }
//                }
//            )
//        }
//    }
//
//    fun showIfAvailableOrLoadAdMobInterstitial(
//        context: Context?,
//        nameFragment: String,
//        adId: String,
//        onAdClosedCallBackAdmob: () -> Unit,
//        onAdShowedCallBackAdmob: () -> Unit
//    ) {
//        mContextAdmob = context
//        isShowDialog = true
//        this.onAdClosedCallBackAdmob = onAdClosedCallBackAdmob
//
//        if (interstitialAdMobHashMap.containsKey(nameFragment)) {
//            showAdmobInterstitial(onAdShowedCallBackAdmob, nameFragment)
//        } else {
//            Log.i("SOT_ADS_TAG", "Ad not available. Requesting new ad: $nameFragment")
//            checkAndLoadAdMobInterstitial(context, nameFragment, adId)
//            onAdClosedCallBackAdmob.invoke()
//        }
//    }
//
//    private fun showAdmobInterstitial(onAdShowedCallBackAdmob: () -> Unit, nameFragment: String) {
//        showWaitDialog()
//        try {
//            Handler(Looper.getMainLooper()).postDelayed({
//                dismissWaitDialog()
//                val interstitialAd = interstitialAdMobHashMap[nameFragment]
//                if (interstitialAd != null) {
//                    interstitialAd.show(mContextAdmob as Activity)
//                    interstitialAd.fullScreenContentCallback =
//                        object : FullScreenContentCallback() {
//                            override fun onAdDismissedFullScreenContent() {
//                                Log.i("SOT_ADS_TAG", "AdMob Interstitial Dismissed: $nameFragment")
//                                onAdClosedCallBackAdmob?.invoke()
//                                interstitialAdMobHashMap.remove(nameFragment)
//                            }
//
//                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
//                                Log.e(
//                                    "SOT_ADS_TAG",
//                                    "Failed to Show AdMob Interstitial: $nameFragment. Error: ${adError.message}"
//                                )
//                                onAdClosedCallBackAdmob?.invoke()
//                                interstitialAdMobHashMap.remove(nameFragment)
//                            }
//
//                            override fun onAdShowedFullScreenContent() {
//                                Log.i("SOT_ADS_TAG", "AdMob Interstitial Shown: $nameFragment")
//                                onAdShowedCallBackAdmob.invoke()
//                            }
//                        }
//                } else {
//                    onAdClosedCallBackAdmob?.invoke()
//                }
//            }, adShowingDelayTime.toLong())
//        } catch (e: Exception) {
//            dismissWaitDialog()
//            Log.e("SOT_ADS_TAG", "Error showing AdMob Interstitial: ${e.message}")
//        }
//    }
//
//    private fun showWaitDialog() {
//        if (isShowDialog) {
//            mContextAdmob?.let {
//                val view = (it as Activity).layoutInflater.inflate(
//                    R.layout.dialog_adloading,
//                    null,
//                    false
//                )
//                AdLoadingDialog.setContentView(it, view = view, isCancelable = false)
//                    .showDialogInterstitial()
//            }
//        }
//    }
//
//    private fun dismissWaitDialog() {
//        mContextAdmob?.let {
//            AdLoadingDialog.dismissDialog(it as Activity)
//        }
//    }
//}


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

    // ✅ ADDED: Boolean to prevent multiple dialogs from stacking
    private var isWaitDialogShowing = false

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

    private fun loadAdmobInterstitial(nameFragment: String, adId: String, onAdLoadedCallAdmob: (() -> Unit)?) {
        isAdLoading = true
        Log.i("SOT_ADS_TAG", "Requesting AdMob Interstitial: $nameFragment")

        val adRequestInterstitial = AdRequest.Builder().build()
        InterstitialAd.load(
            mContextAdmob!!,
            adId,
            adRequestInterstitial,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.i("SOT_ADS_TAG", "AdMob Interstitial Loaded: $nameFragment")
                    isAdLoading = false
                    interstitialAdMobHashMap[nameFragment] = interstitialAd

                    // ✅ FIXED: Safely grab BOTH callbacks before clearing them
                    if (pendingShowCallback != null && pendingClosedCallback != null) {
                        val showCallback = pendingShowCallback!!
                        val closedCallback = pendingClosedCallback!!

                        pendingShowCallback = null
                        pendingClosedCallback = null

                        // ✅ FIXED: Pass the closedCallback so the app continues after the ad!
                        showAdmobInterstitial(showCallback, nameFragment, closedCallback)
                    } else {
                        onAdLoadedCallAdmob?.invoke()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("SOT_ADS_TAG", "AdMob Interstitial Failed: $nameFragment. Error: ${loadAdError.message}")
                    isAdLoading = false

                    pendingClosedCallback?.let {
                        dismissWaitDialog()
                        it.invoke()
                        pendingShowCallback = null
                        pendingClosedCallback = null
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
            // Ad is already loaded. Show immediately.
            showAdmobInterstitial(onAdShowedCallBackAdmob, nameFragment, onAdClosedCallBackAdmob)
        } else {
            // Ad is not ready yet. Show Wait Dialog and hold callbacks.
            Log.i("SOT_ADS_TAG", "Ad not available. Requesting on-demand and waiting: $nameFragment")
            showWaitDialog()

            pendingShowCallback = onAdShowedCallBackAdmob
            pendingClosedCallback = onAdClosedCallBackAdmob

            checkAndLoadAdMobInterstitial(context, nameFragment, adId, null)

            // 5-Second Safety Timeout
            Handler(Looper.getMainLooper()).postDelayed({
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
        showWaitDialog() // This is now safe because of our boolean check
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                dismissWaitDialog()
                val interstitialAd = interstitialAdMobHashMap[nameFragment]
                if (interstitialAd != null && mContextAdmob is Activity) {
                    interstitialAd.show(mContextAdmob as Activity)
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.i("SOT_ADS_TAG", "AdMob Interstitial Dismissed: $nameFragment")
                            onAdClosedCallBackAdmob?.invoke()
                            // ✅ FIXED: Removed duplicate pendingClosedCallback invocation here to prevent crashes
                            interstitialAdMobHashMap.remove(nameFragment)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            Log.e("SOT_ADS_TAG", "Failed to Show AdMob: $nameFragment. Error: ${adError.message}")
                            onAdClosedCallBackAdmob?.invoke()
                            interstitialAdMobHashMap.remove(nameFragment)
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.i("SOT_ADS_TAG", "AdMob Interstitial Shown: $nameFragment")
                            onAdShowedCallBackAdmob.invoke()
                        }
                    }
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

    // ✅ FIXED: Now checks if dialog is already showing before inflating another one
    private fun showWaitDialog() {
        if (isShowDialog && mContextAdmob is Activity && !isWaitDialogShowing) {
            isWaitDialogShowing = true
            val view = (mContextAdmob as Activity).layoutInflater.inflate(R.layout.dialog_adloading, null, false)
            AdLoadingDialog.setContentView(mContextAdmob as Activity, view = view, isCancelable = false).showDialogInterstitial()
        }
    }

    // ✅ FIXED: Resets the boolean when dismissed
    private fun dismissWaitDialog() {
        if (mContextAdmob is Activity && isWaitDialogShowing) {
            isWaitDialogShowing = false
            AdLoadingDialog.dismissDialog(mContextAdmob as Activity)
        }
    }
}