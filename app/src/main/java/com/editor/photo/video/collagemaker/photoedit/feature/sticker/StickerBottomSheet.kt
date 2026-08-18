package com.editor.photo.video.collagemaker.photoedit.feature.sticker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.StickerAdapter
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetStickerBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.UUID

class StickerBottomSheet : BaseEditorBottomSheet<BottomSheetStickerBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    data class EmojiCategory(
        val icon: String,
        val label: String,
        val ranges: List<IntRange>
    )

    private val categories = listOf(
        EmojiCategory("😀", "Smileys", listOf(0x1F600..0x1F64F)),
        EmojiCategory("🐶", "Animals", listOf(0x1F400..0x1F43F)),
        EmojiCategory("🍎", "Food",    listOf(0x1F32D..0x1F37F, 0x1F950..0x1F96F)),
        EmojiCategory("🚗", "Travel",  listOf(0x1F680..0x1F6FF)),
        EmojiCategory("⚽", "Sport",   listOf(0x1F3C0..0x1F3FF)),
        EmojiCategory("💡", "Objects", listOf(0x1F4A1..0x1F4FF)),
        EmojiCategory("🔣", "Symbols", listOf(0x1F500..0x1F5FF)),
        EmojiCategory("🏳", "Flags",   listOf(0x1F1E0..0x1F1FF))
    )

    private var selectedIndex = 0
    private val tabViews = mutableListOf<LinearLayout>()
    private lateinit var stickerAdapter: StickerAdapter

    // ── Transparent Dialog (corners fix) ─────────────────────────────────────
    override fun onCreateDialog(savedInstanceState: android.os.Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<android.view.View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.background = ColorDrawable(Color.TRANSPARENT)
        }
        return dialog
    }

    // ── Binding ───────────────────────────────────────────────────────────────
    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetStickerBinding.inflate(inflater, container, false)

    // ── Setup ─────────────────────────────────────────────────────────────────
    override fun setupUI() {
        buildCategoryTabs()
        setupGrid()
        selectCategory(0)
    }

    private fun buildCategoryTabs() {
        val tabSize = (40 * resources.displayMetrics.density).toInt()
        val dp3 = (3 * resources.displayMetrics.density).toInt()

        categories.forEachIndexed { index, cat ->

            // ── Emoji icon only (no label) ──
            val tvIcon = TextView(requireContext()).apply {
                text = cat.icon
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
            }

            // ── Tab container (small fixed circle/pill) ──
            val tab = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_tab_unselected)

                layoutParams = LinearLayout.LayoutParams(
                    tabSize,
                    tabSize
                ).apply { setMargins(dp3, 0, dp3, 0) }

                addView(tvIcon)
                setOnClickListener { selectCategory(index) }
            }

            binding.llCategories.addView(tab)
            tabViews.add(tab)
        }

    }

    private fun setupGrid() {
        stickerAdapter = StickerAdapter(emptyList()) { _, emoji ->
            emoji?.let { commitStickerToEditor(it) }
        }
        binding.rvStickers.apply {
            layoutManager = GridLayoutManager(requireContext(), 6)
            adapter = stickerAdapter
            itemAnimator = null // no flicker on update
        }
    }

    // ── Category Selection ────────────────────────────────────────────────────
    private fun selectCategory(index: Int) {
        selectedIndex = index

        tabViews.forEachIndexed { i, tab ->
            if (i == index) {
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                animateTab(tab, scaleUp = true)
            } else {
                tab.setBackgroundResource(R.drawable.bg_tab_unselected)
                animateTab(tab, scaleUp = false)
            }
        }

        val emojis = getEmojisFor(categories[index])
        stickerAdapter.updateData(emojis.map { null to it })
        binding.rvStickers.scrollToPosition(0)
    }

    private fun animateTab(tab: LinearLayout, scaleUp: Boolean) {
        val target = if (scaleUp) 1.08f else 1f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(tab, "scaleX", target),
                ObjectAnimator.ofFloat(tab, "scaleY", target)
            )
            duration = 180
            start()
        }
    }

    private fun getEmojisFor(cat: EmojiCategory): List<String> {
        val list = mutableListOf<String>()
        for (range in cat.ranges) {
            for (cp in range) {
                if (Character.isDefined(cp)) {
                    list.add(String(Character.toChars(cp)))
                }
            }
        }
        return list
    }

    // ── Commit Sticker ────────────────────────────────────────────────────────
    private fun commitStickerToEditor(emoji: String) {
        sessionViewModel.addSticker(
            StickerLayer(
                id = UUID.randomUUID().toString(),
                emojiContent = emoji,
                resourceId = -1,
                x = 0.5f,
                y = 0.5f,
                scale = 1f,
                rotation = 0f,
                alpha = 255
            )
        )
        dismiss()
    }

    override fun setupListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance() = StickerBottomSheet()
    }
}