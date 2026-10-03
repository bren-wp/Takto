package hr.takto.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.takto.app.ui.theme.TaktoBlue
import kotlin.math.abs

internal data class CropSelection(
    val left: Float = 0.04f,
    val top: Float = 0.42f,
    val right: Float = 0.96f,
    val bottom: Float = 0.58f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

internal enum class CropDragMode { NONE, MOVE, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

@Composable
fun RosterImageEditorDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    var bitmap by remember(sourceBitmap) { mutableStateOf(sourceBitmap) }
    var crop by remember(bitmap) { mutableStateOf(CropSelection()) }
    var dragMode by remember { mutableStateOf(CropDragMode.NONE) }
    val density = LocalDensity.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Odaberi samo svoj raspored") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pomakni okvir gore, dolje, lijevo ili desno. Obuhvati zaglavlje s datumima i samo svoj red, a druge osobe ostavi izvan okvira.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
                    val maxEditorHeight = 430.dp
                    val naturalHeight = maxWidth / ratio
                    val editorHeight = minOf(naturalHeight, maxEditorHeight)
                    val editorWidth = if (naturalHeight <= maxEditorHeight) maxWidth else maxEditorHeight * ratio
                    val widthPx = with(density) { editorWidth.toPx() }.coerceAtLeast(1f)
                    val heightPx = with(density) { editorHeight.toPx() }.coerceAtLeast(1f)
                    val handleRadiusPx = with(density) { 26.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .width(editorWidth)
                            .height(editorHeight)
                            .align(Alignment.Center)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Fotografija rasporeda",
                            modifier = Modifier.matchParentSize()
                        )

                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(bitmap, crop) {
                                    detectDragGestures(
                                        onDragStart = { point ->
                                            dragMode = cropModeForPoint(
                                                point = point,
                                                crop = crop,
                                                widthPx = widthPx,
                                                heightPx = heightPx,
                                                handleRadiusPx = handleRadiusPx
                                            )
                                        },
                                        onDragEnd = { dragMode = CropDragMode.NONE },
                                        onDragCancel = { dragMode = CropDragMode.NONE },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            val dx = drag.x / widthPx
                                            val dy = drag.y / heightPx
                                            crop = updateCrop(crop, dragMode, dx, dy)
                                        }
                                    )
                                }
                        ) {
                            val l = crop.left * size.width
                            val t = crop.top * size.height
                            val r = crop.right * size.width
                            val b = crop.bottom * size.height
                            val overlay = Color.Black.copy(alpha = 0.58f)

                            drawRect(overlay, Offset.Zero, Size(size.width, t))
                            drawRect(overlay, Offset(0f, b), Size(size.width, size.height - b))
                            drawRect(overlay, Offset(0f, t), Size(l, b - t))
                            drawRect(overlay, Offset(r, t), Size(size.width - r, b - t))

                            drawRect(
                                color = Color.White,
                                topLeft = Offset(l, t),
                                size = Size(r - l, b - t),
                                style = Stroke(width = 3.dp.toPx())
                            )

                            val handleRadius = 7.dp.toPx()
                            listOf(
                                Offset(l, t),
                                Offset((l + r) / 2f, t),
                                Offset(r, t),
                                Offset(l, (t + b) / 2f),
                                Offset(r, (t + b) / 2f),
                                Offset(l, b),
                                Offset((l + r) / 2f, b),
                                Offset(r, b)
                            ).forEach { center ->
                                drawCircle(Color.White, radius = handleRadius, center = center)
                                drawCircle(TaktoBlue, radius = handleRadius * 0.55f, center = center)
                            }
                        }
                    }
                }

                Text(
                    "Pomakni označeno područje",
                    fontWeight = FontWeight.SemiBold
                )
                listOf(
                    "Lijevo" to Pair(Icons.AutoMirrored.Filled.ArrowBack, Offset(-0.02f, 0f)),
                    "Gore" to Pair(Icons.Default.ArrowUpward, Offset(0f, -0.02f)),
                    "Dolje" to Pair(Icons.Default.ArrowDownward, Offset(0f, 0.02f)),
                    "Desno" to Pair(Icons.AutoMirrored.Filled.ArrowForward, Offset(0.02f, 0f))
                ).chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { (label, action) ->
                            EditorActionButton(
                                modifier = Modifier.weight(1f),
                                label = label,
                                icon = action.first,
                                onClick = {
                                    crop = updateCrop(
                                        crop,
                                        CropDragMode.MOVE,
                                        action.second.x,
                                        action.second.y
                                    )
                                }
                            )
                        }
                    }
                }

                Text(
                    "Zakretanje i brzi odabir",
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorActionButton(
                        modifier = Modifier.weight(1f),
                        label = "−90°",
                        icon = Icons.AutoMirrored.Filled.RotateLeft,
                        onClick = {
                            bitmap = rotateBitmap(bitmap, -90f)
                            crop = CropSelection()
                        }
                    )
                    EditorActionButton(
                        modifier = Modifier.weight(1f),
                        label = "+90°",
                        icon = Icons.AutoMirrored.Filled.RotateRight,
                        onClick = {
                            bitmap = rotateBitmap(bitmap, 90f)
                            crop = CropSelection()
                        }
                    )
                }
                EditorActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Vrati okvir na jedan red",
                    icon = Icons.Default.RestartAlt,
                    onClick = { crop = CropSelection() }
                )

                Button(
                    onClick = { crop = CropSelection(0f, 0f, 1f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, tint = TaktoBlue)
                    Spacer(Modifier.size(6.dp))
                    Text("Označi cijelu sliku", color = TaktoBlue, fontWeight = FontWeight.Bold)
                }

                Text(
                    "Povuci kutove ili sredinu ruba za precizno sužavanje/širenje okvira. Skeniranje će obraditi samo označeno područje, zato provjeri da druge osobe nisu unutar okvira.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cropBitmap(bitmap, crop)) },
                colors = ButtonDefaults.buttonColors(containerColor = TaktoBlue)
            ) {
                Text("Skeniraj označeno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Natrag") }
        }
    )
}

