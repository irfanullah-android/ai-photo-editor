package com.example.collage_maker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.clip
import coil.size.Scale
import coil.size.Precision
import com.example.collage_maker.GalleryBottomBarScope
import com.example.collage_maker.GalleryTopBarScope
import com.example.collage_maker.model.GalleryImage
import com.example.collage_maker.ui.theme.AccentViolet
import com.example.collage_maker.ui.theme.ThemeDark
import com.example.collage_maker.viewmodel.GalleryViewModel
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryScreen(
    templateId: String,
    imageCount: Int,
    topBar: (@Composable GalleryTopBarScope.() -> Unit)? = null,
    bottomBar: (@Composable GalleryBottomBarScope.() -> Unit)? = null,
    onImagesSelected: (List<GalleryImage>) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    // Lifecycle observer — agar user settings se permission change kare toh resume par detect ho jaye
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission true hote hi data fetch karega
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.initialize(templateId, imageCount)
        }
    }

    // Pehli baar screen khulne par permission launch karega agar granted nahi hai
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(permission)
        }
    }

    val isComplete = uiState.selectedImages.size == uiState.requiredCount

    val topBarScope = remember(uiState.selectedImages.size, uiState.requiredCount) {
        GalleryTopBarScope(
            selectedCount = uiState.selectedImages.size,
            requiredCount = uiState.requiredCount,
            onBack = onBack
        )
    }

    // Key mein size ke bajaye pure selectImages list ko pass kiya taaki confirm button hamesha sahi/fresh state laye
    val bottomBarScope = remember(uiState.selectedImages, uiState.requiredCount, isComplete) {
        GalleryBottomBarScope(
            selectedCount = uiState.selectedImages.size,
            requiredCount = uiState.requiredCount,
            isComplete = isComplete,
            onConfirm = { onImagesSelected(uiState.selectedImages) }
        )
    }

    Scaffold(
        containerColor = ThemeDark,
        contentColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeDark)
                    .padding(top = topSafeInset())
            ) {
                if (topBar != null) {
                    topBarScope.topBar()
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Select Photos",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "${uiState.selectedImages.size} of ${uiState.requiredCount} selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.selectedImages.size == uiState.requiredCount)
                                        AccentViolet
                                    else
                                        Color.LightGray
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        windowInsets = WindowInsets(0.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (bottomBar != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeDark)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    bottomBarScope.bottomBar()
                }
            } else {
                Surface(
                    color = ThemeDark,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { onImagesSelected(uiState.selectedImages) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = isComplete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentViolet,
                                disabledContainerColor = AccentViolet.copy(alpha = 0.3f),
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isComplete) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                if (isComplete)
                                    "Create Collage"
                                else
                                    "Select ${uiState.requiredCount - uiState.selectedImages.size} more photo(s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            !permissionGranted -> {
                PermissionRequestContent(
                    onRequestPermission = { permissionLauncher.launch(permission) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentViolet)
                }
            }
            uiState.images.isEmpty() -> {
                EmptyGalleryContent(modifier = Modifier.padding(paddingValues))
            }
            else -> {
                val selectedMap = remember(uiState.selectedImages) {
                    uiState.selectedImages
                        .mapIndexed { index, img -> img.id to (index + 1) }
                        .toMap()
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = paddingValues,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.images, key = { it.id }) { image ->
                        val selectionIndex = selectedMap[image.id] ?: 0

                        // onClick lambda ko cache/remember kiya taaki naya instance na baney
                        val onClick = remember(image) {
                            { viewModel.toggleImageSelection(image) }
                        }

                        // Stable primitive keys pass kiye taaki non-selected items faltu recompose na hon
                        GalleryImageItem(
                            imageId = image.id,
                            imageUri = image.uri,
                            displayName = image.displayName,
                            isSelected = selectionIndex != 0,
                            selectionIndex = selectionIndex,
                            canSelect = uiState.selectedImages.size < uiState.requiredCount,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun topSafeInset(): Dp {
    // Native WindowInsets API layout aur status bar glitch ko khatam kar deti hai
    return WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateTopPadding()
}

@Composable
private fun GalleryImageItem(
    imageId: Long,
    imageUri: android.net.Uri,
    displayName: String,
    isSelected: Boolean,
    selectionIndex: Int,
    canSelect: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val request = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(200, 200) // Optimal thumbnail size
            .scale(Scale.FILL)
            .precision(Precision.INEXACT) // Image processing speed aur scroll smoothness ke liye crucial hai
            .crossfade(false)
            .allowHardware(true)
            .memoryCacheKey("thumb_${imageId}")
            .diskCacheKey("thumb_${imageId}")
            .build()
    }

    Box(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.DarkGray)
            .clickable(enabled = canSelect || isSelected) { onClick() }
    ) {
        AsyncImage(
            model = request,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(Color.DarkGray),
            error = ColorPainter(Color.DarkGray),
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AccentViolet.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(AccentViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectionIndex.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (!canSelect && !isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "We need access to your photos to create collages.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)
        ) {
            Text("Grant Permission", color = Color.White)
        }
    }
}

@Composable
private fun EmptyGalleryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No Photos Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Add some photos to your device to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray
        )
    }
}