package com.editor.photo.video.collagemaker.photoedit.helpers
import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentPhotoStudioBinding

/**
 * FullscreenHelper - Fullscreen mode aur UI animations
 *
 * Responsibility:
 * - System UI hide/show
 * - UI elements fade in/out
 * - Fullscreen button animation
 */
class FullscreenHelper(
    private val activity: Activity,
    private val binding: FragmentPhotoStudioBinding
) {

    // ── System UI ────────────────────────────────────────────
    fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun setupSystemUIListener(isFullscreen: () -> Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (isFullscreen() && (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    binding.root.postDelayed({
                        if (isFullscreen()) hideSystemUI()
                    }, 3000)
                }
            }
        }
    }

    // ── UI Elements Animate ───────────────────────────────────
    fun animateUIOut(onHidden: () -> Unit) {
        animateViews(getUIViews(), targetAlpha = 0f, duration = 200L) {
            hideUIElements()
            onHidden()
        }
    }

    fun animateUIIn() {
        showUIElements()
        animateViews(getUIViews(), targetAlpha = 1f, duration = 200L)
    }

    fun animateFullscreenButton(entering: Boolean) {
        binding.btnFullscreen.animate()
            .rotation(180f)
            .setDuration(200L)
            .withEndAction {
                binding.btnFullscreen.setImageResource(
                    if (entering) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
                )
                binding.btnFullscreen.rotation = 0f
            }
            .start()
    }

    // ── Private Helpers ───────────────────────────────────────
    private fun getUIViews() = listOf(
        binding.topToolbar,
        binding.rvEditorTools,
        binding.btnUndo,
        binding.btnRedo,
        binding.btnCompare
    )

    private fun hideUIElements() {
        binding.apply {
            topToolbar.visibility = View.GONE
            rvEditorTools.visibility = View.GONE
            btnUndo.visibility = View.GONE
            btnRedo.visibility = View.GONE
            btnCompare.visibility = View.GONE
        }
    }

    private fun showUIElements() {
        binding.apply {
            listOf(topToolbar, rvEditorTools, btnUndo, btnRedo, btnCompare).forEach {
                it.visibility = View.VISIBLE
                it.alpha = 0f
            }
        }
    }

    private fun animateViews(
        views: List<View>,
        targetAlpha: Float,
        duration: Long,
        onEnd: (() -> Unit)? = null
    ) {
        views.forEachIndexed { index, view ->
            view.animate()
                .alpha(targetAlpha)
                .setDuration(duration)
                .setStartDelay(index * 30L)
                .withEndAction {
                    if (index == views.size - 1) onEnd?.invoke()
                }
                .start()
        }
    }
}