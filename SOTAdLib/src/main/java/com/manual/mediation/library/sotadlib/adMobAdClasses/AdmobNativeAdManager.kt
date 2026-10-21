package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Guideline
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoController
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.manual.mediation.library.sotadlib.BuildConfig
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.utils.NetworkCheck

/**
 * Native Ad Size configurable from Remote Config:
 * - "OFF" / false -> Do not load or show native ad
 * - "SMALL" -> Compact banner-style Native Ad (~70-80dp, R.layout.admob_native_simple_small)
 * - "MEDIUM" -> Medium Native Ad (R.layout.admob_native_mediaview_medium or R.layout.admob_native_simple_large)
 * - "MEDIA_LEFT" -> MediaView on left + content on right (R.layout.native_admob_media_left_side)
 * - "LARGE" / true -> Full Large MediaView Native Ad (R.layout.admob_native_mediaview_large)
 */
enum class NativeAdSize {
    OFF,
    SMALL,
    MEDIUM,
    MEDIA_LEFT,
    LARGE;

    companion object {
        @JvmStatic
        fun fromRemoteConfig(
            value: Any?,
            sizeOverride: Any? = null,
            isMedia: Boolean = true,
            isMediumAd: Boolean = true
        ): NativeAdSize {
            // If an explicit sizeOverride is provided ("SMALL", "MEDIUM", "LARGE", "MEDIA_LEFT"), resolve it first
            if (sizeOverride != null) {
                val parsedOverride = parseSizeString(sizeOverride)
                if (parsedOverride != null) {
                    // Respect boolean false in value if value explicitly disables the ad
                    if (value is Boolean && !value) return OFF
                    if (value is String && isOffString(value)) return OFF
                    return parsedOverride
                }
            }

            return when (value) {
                is NativeAdSize -> value
                is Boolean -> {
                    if (!value) {
                        OFF
                    } else {
                        fromLegacyFlags(isMedia = isMedia, isMediumAd = isMediumAd)
                    }
                }
                is String -> {
                    if (isOffString(value)) {
                        OFF
                    } else {
                        parseSizeString(value)
                            ?: fromLegacyFlags(isMedia = isMedia, isMediumAd = isMediumAd)
                    }
                }
                else -> fromLegacyFlags(isMedia = isMedia, isMediumAd = isMediumAd)
            }
        }

        @JvmStatic
        fun isEnabled(value: Any?, isPurchased: Any? = false): Boolean {
            val purchased = (isPurchased as? Boolean) ?: (isPurchased?.toString()?.toBoolean() ?: false)
            if (purchased) return false
            if (value == null) return false
            return fromRemoteConfig(value) != OFF
        }

        private fun isOffString(raw: String): Boolean {
            return when (raw.trim().uppercase()) {
                "OFF", "FALSE", "0", "NONE", "EMPTY", "" -> true
                else -> false
            }
        }

        private fun parseSizeString(raw: Any?): NativeAdSize? {
            if (raw is NativeAdSize) return raw
            val str = (raw as? String)?.trim()?.uppercase() ?: return null
            return when (str) {
                "OFF", "FALSE", "0", "NONE" -> OFF
                "SMALL", "BANNER_SMALL", "SIMPLE_SMALL", "COMPACT" -> SMALL
                "MEDIUM", "SIMPLE_LARGE", "MEDIA_MEDIUM" -> MEDIUM
                "MEDIA_LEFT", "LEFT", "MEDIA_LEFT_SIDE" -> MEDIA_LEFT
                "LARGE", "MEDIA_LARGE", "FULL", "TRUE", "1" -> LARGE
                else -> null
            }
        }

        private fun fromLegacyFlags(isMedia: Boolean, isMediumAd: Boolean): NativeAdSize {
            return when {
                isMedia && isMediumAd -> LARGE
                isMedia -> MEDIUM
                isMediumAd -> MEDIUM
                else -> SMALL
            }
        }
    }
}

