package com.manual.mediation.library.sotadlib.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdMobInterstitialInside
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdFullScreen
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.data.WalkThroughItem
import com.manual.mediation.library.sotadlib.databinding.FragmentWTThreeBinding
import com.manual.mediation.library.sotadlib.interfaces.CommonEventTracker
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import com.manual.mediation.library.sotadlib.utils.PrefHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WTThreeFragment : Fragment() {
    private var _binding: FragmentWTThreeBinding? = null
    private val binding get() = _binding!!
    private var sotAdsConfigurations: SOTAdsConfigurations? = null
    private lateinit var item: WalkThroughItem
    private var eventTracker: CommonEventTracker? = null
    private var adShown = false
    private var scaleType: Int = 0
    private var blurVisibility: Boolean = false

    companion object {
        private const val ARG_ITEM = "walkThroughItem"

        fun newInstance(
            item: WalkThroughItem,
            tracker: CommonEventTracker? = null
        ): WTThreeFragment {
            return WTThreeFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                }
                eventTracker = tracker
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<WalkThroughItem>(ARG_ITEM)?.let {
            item = it
            scaleType = item.imageScale
            blurVisibility = item.blurVisibility
        } ?: throw IllegalStateException("WalkThroughItem must be provided")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWTThreeBinding.inflate(inflater, container, false)
        binding.blure.visibility = if (blurVisibility) View.VISIBLE else View.GONE
        if (scaleType == 0) {
            binding.main.visibility = View.GONE
            binding.mainCopy.visibility = View.VISIBLE
        } else {
            binding.main.visibility = View.VISIBLE
            binding.mainCopy.visibility = View.GONE
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sotAdsConfigurations = SOTAdsManager.getConfigurations()
        eventTracker?.logEvent(requireActivity(), "walkthrough3_scr")

        Log.i("SOTStartTestActivity", "walkthrough3_scr")
        val interstitialLetsStartEnabled =
            (sotAdsConfigurations?.getRemoteConfigData()?.get("INTERSTITIAL_LETS_START") as? Boolean
                ?: false) &&
                (sotAdsConfigurations?.getRemoteConfigData()
                    ?.get("IS_PURCHASED") as? Boolean == false)

        if (interstitialLetsStartEnabled) {
            loadAdmobWTThreeInterstitial()
        }

        loadImages()
        setupTextViews()
        setupButton()
    }

    private fun loadImages() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                context?.let {
                    val targetImageView = if (scaleType == 0) {
                        binding.main.visibility = View.GONE
                        binding.mainCopy.visibility = View.VISIBLE
                        binding.mainCopy
                    } else {
                        binding.main.visibility = View.VISIBLE
                        binding.mainCopy.visibility = View.GONE
                        binding.main
                    }

                    withContext(Dispatchers.Main) {
                        Glide.with(requireActivity())
                            .load(item.drawableResId)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .skipMemoryCache(true)
                            .into(targetImageView)
                    }

                    Glide.with(it)
                        .load(item.drawableBubbleResId)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .skipMemoryCache(true)
                        .into(binding.bubble)
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupTextViews() {
        context?.let {
            if (item.headingColor != 0) {
                binding.txtHeading.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        item.headingColor
                    )
                )
            }
            if (item.descriptionColor != 0) {
                binding.txtDescription.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        item.descriptionColor
                    )
                )
            }
            if (item.nextColor != 0) {
                binding.btnNext.setTextColor(ContextCompat.getColor(requireContext(), item.nextColor))
            }
            if (item.nextBackground != 0) { binding.btnNext.background = ContextCompat.getDrawable(requireContext(), item.nextBackground) }
            if (item.viewBackgroundColor != 0) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        item.viewBackgroundColor
                    )
                )
            }
        }

        binding.txtHeading.text = item.heading
        binding.txtDescription.text = item.description
    }

    private fun setupButton() {
        binding.btnNext.setOnClickListener {
            eventTracker?.logEvent(requireActivity(), "walkthrough3_scr_tap_start")
            if (sotAdsConfigurations?.getRemoteConfigData()
                    ?.get("INTERSTITIAL_LETS_START") as? Boolean == true &&
                sotAdsConfigurations?.getRemoteConfigData()
                    ?.get("IS_PURCHASED") as? Boolean == false
            ) {
                safeShowAdmobWTThreeInterstitial()
            } else {
                safeLetsStartClick()
            }
            AdmobNativeAdFullScreen.clearAdCache()
            AdmobNativeAdManager.clearAdCache()
        }
    }

    private fun safeShowAdmobWTThreeInterstitial() {
        if (!isAdded || activity == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            AdMobInterstitialInside.showIfAvailableOrLoadAdMobInterstitial(
                context = requireActivity(),
                nameFragment = "WALKTHROUGH_3",
                adId = sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_INTERSTITIAL_LETS_START")
                    ?: "",
                onAdClosedCallBackAdmob = {
                    Log.i("SOT_ADS_TAG", "Interstitial: WALKTHROUGH_3: onAdClosedCallBackAdmob()")
                    if (isAdded) {
                        safeLetsStartClick()
                    }
                },
                onAdShowedCallBackAdmob = {
                    Log.i("SOT_ADS_TAG", "Interstitial: WALKTHROUGH_3: onAdShowedCallBackAdmob()")
                }
            )
        }
    }

    private fun safeLetsStartClick() {
        if (!isAdded || activity == null) return
        Log.i("SOTStartTestActivity", "walkthrough3_scr_tap_start")
        PrefHelper(requireActivity()).putBoolean("StartScreens", value = true)
        SOTAdsManager.notifyFlowFinished()
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded) return

        if (!NetworkCheck.isNetworkAvailable(context)) {
            binding.glOne.setGuidelinePercent(0.8f)
            binding.nativeAdContainerAd.visibility = View.GONE
            return
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_3") == true) {
            safeShowAdmobWTThreeNatives()
        } else {
            binding.nativeAdContainerAd.visibility = View.GONE
        }
    }

    private fun safeShowAdmobWTThreeNatives() {
        if (!isAdded || activity == null) return

        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_3")
            ?.let { adId ->
                AdmobNativeAdManager.requestAd(
                    mContext = requireActivity(),
                    adId = adId,
                    adName = "WALKTHROUGH_3",
                    isMedia = true,
                    isMediumAd = true,
                    remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_WALKTHROUGH_3") ?: true,
                    nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_WALKTHROUGH_3"),
                    populateView = true,
                    requestAgain = false,
                    adContainer = binding.nativeAdContainerAd,
                    onAdFailed = {
                        if (_binding != null && isAdded) {
                            binding.nativeAdContainerAd.visibility = View.GONE
                        }
                        Log.i("SOT_ADS_TAG", "WALKTHROUGH_3: Admob: onAdFailed()")
                    },
                    onAdLoaded = {
                        if (_binding != null && isAdded) {
                            binding.nativeAdContainerAd.visibility = View.VISIBLE
                        }
                        Log.i("SOT_ADS_TAG", "WALKTHROUGH_3: Admob: onAdLoaded()")
                    }
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadAdmobWTThreeInterstitial() {
        val adId =
            sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_INTERSTITIAL_LETS_START")
        if (!adId.isNullOrEmpty()) {
            AdMobInterstitialInside.checkAndLoadAdMobInterstitial(
                context = requireActivity(),
                nameFragment = "WALKTHROUGH_3",
                adId = adId,
                onAdLoadedCallAdmob = {
                    Log.i("SOT_ADS_TAG", "Admob: Interstitial : WALKTHROUGH_3 : adLoaded()")
                }
            )
        } else {
            Log.e("SOT_ADS_TAG", "Admob: Interstitial ad ID not found for WALKTHROUGH_3")
        }
    }

}
