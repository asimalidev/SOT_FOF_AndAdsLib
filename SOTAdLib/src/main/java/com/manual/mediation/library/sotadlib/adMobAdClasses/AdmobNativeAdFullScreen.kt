package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.utils.NetworkCheck

object AdmobNativeAdFullScreen {
    private val nativeAdCache = HashMap<String, NativeAd?>()
    private val adLoadingState = HashMap<String, Boolean>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun requestAd(
        mContext: Activity?,
        adId: String,
        adName: String = "",
        remoteConfig: Boolean = true,
        populateView: Boolean = false,
        adContainer: CardView? = null,
        onAdFailed: (() -> Unit)? = null,
        onAdLoaded: (() -> Unit)? = null
    ) {
        if (mContext == null || mContext.isFinishing || mContext.isDestroyed || !remoteConfig) {
            onAdFailed?.invoke()
            return
        }

        if (populateView && nativeAdCache[adName] != null) {
            Log.i("SOT_ADS_TAG", "Native: Admob: $adName showing from cache.")
            onAdLoaded?.invoke()
            showCachedAd(adName, adContainer)
            return
        }

        if (!populateView && (nativeAdCache[adName] != null || adLoadingState[adName] == true)) {
            Log.i("SOT_ADS_TAG", "Native: Admob: $adName already cached or loading. Skipping.")
            return
        }

        if (adId.isEmpty() || !NetworkCheck.isNetworkAvailable(mContext)) {
            onAdFailed?.invoke()
            return
        }

        adLoadingState[adName] = true
        Log.i("SOT_ADS_TAG", "Native: Admob Next-Gen: $adName starting network request...")

        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adRequest = NativeAdRequest.Builder(
            adId,
            listOf(NativeAd.NativeAdType.NATIVE)
        )
            .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
            .setVideoOptions(videoOptions)
            .build()

        NativeAdLoader.load(
            adRequest,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdClicked() {
                            Log.i("SOT_ADS_TAG", "Native FullScreen: $adName clicked")
                        }
                    }

                    mainHandler.post {
                        if (mContext.isFinishing || mContext.isDestroyed) {
                            nativeAd.destroy()
                            return@post
                        }

                        nativeAdCache[adName] = nativeAd
                        adLoadingState[adName] = false
                        Log.i("SOT_ADS_TAG", "Native: Admob Next-Gen: $adName loaded successfully.")

                        onAdLoaded?.invoke()
                        if (populateView) {
                            showCachedAd(adName, adContainer)
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mainHandler.post {
                        nativeAdCache[adName] = null
                        adLoadingState[adName] = false
                        Log.e("SOT_ADS_TAG", "Native: Admob Next-Gen: $adName failed: ${adError.message}")
                        onAdFailed?.invoke()
                    }
                }
            }
        )
    }

    private fun showCachedAd(adName: String, adContainer: CardView?) {
        val cachedAd = nativeAdCache[adName]
        val adView = adContainer?.findViewById<NativeAdView>(R.id.nativeAdViewAdmob)

        if (cachedAd != null && adView != null) {
            adContainer.visibility = View.VISIBLE
            adView.visibility = View.VISIBLE
            populateNativeAd(cachedAd, adView)
        } else {
            Log.e("SOT_ADS_TAG", "Native: Admob: Cannot show $adName. Ad or View is null.")
        }
    }

    private fun populateNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.adHeadline)
        adView.bodyView = adView.findViewById(R.id.adBody)
        adView.callToActionView = adView.findViewById(R.id.adCallToAction)
        adView.iconView = adView.findViewById(R.id.adAppIcon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline ?: ""

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        }

        val iconCard = adView.findViewById<CardView>(R.id.adIconCard)
        if (nativeAd.icon == null) {
            iconCard?.visibility = View.GONE
        } else {
            iconCard?.visibility = View.VISIBLE
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon!!.drawable)
        }

        val mediaView = configureMediaView(nativeAd, adView)
        if (mediaView != null && (mediaView.width == 0 || mediaView.height == 0)) {
            mediaView.post {
                adView.registerNativeAd(nativeAd, mediaView)
            }
        } else {
            adView.registerNativeAd(nativeAd, mediaView)
        }
    }

    private fun configureMediaView(nativeAd: NativeAd, adView: NativeAdView): MediaView? {
        val mediaView = adView.findViewById<View>(R.id.adMedia) as? MediaView
        if (mediaView != null) {
            mediaView.visibility = View.VISIBLE
            mediaView.mediaContent = nativeAd.mediaContent
        }
        return mediaView
    }

    fun clearAdCache() {
        try {
            nativeAdCache.values.forEach { it?.destroy() }
            nativeAdCache.clear()
            adLoadingState.clear()
            Log.i("SOT_ADS_TAG", "Native Ad Cache: All ads destroyed and maps cleared.")
        } catch (e: Exception) {
            Log.e("SOT_ADS_TAG", "Error clearing ad cache: ${e.message}")
        }
    }
}
