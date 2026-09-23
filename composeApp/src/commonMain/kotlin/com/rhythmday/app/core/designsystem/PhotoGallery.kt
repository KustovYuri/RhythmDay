package com.rhythmday.app.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun PhotoGallery(
    photos: List<ByteArray>,
    thumbnailWidth: Dp,
    thumbnailHeight: Dp,
    onRemove: ((Int) -> Unit)? = null
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PhotoSpacing)
        ) {
            itemsIndexed(photos) { index, bytes ->
                Box(
                    modifier = Modifier
                        .width(thumbnailWidth)
                        .height(thumbnailHeight)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE9F4F2))
                ) {
                    PhotoImage(
                        bytes = bytes,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clickable { selectedIndex = index }
                    )
                    if (onRemove != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xBB19272D))
                                .clickable(role = Role.Button) { onRemove(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Удалить фото ${index + 1}",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        if (listState.canScrollBackward || listState.canScrollForward) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(11.dp)
                    .padding(top = 8.dp)
            ) {
                val itemStride = thumbnailWidth.toPx() + PhotoSpacing.toPx()
                val totalContentWidth = thumbnailWidth.toPx() * photos.size +
                    PhotoSpacing.toPx() * (photos.size - 1).coerceAtLeast(0)
                val viewportWidth = (
                    listState.layoutInfo.viewportEndOffset -
                        listState.layoutInfo.viewportStartOffset
                    ).toFloat()
                val maxScroll = (totalContentWidth - viewportWidth).coerceAtLeast(1f)
                val currentScroll = (
                    listState.firstVisibleItemIndex * itemStride +
                        listState.firstVisibleItemScrollOffset
                    ).coerceIn(0f, maxScroll)
                val thumbWidth = (
                    size.width * (viewportWidth / totalContentWidth)
                    ).coerceIn(32.dp.toPx(), size.width)
                val thumbStart = (size.width - thumbWidth) * (currentScroll / maxScroll)
                val centerY = size.height / 2f

                drawLine(
                    color = RhythmDayPalette.Line,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RhythmDayPalette.Teal,
                    start = Offset(thumbStart, centerY),
                    end = Offset(thumbStart + thumbWidth, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    selectedIndex?.takeIf { it in photos.indices }?.let { initialIndex ->
        FullScreenPhotoViewerDialog(onDismissRequest = { selectedIndex = null }) {
            val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    PhotoImage(
                        bytes = photos[page],
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xDD152427))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(role = Role.Button) { selectedIndex = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Закрыть фото",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(bottom = 18.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
internal expect fun FullScreenPhotoViewerDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)

private val PhotoSpacing = 8.dp

@Composable
private fun PhotoImage(bytes: ByteArray, contentScale: ContentScale, modifier: Modifier) {
    val bitmap = remember(bytes) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Фотография дня",
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
