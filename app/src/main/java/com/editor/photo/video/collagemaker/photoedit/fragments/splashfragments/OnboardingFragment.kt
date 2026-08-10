package com.editor.photo.video.collagemaker.photoedit.fragments.splashfragments

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.activities.MainActivity
import com.editor.photo.video.collagemaker.photoedit.adapters.OnBoardingAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentOnboardingBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.models.OnBoardingItem
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import com.editor.photo.video.collagemaker.photoedit.utlis.DebounceListener.setDebounceClickListener
import com.editor.photo.video.collagemaker.photoedit.utlis.SharedPreference
import com.editor.photo.video.collagemaker.photoedit.utlis.ZoomOutPageTransformer

class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(R.layout.fragment_onboarding) {

    private val tag = "OnboardingFragment"
    private val currentPagePosition = MutableLiveData(0)
    private lateinit var mAdapter: OnBoardingAdapter
    private lateinit var listOfOnBoardings: List<OnBoardingItem>

    private val purpleColor = Color.parseColor("#A855F7")

    override fun onViewCreatedOneTime() {
        listOfOnBoardings = listOf(
            OnBoardingItem(
                R.drawable.on_boarding_1,
                R.string.on_boarding_1_title_1,
                R.string.on_boarding_1_white_word,
                R.string.on_boarding_1_title_2
            ),
            OnBoardingItem(
                R.drawable.on_boarding_2,
                R.string.on_boarding_2_title_1,
                R.string.on_boarding_2_white_word,
                R.string.on_boarding_2_title_2
            ),
            OnBoardingItem(
                R.drawable.on_boarding_3,
                R.string.on_boarding_3_title_1,
                R.string.on_boarding_3_white_word,
                R.string.on_boarding_3_title_2
            ),
            OnBoardingItem(
                R.drawable.on_boarding_4,
                R.string.on_boarding_4_title_1,
                R.string.on_boarding_4_white_word,
                R.string.on_boarding_4_title_2
            )
        )
        setupViewPager()
    }

    override fun onViewCreatedEverytime() {
        setupClickListeners()
    }

    private fun setupViewPager() = with(binding) {
        mAdapter = OnBoardingAdapter(listOfOnBoardings)
        viewpager2.adapter = mAdapter
        viewpager2.setPageTransformer(ZoomOutPageTransformer())
        wormDotsIndicator.attachTo(viewpager2)

        viewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPagePosition.value = position
            }
        })

        currentPagePosition.observe(viewLifecycleOwner) { position ->
            updateTexts(position)
            btnNext.text = if (position == mAdapter.itemCount - 1) {
                getString(R.string.start)
            } else {
                getString(R.string.next)
            }
        }
    }

    private fun setupClickListeners() = with(binding) {
        btnNext.setDebounceClickListener {
            val currentItem = viewpager2.currentItem
            if (currentItem < mAdapter.itemCount - 1) {
                viewpager2.currentItem = currentItem + 1
            } else {
                handleFinish()
            }
        }
    }

    private fun handleFinish() {
        SharedPreference.setOnBoardingShown(requireContext(), true)
        CommonData.navigateToActivity(requireContext(), MainActivity::class.java)
    }

    private fun makeColoredTitle(fullText: String, whiteWord: String): SpannableString {
        val spannable = SpannableString(fullText)
        spannable.setSpan(ForegroundColorSpan(purpleColor), 0, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val whiteStart = fullText.indexOf(whiteWord)
        if (whiteStart >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(Color.WHITE),
                whiteStart,
                whiteStart + whiteWord.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun updateTexts(position: Int) = with(binding) {
        val currentItem = listOfOnBoardings[position]
        headline1.setText(
            makeColoredTitle(
                getString(currentItem.titleResId),
                getString(currentItem.whiteWordResId)
            ),
            TextView.BufferType.SPANNABLE
        )
        headline2.text = getString(currentItem.descriptionResId)
    }
}