private fun cropModeForPoint(
    point: Offset,
    crop: CropSelection,
    widthPx: Float,
    heightPx: Float,
    handleRadiusPx: Float
): CropDragMode {
    val l = crop.left * widthPx
    val t = crop.top * heightPx
    val r = crop.right * widthPx
    val b = crop.bottom * heightPx

    fun near(x: Float, y: Float): Boolean =
        abs(point.x - x) <= handleRadiusPx && abs(point.y - y) <= handleRadiusPx

    return when {
        near(l, t) -> CropDragMode.TOP_LEFT
        near(r, t) -> CropDragMode.TOP_RIGHT
        near(l, b) -> CropDragMode.BOTTOM_LEFT
        near(r, b) -> CropDragMode.BOTTOM_RIGHT
        abs(point.y - t) <= handleRadiusPx && point.x in l..r -> CropDragMode.TOP
        abs(point.y - b) <= handleRadiusPx && point.x in l..r -> CropDragMode.BOTTOM
        abs(point.x - l) <= handleRadiusPx && point.y in t..b -> CropDragMode.LEFT
        abs(point.x - r) <= handleRadiusPx && point.y in t..b -> CropDragMode.RIGHT
        point.x in l..r && point.y in t..b -> CropDragMode.MOVE
        else -> CropDragMode.NONE
    }
}

internal fun updateCrop(
    current: CropSelection,
    mode: CropDragMode,
    dx: Float,
    dy: Float
): CropSelection {
    val minWidth = 0.10f
    val minHeight = 0.08f
    return when (mode) {
        CropDragMode.MOVE -> {
            val left = (current.left + dx).coerceIn(0f, 1f - current.width)
            val top = (current.top + dy).coerceIn(0f, 1f - current.height)
            current.copy(
                left = left,
                right = left + current.width,
                top = top,
                bottom = top + current.height
            )
        }
        CropDragMode.LEFT -> current.copy(
            left = (current.left + dx).coerceIn(0f, current.right - minWidth)
        )
        CropDragMode.RIGHT -> current.copy(
            right = (current.right + dx).coerceIn(current.left + minWidth, 1f)
        )
        CropDragMode.TOP -> current.copy(
            top = (current.top + dy).coerceIn(0f, current.bottom - minHeight)
        )
        CropDragMode.BOTTOM -> current.copy(
            bottom = (current.bottom + dy).coerceIn(current.top + minHeight, 1f)
        )
        CropDragMode.TOP_LEFT -> current.copy(
            left = (current.left + dx).coerceIn(0f, current.right - minWidth),
            top = (current.top + dy).coerceIn(0f, current.bottom - minHeight)
        )
        CropDragMode.TOP_RIGHT -> current.copy(
            right = (current.right + dx).coerceIn(current.left + minWidth, 1f),
            top = (current.top + dy).coerceIn(0f, current.bottom - minHeight)
        )
        CropDragMode.BOTTOM_LEFT -> current.copy(
            left = (current.left + dx).coerceIn(0f, current.right - minWidth),
            bottom = (current.bottom + dy).coerceIn(current.top + minHeight, 1f)
        )
        CropDragMode.BOTTOM_RIGHT -> current.copy(
            right = (current.right + dx).coerceIn(current.left + minWidth, 1f),
            bottom = (current.bottom + dy).coerceIn(current.top + minHeight, 1f)
        )
        CropDragMode.NONE -> current
    }
}

private fun cropBitmap(bitmap: Bitmap, crop: CropSelection): Bitmap {
    val left = (crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val top = (crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val right = (crop.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
    val bottom = (crop.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}

@Composable
private fun EditorActionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TaktoBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(3.dp))
        Text(
            label,
            color = TaktoBlue,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