object AdmobNativeAdManager {
    private val nativeAdCache = HashMap<String, NativeAd?>()
    private val adLoadingState = HashMap<String, Boolean>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun requestAd(
        mContext: Activity?,
        adId: String,
        adName: String = "",
        isMedia: Boolean = true,
        isMediumAd: Boolean = false,
        remoteConfig: Any? = true,
        nativeSize: Any? = null,
        populateView: Boolean = false,
        requestAgain: Boolean = true,
        adContainer: CardView? = null,
        onAdFailed: (() -> Unit)? = null,
        onAdLoaded: (() -> Unit)? = null
    ) {
        if (mContext == null || mContext.isFinishing || mContext.isDestroyed) {
            Log.i("SOT_ADS_TAG", "Context is null/destroyed; cannot load ad.")
            onAdFailed?.invoke()
            return
        }

        val resolvedSize = NativeAdSize.fromRemoteConfig(
            value = remoteConfig,
            sizeOverride = nativeSize,
            isMedia = isMedia,
            isMediumAd = isMediumAd
        )
        val isAdEnabled = resolvedSize != NativeAdSize.OFF

        if (populateView) {
            if (!NetworkCheck.isNetworkAvailable(mContext) || !isAdEnabled) {
                adContainer?.visibility = View.GONE
                Log.i("SOT_ADS_TAG", "Native : Admob : View is gone ($adName, size=$resolvedSize)")
                onAdFailed?.invoke()
                return
            } else {
                adContainer?.visibility = View.VISIBLE
                updateContainerShimmer(mContext, adContainer, resolvedSize, isMedia)
                Log.i("SOT_ADS_TAG", "Native : Admob : View is VISIBLE ($adName, size=$resolvedSize)")
            }
        } else {
            if (!isAdEnabled) {
                onAdFailed?.invoke()
                return
            }
            Log.i("SOT_ADS_TAG", "Native : Admob : canPopulateView ($adName, size=$resolvedSize)")
        }

        if (adLoadingState[adName] == true && nativeAdCache[adName] != null) {
            Log.i("SOT_ADS_TAG", "Admob: Native : $adName : showCachedAd(size=$resolvedSize)")
            if (populateView) {
                showCachedAd(adName, resolvedSize, isMedia, adContainer, isMediumAd)
                onAdLoaded?.invoke()
            }
            return
        } else {
            if (!requestAgain) {
                nativeAdCache[adName] = null
                adLoadingState[adName] = false
                onAdFailed?.invoke()
                return
            }
        }

        if (adId.isEmpty() || !NetworkCheck.isNetworkAvailable(mContext)) {
            onAdFailed?.invoke()
            return
        }

        adLoadingState[adName] = true

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
                    Log.i("SOT_ADS_TAG", "Admob Next-Gen: Native : $adName : onNativeAdLoaded()")

                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdClicked() {
                            Log.i("SOT_ADS_TAG", "Admob: Native : $adName : onAdClicked()")
                        }

                        override fun onAdImpression() {
                            Log.i("SOT_ADS_TAG", "Admob: Native : $adName : onAdImpression()")
                        }
                    }

