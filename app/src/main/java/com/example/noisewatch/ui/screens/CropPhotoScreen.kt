package com.example.noisewatch.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.example.noisewatch.photo.PhotoStorageManager
import com.example.noisewatch.ui.theme.DeepNavyCharcoal
import com.example.noisewatch.ui.theme.MutedAmberGold
import com.example.noisewatch.ui.theme.MutedBrickRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropPhotoScreen(
    rawPhotoPath: String,
    onPhotoCropped: (String) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isCropping by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    BackHandler {
        onCancel()
    }

    LaunchedEffect(rawPhotoPath) {
        isLoading = true
        errorMessage = null
        val decoded = withContext(Dispatchers.IO) {
            loadRotatedBitmap(rawPhotoPath)
        }
        if (decoded != null) {
            bitmap = decoded
            isLoading = false
        } else {
            isLoading = false
            errorMessage = "Failed to load image. Please retake photo."
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Adjust photo",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavyCharcoal
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DeepNavyCharcoal)
                }
            } else if (errorMessage != null || bitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Error loading image",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetake) {
                                Text("Retake")
                            }
                        }
                    }
                }
            } else {
                val loadedBitmap = bitmap!!
                val density = LocalDensity.current

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    // Fixed 16:9 crop frame
                    val frameWidthPx = containerWidthPx.coerceAtMost(containerHeightPx * (16f / 9f))
                    val frameHeightPx = frameWidthPx / (16f / 9f)

                    val bitmapWidthPx = loadedBitmap.width.toFloat()
                    val bitmapHeightPx = loadedBitmap.height.toFloat()

                    // Cover scale so image fills frame
                    val baseScale = max(
                        frameWidthPx / bitmapWidthPx,
                        frameHeightPx / bitmapHeightPx
                    )

                    val maxOffsetX = max(0f, (bitmapWidthPx * baseScale * scale - frameWidthPx) / 2f)
                    val maxOffsetY = max(0f, (bitmapHeightPx * baseScale * scale - frameHeightPx) / 2f)

                    val clampedOffsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                    val clampedOffsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)

                    // Image container with gesture detection
                    Box(
                        modifier = Modifier
                            .size(
                                width = with(density) { frameWidthPx.toDp() },
                                height = with(density) { frameHeightPx.toDp() }
                            )
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    val newMaxX = max(0f, (bitmapWidthPx * baseScale * scale - frameWidthPx) / 2f)
                                    val newMaxY = max(0f, (bitmapHeightPx * baseScale * scale - frameHeightPx) / 2f)
                                    offsetX = (offsetX + pan.x).coerceIn(-newMaxX, newMaxX)
                                    offsetY = (offsetY + pan.y).coerceIn(-newMaxY, newMaxY)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = loadedBitmap.asImageBitmap(),
                            contentDescription = "Photo preview to crop",
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = baseScale * scale,
                                    scaleY = baseScale * scale,
                                    translationX = clampedOffsetX,
                                    translationY = clampedOffsetY
                                )
                        )
                    }

                    // Semi-transparent overlay outside 16:9 frame + border
                    val frameLeft = (containerWidthPx - frameWidthPx) / 2f
                    val frameTop = (containerHeightPx - frameHeightPx) / 2f
                    val amberColor = MutedAmberGold

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val overlayColor = Color.Black.copy(alpha = 0.55f)

                        // Top
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, frameTop)
                        )
                        // Bottom
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, frameTop + frameHeightPx),
                            size = Size(size.width, size.height - (frameTop + frameHeightPx))
                        )
                        // Left
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(0f, frameTop),
                            size = Size(frameLeft, frameHeightPx)
                        )
                        // Right
                        drawRect(
                            color = overlayColor,
                            topLeft = Offset(frameLeft + frameWidthPx, frameTop),
                            size = Size(size.width - (frameLeft + frameWidthPx), frameHeightPx)
                        )

                        // 16:9 frame border
                        drawRoundRect(
                            color = amberColor,
                            topLeft = Offset(frameLeft, frameTop),
                            size = Size(frameWidthPx, frameHeightPx),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Bottom Action Buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isCropping) return@Button
                                isCropping = true
                                coroutineScope.launch {
                                    val croppedPath = withContext(Dispatchers.IO) {
                                        val bitmapW = loadedBitmap.width.toFloat()
                                        val bitmapH = loadedBitmap.height.toFloat()

                                        val totalS = baseScale * scale

                                        val cropLeftInBitmapScreenPx = (bitmapW * totalS / 2f) - (frameWidthPx / 2f) - clampedOffsetX
                                        val cropTopInBitmapScreenPx = (bitmapH * totalS / 2f) - (frameHeightPx / 2f) - clampedOffsetY

                                        val cropLeft = (cropLeftInBitmapScreenPx / totalS).roundToInt().coerceIn(0, loadedBitmap.width - 1)
                                        val cropTop = (cropTopInBitmapScreenPx / totalS).roundToInt().coerceIn(0, loadedBitmap.height - 1)
                                        val cropW = (frameWidthPx / totalS).roundToInt().coerceIn(1, loadedBitmap.width - cropLeft)
                                        val cropH = (frameHeightPx / totalS).roundToInt().coerceIn(1, loadedBitmap.height - cropTop)

                                        val croppedFile = PhotoStorageManager.cropAndSavePhoto(
                                            context = context,
                                            sourceFile = File(rawPhotoPath),
                                            cropLeft = cropLeft,
                                            cropTop = cropTop,
                                            cropWidth = cropW,
                                            cropHeight = cropH
                                        )
                                        croppedFile?.absolutePath
                                    }

                                    isCropping = false
                                    if (croppedPath != null) {
                                        onPhotoCropped(croppedPath)
                                    } else {
                                        errorMessage = "Failed to crop image. Please retake photo."
                                    }
                                }
                            },
                            enabled = !isCropping,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutedAmberGold,
                                contentColor = DeepNavyCharcoal
                            )
                        ) {
                            if (isCropping) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = DeepNavyCharcoal,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "USE PHOTO",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = onRetake,
                                enabled = !isCropping,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Retake",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepNavyCharcoal
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            TextButton(
                                onClick = onCancel,
                                enabled = !isCropping,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MutedBrickRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadRotatedBitmap(filePath: String): Bitmap? {
    val file = File(filePath)
    if (!file.exists() || file.length() == 0L) return null

    return try {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        val maxDim = 2560
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        var bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        if (!matrix.isIdentity) {
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}
