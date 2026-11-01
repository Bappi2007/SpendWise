package com.example.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySpending
import com.example.data.model.DailySpending
import com.example.data.model.MonthlyTrend
import java.util.Locale
import kotlin.math.atan2

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categorySpendings: List<CategorySpending>,
    totalSpent: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (categorySpendings.isEmpty() || totalSpent <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded for this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySpendings) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .testTag("donut_chart_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(categorySpendings) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var cumulativeAngle = 0f
                            var clickedIndex: Int? = null
                            for ((idx, item) in categorySpendings.withIndex()) {
                                val sweep = (item.totalAmount / totalSpent * 360.0).toFloat()
                                if (angle >= cumulativeAngle && angle < cumulativeAngle + sweep) {
                                    clickedIndex = idx
                                    break
                                }
                                cumulativeAngle += sweep
                            }
                            selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex
                        }
                    }
            ) {
                val strokeWidth = 36.dp.toPx()
                val selectedStrokeWidth = 44.dp.toPx()
                val diameter = size.minDimension - selectedStrokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f
                for ((index, item) in categorySpendings.withIndex()) {
                    val sweep = ((item.totalAmount / totalSpent) * 360.0).toFloat() * animationProgress.value
                    val isSelected = selectedIndex == index
                    val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth

                    drawArc(
                        color = Color(item.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            // Center Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                val displayItem = selectedIndex?.let { categorySpendings.getOrNull(it) }
                if (displayItem != null) {
                    Text(
                        text = displayItem.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", displayItem.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(displayItem.colorHex)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f%%", displayItem.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalSpent)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Breakdown Grid
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categorySpendings.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable {
                        selectedIndex = if (isSelected) null else index
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(item.colorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.0f%%", item.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBarChart(
    dailySpendings: List<DailySpending>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (dailySpendings.isEmpty()) return

    val maxAmount = (dailySpendings.maxOfOrNull { it.totalAmount } ?: 1.0).coerceAtLeast(100.0)
    var selectedDay by remember { mutableStateOf<DailySpending?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(dailySpendings) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val highestSpend = dailySpendings.maxByOrNull { it.totalAmount }

    Column(modifier = modifier.fillMaxWidth()) {
        selectedDay?.let { day ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Day ${day.dayOfMonth}: $currencySymbol${String.format(Locale.US, "%,.2f", day.totalAmount)} (${day.transactionCount} transactions)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            dailySpendings.forEach { day ->
                val barFraction = if (maxAmount > 0) (day.totalAmount / maxAmount).toFloat() else 0f
                val isPeak = highestSpend != null && day.dayOfMonth == highestSpend.dayOfMonth && day.totalAmount > 0
                val isSelected = selectedDay?.dayOfMonth == day.dayOfMonth

                val barColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isPeak -> MaterialTheme.colorScheme.tertiary
                    day.isWeekend -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                    day.totalAmount > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(22.dp)
                        .clickable { selectedDay = if (selectedDay?.dayOfMonth == day.dayOfMonth) null else day }
                ) {
                    Box(
                        modifier = Modifier
                            .height(130.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val animatedHeight = (120.dp * barFraction * animationProgress.value).coerceAtLeast(4.dp)
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(animatedHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${day.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isPeak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isPeak || isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendLineChart(
    trends: List<MonthlyTrend>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxAmount = (trends.maxOfOrNull { it.totalAmount } ?: 100.0).coerceAtLeast(100.0)
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(trends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .testTag("trend_line_chart")
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height - 24.dp.toPx()
                val stepX = width / (trends.size - 1).coerceAtLeast(1)

                val points = trends.mapIndexed { index, trend ->
                    val x = index * stepX
                    val yFraction = (trend.totalAmount / maxAmount).toFloat() * animationProgress.value
                    val y = height - (yFraction * (height - 16.dp.toPx()))
                    Offset(x, y)
                }

                // Draw gradient under the curve
                val fillPath = Path()
                if (points.isNotEmpty()) {
                    fillPath.moveTo(points.first().x, height)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f)),
                            startY = 0f,
                            endY = height
                        )
                    )
                }

                // Draw smooth line
                val strokePath = Path()
                if (points.isNotEmpty()) {
                    strokePath.moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        strokePath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }

                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw data points
                points.forEach { point ->
                    drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = point)
                    drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = point)
                }
            }
        }

        // Month Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trends.forEach { trend ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = trend.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", trend.totalAmount)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SplitExpenseGraph(
    overview: com.example.ui.viewmodel.SettlementOverview,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (overview.totalSplitVolume <= 0 && overview.friendStats.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No split expenses logged yet to show graph",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(overview) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val settledFraction = if (overview.totalSplitVolume > 0) {
        (overview.totalSettledAmount / overview.totalSplitVolume).toFloat()
    } else 0f

    val settledColor = Color(0xFF10B981) // Emerald
    val pendingColor = Color(0xFFF59E0B) // Amber

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Top High-Level Metric Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Total Split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", overview.totalSplitVolume)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = settledColor.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Settled", style = MaterialTheme.typography.labelSmall, color = settledColor)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", overview.totalSettledAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = settledColor
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pendingColor.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Pending", style = MaterialTheme.typography.labelSmall, color = pendingColor)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", overview.totalPendingAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = pendingColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Ratio Bar
        Text(
            text = "Settlement Progress: ${(settledFraction * 100).toInt()}% Settled",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(pendingColor.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (settledFraction * animationProgress.value).coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(settledColor)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Split Volume by Friend:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val maxFriendAllocated = (overview.friendStats.maxOfOrNull { it.totalAllocated } ?: 1.0).coerceAtLeast(1.0)

        overview.friendStats.forEach { stat ->
            val friendBarFraction = (stat.totalAllocated / maxFriendAllocated).toFloat()
            val friendPaidFraction = if (stat.totalAllocated > 0) (stat.totalPaid / stat.totalAllocated).toFloat() else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.friendName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", stat.totalAllocated)} (${stat.percentageOfTotalSplits.toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (friendBarFraction * animationProgress.value).coerceIn(0.05f, 1f))
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(pendingColor)
                ) {
                    if (friendPaidFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = friendPaidFraction.coerceIn(0f, 1f))
                                .height(12.dp)
                                .background(settledColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Settled: $currencySymbol${String.format(Locale.US, "%,.0f", stat.totalPaid)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = settledColor
                    )
                    if (stat.totalPending > 0) {
                        Text(
                            text = "Owes you: $currencySymbol${String.format(Locale.US, "%,.0f", stat.totalPending)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = pendingColor
                        )
                    } else {
                        Text(
                            text = "All settled",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = settledColor
                        )
                    }
                }
            }
        }
    }
}

