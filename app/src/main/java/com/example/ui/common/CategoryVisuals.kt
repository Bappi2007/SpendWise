package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
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
        "groceries" to Icons.Default.LocalGroceryStore,
        "coffee" to Icons.Default.LocalCafe,
        "transport" to Icons.Default.DirectionsCar,
        "fuel" to Icons.Default.LocalGasStation,
        "travel" to Icons.Default.Flight,
        "rent" to Icons.Default.Home,
        "bills" to Icons.AutoMirrored.Filled.ReceiptLong,
        "electricity" to Icons.Default.Bolt,
        "water" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "subscriptions" to Icons.Default.Subscriptions,
        "shopping" to Icons.Default.ShoppingBag,
        "clothing" to Icons.Default.Checkroom,
        "salon" to Icons.Default.ContentCut,
        "health" to Icons.Default.MedicalServices,
        "fitness" to Icons.Default.FitnessCenter,
        "entertainment" to Icons.Default.Movie,
        "gaming" to Icons.Default.SportsEsports,
        "music" to Icons.Default.MusicNote,
        "education" to Icons.Default.School,
        "books" to Icons.AutoMirrored.Filled.MenuBook,
        "tech" to Icons.Default.Laptop,
        "gift" to Icons.Default.CardGiftcard,
        "family" to Icons.Default.FamilyRestroom,
        "pets" to Icons.Default.Pets,
        "savings" to Icons.Default.Savings,
        "investment" to Icons.AutoMirrored.Filled.TrendingUp,
        "repair" to Icons.Default.Build,
        "charity" to Icons.Default.VolunteerActivism,
        "money_lent" to Icons.Default.Handshake,
        "other" to Icons.Default.Category
    )

    val PRESET_COLORS = listOf(
        0xFF0171DFL, // SpendWise Blue
        0xFF3CAE8BL, // SpendWise Green
        0xFFFFD140L, // SpendWise Yellow
        0xFF121660L, // SpendWise Navy
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
        0xFF06B6D4L, // Cyan
        0xFFE11D48L, // Rose
        0xFF9333EAL, // Fuchsia Purple
        0xFF059669L, // Forest Green
        0xFFEA580CL, // Deep Orange
        0xFF2563EBL, // Royal Blue
        0xFF7C3AEDL, // Deep Violet
        0xFF475569L, // Slate Dark
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
