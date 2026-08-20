package com.editor.photo.video.collagemaker.photoedit.fragments.splashfragments

import android.util.Log
import com.editor.photo.video.collagemaker.photoedit.activities.MainActivity
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentSplashBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import com.editor.photo.video.collagemaker.photoedit.utlis.SharedPreference

class SplashFragment : BaseFragment<FragmentSplashBinding>(R.layout.fragment_splash) {

    private val tag = "Splash Fragment"

    override fun onViewCreatedOneTime() {
        binding.btnGetStarted.setOnClickListener {
            navigateScreen()
        }
        Log.d(tag, "This Will Run onViewCreatedOneTime")
    }

    override fun onViewCreatedEverytime() {
        Log.d(tag, "This Will Run onViewCreatedEveryTime")
    }

    private fun navigateScreen() {
        val context = requireContext()

        val isLanguageSelected = SharedPreference.isLanguageSelected(context)
        val isOnBoardingShown = SharedPreference.isOnBoardingShown(context)

        if (!isLanguageSelected) {
            // Agar language select nahi hui, toh Language screen par bhejo
            navigateTo(
                R.id.splashFragment,
                R.id.action_splashFragment_to_languageFragment
            )
        } else if (!isOnBoardingShown) {
            // Agar language ho gayi hai par onboarding nahi hui, toh Onboarding par bhejo
            navigateTo(
                R.id.splashFragment,
                R.id.action_splashFragment_to_onBoardingFragment
            )
        } else {
            // Dono ho chuke hain toh MainActivity par bhejo
            CommonData.navigateToActivity(
                context,
                MainActivity::class.java
            )
        }
    }
}