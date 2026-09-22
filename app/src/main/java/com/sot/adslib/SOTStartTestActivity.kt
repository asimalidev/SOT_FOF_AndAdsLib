package com.sot.adslib

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.manual.mediation.library.sotadlib.activities.AppCompatBaseActivity
import com.manual.mediation.library.sotadlib.adMobAdClasses.AdMobBannerAdSplash
import com.manual.mediation.library.sotadlib.callingClasses.LanguageScreensConfiguration
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsConfigurations
import com.manual.mediation.library.sotadlib.callingClasses.SOTAdsManager
import com.manual.mediation.library.sotadlib.callingClasses.WalkThroughScreensConfiguration
import com.manual.mediation.library.sotadlib.callingClasses.WelcomeScreensConfiguration
import com.manual.mediation.library.sotadlib.data.Language
import com.manual.mediation.library.sotadlib.data.WalkThroughItem
import com.manual.mediation.library.sotadlib.utils.FirebaseCommonTracker
import com.manual.mediation.library.sotadlib.utils.MyLocaleHelper
import com.manual.mediation.library.sotadlib.utils.NetworkCheck
import com.manual.mediation.library.sotadlib.utils.PrefHelper
import com.manual.mediation.library.sotadlib.utils.hideSystemUIUpdated
import com.manual.mediation.library.sotadlib.utilsGoogleAdsConsent.ConsentConfigurations
import com.sot.adslib.databinding.ActivitySotstartTestBinding
import com.urdu_keyboard.utilityClasses.RemoteConfigConstTest

class SOTStartTestActivity : AppCompatBaseActivity() {

