package com.editor.photo.video.collagemaker.photoedit.fragments.splashfragments

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.activities.MainActivity
import com.editor.photo.video.collagemaker.photoedit.adapters.LanguageAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentLanguageBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.models.LanguageModel
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import com.editor.photo.video.collagemaker.photoedit.utlis.DebounceListener.setDebounceClickListener
import com.editor.photo.video.collagemaker.photoedit.utlis.SharedPreference
import java.util.Locale

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(R.layout.fragment_language) {

    private val tag = "Language Fragment"
    private var selectedLanguageCode: String = "en"

    override fun onViewCreatedOneTime() {
        Log.d(tag, "This Will Run onViewCreatedOneTime")
        setUpLanguageList()
    }

    override fun onViewCreatedEverytime() {
        Log.d(tag, "This Will Run onViewCreatedEveryTime")
        setUpClickListeners()
    }

    private fun setUpClickListeners() {
        with(binding) {
            btnNext.setDebounceClickListener {
                SharedPreference.AppLanguageCode = selectedLanguageCode
                updateLocale(SharedPreference.AppLanguageCode)

                val context = requireContext()
                SharedPreference.setLanguageSelected(context, true)

                if (SharedPreference.isOnBoardingShown(context)) {

                    CommonData.navigateToActivity(requireContext(), MainActivity::class.java)
                } else {
                    navigateTo(
                        R.id.languageFragment,
                        R.id.action_languageFragment_to_onBoardingFragment
                    )
                }
            }
        }
    }

    private fun setUpLanguageList() {
        val languageList = listOf(
            // 🌍 Europe Languages
            LanguageModel("en", "English – Us", R.drawable.flag_united_states_of_america),
            LanguageModel("de", "German – Deutsch", R.drawable.flag_germany),
            LanguageModel("fr", "French – Français", R.drawable.flag_france),
            LanguageModel("it", "Italian – Italiano", R.drawable.flag_italy),
            LanguageModel("es", "Spanish – Español", R.drawable.flag_spain),
            LanguageModel("pt-rPT", "Portuguese (Portugal) – Português", R.drawable.flag_portugal),
            LanguageModel("pt-rBR", "Portuguese (Brazil) – Português", R.drawable.flag_brazil),
            LanguageModel("ru", "Russian – Русский", R.drawable.flag_russia),
            LanguageModel("uk", "Ukrainian – Українська", R.drawable.flag_ukraine),
            LanguageModel("nl", "Dutch – Nederlands", R.drawable.flag_netherlands),
            LanguageModel("pl", "Polish – Polski", R.drawable.flag_poland),
            LanguageModel("ro", "Romanian – Română", R.drawable.flag_romania),
            LanguageModel("el", "Greek – Ελληνικά", R.drawable.flag_greece),
            LanguageModel("hu", "Hungarian – Magyar", R.drawable.flag_hungary),
            LanguageModel("cs", "Czech – Čeština", R.drawable.flag_czech_republic),
            LanguageModel("tr", "Turkish – Türkçe", R.drawable.flag_turkey),

            // 🌏 East Asian Languages
            LanguageModel("zh-rCN", "Chinese (Simplified) – 简体中文", R.drawable.flag_china),
            LanguageModel("zh-rTW", "Chinese (Traditional) – 繁體中文", R.drawable.flag_taiwan),
            LanguageModel("ja", "Japanese – 日本語", R.drawable.flag_japan),
            LanguageModel("ko", "Korean – 한국어", R.drawable.flag_south_korea),

            // 🌴 Southeast Asian Languages
            LanguageModel("in", "Indonesian – Bahasa Indonesia", R.drawable.flag_indonesia),
            LanguageModel("ms", "Malay – Bahasa Melayu", R.drawable.flag_malaysia),
            LanguageModel("th", "Thai – ไทย", R.drawable.flag_thailand),
            LanguageModel("vi", "Vietnamese – Tiếng Việt", R.drawable.flag_vietnam),
            LanguageModel("sc", "Sinhalese – සිංහල", R.drawable.flag_sri_lanka),

            // 🕌 South Asia & Middle East
            LanguageModel("ur", "Urdu – اردو", R.drawable.flag_pakistan),
            LanguageModel("ar", "Arabic – العربية", R.drawable.flag_saudi_arabia),
            LanguageModel("hi", "Hindi – हिन्दी", R.drawable.flag_india),
            LanguageModel("bn", "Bengali – বাংলা", R.drawable.flag_bangladesh),
            LanguageModel("ta", "Tamil – தமிழ்", R.drawable.flag_india),
            LanguageModel("fa", "Persian – فارسی", R.drawable.flag_iran)
        )

        selectedLanguageCode = SharedPreference.AppLanguageCode.ifEmpty { "en" }

        val adapter = LanguageAdapter(languageList, { selectedLanguage ->
            selectedLanguageCode = selectedLanguage.code
            updateTextsForNewLocale()
        }, selectedLanguageCode)

        binding.recyclerLanguages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLanguages.adapter = adapter
    }

    private fun updateTextsForNewLocale() {
        val contextWithLocale = requireContext().createConfigurationContext(
            resources.configuration.apply {
                setLocale(Locale.forLanguageTag(selectedLanguageCode))
            }
        )
        binding.textLanguage.text = contextWithLocale.getString(R.string.select_your_language)
        binding.btnNext.text =
            contextWithLocale.getString(R.string.select)
    }

    private fun updateLocale(appLanguageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(appLanguageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}