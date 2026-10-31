package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CategoryVisuals {

    val AVAILABLE_ICONS = listOf(
        "food" to Icons.Default.Restaurant,
        "transport" to Icons.Default.DirectionsCar,
        "rent" to Icons.Default.Home,
        "bills" to Icons.Default.ReceiptLong,
        "shopping" to Icons.Default.ShoppingBag,
        "health" to Icons.Default.MedicalServices,
        "entertainment" to Icons.Default.Movie,
        "education" to Icons.Default.School,
        "coffee" to Icons.Default.LocalCafe,
        "travel" to Icons.Default.Flight,
        "gift" to Icons.Default.CardGiftcard,
        "fitness" to Icons.Default.FitnessCenter,
        "tech" to Icons.Default.Laptop,
        "other" to Icons.Default.Category
    )

    val PRESET_COLORS = listOf(
        0xFFF97316L, // Orange
        0xFF0284C7L, // Sky Blue
        0xFF8B5CF6L, // Purple
        0xFFDC2626L, // Red
        0xFFDB2777L, // Pink
        0xFF0D9488L, // Teal
        0xFFD97706L, // Amber
        0xFF4F46E5L, // Indigo
        0xFF10B981L, // Emerald
        0xFF6366F1L, // Violet
        0xFF84CC16L, // Lime
        0xFF64748BL  // Slate
    )

    fun getIcon(iconKey: String): ImageVector {
        return AVAILABLE_ICONS.firstOrNull { it.first.equals(iconKey, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }
}

@Composable
fun CategoryIconBadge(
    iconKey: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp
) {
    val categoryColor = Color(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(categoryColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.getIcon(iconKey),
            contentDescription = null,
            tint = categoryColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
