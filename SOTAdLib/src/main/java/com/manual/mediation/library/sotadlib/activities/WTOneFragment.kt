package com.manual.mediation.library.sotadlib.activities

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
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdFullScreen
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdmobNativeAdManager
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.data.WalkThroughItem
import com.manual.mediation.library.sotadlib.databinding.FragmentWTOneBinding
import com.manual.mediation.library.sotadlib.interfaces.CommonEventTracker
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WTOneFragment : Fragment() {
    private lateinit var binding: FragmentWTOneBinding
    private var sotAdsConfigurations: SOTAdsConfigurations? = null
    private var eventTracker: CommonEventTracker? = null
    private var scaleType: Int = 0
    private var blurVisibility: Boolean = false

    companion object {
        private const val ARG_ITEM = "walkThroughItem"

        fun newInstance(item: WalkThroughItem, tracker: CommonEventTracker? = null): WTOneFragment {
            val fragment = WTOneFragment()
            val args = Bundle().apply {
                putParcelable(ARG_ITEM, item)
            }

            fragment.arguments = args
            fragment.eventTracker = tracker
            return fragment
        }
    }

    private lateinit var item: WalkThroughItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        item = arguments?.getParcelable(ARG_ITEM) ?: throw IllegalStateException("WalkThroughItem must be provided")
        scaleType = item.imageScale
        blurVisibility = item.blurVisibility
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWTOneBinding.inflate(inflater, container, false)
        eventTracker?.logEvent(
            requireActivity(),
            "walkthrough1_scr"
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sotAdsConfigurations = SOTAdsManager.getConfigurations()
        Log.i("SOT_ADS_TAG", "walkthrough1_scr")

        val nativeWalkThroughFullEnabled = sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_FULLSCR") == true
        Log.i("SOT_ADS_TAG", "nativeWalkThroughFullEnabled:$nativeWalkThroughFullEnabled")
        if (nativeWalkThroughFullEnabled) {
            Log.i("SOT_ADS_TAG", "NATIVE_WALKTHROUGH_FULLSCR from w1")
            loadAdmobWTFullNatives()
        }

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

        binding.txtHeading.setTextColor(ContextCompat.getColor(requireContext(), item.headingColor))
        binding.txtDescription.setTextColor(ContextCompat.getColor(requireContext(), item.descriptionColor))
        binding.btnNext.setTextColor(ContextCompat.getColor(requireContext(), item.nextColor))
        binding.btnNext.background = resources.getDrawable(item.nextBackground)
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), item.viewBackgroundColor))

        binding.txtHeading.text = item.heading
        binding.txtDescription.text = item.description

        binding.btnNext.setOnClickListener {
            eventTracker?.logEvent(
                requireActivity(),
                "walkthrough1_scr_tap_next"
            )
            Log.i("SOTStartTestActivity", "walkthrough1_scr_tap_next")
            val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
            viewPager?.currentItem = 1
        }
    }

    private fun loadAdmobWTFullNatives() {
        val adId = sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_FULLSCR")
        Log.i("SOT_ADS_TAG", "nativeWalkThroughFull adId:$adId")
        if (!adId.isNullOrEmpty()) {
            AdmobNativeAdFullScreen.requestAd(
                mContext = requireActivity(),
                adId = adId,
                adName = "WALKTHROUGH_FULL_SCREEN",
                populateView = false
            )
        } else {
            Log.e("SOT_ADS_TAG", "Admob ad ID not found for WALKTHROUGH_FULL_SCREEN")
        }
    }

    override fun onResume() {
        super.onResume()
        if (!NetworkCheck.isNetworkAvailable(context)) {
            binding.glOne.setGuidelinePercent(0.8f)
            binding.nativeAdContainerAd.visibility = View.GONE
        }

        val nativeWalkThrough1Enabled = sotAdsConfigurations?.isNativeAdEnabled("NATIVE_WALKTHROUGH_1") == true
        if (nativeWalkThrough1Enabled) {
            showAdmobWTOneNatives()
        } else {
            binding.nativeAdContainerAd.visibility = View.GONE
        }
    }

    private fun showAdmobWTOneNatives() {
        sotAdsConfigurations?.firstOpenFlowAdIds?.get("ADMOB_NATIVE_WALKTHROUGH_1")?.let { adId ->
            AdmobNativeAdManager.requestAd(
                mContext = requireActivity(),
                adId = adId,
                adName = "WALKTHROUGH_1",
                isMedia = true,
                isMediumAd = true,
                remoteConfig = sotAdsConfigurations?.getRemoteConfigData()?.get("NATIVE_WALKTHROUGH_1") ?: true,
                nativeSize = sotAdsConfigurations?.getNativeAdSize("NATIVE_WALKTHROUGH_1"),
                populateView = true,
                requestAgain = false,
                adContainer = binding.nativeAdContainerAd,
                onAdFailed = {
                    binding.nativeAdContainerAd.visibility = View.GONE
                    Log.i("SOT_ADS_TAG", "WALKTHROUGH_1: Admob: onAdFailed()")
                },
                onAdLoaded = {
                    binding.nativeAdContainerAd.visibility = View.VISIBLE
                    Log.i("SOT_ADS_TAG", "WALKTHROUGH_1: Admob: onAdLoaded()")
                }
            )
        } ?: Log.w("WTOneFragment", "ADMOB_NATIVE_WALKTHROUGH_1 ad ID is missing.")
    }
}
