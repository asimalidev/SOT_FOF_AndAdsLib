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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.manual.mediation.library.sotadlib.R
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.data.WalkThroughItem
import com.manual.mediation.library.sotadlib.databinding.FragmentWTTwoBinding
import com.manual.mediation.library.sotadlib.interfaces.CommonEventTracker
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WTTwoFragment : Fragment() {
    private lateinit var binding: FragmentWTTwoBinding
    private var sotAdsConfigurations: SOTAdsConfigurations? = null
    private lateinit var item: WalkThroughItem
    private var eventTracker: CommonEventTracker? = null
    private var scaleType: Int = 0
    private var blurVisibility: Boolean = false

    companion object {
        private const val ARG_ITEM = "walkThroughItem"

        fun newInstance(item: WalkThroughItem, tracker: CommonEventTracker? = null): WTTwoFragment {
            val fragment = WTTwoFragment()
            val args = Bundle().apply {
                putParcelable(ARG_ITEM, item)
            }
            fragment.arguments = args
            fragment.eventTracker = tracker
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = arguments?.getParcelable(ARG_ITEM) ?: throw IllegalStateException("WalkThroughItem must be provided")
        scaleType = item.imageScale
        blurVisibility = item.blurVisibility
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWTTwoBinding.inflate(inflater, container, false)
        eventTracker?.logEvent(
            requireActivity(),
            "walkthrough2_scr"
        )
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

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sotAdsConfigurations = SOTAdsManager.getConfigurations()
        Log.i("SOTStartTestActivity", "walkthrough2_scr")
        lifecycleScope.launch {
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
        }
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                Glide.with(requireActivity())
                    .asDrawable()
                    .load(item.drawableBubbleResId)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .skipMemoryCache(true)
                    .into(binding.bubble)
            }
        }

        if (item.headingColor != 0) binding.txtHeading.setTextColor(ContextCompat.getColor(requireContext(), item.headingColor))
        if (item.descriptionColor != 0) binding.txtDescription.setTextColor(ContextCompat.getColor(requireContext(), item.descriptionColor))
        if (item.nextColor != 0) binding.btnNext.setTextColor(ContextCompat.getColor(requireContext(), item.nextColor))
        if (item.nextBackground != 0) { binding.btnNext.background = ContextCompat.getDrawable(requireContext(), item.nextBackground) } else { binding.btnNext.background = null }
        binding.txtHeading.text = item.heading
        binding.txtDescription.text = item.description

        if (item.viewBackgroundColor != 0) binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), item.viewBackgroundColor))

        if (!NetworkCheck.isNetworkAvailable(context)) {
            binding.glOne.setGuidelinePercent(0.8f)
        }
        binding.btnNext.setOnClickListener {
            Log.i("SOTStartTestActivity", "walkthrough2_scr_tap_next")
            eventTracker?.logEvent(
                requireActivity(),
                "walkthrough2_scr_tap_next"
            )
            val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
            viewPager?.currentItem = 2
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_3") == true) {
            loadAdmobWTThreeNatives()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkCheck.isNetworkAvailable(context)) {
            binding.glOne.setGuidelinePercent(0.8f)
            binding.nativeAdContainerAd.visibility = View.GONE
        }

        if (sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_2") == true) {
            binding.nativeAdContainerAd.visibility = View.VISIBLE
            showAdmobWTTwoNatives()
        } else {
            binding.nativeAdContainerAd.visibility = View.GONE
        }
    }

    private fun loadAdmobWTThreeNatives() {
        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_3")?.let { adId ->
            AdmobNativeAdManager.requestAd(
                mContext = requireActivity(),
                adId = adId,
                adName = "WALKTHROUGH_3",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_WALKTHROUGH_3") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_WALKTHROUGH_3"),
                populateView = false
            )
        } ?: Log.i("WTTwoFragment", "ADMOB_NATIVE_WALKTHROUGH_3 ad ID is missing.")
    }

    private fun showAdmobWTTwoNatives() {
        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_2")?.let { adId ->
            AdmobNativeAdManager.requestAd(
                mContext = requireActivity(),
                adId = adId,
                adName = "WALKTHROUGH_2",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_WALKTHROUGH_2") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_WALKTHROUGH_2"),
                populateView = true,
                requestAgain = false,
                adContainer = binding.nativeAdContainerAd,
                onAdFailed = {
                    binding.nativeAdContainerAd.visibility = View.GONE
                    Log.i("SOT_ADS_TAG", "WALKTHROUGH_2: Admob: onAdFailed()")
                },
                onAdLoaded = {
                    binding.nativeAdContainerAd.visibility = View.VISIBLE
                    Log.i("SOT_ADS_TAG", "WALKTHROUGH_2: Admob: onAdLoaded()")
                }
            )
        } ?: Log.w("WTTwoFragment", "ADMOB_NATIVE_WALKTHROUGH_2 ad ID is missing.")
    }
}