    private lateinit var binding: ActivitySotstartTestBinding
    private lateinit var sotAdsConfigurations: SOTAdsConfigurations
    private var firstOpenFlowAdIds: HashMap<String, String> = HashMap()
    private var isDuplicateScreenStarted = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySotstartTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemUIUpdated()
        startFirstOpenFlow()
    }

    private fun startFirstOpenFlow() {
        Log.i("SOTStartTestActivity", "splash_scr")
        firstOpenFlowAdIds.apply {
            this["ADMOB_SPLASH_INTERSTITIAL"] = "ca-app-pub-3940256099942544/1033173712"
            this["ADMOB_SPLASH_RESUME"] = "ca-app-pub-3940256099942544/9257395921"
            this["ADMOB_BANNER_SPLASH"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_LANGUAGE_1"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_LANGUAGE_2"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_SURVEY_1"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_SURVEY_2"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_WALKTHROUGH_1"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_WALKTHROUGH_2"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_WALKTHROUGH_FULLSCR"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_NATIVE_WALKTHROUGH_3"] = "ca-app-pub-3940256099942544/2247696110"
            this["ADMOB_INTERSTITIAL_LETS_START"] = "ca-app-pub-3940256099942544/1033173712"

        }

        SOTAdsManager.setOnFlowStateListener(
            reConfigureBuilders = {
                SOTAdsManager.refreshStrings(
                    setUpWelcomeScreen(this),
                    getWalkThroughList(this)
                )
            },
            onFinish = {
                gotoMainActivity()
            }
        )

        val consentConfig = ConsentConfigurations.Builder()
            .setApplicationContext(application)
//            .setMintegralInitializationId(
//                appId = "144002",
//                appKey = "7c22942b749fe6a6e361b675e96b3ee9"
//            )
//            .setUnityInitializationId(gameId = "1234567", testMode = true)
            .setActivityContext(this)
            .setTestDeviceHashedIdList(
                arrayListOf(
                    "09DD12A6DB3BBF9B55D65FAA9FD7D8E0",
                    "3F8FB4EE64D851EDBA704E705EC63A62",
                    "84C3994693FB491110A5A4AEF8C5561B",
                    "CB2F3812ACAA2A3D8C0B31682E1473EB",
                    "F02B044F22C917805C3DF6E99D3B8800",
                    "FF98A451C9A8BB057CA62040BCB32743"
                           )
            )
            .setOnConsentGatheredCallback {
                Log.i("ConsentMessage", "SOTStartActivity: setOnConsentGatheredCallback")
                fetchAdIDS(
                    remoteConfigOperationsCompleted = {
                            remoteData ->

                        if (NetworkCheck.isNetworkAvailable(this)) {
                            if (remoteData.getValue(RemoteConfigConstTest.BANNER_SPLASH) == true) {
                                binding.bannerAd.visibility = View.VISIBLE

                                loadAdmobBannerAd(remoteData)
                            } else {
                                /**If banner is OFF in remote config, start heavy ads immediately**/
                                startHeavyAdsFlow(remoteData)
                            }
                        } else {
                            startHeavyAdsFlow(remoteData)
                        }
                    }
                )
            }
            .build()

        val welcomeScreensConfiguration = WelcomeScreensConfiguration.Builder()
            .setActivityContext(this)
            .setXMLLayout(setUpWelcomeScreen(this))
            .build()

        val languageScreensConfiguration = LanguageScreensConfiguration.Builder()
            .setActivityContext(this)
            .setDrawableColors(
                selectedDrawable = AppCompatResources.getDrawable(
                    this,
                    com.manual.mediation.library.sotadlib.R.drawable.ad_att_bg
                )!!,
                unSelectedDrawable = AppCompatResources.getDrawable(
                    this,
                    com.manual.mediation.library.sotadlib.R.drawable.ad_att_bg
                )!!,
                selectedRadio = AppCompatResources.getDrawable(
                    this,
                    com.manual.mediation.library.sotadlib.R.drawable.ic_radio_button_checked
                )!!,
                unSelectedRadio = AppCompatResources.getDrawable(
                    this,
                    com.manual.mediation.library.sotadlib.R.drawable.ic_radio_button_unchecked
                )!!,
                tickSelector =AppCompatResources.getDrawable(
                    this,
                    com.manual.mediation.library.sotadlib.R.drawable.done_btn
                )!!,
                themeColor = ContextCompat.getColor(this, R.color.white),
                statusBarColor = ContextCompat.getColor(this, R.color.white),
                font = ContextCompat.getColor(this, R.color.yellow),
                headingColor = ContextCompat.getColor(this, R.color.yellow),
            )
            .setEventTracker(FirebaseCommonTracker())
            .setLanguages(
                arrayListOf(
                    Language.Urdu,
                    Language.English,
                    Language.Hindi,
                    Language.French,
                    Language.Dutch,
                    Language.Arabic,
                    Language.German
                )
            )
            .build()

        val walkThroughScreensConfiguration = WalkThroughScreensConfiguration.Builder()
            .setActivityContext(this)
            .setWalkThroughContent(getWalkThroughList(this))
            .setEventTracker(FirebaseCommonTracker())
            .build()

        sotAdsConfigurations = SOTAdsConfigurations.Builder()
            .setFirstOpenFlowAdIds(firstOpenFlowAdIds)
            .setConsentConfig(consentConfig)
            .setLanguageScreenConfiguration(languageScreensConfiguration)
            .setWelcomeScreenConfiguration(welcomeScreensConfiguration)
            .setWalkThroughScreenConfiguration(walkThroughScreensConfiguration)
            .build()

        SOTAdsManager.startFlow(sotAdsConfigurations)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun loadAdmobBannerAd(remoteData: HashMap<String, Any>) {
        AdMobBannerAdSplash(
            activity = this@SOTStartTestActivity,
            placementID = "ca-app-pub-3940256099942544/9214589741",
            bannerContainer = binding.bannerAd,
            shimmerContainer = binding.bannerShimmerLayout.root,
            onAdFailed = {
                Log.i("SOT_ADS_TAG", "Banner Failed. Starting Interstitial/Native Flow.")
                binding.bannerAd.visibility = View.GONE
                startHeavyAdsFlow(remoteData)
            },
            onAdLoaded = {
                Log.i("SOT_ADS_TAG", "Banner Loaded. Starting Interstitial/Native Flow.")
            },
            onAdImpression = {
                Log.i("SOT_ADS_TAG", "Banner is VISIBLE! Starting Interstitial/Native Flow.")
                startHeavyAdsFlow(remoteData)
            },
            onAdClicked = {

            }
        )
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun startHeavyAdsFlow(remoteData: HashMap<String, Any>) {

        sotAdsConfigurations.setRemoteConfigData(
            activityContext = this@SOTStartTestActivity,
            myRemoteConfigData = remoteData
        )
    }
    private fun gotoMainActivity() {
        val time = if (PrefHelper(this).getBooleanDefault("StartScreens", default = false)) {
            0
        } else {
            if (NetworkCheck.isNetworkAvailable(this)) {
                0
            } else {
                3000
            }
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, FinalActivity::class.java)
            startActivity(intent)
            finish()
        }, time.toLong())
    }

    private fun setUpWelcomeScreen(context: Context): View {
        val localizedConfig =
            resources.configuration.apply { MyLocaleHelper.onAttach(context, "en") }
        val localizedContext = ContextWrapper(context).createConfigurationContext(localizedConfig)

        val welcomeScreenView = LayoutInflater.from(localizedContext)
            .inflate(R.layout.layout_welcome_scr_test, null, false)

        val txtWallpapers = welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtWallpapers)
        val txtEditor = welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtEditor)
        val txtLiveThemes = welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtLiveThemes)
        val txtPhotoOnKeyboard =
            welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtPhotoOnKeyboard)
        val txtPhotoTranslator =
            welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtPhotoTranslator)
        val txtInstantSticker =
            welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtInstantSticker)
        val txtLiveTranslator =
            welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtLiveTranslator)
        val txtUrduSticker = welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtUrduSticker)

        var txtWallpapersBool = false
        var txtEditorBool = false
        var txtLiveThemesBool = false
        var txtPhotoOnKeyboardBool = false
        var txtPhotoTranslatorBool = false
        var txtInstantStickerBool = false
        var txtLiveTranslatorBool = false
        var txtUrduStickerBool = false

        val nextButton = welcomeScreenView.findViewById<AppCompatTextView>(R.id.txtNext)

        txtWallpapers.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_wallpaper")


            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }

            isDuplicateScreenStarted = false
            if (txtWallpapersBool) {
                txtWallpapersBool = false
            } else {
                txtWallpapersBool = true
            }
        }
        txtEditor.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_urdu_editor")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtEditorBool) {
                txtEditorBool = false
            } else {
                txtEditorBool = true
            }
        }
        txtLiveThemes.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_live_themes")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtLiveThemesBool) {
                txtLiveThemesBool = false
            } else {
                txtLiveThemesBool = true
            }
        }
        txtPhotoOnKeyboard.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_photo_on_keyboard")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtPhotoOnKeyboardBool) {
                txtPhotoOnKeyboardBool = false
            } else {
                txtPhotoOnKeyboardBool = true
            }
        }
        txtPhotoTranslator.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_photo_translator")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtPhotoTranslatorBool) {
                txtPhotoTranslatorBool = false
            } else {
                txtPhotoTranslatorBool = true
            }
        }
        txtInstantSticker.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_instant_stickers")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtInstantStickerBool) {
                txtInstantStickerBool = false
            } else {
                txtInstantStickerBool = true
            }
        }
        txtLiveTranslator.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_live_translator")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtLiveTranslatorBool) {
                txtLiveTranslatorBool = false
            } else {
                txtLiveTranslatorBool = true
            }
        }
        txtUrduSticker.setOnClickListener {
            Log.i("SOTStartTestActivity", "survey_scr_check_urdu_stickers")
            if (isDuplicateScreenStarted) {
                SOTAdsManager.showWelcomeDupScreen()
            }
            isDuplicateScreenStarted = false
            if (txtUrduStickerBool) {
                txtUrduStickerBool = false
            } else {
                txtUrduStickerBool = true
            }
        }

        nextButton.setOnClickListener {
            if (txtWallpapersBool || txtEditorBool ||
                txtLiveThemesBool || txtPhotoOnKeyboardBool ||
                txtPhotoTranslatorBool || txtInstantStickerBool ||
                txtLiveTranslatorBool || txtUrduStickerBool
            ) {
                Log.i("SOTStartTestActivity", "survey2_scr")
                Log.i("SOTStartTestActivity", "survey2_scr_tap_continue")
                SOTAdsManager.completeWelcomeScreens()
            } else {
                Log.i("SOTStartTestActivity", "survey1_scr")
                Log.i("SOTStartTestActivity", "survey1_scr_tap_continue")
                if (isDuplicateScreenStarted) {
                    SOTAdsManager.showWelcomeDupScreen()
                }
                isDuplicateScreenStarted = false
                val toast = Toast.makeText(context, "Please check the checkbox", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
        }
        return welcomeScreenView
    }


    private fun getWalkThroughList(context: Context): ArrayList<WalkThroughItem> {
        val localizedContext = ContextWrapper(context).createConfigurationContext(
            resources.configuration.apply { MyLocaleHelper.onAttach(context, "en") }
        )
        return arrayListOf(
            WalkThroughItem(
                heading = "Screen 1",
                description = "This is screen one",
                headingColor = com.manual.mediation.library.sotadlib.R.color.redLib,
                descriptionColor = com.manual.mediation.library.sotadlib.R.color.yellowLib,
                nextColor = com.manual.mediation.library.sotadlib.R.color.orangeLib,
                nextBackground = com.manual.mediation.library.sotadlib.R.drawable.done_btn,
                drawableResId = com.manual.mediation.library.sotadlib.R.drawable.ic_wt_1,
                drawableBubbleResId = R.drawable.ic_launcher_foreground,
                viewBackgroundColor = R.color.white,
                imageScale = 1,
                blurVisibility = true
            ),
            WalkThroughItem(
                heading = "Screen 2",
                description = "This is screen two",
                headingColor = com.manual.mediation.library.sotadlib.R.color.redLib,
                descriptionColor = com.manual.mediation.library.sotadlib.R.color.yellowLib,
                nextColor = com.manual.mediation.library.sotadlib.R.color.orangeLib,
                nextBackground = com.manual.mediation.library.sotadlib.R.drawable.done_btn,
                drawableResId = com.manual.mediation.library.sotadlib.R.drawable.ic_wt_1,
                drawableBubbleResId = R.drawable.ic_launcher_foreground,
                viewBackgroundColor = R.color.white,
                imageScale = 1
            ),
            WalkThroughItem(
                heading = "Screen 3",
                description = "This is screen three",
                headingColor = com.manual.mediation.library.sotadlib.R.color.redLib,
                descriptionColor = com.manual.mediation.library.sotadlib.R.color.yellowLib,
                nextColor = com.manual.mediation.library.sotadlib.R.color.orangeLib,
                nextBackground = com.manual.mediation.library.sotadlib.R.drawable.done_btn,
                drawableResId = com.manual.mediation.library.sotadlib.R.drawable.ic_wt_1,
                drawableBubbleResId = R.drawable.ic_launcher_foreground,
                viewBackgroundColor = R.color.white,
                imageScale = 0
            )
        )
    }



    private fun fetchAdIDS(remoteConfigOperationsCompleted: (HashMap<String, Any>) -> Unit) {
        if (NetworkCheck.isNetworkAvailable(this@SOTStartTestActivity)) {
            saveAllValues()
            remoteConfigOperationsCompleted.invoke(getSharedPreferencesValues())
        } else {
            remoteConfigOperationsCompleted.invoke(getSharedPreferencesValues())
        }
    }

    private fun saveAllValues() {
        val editor = getSharedPreferences("RemoteConfig", MODE_PRIVATE).edit()
        // SOT-Ads-Visibility-Config
        //editor.putString(RemoteConfigConstTest.RESUME_INTER_SPLASH, "INTERSTITIAL")
        editor.putString(RemoteConfigConstTest.RESUME_INTER_SPLASH, "RESUME")
        editor.putBoolean(RemoteConfigConstTest.BANNER_SPLASH, true)
        editor.putBoolean(RemoteConfigConstTest.RESUME_OVERALL, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_LANGUAGE_1, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_LANGUAGE_2, true )
        editor.putBoolean(RemoteConfigConstTest.NATIVE_SURVEY_1, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_SURVEY_2, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_1, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_2, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_FULLSCR, true)
        editor.putBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_3, true)
        editor.putBoolean(RemoteConfigConstTest.INTERSTITIAL_LETS_START, true)
        editor.putString(RemoteConfigConstTest.TIMER_NATIVE_F_SRC, "5")
        editor.putString(RemoteConfigConstTest.DELAY_TO_SHOW_LANGUAGE_DONE, "2000")
        editor.putBoolean(RemoteConfigConstTest.IS_PURCHASED, false)

        editor.apply()
    }

    private fun getSharedPreferencesValues(): HashMap<String, Any> {
        val remoteConfigHashMap: HashMap<String, Any> = HashMap()
        val prefs: SharedPreferences = getSharedPreferences("RemoteConfig", Context.MODE_PRIVATE)

        remoteConfigHashMap.apply {
            this["RESUME_INTER_SPLASH"] =
                "${prefs.getString(RemoteConfigConstTest.RESUME_INTER_SPLASH, "Empty")}"
            this["BANNER_SPLASH"] = prefs.getBoolean(RemoteConfigConstTest.BANNER_SPLASH, false)
            this["RESUME_OVERALL"] = prefs.getBoolean(RemoteConfigConstTest.RESUME_OVERALL, false)
            this["NATIVE_LANGUAGE_1"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_LANGUAGE_1, false)
            this["NATIVE_LANGUAGE_2"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_LANGUAGE_2, false)
            this["NATIVE_SURVEY_1"] = prefs.getBoolean(RemoteConfigConstTest.NATIVE_SURVEY_1, false)
            this["NATIVE_SURVEY_2"] = prefs.getBoolean(RemoteConfigConstTest.NATIVE_SURVEY_2, false)
            this["NATIVE_WALKTHROUGH_1"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_1, false)
            this["NATIVE_WALKTHROUGH_2"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_2, false)
            this["NATIVE_WALKTHROUGH_FULLSCR"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_FULLSCR, false)
            this["NATIVE_WALKTHROUGH_3"] =
                prefs.getBoolean(RemoteConfigConstTest.NATIVE_WALKTHROUGH_3, false)
            this["INTERSTITIAL_LETS_START"] =
                prefs.getBoolean(RemoteConfigConstTest.INTERSTITIAL_LETS_START, false)
            this["TIMER_NATIVE_F_SRC"] =
                "${prefs.getString(RemoteConfigConstTest.TIMER_NATIVE_F_SRC, "Empty")}"
            this["DELAY_TO_SHOW_LANGUAGE_DONE"] =
                "${prefs.getString(RemoteConfigConstTest.DELAY_TO_SHOW_LANGUAGE_DONE, "Empty")}"
            this["IS_PURCHASED"] =
                prefs.getBoolean(RemoteConfigConstTest.IS_PURCHASED, false)


        }
        return remoteConfigHashMap
    }
}