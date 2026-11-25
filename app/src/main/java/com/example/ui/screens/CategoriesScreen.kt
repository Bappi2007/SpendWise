package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.data.seeder.SampleDataSeeder
import com.example.ui.common.CategoryIconBadge
import com.example.ui.common.CategoryVisuals
import com.example.ui.viewmodel.ExpenseViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    categoryToEdit = null
                    isAddDialogOpen = true
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("categories_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Explanatory Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Manage Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customize colors with the interactive color wheel or presets, and choose from 30+ icons. Deleting any category safely reassigns its transactions to 'Other'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Categories List
            items(
                items = categories,
                key = { it.id }
            ) { category ->
                CategoryRowCard(
                    category = category,
                    onEdit = {
                        categoryToEdit = category
                        isAddDialogOpen = true
                    },
                    onDelete = {
                        categoryToDelete = category
                    }
                )
            }
        }
    }

    // Add / Edit Dialog
    if (isAddDialogOpen) {
        CategoryEditDialog(
            category = categoryToEdit,
            onDismiss = {
                isAddDialogOpen = false
                categoryToEdit = null
            },
            onSave = { name, iconKey, colorHex ->
                if (categoryToEdit == null) {
                    viewModel.addCategory(name, iconKey, colorHex)
                } else {
                    viewModel.updateCategory(
                        categoryToEdit!!.copy(
                            name = name,
                            iconKey = iconKey,
                            colorHex = colorHex
                        )
                    )
                }
                isAddDialogOpen = false
                categoryToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete \"${cat.name}\"?") },
            text = {
                Text(
                    "All past expenses associated with \"${cat.name}\" will be moved to the \"Other\" category so your spending totals stay accurate."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete & Move to Other", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryRowCard(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOther = category.id == SampleDataSeeder.DEFAULT_CATEGORY_OTHER_ID || category.name.equals("Other", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconKey = category.iconKey,
                colorHex = category.colorHex,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (category.isDefault) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (category.isDefault) "Default" else "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (category.isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!isOther) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: Long) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(category?.iconKey ?: "shopping") }
    var selectedColor by remember { mutableLongStateOf(category?.colorHex ?: CategoryVisuals.PRESET_COLORS.first()) }
    var useColorWheel by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (category == null) "New Category" else "Edit Category",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIconBadge(
                        iconKey = selectedIcon,
                        colorHex = selectedColor,
                        size = 56.dp,
                        iconSize = 30.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name.ifBlank { "Preview" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "#%06X", (selectedColor and 0xFFFFFFL)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Subscriptions, Gifts") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Grid
                Text(
                    text = "Select Icon (${CategoryVisuals.AVAILABLE_ICONS.size} options)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryVisuals.AVAILABLE_ICONS.forEach { (key, iconVector) ->
                        val isSelected = key.equals(selectedIcon, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = key,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Selection Mode Header + Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Color",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !useColorWheel,
                            onClick = { useColorWheel = false },
                            label = { Text("Swatches", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                        FilterChip(
                            selected = useColorWheel,
                            onClick = { useColorWheel = true },
                            label = { Text("Color Wheel", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("toggle_color_wheel_chip")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (!useColorWheel) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryVisuals.PRESET_COLORS.forEach { colorHex ->
                            val isSelected = colorHex == selectedColor
                            val color = Color(colorHex)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (colorHex == 0xFFFFD140L) Color(0xFF121660) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    HsvColorWheelPicker(
                        initialColorHex = selectedColor,
                        onColorChanged = { newHex ->
                            selectedColor = newHex
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = "Name cannot be empty"
                            return@Button
                        }
                        onSave(name.trim(), selectedIcon, selectedColor)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = if (category == null) "Create Category" else "Save Changes")
                }
            }
        }
    }
}

@Composable
fun HsvColorWheelPicker(
    initialColorHex: Long,
    onColorChanged: (Long) -> Unit
) {
    val initialHsv = remember {
        val hsv = FloatArray(3)
        val r = ((initialColorHex shr 16) and 0xFF).toInt()
        val g = ((initialColorHex shr 8) and 0xFF).toInt()
        val b = (initialColorHex and 0xFF).toInt()
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1].coerceIn(0.15f, 1f)) }
    var value by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0.25f, 1f)) }

    fun emitCurrentHsv() {
        val argbInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val unsignedLong = argbInt.toLong() and 0xFFFFFFFFL
        onColorChanged(unsignedLong)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hsv_color_wheel_picker"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(185.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val hueColors = remember {
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = min(cx, cy)
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            val angleDeg = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                            hue = angleDeg
                            saturation = (dist / radius).coerceIn(0f, 1f)
                            emitCurrentHsv()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val offset = change.position
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val radius = min(cx, cy)
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            val angleDeg = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
                            hue = angleDeg
                            saturation = (dist / radius).coerceIn(0f, 1f)
                            emitCurrentHsv()
                        }
                    }
            ) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Sweep gradient for Hue around the wheel
                drawCircle(
                    brush = Brush.sweepGradient(hueColors, center = center),
                    radius = radius,
                    center = center
                )

                // Radial white gradient in center for Saturation
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Outer border ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Thumb indicator position
                val angleRad = (hue * PI / 180.0).toFloat()
                val thumbDist = saturation * radius
                val thumbCenter = Offset(
                    x = center.x + cos(angleRad) * thumbDist,
                    y = center.y + sin(angleRad) * thumbDist
                )

                val selectedComposeColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = thumbCenter
                )
                drawCircle(
                    color = selectedComposeColor,
                    radius = 8.dp.toPx(),
                    center = thumbCenter
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 11.dp.toPx(),
                    center = thumbCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Brightness / Value Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brightness",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(68.dp)
            )
            Slider(
                value = value,
                onValueChange = { newVal ->
                    value = newVal
                    emitCurrentHsv()
                },
                valueRange = 0.25f..1f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

