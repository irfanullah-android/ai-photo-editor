package com.editor.photo.video.collagemaker.photoedit.fragments.mainfragment

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentMainBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : BaseFragment<FragmentMainBinding>(R.layout.fragment_main) {

    private val tag = "MainFragment"
    private var navController: NavController? = null
    private var cameraImageUri: Uri? = null

    // ✅ FIXED: Navigation ka logic change kiya
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            Toast.makeText(requireContext(), "Photo captured!", Toast.LENGTH_SHORT).show()
            val bundle = Bundle().apply { putString("imageUri", cameraImageUri.toString()) }

            // FIX: Main Graph (nev_main) mein navigate karne ke liye parent/root controller use karein
            try {
                findNavController().navigate(R.id.photoStudioFragment, bundle)
            } catch (e: Exception) {
                Log.e(tag, "Navigation error: ${e.message}")
            }
        } else {
            cameraImageUri = null
            Toast.makeText(requireContext(), "Camera cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(requireContext(), "Camera permission required.", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreatedOneTime() {
        setupBottomNavigation()
    }

    override fun onViewCreatedEverytime() {
        restoreBottomNavState()
    }

    private fun setupBottomNavigation() {
        try {
            val navHostFragment = childFragmentManager.findFragmentById(R.id.fcv_container_Navigation) as? NavHostFragment ?: return
            navController = navHostFragment.navController

            // NOTE: Agar photoStudioFragment ko yahan (nev_home) mein navigate karna hai,
            // toh use nev_home.xml mein add karna zaroori hai.
            val navGraph = navController!!.navInflater.inflate(R.navigation.nev_home)
            navGraph.setStartDestination(R.id.homeFragment)
            navController!!.graph = navGraph
            setupNavListener()
        } catch (e: Exception) {
            Log.e(tag, "Bottom nav setup error", e)
        }
    }

    private fun restoreBottomNavState() {
        if (navController == null) {
            val navHostFragment = childFragmentManager.findFragmentById(R.id.fcv_container_Navigation) as? NavHostFragment ?: return
            navController = navHostFragment.navController
        }
        setupNavListener()
    }

    private fun setupNavListener() {
        val nc = navController ?: return
        binding.bottomNavigationView.setOnApplyWindowInsetsListener(null)
        binding.bottomNavigationView.setPadding(0, 0, 0, 0)

        binding.ivCenterCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val index = when (item.itemId) {
                R.id.homeFragment -> 0
                R.id.settingsFragment -> 2
                else -> return@setOnItemSelectedListener false
            }
            navigate(nc, item.itemId)
            moveIndicator(index)
            true
        }
    }

    private fun openCamera() {
        try {
            val photoFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")

            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(cameraImageUri)
        } catch (e: Exception) {
            Log.e(tag, "Camera open error: ${e.message}")
        }
    }

    private fun navigate(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return
        try {
            navController.navigate(destinationId, null, NavOptions.Builder()
                .setLaunchSingleTop(true).setRestoreState(true).setPopUpTo(navController.graph.startDestinationId, false).build())
        } catch (e: Exception) { Log.e(tag, "Nav error", e) }
    }

    private fun moveIndicator(index: Int) {
        val navView = binding.bottomNavigationView
        val indicator = binding.indicatorLine
        navView.post {
            val menuView = navView.getChildAt(0) as? ViewGroup ?: return@post
            val actualIndex = if (index == 2) 2 else 0
            if (actualIndex >= menuView.childCount) return@post
            val itemView = menuView.getChildAt(actualIndex) ?: return@post
            indicator.animate().x(itemView.x + (itemView.width / 2f) - (indicator.width / 2f)).setDuration(200).start()
        }
    }
}