package com.editor.photo.video.collagemaker.photoedit.fragments.basefragments

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.editor.photo.video.collagemaker.photoedit.R
@Suppress("DEPRECATION")
abstract class BaseFragment<T : ViewDataBinding>(@LayoutRes private val layoutId: Int) :
    GeneralFragment() {
    var _binding: T? = null
    val binding get() = _binding!!

    private var hasInitializedRootView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Immersive mode sirf ek baar (per Activity lifetime) apply hota hai,
        // har fragment transition par nahi — isse insets resize/flicker
        // navigation animation ke saath conflict nahi karta
        applyImmersiveModeOnce()

        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            onViewCreatedOneTime()
        }
        onViewCreatedEverytime()
    }

    private fun applyImmersiveModeOnce() {
        val activity = requireActivity()
        val decorView = activity.window.decorView

        if (decorView.getTag(R.id.tag_immersive_applied) != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }

        decorView.setTag(R.id.tag_immersive_applied, true)
    }

    abstract fun onViewCreatedOneTime()

    abstract fun onViewCreatedEverytime()


    protected fun navigateTo(fragmentId: Int, action: Int, bundle: Bundle) {
        if (isAdded && isCurrentDestination(fragmentId)) {
            try {
                findNavController().navigate(action, bundle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun navigateTo(fragmentId: Int, action: Int) {
        if (isAdded && isCurrentDestination(fragmentId)) {
            try {
                findNavController().navigate(action)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun parentNavigate(destinationId: Int) {
        if (isAdded) {
            try {
                val navController = requireActivity().findNavController(R.id.fcv_container_main)
                val navOptions = navOptions {
                    anim {
                        enter = R.anim.slide_in_right
                        exit = R.anim.slide_out_left
                        popEnter = R.anim.slide_in_left
                        popExit = R.anim.slide_out_right
                    }
                }
                navController.navigate(destinationId, null, navOptions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    protected fun parentNavigate(destinationId: Int, bundle: Bundle) {
        if (isAdded) {
            try {
                val navController = requireActivity().findNavController(R.id.fcv_container_main)
                val navOptions = navOptions {
                    anim {
                        enter = R.anim.slide_in_right
                        exit = R.anim.slide_out_left
                        popEnter = R.anim.slide_in_left
                        popExit = R.anim.slide_out_right
                    }
                }
                navController.navigate(destinationId, bundle, navOptions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun parentNavigateForSkip(destinationId: Int) {
        if (isAdded) {
            try {
                val navController = requireActivity().findNavController(R.id.fcv_container_main)
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.currentDestination!!.id, true)
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                navController.navigate(destinationId, null, navOptions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    protected fun parentNavigateForSkip(destinationId: Int, bundle: Bundle) {
        if (isAdded) {
            try {
                val navController = requireActivity().findNavController(R.id.fcv_container_main)
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.currentDestination!!.id, true)
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                navController.navigate(destinationId, bundle, navOptions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun isInternetConnected(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun isCurrentDestination(fragmentId: Int): Boolean {
        return findNavController().currentDestination?.id == fragmentId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hasInitializedRootView = false
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}