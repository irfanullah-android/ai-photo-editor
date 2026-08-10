package com.example.collage_maker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BorderStyle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.collage_maker.CollageBottomBarScope
import com.example.collage_maker.CollageTopBarScope
import com.example.collage_maker.collage.TemplateProvider
import com.example.collage_maker.model.CollageState
import com.example.collage_maker.model.CollageTemplate
import com.example.collage_maker.ui.theme.AccentCyan
import com.example.collage_maker.ui.theme.AccentGreen
import com.example.collage_maker.ui.theme.AccentPink
import com.example.collage_maker.ui.theme.AccentPurple
import com.example.collage_maker.ui.theme.AccentViolet
import com.example.collage_maker.ui.theme.DarkCard
import com.example.collage_maker.ui.theme.DarkSurfaceVariant
import com.example.collage_maker.ui.theme.GradientEnd
import com.example.collage_maker.ui.theme.GradientStart
import com.example.collage_maker.ui.theme.TextPrimary
import com.example.collage_maker.ui.theme.TextSecondary
import com.example.collage_maker.ui.theme.ThemeDark
import com.example.collage_maker.viewmodel.EditorUiState

// ─────────────────────────────────────────────────────────────
// EditorScreen  (simple version — Save button)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EditorScreen(
    uiState: EditorUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Int) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onTemplateChange: (CollageTemplate) -> Unit,
    onNavigateToReplaceImage: (Int) -> Unit,
    onSlotTransform: (slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var selectedTool    by remember { mutableStateOf<EditorTool>(EditorTool.Frame) }
    var showColorPicker by remember { mutableStateOf(false) }

    val collageState = uiState.collageState

    Scaffold(
        containerColor = ThemeDark,
        topBar = {
            TopAppBar(
                title          = { Text("Edit Collage", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isGenerating) {
                        if (uiState.isGenerating)
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AccentViolet
                            )
                        else
                            Text("Save", color = AccentViolet, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (collageState != null) {
                    CollagePreview(
                        collageState    = collageState,
                        onSwapImages    = onSwapImages,
                        onSlotClick     = { slotIndex ->
                            if (!collageState.images.any { it.slotIndex == slotIndex })
                                onNavigateToReplaceImage(slotIndex)
                        },
                        onLongPressSlot = { onNavigateToReplaceImage(it) },
                        onSlotTransform = onSlotTransform
                    )
                }
            }

            ToolSelector(selectedTool = selectedTool, onToolSelected = { selectedTool = it })
            Spacer(modifier = Modifier.height(8.dp))

            if (collageState != null) {
                ToolOptions(
                    tool                = selectedTool,
                    collageState        = collageState,
                    onBorderWidthChange = onBorderWidthChange,
                    onBorderColorClick  = { showColorPicker = true },
                    onCornerRadiusChange = onCornerRadiusChange,
                    onTemplateSelected  = { onTemplateChange(it) },
                    currentTemplate     = collageState.template
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor    = collageState?.borderColor ?: 0xFFFFFFFF.toInt(),
            onColorSelected = { color -> onBorderColorChange(color); showColorPicker = false },
            onDismiss       = { showColorPicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CollagePreview  (used in EditorScreen)
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollagePreview(
    collageState: CollageState,
    onSwapImages: (Int, Int) -> Unit,
    onSlotClick: (Int) -> Unit,
    onLongPressSlot: (Int) -> Unit,
    onSlotTransform: (slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<Int?>(null) }

    val padding     = collageState.borderWidth.dp
    val halfPadding = padding / 2

    Card(
        modifier  = Modifier.fillMaxWidth().aspectRatio(1f),
        shape     = RoundedCornerShape(collageState.cornerRadius.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(collageState.borderColor))
                .padding(halfPadding)
        ) {
            val totalWidth  = this.maxWidth
            val totalHeight = this.maxHeight

            collageState.template.slots.forEachIndexed { index, slot ->
                val slotImage  = collageState.images.find { it.slotIndex == index }
                val isSelected = selectedSlot == index
                val hasImage   = slotImage != null

                val slotLeft   = totalWidth  * slot.left   + halfPadding
                val slotTop    = totalHeight * slot.top    + halfPadding
                val slotWidth  = totalWidth  * slot.width  - padding
                val slotHeight = totalHeight * slot.height - padding

                Box(
                    modifier = Modifier
                        .offset(x = slotLeft, y = slotTop)
                        .size(width = slotWidth, height = slotHeight)
                        .clip(RoundedCornerShape((collageState.cornerRadius / 2).dp))
                        .background(Color(collageState.backgroundColor))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) AccentViolet else Color.Transparent,
                            shape = RoundedCornerShape((collageState.cornerRadius / 2).dp)
                        )
                        .combinedClickable(
                            onClick = {
                                when {
                                    !hasImage             -> onSlotClick(index)
                                    selectedSlot == null  -> selectedSlot = index
                                    selectedSlot == index -> selectedSlot = null
                                    else -> { onSwapImages(selectedSlot!!, index); selectedSlot = null }
                                }
                            },
                            onLongClick = { onLongPressSlot(index) }
                        )
                ) {
                    if (slotImage != null) {
                        ZoomableSlotImage(
                            slotImage   = slotImage,
                            onTransform = { s, ox, oy -> onSlotTransform(index, s, ox, oy) }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = Icons.Default.Add,
                                contentDescription = "Add photo",
                                tint               = Color.White.copy(alpha = 0.5f),
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(AccentViolet.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, "Swap", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Tool enums + selectors
// ─────────────────────────────────────────────────────────────
private enum class EditorTool { Frame, Border, Corners }

@Composable
private fun ToolSelector(selectedTool: EditorTool, onToolSelected: (EditorTool) -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToolButton(Icons.Default.GridView,      "Frame",   selectedTool == EditorTool.Frame)   { onToolSelected(EditorTool.Frame) }
        ToolButton(Icons.Default.BorderStyle,   "Border",  selectedTool == EditorTool.Border)  { onToolSelected(EditorTool.Border) }
        ToolButton(Icons.Default.RoundedCorner, "Corners", selectedTool == EditorTool.Corners) { onToolSelected(EditorTool.Corners) }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) AccentViolet.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(icon, label, tint = if (isSelected) AccentViolet else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AccentViolet else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────
// ToolOptions panel
// ─────────────────────────────────────────────────────────────
@Composable
private fun ToolOptions(
    tool: EditorTool,
    collageState: CollageState,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorClick: () -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onTemplateSelected: (CollageTemplate) -> Unit,
    currentTemplate: CollageTemplate
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors   = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        shape    = RoundedCornerShape(16.dp)
    ) {
        when (tool) {
            EditorTool.Frame -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Choose Frame", color = TextPrimary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(TemplateProvider.getAllTemplates()) { template ->
                            FrameTemplateItem(
                                template   = template,
                                isSelected = template.id == currentTemplate.id,
                                onClick    = { onTemplateSelected(template) }
                            )
                        }
                    }
                }
            }

            EditorTool.Border -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Width", color = TextPrimary)
                        Text("${collageState.borderWidth.toInt()}px", color = TextSecondary)
                    }
                    Slider(
                        value         = collageState.borderWidth,
                        onValueChange = onBorderWidthChange,
                        valueRange    = 0f..30f,
                        colors        = SliderDefaults.colors(thumbColor = AccentViolet, activeTrackColor = AccentViolet)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Color", color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(collageState.borderColor))
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable { onBorderColorClick() }
                        )
                    }
                }
            }

            EditorTool.Corners -> {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text("Radius", color = TextPrimary)
                        Text("${collageState.cornerRadius.toInt()}dp", color = TextSecondary)
                    }
                    Slider(
                        value         = collageState.cornerRadius,
                        onValueChange = onCornerRadiusChange,
                        valueRange    = 0f..48f,
                        colors        = SliderDefaults.colors(thumbColor = AccentViolet, activeTrackColor = AccentViolet)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// FrameTemplateItem
// ─────────────────────────────────────────────────────────────
@Composable
private fun FrameTemplateItem(template: CollageTemplate, isSelected: Boolean, onClick: () -> Unit) {
    val slotColors = listOf(
        AccentViolet.copy(alpha = 0.6f), AccentPurple.copy(alpha = 0.6f),
        AccentPink.copy(alpha = 0.6f),   AccentCyan.copy(alpha = 0.6f),
        AccentGreen.copy(alpha = 0.6f),  GradientEnd.copy(alpha = 0.6f)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(70.dp).clickable { onClick() },
            shape    = RoundedCornerShape(12.dp),
            colors   = CardDefaults.cardColors(
                containerColor = if (isSelected) AccentViolet.copy(alpha = 0.2f) else DarkCard
            ),
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, AccentViolet)
            else null
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val w = this.maxWidth
                val h = this.maxHeight

                template.slots.forEachIndexed { idx, slot ->
                    Box(
                        modifier = Modifier
                            .offset(x = w * slot.left, y = h * slot.top)
                            .size(width = w * slot.width - 2.dp, height = h * slot.height - 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(slotColors[idx % slotColors.size])
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${template.imageCount}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AccentViolet else TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────
// ColorPickerDialog
// ─────────────────────────────────────────────────────────────
@Composable
private fun ColorPickerDialog(currentColor: Int, onColorSelected: (Int) -> Unit, onDismiss: () -> Unit) {
    val presetColors = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF1A1A2E.toInt(), 0xFF16213E.toInt(),
        0xFF667EEA.toInt(), 0xFF764BA2.toInt(), 0xFFFF6B9D.toInt(), 0xFF00D4FF.toInt(),
        0xFF00D9A5.toInt(), 0xFFFFD93D.toInt(), 0xFFFF6B35.toInt(), 0xFFE63946.toInt(),
        0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt(), 0xFF84CC16.toInt(), 0xFFF97316.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Choose Color") },
        text = {
            Column {
                presetColors.chunked(4).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp).padding(4.dp).clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (color == currentColor) 3.dp else 1.dp,
                                        color = if (color == currentColor) AccentViolet
                                        else Color.White.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(color) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────
// EditorScreenWithCustomBars
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EditorScreenWithCustomBars(
    uiState: EditorUiState,
    topBar: (@Composable CollageTopBarScope.() -> Unit)?,
    bottomBar: (@Composable CollageBottomBarScope.() -> Unit)?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Int) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onTemplateChange: (CollageTemplate) -> Unit,
    onNavigateToReplaceImage: (Int) -> Unit,
    onSlotTransform: (slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var selectedTool    by remember { mutableStateOf<EditorTool>(EditorTool.Frame) }
    var showColorPicker by remember { mutableStateOf(false) }

    val collageState = uiState.collageState

    val topBarScope = remember(collageState, uiState.isGenerating) {
        CollageTopBarScope(collageState = collageState, onBack = onBack, onDone = onDone, isGenerating = uiState.isGenerating)
    }
    val bottomBarScope = remember(collageState, uiState.isGenerating) {
        CollageBottomBarScope(collageState = collageState, onDone = onDone, isGenerating = uiState.isGenerating)
    }

    Scaffold(
        containerColor = ThemeDark,
        topBar = {
            if (topBar != null) {
                topBarScope.topBar()
            } else {
                TopAppBar(
                    title          = { Text("Edit Collage", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                    actions = {
                        TextButton(onClick = onDone, enabled = !uiState.isGenerating) {
                            if (uiState.isGenerating)
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentViolet
                                )
                            else
                                Text("Done", color = AccentViolet, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors   = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            }
        },
        bottomBar = { if (bottomBar != null) bottomBarScope.bottomBar() }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (collageState != null) {
                    CollagePreviewEditable(
                        collageState    = collageState,
                        onSwapImages    = onSwapImages,
                        onSlotClick     = { slotIndex ->
                            if (!collageState.images.any { it.slotIndex == slotIndex })
                                onNavigateToReplaceImage(slotIndex)
                        },
                        onLongPressSlot = { onNavigateToReplaceImage(it) },
                        onSlotTransform = onSlotTransform
                    )
                }
            }

            ToolSelector(selectedTool = selectedTool, onToolSelected = { selectedTool = it })
            Spacer(modifier = Modifier.height(8.dp))

            if (collageState != null) {
                ToolOptions(
                    tool                = selectedTool,
                    collageState        = collageState,
                    onBorderWidthChange = onBorderWidthChange,
                    onBorderColorClick  = { showColorPicker = true },
                    onCornerRadiusChange = onCornerRadiusChange,
                    onTemplateSelected  = { onTemplateChange(it) },
                    currentTemplate     = collageState.template
                )
            }

            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor    = collageState?.borderColor ?: 0xFFFFFFFF.toInt(),
            onColorSelected = { color -> onBorderColorChange(color); showColorPicker = false },
            onDismiss       = { showColorPicker = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CollagePreviewEditable
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollagePreviewEditable(
    collageState: CollageState,
    onSwapImages: (Int, Int) -> Unit,
    onSlotClick: (Int) -> Unit,
    onLongPressSlot: (Int) -> Unit,
    onSlotTransform: (slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<Int?>(null) }

    val padding     = collageState.borderWidth.dp
    val halfPadding = padding / 2

    Card(
        modifier  = Modifier.fillMaxWidth().aspectRatio(1f),
        shape     = RoundedCornerShape(collageState.cornerRadius.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(collageState.borderColor))
                .padding(halfPadding)
        ) {
            val totalWidth  = this.maxWidth
            val totalHeight = this.maxHeight

            collageState.template.slots.forEachIndexed { index, slot ->
                val slotImage  = collageState.images.find { it.slotIndex == index }
                val isSelected = selectedSlot == index
                val hasImage   = slotImage != null

                val slotLeft   = totalWidth  * slot.left   + halfPadding
                val slotTop    = totalHeight * slot.top    + halfPadding
                val slotWidth  = totalWidth  * slot.width  - padding
                val slotHeight = totalHeight * slot.height - padding

                Box(
                    modifier = Modifier
                        .offset(x = slotLeft, y = slotTop)
                        .size(width = slotWidth, height = slotHeight)
                        .clip(RoundedCornerShape((collageState.cornerRadius / 2).dp))
                        .background(Color(collageState.backgroundColor))
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) AccentViolet else Color.Transparent,
                            shape = RoundedCornerShape((collageState.cornerRadius / 2).dp)
                        )
                        .combinedClickable(
                            onClick = {
                                when {
                                    !hasImage             -> onSlotClick(index)
                                    selectedSlot == null  -> selectedSlot = index
                                    selectedSlot == index -> selectedSlot = null
                                    else -> { onSwapImages(selectedSlot!!, index); selectedSlot = null }
                                }
                            },
                            onLongClick = { onLongPressSlot(index) }
                        )
                ) {
                    if (slotImage != null) {
                        ZoomableSlotImage(
                            slotImage   = slotImage,
                            onTransform = { s, ox, oy -> onSlotTransform(index, s, ox, oy) }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = Icons.Default.Add,
                                contentDescription = "Add photo",
                                tint               = Color.White.copy(alpha = 0.5f),
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier         = Modifier.fillMaxSize().background(AccentViolet.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapHoriz, "Swap", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        }
    }
}