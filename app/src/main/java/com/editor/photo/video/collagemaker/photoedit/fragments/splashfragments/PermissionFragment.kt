package com.editor.photo.video.collagemaker.photoedit.fragments.splashfragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.activities.MainActivity
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentPermissionBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import com.editor.photo.video.collagemaker.photoedit.utlis.DebounceListener.setDebounceClickListener

class PermissionFragment : BaseFragment<FragmentPermissionBinding>(R.layout.fragment_permission) {

    override fun onViewCreatedOneTime() {
        // Optional: One-time initialization
    }

    override fun onViewCreatedEverytime() {
        // Set initial switch states
        updateSwitchStates()
        updateContinueButtonState()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Continue button
        binding.btnContinue.setDebounceClickListener {
            if (CommonData.isAllPermissionsGranted(requireContext())) {
                CommonData.navigateToActivity(requireContext(), MainActivity::class.java)
            } else {
                showToast("Please grant all required permissions to continue.")
            }
        }

        // Camera switch
        binding.switchCamera.setOnCheckedChangeListener { _, isChecked ->
            handleCameraSwitch(isChecked)
        }

        // Storage switch
        binding.switchStorage.setOnCheckedChangeListener { _, isChecked ->
            handleStorageSwitch(isChecked)
        }

        // Notification switch (only Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
                handleNotificationSwitch(isChecked)
            }
        }
    }

    private fun handleCameraSwitch(isChecked: Boolean) {
        if (isChecked && !CommonData.isCameraPermissionGranted(requireContext())) {
            CommonData.requestCameraPermissionWithDexter(
                requireContext(),
                onGranted = {
                    setSwitchChecked(binding.switchCamera, true)
                },
                onDenied = {
                    setSwitchChecked(binding.switchCamera, false)
                }
            )
        } else {
            setSwitchChecked(binding.switchCamera, CommonData.isCameraPermissionGranted(requireContext()))
        }
    }

    private fun handleStorageSwitch(isChecked: Boolean) {
        if (isChecked && !CommonData.isStoragePermissionGranted(requireContext())) {
            CommonData.requestMediaPermissionsWithDexter(
                requireContext(),
                onGranted = {
                    setSwitchChecked(binding.switchStorage, true)
                },
                onDenied = {
                    setSwitchChecked(binding.switchStorage, false)
                }
            )
        } else {
            setSwitchChecked(binding.switchStorage, CommonData.isStoragePermissionGranted(requireContext()))
        }
    }

    private fun handleNotificationSwitch(isChecked: Boolean) {
        if (isChecked && !CommonData.isNotificationPermissionGranted(requireContext())) {
            CommonData.requestNotificationPermissionWithDexter(
                requireContext(),
                onGranted = {
                    setSwitchChecked(binding.switchNotification, true)
                },
                onDenied = {
                    setSwitchChecked(binding.switchNotification, false)
                }
            )
        } else {
            setSwitchChecked(binding.switchNotification, CommonData.isNotificationPermissionGranted(requireContext()))
        }
    }

    private fun setSwitchChecked(switch: SwitchCompat, isChecked: Boolean) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = isChecked

        // Apply color #EC8C7C for storage switch
        val storageColor = Color.parseColor("#EC8C7C")
        val darkGray = ContextCompat.getColor(requireContext(), R.color.dark_gray)

        when (switch.id) {
            binding.switchStorage.id -> {
                // Storage switch uses #EC8C7C color
                if (isChecked) {
                    switch.thumbTintList = ColorStateList.valueOf(storageColor)
                    switch.trackTintList = ColorStateList.valueOf(storageColor)
                } else {
                    switch.thumbTintList = ColorStateList.valueOf(darkGray)
                    switch.trackTintList = ColorStateList.valueOf(darkGray)
                }
            }
            else -> {
                // Other switches use app_color
                val appColor = ContextCompat.getColor(requireContext(), R.color.app_color)
                if (isChecked) {
                    switch.thumbTintList = ColorStateList.valueOf(appColor)
                    switch.trackTintList = ColorStateList.valueOf(appColor)
                } else {
                    switch.thumbTintList = ColorStateList.valueOf(darkGray)
                    switch.trackTintList = ColorStateList.valueOf(darkGray)
                }
            }
        }

        CommonData.updateSwitchUI(switch, isChecked)
        switch.isEnabled = !isChecked

        switch.setOnCheckedChangeListener { _, checked ->
            when (switch) {
                binding.switchCamera -> handleCameraSwitch(checked)
                binding.switchStorage -> handleStorageSwitch(checked)
                binding.switchNotification -> handleNotificationSwitch(checked)
            }
        }
        updateContinueButtonState()
    }

    private fun updateSwitchStates() {
        setSwitchChecked(binding.switchCamera, CommonData.isCameraPermissionGranted(requireContext()))
        setSwitchChecked(binding.switchStorage, CommonData.isStoragePermissionGranted(requireContext()))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setSwitchChecked(binding.switchNotification, CommonData.isNotificationPermissionGranted(requireContext()))
            binding.notificationsLayout.visibility = View.VISIBLE
        } else {
            binding.notificationsLayout.visibility = View.GONE
        }
    }

    private fun updateContinueButtonState() {
        val enabled = CommonData.isAllPermissionsGranted(requireContext())
        binding.btnContinue.isEnabled = enabled
        binding.btnContinue.alpha = if (enabled) 1.0f else 0.5f
    }

    override fun onResume() {
        super.onResume()
        // Refresh switches in case user returned from settings
        updateSwitchStates()
        updateContinueButtonState()
        if (CommonData.isAllPermissionsGranted(requireContext())) {
            CommonData.navigateToActivity(requireContext(), MainActivity::class.java)
        }
    }
}