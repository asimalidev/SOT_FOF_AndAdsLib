package com.manual.mediation.library.sotadlib.adMobAdClasses

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.ExperimentalApi
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdOptions
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPosition
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPresentationScope
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdRequest
import com.manual.mediation.library.sotadlib.BuildConfig
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import java.lang.ref.WeakReference

@OptIn(ExperimentalApi::class)
class PictureInPictureAdClass {

    private var pictureInPictureAd: PictureInPictureAd? = null
    private var isAdLoading: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())

    val isAdAvailable: Boolean
        get() = pictureInPictureAd != null

    /**
     * Loads and immediately shows a Picture-in-Picture ad when ready.
     */
    fun loadAndShowPipAd(
        activity: Activity,
        adId: String,
        screenName: String = "",
        isPurchased: Boolean = false,
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((LoadAdError) -> Unit)? = null,
        onAdDismissed: (() -> Unit)? = null
    ) {
        if (isPurchased || !NetworkCheck.isNetworkAvailable(activity)) {
            Log.d(TAG, "[$screenName] Skipping PiP Ad (isPurchased=$isPurchased or no network).")
            return
        }

        loadPipAd(
            activity = activity,
            adId = adId,
            screenName = screenName,
            isPurchased = isPurchased,
            onAdLoaded = {
                onAdLoaded?.invoke()
                showPipAd(
                    activity = activity,
                    screenName = screenName,
                    scope = scope,
                    position = position,
                    onAdDismissed = onAdDismissed
                )
            },
            onAdFailed = onAdFailed
        )
    }

    /**
     * Loads a Picture-in-Picture ad without immediately showing it.
     */
    fun loadPipAd(
        activity: Activity,
        adId: String,
        screenName: String = "",
        isPurchased: Boolean = false,
        onAdLoaded: (() -> Unit)? = null,
        onAdFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (isPurchased || !NetworkCheck.isNetworkAvailable(activity)) {
            return
        }

        if (isAdLoading || pictureInPictureAd != null) {
            Log.d(TAG, "[$screenName] PiP Ad is already loading or loaded.")
            if (pictureInPictureAd != null) {
                mainHandler.post { onAdLoaded?.invoke() }
            }
            return
        }

        isAdLoading = true
        Log.d(TAG, "[$screenName] Requesting PiP Ad with ID: $adId")

        val adRequest = PictureInPictureAdRequest.Builder(adId).build()
        PictureInPictureAd.load(
            adRequest,
            object : AdLoadCallback<PictureInPictureAd> {
                override fun onAdLoaded(ad: PictureInPictureAd) {
                    isAdLoading = false
                    pictureInPictureAd = ad
                    Log.d(TAG, "[$screenName] PictureInPictureAd loaded successfully.")
                    mainHandler.post {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            onAdLoaded?.invoke()
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoading = false
                    pictureInPictureAd = null
                    Log.e(TAG, "[$screenName] PictureInPictureAd failed to load: ${adError.message}")
                    mainHandler.post {
                        onAdFailed?.invoke(adError)
                    }
                }
            }
        )
    }

    /**
     * Displays the loaded Picture-in-Picture ad on the UI thread.
     */
    fun showPipAd(
        activity: Activity,
        screenName: String = "",
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        onAdDismissed: (() -> Unit)? = null
    ) {
        val ad = pictureInPictureAd
        if (ad == null) {
            Log.w(TAG, "[$screenName] PictureInPictureAd was not ready to be shown.")
            return
        }

        mainHandler.post {
            if (activity.isFinishing || activity.isDestroyed) return@post

            ad.adEventCallback = object : PictureInPictureAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "[$screenName] PiP Ad showed content.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "[$screenName] PiP Ad dismissed.")
                    pictureInPictureAd = null
                    mainHandler.post { onAdDismissed?.invoke() }
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    Log.e(TAG, "[$screenName] PiP Ad failed to show: ${error.message}")
                    pictureInPictureAd = null
                }

                override fun onAdImpression() {
                    Log.d(TAG, "[$screenName] PiP Ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "[$screenName] PiP Ad clicked.")
                }

                override fun onAdPaid(value: AdValue) {
                    Log.d(
                        TAG,
                        "[$screenName] PiP Ad paid: ${value.valueMicros} ${value.currencyCode}"
                    )
                }
            }

            val options = PictureInPictureAdOptions.Builder()
                .setPresentationScope(scope)
                .setPosition(position)
                .build()
            ad.show(activity, options)
        }
    }

    /**
     * Hides the currently displayed Picture-in-Picture ad.
     */
    fun hidePipAd() {
        mainHandler.post {
            try {
                pictureInPictureAd?.hide()
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding PictureInPictureAd", e)
            }
        }
    }

    /**
     * Destroys the Picture-in-Picture ad and releases resources.
     */
    fun destroyPipAd() {
        mainHandler.post {
            try {
                pictureInPictureAd?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying PictureInPictureAd", e)
            } finally {
                pictureInPictureAd = null
                isAdLoading = false
            }
        }
    }

    companion object {
        private const val TAG = "SOT_ADS_TAG"
        const val TEST_PIP_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

        private data class PendingShowRequest(
            val activityRef: WeakReference<Activity>,
            val position: PictureInPictureAdPosition,
            val scope: PictureInPictureAdPresentationScope,
            val onAdLoaded: (() -> Unit)?,
            val onAdFailed: (() -> Unit)?,
            val onAdDismissed: (() -> Unit)?
        )

        private val pipAdCache = HashMap<String, PictureInPictureAd?>()
        private val adLoadingState = HashMap<String, Boolean>()
        private val pendingShowRequests = HashMap<String, PendingShowRequest?>()
        private var activeDisplayedAdName: String? = null
        private var activeDisplayedAd: PictureInPictureAd? = null
        private val staticHandler = Handler(Looper.getMainLooper())

        /**
         * Checks whether a PiP Ad slot is enabled from Remote Config:
         * Supports Boolean (`true`/`false`) or String (`"TRUE"`, `"ON"`, `"TOP_LEFT"`, `"TOP_RIGHT"`, `"BOTTOM_LEFT"`, `"BOTTOM_RIGHT"`, `"OFF"`, `"FALSE"`).
         */
        @JvmStatic
        fun isEnabled(value: Any?, isPurchased: Any? = false): Boolean {
            val purchased = (isPurchased as? Boolean) ?: (isPurchased?.toString()?.toBoolean() ?: false)
            if (purchased) return false
            if (value == null) return false
            return when (value) {
                is Boolean -> value
                is String -> !isOffString(value)
                is Number -> value.toInt() != 0
                else -> false
            }
        }

        private fun isOffString(raw: String): Boolean {
            return when (raw.trim().uppercase()) {
                "OFF", "FALSE", "0", "NONE", "EMPTY", "" -> true
                else -> false
            }
        }

        /**
         * Resolves [PictureInPictureAdPosition] from Remote Config position parameter:
         * - `"TOP_LEFT"` -> [PictureInPictureAdPosition.TOP_LEFT]
         * - `"TOP_RIGHT"` -> [PictureInPictureAdPosition.TOP_RIGHT]
         * - `"BOTTOM_LEFT"` -> [PictureInPictureAdPosition.BOTTOM_LEFT]
         * - `"BOTTOM_RIGHT"` -> [PictureInPictureAdPosition.BOTTOM_RIGHT]
         */
        @JvmStatic
        fun resolvePosition(
            positionValue: Any?,
            fallbackValue: Any? = null
        ): PictureInPictureAdPosition {
            if (positionValue is PictureInPictureAdPosition) return positionValue
            parsePositionString(positionValue)?.let { return it }
            if (fallbackValue is PictureInPictureAdPosition) return fallbackValue
            parsePositionString(fallbackValue)?.let { return it }
            return PictureInPictureAdPosition.BOTTOM_RIGHT
        }

        private fun parsePositionString(raw: Any?): PictureInPictureAdPosition? {
            val str = (raw as? String)?.trim()?.uppercase()?.replace("-", "_")?.replace(" ", "_")
                ?: return null
            return when (str) {
                "TOP_LEFT", "TOPLEFT", "TL" -> PictureInPictureAdPosition.TOP_LEFT
                "TOP_RIGHT", "TOPRIGHT", "TR" -> PictureInPictureAdPosition.TOP_RIGHT
                "BOTTOM_LEFT", "BOTTOMLEFT", "BL" -> PictureInPictureAdPosition.BOTTOM_LEFT
                "BOTTOM_RIGHT", "BOTTOMRIGHT", "BR" -> PictureInPictureAdPosition.BOTTOM_RIGHT
                else -> null
            }
        }

        /**
         * Requests or shows a cached Picture-in-Picture ad (handled just like [AdmobNativeAdManager.requestAd]).
         * - When [populateView] is `false`: preloads the PiP ad into cache under [adName].
         * - When [populateView] is `true`: shows the cached PiP ad at [pipPosition] (or loads and shows it).
         */
        @JvmStatic
        @JvmOverloads
        fun requestAd(
            mContext: Activity?,
            adId: String,
            adName: String = "",
            remoteConfig: Any? = true,
            pipPosition: Any? = null,
            isPurchased: Any? = false,
            populateView: Boolean = false,
            requestAgain: Boolean = true,
            onAdFailed: (() -> Unit)? = null,
            onAdLoaded: (() -> Unit)? = null,
            onAdDismissed: (() -> Unit)? = null
        ) {
            val scope = PictureInPictureAdPresentationScope.SCREEN
            if (mContext == null || mContext.isFinishing || mContext.isDestroyed) {
                Log.i(TAG, "PiP : Context is null/destroyed; cannot load ad ($adName).")
                onAdFailed?.invoke()
                return
            }

            val isAdEnabled = isEnabled(value = remoteConfig, isPurchased = isPurchased)
            val resolvedPosition = resolvePosition(positionValue = pipPosition, fallbackValue = remoteConfig)

            if (!NetworkCheck.isNetworkAvailable(mContext) || !isAdEnabled) {
                if (populateView) {
                    destroyAd(adName)
                }
                Log.i(TAG, "PiP : Admob : Disabled or offline ($adName, enabled=$isAdEnabled)")
                onAdFailed?.invoke()
                return
            }

            val effectiveAdId = if (adId.isNotEmpty()) {
                adId
            } else if (BuildConfig.DEBUG) {
                TEST_PIP_AD_UNIT_ID
            } else {
                ""
            }

            if (effectiveAdId.isEmpty()) {
                Log.w(TAG, "PiP : Admob : Ad ID is empty for $adName")
                onAdFailed?.invoke()
                return
            }

            // 1. If already cached, show immediately when populateView=true
            val cachedAd = pipAdCache[adName]
            if (cachedAd != null) {
                Log.i(TAG, "Admob: PiP : $adName : cachedAd ready (populateView=$populateView, position=$resolvedPosition)")
                if (populateView) {
                    showCachedAd(
                        activity = mContext,
                        adName = adName,
                        position = resolvedPosition,
                        scope = scope,
                        onAdDismissed = onAdDismissed
                    )
                    onAdLoaded?.invoke()
                }
                return
            }

            // 2. If currently loading in-flight (e.g. preloaded on previous screen), attach pending show request
            if (adLoadingState[adName] == true) {
                Log.i(TAG, "Admob: PiP : $adName : load in flight (populateView=$populateView, position=$resolvedPosition)")
                if (populateView) {
                    pendingShowRequests[adName] = PendingShowRequest(
                        activityRef = WeakReference(mContext),
                        position = resolvedPosition,
                        scope = scope,
                        onAdLoaded = onAdLoaded,
                        onAdFailed = onAdFailed,
                        onAdDismissed = onAdDismissed
                    )
                }
                return
            }

            if (populateView && !requestAgain) {
                onAdFailed?.invoke()
                return
            }

            // 3. Start loading PiP ad
            adLoadingState[adName] = true
            if (populateView) {
                pendingShowRequests[adName] = PendingShowRequest(
                    activityRef = WeakReference(mContext),
                    position = resolvedPosition,
                    scope = scope,
                    onAdLoaded = onAdLoaded,
                    onAdFailed = onAdFailed,
                    onAdDismissed = onAdDismissed
                )
            }

            Log.i(TAG, "Admob Next-Gen: PiP : $adName : Requesting with ID=$effectiveAdId, position=$resolvedPosition")
            val adRequest = PictureInPictureAdRequest.Builder(effectiveAdId).build()
            PictureInPictureAd.load(
                adRequest,
                object : AdLoadCallback<PictureInPictureAd> {
                    override fun onAdLoaded(ad: PictureInPictureAd) {
                        Log.i(TAG, "Admob Next-Gen: PiP : $adName : onAdLoaded()")
                        staticHandler.post {
                            pipAdCache[adName] = ad
                            adLoadingState[adName] = false

                            val pending = pendingShowRequests.remove(adName)
                            if (pending != null) {
                                val targetActivity = pending.activityRef.get()
                                if (targetActivity != null && !targetActivity.isFinishing && !targetActivity.isDestroyed) {
                                    showCachedAd(
                                        activity = targetActivity,
                                        adName = adName,
                                        position = pending.position,
                                        scope = pending.scope,
                                        onAdDismissed = pending.onAdDismissed
                                    )
                                    pending.onAdLoaded?.invoke()
                                } else {
                                    pending.onAdFailed?.invoke()
                                }
                            } else {
                                if (!mContext.isFinishing && !mContext.isDestroyed && BuildConfig.DEBUG) {
                                    Toast.makeText(
                                        mContext,
                                        "Admob: PiP : Loaded()\n$adName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                onAdLoaded?.invoke()
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(
                            TAG,
                            "Admob Next-Gen: PiP : $adName : onAdFailedToLoad() ${adError.message}"
                        )
                        staticHandler.post {
                            pipAdCache[adName] = null
                            adLoadingState[adName] = false
                            val pending = pendingShowRequests.remove(adName)
                            if (pending != null) {
                                pending.onAdFailed?.invoke()
                            } else {
                                onAdFailed?.invoke()
                            }
                            if (!mContext.isFinishing && !mContext.isDestroyed && BuildConfig.DEBUG) {
                                Toast.makeText(
                                    mContext,
                                    "Admob: PiP : onAdFailedToLoad() $adName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            )
        }

        private fun showCachedAd(
            activity: Activity,
            adName: String,
            position: PictureInPictureAdPosition,
            scope: PictureInPictureAdPresentationScope,
            onAdDismissed: (() -> Unit)?
        ) {
            if (activity.isFinishing || activity.isDestroyed) return
            val ad = pipAdCache.remove(adName) ?: return

            // Destroy any previously active PiP ad before showing the new one
            if (activeDisplayedAd != null && activeDisplayedAd !== ad) {
                try {
                    activeDisplayedAd?.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error destroying previous active PiP ad", e)
                }
                activeDisplayedAd = null
                activeDisplayedAdName = null
            }

            activeDisplayedAd = ad
            activeDisplayedAdName = adName

            ad.adEventCallback = object : PictureInPictureAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.i(TAG, "Admob: PiP : $adName : onAdShowedFullScreenContent(position=$position)")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.i(TAG, "Admob: PiP : $adName : onAdDismissedFullScreenContent()")
                    if (activeDisplayedAd === ad) {
                        activeDisplayedAd = null
                        activeDisplayedAdName = null
                    }
                    staticHandler.post { onAdDismissed?.invoke() }
                }

                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    Log.e(TAG, "Admob: PiP : $adName : onAdFailedToShowFullScreenContent(${error.message})")
                    if (activeDisplayedAd === ad) {
                        activeDisplayedAd = null
                        activeDisplayedAdName = null
                    }
                }

                override fun onAdImpression() {
                    Log.i(TAG, "Admob: PiP : $adName : onAdImpression()")
                }

                override fun onAdClicked() {
                    Log.i(TAG, "Admob: PiP : $adName : onAdClicked()")
                }

                override fun onAdPaid(value: AdValue) {
                    Log.d(
                        TAG,
                        "Admob: PiP : $adName : onAdPaid(${value.valueMicros} ${value.currencyCode})"
                    )
                }
            }

            val options = PictureInPictureAdOptions.Builder()
                .setPresentationScope(scope)
                .setPosition(position)
                .build()
            ad.show(activity, options)
        }

        @JvmStatic
        fun destroyAd(adName: String) {
            staticHandler.post {
                pendingShowRequests.remove(adName)
                if (activeDisplayedAdName == adName) {
                    try {
                        activeDisplayedAd?.destroy()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error destroying active PiP ad ($adName): ${e.message}")
                    } finally {
                        activeDisplayedAd = null
                        activeDisplayedAdName = null
                    }
                }
            }
        }

        @JvmStatic
        fun destroyActiveAd() {
            staticHandler.post {
                try {
                    activeDisplayedAd?.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error destroying active PiP ad: ${e.message}")
                } finally {
                    activeDisplayedAd = null
                    activeDisplayedAdName = null
                }
            }
        }

        @JvmStatic
        fun clearAdCache() {
            staticHandler.post {
                try {
                    pendingShowRequests.clear()
                    activeDisplayedAd?.destroy()
                    activeDisplayedAd = null
                    activeDisplayedAdName = null
                    pipAdCache.values.forEach { it?.destroy() }
                    pipAdCache.clear()
                    adLoadingState.clear()
                    Log.i(TAG, "PiP Ad Cache: All ads destroyed and maps cleared.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing PiP ad cache: ${e.message}")
                }
            }
        }
    }
}
