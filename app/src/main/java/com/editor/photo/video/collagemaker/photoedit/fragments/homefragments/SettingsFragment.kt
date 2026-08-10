package com.editor.photo.video.collagemaker.photoedit.fragments.homefragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentSettingsBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings) {

    private val tag = "SettingsFragment"

    override fun onViewCreatedOneTime() {
        Log.d(tag, "This Will Run onViewCreatedOneTime")

        // Clicks Setup karna
        setupClickListeners()
    }

    override fun onViewCreatedEverytime() {
        Log.d(tag, "This Will Run onViewCreatedEveryTime")
    }

    private fun setupClickListeners() {

        binding.icBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 2. Select Language Logic
        binding.selectLangSection.setOnClickListener {
            parentNavigate(R.id.languageFragment)
        }

        // 3. Privacy Policy Logic
        binding.privacySection.setOnClickListener {
            val privacyUrl = "https://www.yourwebsite.com/privacy-policy"
            openWebPage(privacyUrl)
        }

        // 4. Share App Logic
        binding.shareSection.setOnClickListener {
            shareApp()
        }

        // 5. Rate Us Logic
        binding.rateSection.setOnClickListener {
            rateApp()
        }

        // 6. More Apps Logic
        binding.moreAppsSection.setOnClickListener {
            openMoreApps()
        }
    }



    private fun openWebPage(url: String) {
        try {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Browser not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))

            // App ka PlayStore link generate karein
            val appLink = "https://play.google.com/store/apps/details?id=${requireActivity().packageName}"
            val shareMessage = "Check out this amazing app:\n$appLink"

            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "Share App via"))
        } catch (e: Exception) {
            Log.e(tag, "Error sharing app: ${e.message}")
        }
    }

    private fun rateApp() {
        val packageName = requireActivity().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun openMoreApps() {
        val developerName = "Your_Developer_Account_Name"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:$developerName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=$developerName")))
        }
    }
}