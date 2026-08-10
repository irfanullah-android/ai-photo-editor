package com.editor.photo.video.collagemaker.photoedit.fragments.basefragments
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.editor.photo.video.collagemaker.photoedit.BuildConfig
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.helpers.FirebaseUtils.recordException

open class GeneralFragment : Fragment() {
    protected fun withDelay(delay: Long = 300, block: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(block, delay)
    }
    protected fun getResString(stringId: Int): String {
        return context?.resources?.getString(stringId) ?: ""
    }

    /* ---------- Toast ---------- */
    protected open fun showToast(message: String) {
        activity?.let {
            try {
                it.runOnUiThread {
                    Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
                }
            } catch (ex: Exception) {
                ex.recordException("showToast : ${it.javaClass.simpleName}")
            }
        }
    }

    protected fun debugToast(message: String) {
        if (BuildConfig.DEBUG) {
            showToast(message)
        }
    }

    protected fun showToast(stringId: Int) {
        val message = getResString(stringId)
        showToast(message)
    }


    /* ---------- Snack-Bar ---------- */

    @SuppressLint("RestrictedApi")
    protected fun showSnackBar(message: String) {
        this.view?.let { v ->
            activity?.let { activity ->
                try {
                    activity.runOnUiThread {
                        val rootView = v.rootView
                        val inflater = LayoutInflater.from(context)
                        val customView = inflater.inflate(R.layout.custom_snackbar, null)
                        customView.findViewById<TextView>(R.id.snackBarText).text = message
                        val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_SHORT)
                        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
                        snackbarLayout.setPadding(0, 0, 0, 0)
                        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
                        val layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            40.dpToPx()
                        )
                        customView.layoutParams = layoutParams
                        snackbarLayout.addView(customView, 0)

                        val snackbarView = snackbar.view
                        val params = snackbarView.layoutParams as ViewGroup.MarginLayoutParams
                        params.setMargins(50, 0, 50, 0.dpToPx())
                        snackbarView.layoutParams = params

                        snackbar.animationMode = Snackbar.ANIMATION_MODE_FADE
                        snackbar.show()

                        // Animation for entry
                        snackbarView.translationY = 300f
                        snackbarView.animate().translationY(0f).setDuration(500).start()

                        // Animation for exit after delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            snackbarView.animate().translationY(300f).setDuration(500)
                                .withEndAction {
                                    snackbar.dismiss()
                                }.start()
                        }, 1500)
                    }
                } catch (ex: Exception) {
                    ex.recordException("showCustomSnackBar : ${activity.javaClass.simpleName}")
                }
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }


    /* ---------- Keyboard (Input Method) ---------- */
    protected open fun hideKeyboard() {
        try {
            val inputMethodManager: InputMethodManager =
                context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            val view: IBinder? = activity?.findViewById<View?>(android.R.id.content)?.windowToken
            inputMethodManager.hideSoftInputFromWindow(view, 0)
        } catch (ex: Exception) {
            ex.recordException("hideKeyboard")
        }
    }

    /* ---------- Share Message ---------- */

    protected fun shareMessage(message: String) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (ex: Exception) {
            ex.recordException("shareMessage")
        }
    }
}