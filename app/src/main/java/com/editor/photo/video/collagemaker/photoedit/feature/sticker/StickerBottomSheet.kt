package com.editor.photo.video.collagemaker.photoedit.feature.sticker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.StickerAdapter
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetStickerBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import java.util.UUID

/**
 * StickerBottomSheet — lets the user pick an emoji sticker, then commits a [StickerLayer]
 * to [EditorSessionViewModel] (→ [ApplyStickerUseCase] → [EditorRepository] → [HistoryManager]).
 *
 * The sticker content is stored as an emoji string in [StickerLayer.emojiContent].
 * EditorEngine renders it during preview and high-res export using normalized coordinates.
 *
 * The PhotoEditor library emoji API (addEmoji) is NOT used — it was the old transient path
 * and cannot participate in undo/redo or high-res export.
 */
class StickerBottomSheet : BaseEditorBottomSheet<BottomSheetStickerBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = BottomSheetStickerBinding.inflate(inflater, container, false)

    override fun setupUI() {
        val emojis = generateEmojis().take(500)
        val adapter = StickerAdapter(emojis.map { null to it }) { _, emojiText ->
            emojiText?.let { commitStickerToEditor(it) }
        }
        binding.rvStickers.layoutManager =
            GridLayoutManager(requireContext(), 5, GridLayoutManager.VERTICAL, false)
        binding.rvStickers.adapter = adapter
    }

    /**
     * Creates a [StickerLayer] for the selected emoji and commits it to the editor.
     *
     * Default position is the visual center (0.5, 0.5) in normalized coordinates.
     * Scale 1f → approximately 10% of the canvas width (see EditorEngine.drawStickerOnBitmap).
     */
    private fun commitStickerToEditor(emoji: String) {
        val sticker = StickerLayer(
            id = UUID.randomUUID().toString(),
            emojiContent = emoji,
            resourceId = -1,
            x = 0.5f,   // normalized center X
            y = 0.5f,   // normalized center Y
            scale = 1f,
            rotation = 0f,
            alpha = 255
        )
        sessionViewModel.addSticker(sticker)
        dismiss()
    }

    private fun generateEmojis(): List<String> {
        val list = mutableListOf<String>()
        val ranges = listOf(
            0x1F600..0x1F64F, // Emoticons
            0x1F300..0x1F5FF, // Misc Symbols & Pictographs
            0x1F680..0x1F6FF, // Transport & Map
            0x2600..0x26FF,   // Misc symbols
            0x2700..0x27BF    // Dingbats
        )
        for (range in ranges) {
            for (codePoint in range) {
                if (Character.isDefined(codePoint)) {
                    list.add(String(Character.toChars(codePoint)))
                }
            }
        }
        return list
    }

    override fun setupListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance(): StickerBottomSheet {
            return StickerBottomSheet()
        }
    }
}