                    mainHandler.post {
                        if (mContext.isFinishing || mContext.isDestroyed) {
                            nativeAd.destroy()
                            return@post
                        }

                        nativeAdCache[adName] = nativeAd
                        adLoadingState[adName] = true

                        if (populateView) {
                            showCachedAd(adName, resolvedSize, isMedia, adContainer, isMediumAd)
                        } else if (BuildConfig.DEBUG) {
                            Toast.makeText(
                                mContext,
                                "Admob: Native : Loaded()\n$adName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        onAdLoaded?.invoke()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i(
                        "SOT_ADS_TAG",
                        "Admob Next-Gen: Native : $adName : onAdFailedToLoad()\n${adError.message}"
                    )
                    mainHandler.post {
                        nativeAdCache[adName] = null
                        adLoadingState[adName] = false
                        onAdFailed?.invoke()
                        if (!mContext.isFinishing && !mContext.isDestroyed && BuildConfig.DEBUG) {
                            Toast.makeText(
                                mContext,
                                "Admob: Native : onAdFailedToLoad() $adName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )
    }

    private fun updateContainerShimmer(
        context: Activity,
        adContainer: CardView?,
        size: NativeAdSize,
        isMedia: Boolean
    ) {
        val shimmerFrame = adContainer?.findViewById<ShimmerFrameLayout>(R.id.shimmerLayout) ?: return
        val shimmerLayoutRes = when (size) {
            NativeAdSize.LARGE -> R.layout.admob_native_media_view_shimmer
            NativeAdSize.MEDIA_LEFT -> R.layout.native_admob_media_left_side_shimmer
            NativeAdSize.MEDIUM -> if (isMedia) R.layout.admob_native_media_view_shimmer else R.layout.admob_native_medium_shimmer
            NativeAdSize.SMALL, NativeAdSize.OFF -> R.layout.admob_native_small_shimmer
        }
        shimmerFrame.removeAllViews()
        LayoutInflater.from(context).inflate(shimmerLayoutRes, shimmerFrame, true)
        shimmerFrame.visibility = View.VISIBLE
        shimmerFrame.startShimmer()
    }

    private fun resolveNativeLayoutRes(
        size: NativeAdSize,
        isMedia: Boolean,
        hasVideoContent: Boolean
    ): Pair<Int, Boolean> {
        // If AdMob returns a video creative for a non-media slot, upgrade to MEDIA_LEFT so MediaView >= 120x120dp
        if (hasVideoContent && (size == NativeAdSize.SMALL || (size == NativeAdSize.MEDIUM && !isMedia))) {
            return R.layout.native_admob_media_left_side to true
        }

        return when (size) {
            NativeAdSize.LARGE -> R.layout.admob_native_mediaview_large to true
            NativeAdSize.MEDIA_LEFT -> R.layout.native_admob_media_left_side to true
            NativeAdSize.MEDIUM -> {
                if (isMedia) {
                    R.layout.admob_native_mediaview_medium to true
                } else {
                    R.layout.admob_native_simple_large to false
                }
            }
            NativeAdSize.SMALL, NativeAdSize.OFF -> R.layout.admob_native_simple_small to false
        }
    }

    private fun showCachedAd(
        adName: String,
        size: NativeAdSize,
        isMedia: Boolean,
        adContainer: CardView?,
        isMediumAd: Boolean
    ) {
        val container = adContainer ?: return
        val context = container.context ?: return
        val cachedAd = nativeAdCache[adName]
        if (cachedAd == null) {
            Log.i("SOT_ADS_TAG", "Ad is not available in cache for adName: $adName")
            return
        }

        val (layoutRes, effectiveHasMediaView) = resolveNativeLayoutRes(
            size = size,
            isMedia = isMedia,
            hasVideoContent = cachedAd.mediaContent.hasVideoContent
        )

        val adView = LayoutInflater.from(context).inflate(layoutRes, container, false) as? NativeAdView
            ?: return

        // Stop shimmer if present inside the CardView container
        container.findViewById<ShimmerFrameLayout>(R.id.shimmerLayout)?.let { shimmer ->
            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
        }

        // Remove any previously added NativeAdView before attaching the new one
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child is NativeAdView) {
                container.removeViewAt(i)
            }
        }

        // Attach to container FIRST so NativeAdView & MediaView have valid parent LayoutParams
        container.visibility = View.VISIBLE
        container.addView(adView)

        populateNativeAd(
            isMediumAd = isMediumAd || size == NativeAdSize.LARGE || size == NativeAdSize.MEDIUM,
            nativeAd = cachedAd,
            adView = adView,
            hasMediaView = effectiveHasMediaView
        )
    }

    private fun populateNativeAd(
        isMediumAd: Boolean,
        nativeAd: NativeAd,
        adView: NativeAdView,
        hasMediaView: Boolean
    ) {
        adView.headlineView = adView.findViewById(R.id.adHeadline)
        adView.bodyView = adView.findViewById(R.id.adBody)
        adView.callToActionView = adView.findViewById(R.id.adCallToAction)
        adView.iconView = adView.findViewById(R.id.adAppIcon)

        (adView.headlineView as? TextView)?.text = nativeAd.headline ?: ""

        // Configure Body
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as? TextView)?.text = nativeAd.body
        }

        // Configure Call to Action
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as? TextView)?.text = nativeAd.callToAction
        }

        // Configure Icon
        if (nativeAd.icon == null) {
            handleIconVisibility(isMediumAd, adView, false)
        } else {
            handleIconVisibility(isMediumAd, adView, true)
            (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon!!.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        val mediaView: MediaView? = if (hasMediaView) {
            configureMediaView(nativeAd, adView)
        } else {
            null
        }

        // GMA Next-Gen SDK: registerNativeAd(nativeAd, mediaView) after MediaView is laid out
        adView.visibility = View.VISIBLE
        if (mediaView != null && (mediaView.width == 0 || mediaView.height == 0)) {
            mediaView.post {
                adView.registerNativeAd(nativeAd, mediaView)
            }
        } else {
            adView.registerNativeAd(nativeAd, mediaView)
        }
    }

    private fun handleIconVisibility(isMediumAd: Boolean, adView: NativeAdView, isVisible: Boolean) {
        val guidelinePercent = if (isVisible) if (isMediumAd) 0.17f else 0.15f else 0f

        adView.findViewById<Guideline>(R.id.glNativeAdmobMedium1)?.setGuidelinePercent(guidelinePercent)
        adView.findViewById<Guideline>(R.id.glNativeAdmobSmall1)?.setGuidelinePercent(guidelinePercent)
        adView.findViewById<Guideline>(R.id.glNativeAdmobBannerNormal1)?.setGuidelinePercent(guidelinePercent)

        adView.findViewById<CardView>(R.id.adIconCard)?.visibility =
            if (isVisible) View.VISIBLE else View.GONE
    }

    private fun configureMediaView(nativeAd: NativeAd, adView: NativeAdView): MediaView? {
        val mediaView = adView.findViewById<View>(R.id.adMedia) as? MediaView ?: return null
        mediaView.mediaContent = nativeAd.mediaContent

        mediaView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                if (child is ImageView) {
                    child.scaleType = ImageView.ScaleType.FIT_XY
                }
            }

            override fun onChildViewRemoved(view: View, view1: View) {}
        })

        val mediaContent = nativeAd.mediaContent
        val videoController = mediaContent.videoController
        if (mediaContent.hasVideoContent && videoController != null) {
            videoController.videoLifecycleCallbacks =
                object : VideoController.VideoLifecycleCallbacks {}
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
