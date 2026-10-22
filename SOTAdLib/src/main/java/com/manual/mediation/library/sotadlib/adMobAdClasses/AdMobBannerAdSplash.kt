package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.manual.mediation.library.sotadlib.BuildConfig
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.utils.NetworkCheck

/**
 * Splash Banner Type configurable from Remote Config:
 * - "OFF" / false -> Do not show banner
 * - "SMALL" / "ADAPTIVE" / true -> Standard Anchored Adaptive Banner (~50-60dp)
 * - "LARGE" / "LARGE_BANNER" -> Large Anchored Adaptive Banner (~90-100dp)
 * - "COLLAPSIBLE" / "COLLAPSIBLE_BOTTOM" -> Collapsible Bottom Banner
 * - "COLLAPSIBLE_TOP" -> Collapsible Top Banner
 */
enum class SplashBannerType {
    OFF,
    SMALL,
    LARGE,
    COLLAPSIBLE_BOTTOM,
    COLLAPSIBLE_TOP;

    companion object {
        @JvmStatic
        fun fromRemoteConfig(
            value: Any?,
            defaultType: SplashBannerType = SMALL
        ): SplashBannerType {
            return when (value) {
                is SplashBannerType -> value
                is Boolean -> if (value) defaultType else OFF
                is String -> {
                    when (value.trim().uppercase()) {
                        "OFF", "FALSE", "0", "NONE", "EMPTY", "" -> OFF
                        "SMALL", "ADAPTIVE", "ANCHORED_ADAPTIVE", "STANDARD", "TRUE", "1" -> SMALL
                        "LARGE", "LARGE_BANNER", "LARGE_ADAPTIVE" -> LARGE
                        "COLLAPSIBLE", "COLLAPSIBLE_BOTTOM", "BOTTOM" -> COLLAPSIBLE_BOTTOM
                        "COLLAPSIBLE_TOP", "TOP" -> COLLAPSIBLE_TOP
                        else -> defaultType
                    }
                }
                else -> defaultType
            }
        }

        @JvmStatic
        fun fromRemoteValue(
            value: Any?,
            defaultType: SplashBannerType = SMALL
        ): SplashBannerType = fromRemoteConfig(value, defaultType)

        @JvmStatic
        fun isEnabled(value: Any?): Boolean {
            return fromRemoteConfig(value) != OFF
        }
    }
}

class AdMobBannerAdSplash(
    activity: Activity? = null,
    private val placementID: String,
    private val bannerContainer: FrameLayout,
    private val shimmerContainer: View,
    private val onAdFailed: (() -> Unit)? = null,
    private val onAdLoaded: (() -> Unit)? = null,
    val onAdImpression: (() -> Unit)? = null,
    private val onAdClicked: (() -> Unit)? = null,
    private val bannerType: Any? = SplashBannerType.SMALL
) {
    private var adView: AdView? = null
    private var currentActivity: Activity? = activity
    var isBannerLoaded = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val resolvedBannerType: SplashBannerType = SplashBannerType.fromRemoteConfig(bannerType)

    init {
        currentActivity?.let { act ->
            if (resolvedBannerType == SplashBannerType.OFF) {
                Log.i("SOT_ADS_TAG", "AdMob: BannerAd : OFF via RemoteConfig")
                bannerContainer.visibility = View.GONE
                shimmerContainer.visibility = View.GONE
                onAdFailed?.invoke()
                return@let
            }

            if (NetworkCheck.isNetworkAvailable(act)) {
                loadBannerAd(onAdFailed, onAdLoaded, onAdClicked)
            } else {
                Log.i("SOT_ADS_TAG", "AdMob: BannerAd : No Network Available")
                onAdFailed?.invoke()
            }
        }
    }

    private fun loadBannerAd(
        onAdFailed: (() -> Unit)? = null,
        onAdLoaded: (() -> Unit)? = null,
        onAdClicked: (() -> Unit)? = null
    ) {
        if (isBannerLoaded) {
            return
        }

        val activity = currentActivity ?: return

        // Ensure container height wraps content so LARGE / COLLAPSIBLE banners aren't clipped
        bannerContainer.layoutParams = bannerContainer.layoutParams?.apply {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        bannerContainer.visibility = View.VISIBLE
        val adSize = resolveAdSize(activity, resolvedBannerType)
        applySizedShimmer(activity, adSize)
        shimmerContainer.visibility = View.VISIBLE
        val requestBuilder = BannerAdRequest.Builder(placementID, adSize)

        when (resolvedBannerType) {
            SplashBannerType.COLLAPSIBLE_BOTTOM -> {
                val extras = Bundle().apply { putString("collapsible", "bottom") }
                requestBuilder.setGoogleExtrasBundle(extras)
            }
            SplashBannerType.COLLAPSIBLE_TOP -> {
                val extras = Bundle().apply { putString("collapsible", "top") }
                requestBuilder.setGoogleExtrasBundle(extras)
            }
            else -> Unit
        }

        val nextGenAdView = AdView(activity)
        adView = nextGenAdView

        nextGenAdView.loadAd(
            requestBuilder.build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Log.i(
                        "SOT_ADS_TAG",
                        "AdMob Next-Gen: BannerAd ($resolvedBannerType) : onAdLoaded(isCollapsible=${ad.isCollapsible()})"
                    )
                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() {
                            Log.i("SOT_ADS_TAG", "Banner Impression Recorded!")
                            mainHandler.post {
                                onAdImpression?.invoke()
                            }
                        }

                        override fun onAdClicked() {
                            Log.i("SOT_ADS_TAG", "AdMob: BannerAd : onAdClicked()")
                            mainHandler.post {
                                onAdClicked?.invoke()
                            }
                        }
                    }

                    mainHandler.post {
                        shimmerContainer.visibility = View.GONE
                        (nextGenAdView.parent as? ViewGroup)?.removeView(nextGenAdView)
                        bannerContainer.removeAllViews()
                        bannerContainer.addView(nextGenAdView)
                        bannerContainer.visibility = View.VISIBLE
                        isBannerLoaded = true
                        onAdLoaded?.invoke()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i("SOT_ADS_TAG", "AdMob: BannerAd : onAdFailedToLoad: $adError")
                    mainHandler.post {
                        shimmerContainer.visibility = View.GONE
                        bannerContainer.visibility = View.GONE
                        onAdFailed?.invoke()
                        currentActivity?.let {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(
                                    it,
                                    "AdMob Banner Ad Failed to Load",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Makes the shimmer match the banner that is about to load: LARGE gets the large shimmer,
     * every other type gets the small one, and the height follows the resolved ad size.
     */
    private fun applySizedShimmer(activity: Activity, adSize: AdSize) {
        val group = shimmerContainer as? ViewGroup ?: return
        val contentRes = when (resolvedBannerType) {
            SplashBannerType.LARGE -> R.layout.banner_shimmer_content_large
            else -> R.layout.banner_shimmer_content_small
        }
        group.removeAllViews()
        LayoutInflater.from(activity).inflate(contentRes, group, true)
        val heightPx = adSize.getHeightInPixels(activity)
        if (heightPx > 0) {
            group.layoutParams = group.layoutParams?.apply { height = heightPx }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveAdSize(activity: Activity, type: SplashBannerType): AdSize {
        val display: Display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt().coerceAtLeast(320)

        return when (type) {
            SplashBannerType.LARGE ->
                AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, adWidth)
            SplashBannerType.SMALL,
            SplashBannerType.COLLAPSIBLE_BOTTOM,
            SplashBannerType.COLLAPSIBLE_TOP,
            SplashBannerType.OFF ->
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
    }
}
