package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ExpenseWithCategory
import com.example.data.model.FriendEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.SplitAllocationMode
import com.example.ui.common.CategoryIconBadge
import com.example.ui.common.CategoryVisuals
import com.example.ui.viewmodel.SplitDraftItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParticipantInputState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val percentageText: String = "",
    val manualAmountText: String = "",
    val isPaid: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseBottomSheet(
    categories: List<CategoryEntity>,
    currencySymbol: String,
    expenseToEdit: ExpenseWithCategory? = null,
    friends: List<FriendEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amountText: String,
        categoryId: String,
        timestamp: Long,
        paymentMethod: PaymentMethod,
        notes: String?,
        splits: List<SplitDraftItem>
    ) -> Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var amount by remember {
        mutableStateOf(expenseToEdit?.expense?.amount?.let {
            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
        } ?: "")
    }
    var title by remember { mutableStateOf(expenseToEdit?.expense?.title ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(expenseToEdit?.expense?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }
    var timestamp by remember {
        mutableLongStateOf(expenseToEdit?.expense?.timestamp ?: System.currentTimeMillis())
    }
    var paymentMethod by remember {
        mutableStateOf(
            expenseToEdit?.expense?.paymentMethod?.let { PaymentMethod.fromString(it) } ?: PaymentMethod.UPI
        )
    }
    var notes by remember { mutableStateOf(expenseToEdit?.expense?.notes ?: "") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var splitError by remember { mutableStateOf<String?>(null) }

    // Split Expenses State
    var isSplitEnabled by remember {
        mutableStateOf(expenseToEdit?.splits?.isNotEmpty() == true)
    }
    var splitMode by remember {
        mutableStateOf(
            if (expenseToEdit?.splits?.any { it.percentage > 0 } == true) SplitAllocationMode.PERCENTAGE
            else SplitAllocationMode.EQUAL
        )
    }
    var newPersonName by remember { mutableStateOf("") }
    val participants = remember {
        mutableStateListOf<ParticipantInputState>().apply {
            expenseToEdit?.splits?.forEach { sp ->
                add(
                    ParticipantInputState(
                        id = sp.id,
                        name = sp.personName,
                        percentageText = if (sp.percentage > 0) sp.percentage.toString() else "",
                        manualAmountText = if (sp.allocatedAmount > 0) sp.allocatedAmount.toString() else "",
                        isPaid = sp.isPaid
                    )
                )
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val quickTitles = listOf("Groceries", "Coffee", "Cab", "Dinner", "Rent", "Online Shopping", "Pharmacy")
    val suggestedFriends = listOf("Rahul", "Priya", "Alex", "Rohan", "Ananya", "Sara")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("add_expense_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Expense" else "Edit Expense",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input with currency
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    if (amountError != null) amountError = null
                    if (splitError != null) splitError = null
                },
                label = { Text("Amount") },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError != null) titleError = null
                },
                label = { Text("Title / Merchant") },
                placeholder = { Text("e.g. Dinner, Grocery Store, Cab") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input_field")
            )

            // Quick Title Suggestions
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickTitles.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable {
                            title = suggestion
                            titleError = null
                        }
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    val catColor = Color(category.colorHex)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) catColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, catColor) else null,
                        modifier = Modifier
                            .clickable { selectedCategoryId = category.id }
                            .testTag("category_chip_${category.name.lowercase(Locale.getDefault())}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                iconKey = category.iconKey,
                                colorHex = category.colorHex,
                                size = 28.dp,
                                iconSize = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Date & Time pickers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newC = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                timestamp = newC.timeInMillis
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("date_picker_button")
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val newC = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                    set(Calendar.MINUTE, minute)
                                }
                                timestamp = newC.timeInMillis
                            },
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE),
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("time_picker_button")
                ) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeFormat.format(Date(timestamp)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    val isSelected = paymentMethod == method
                    val icon = when (method) {
                        PaymentMethod.CASH -> Icons.Default.Money
                        PaymentMethod.UPI -> Icons.Default.QrCode
                        PaymentMethod.CARD -> Icons.Default.CreditCard
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { paymentMethod = method },
                        label = { Text(method.displayName) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_method_${method.name.lowercase(Locale.getDefault())}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SPLIT EXPENSE SECTION
            // ==========================================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSplitEnabled) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_expense_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CallSplit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Split with People",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Divide amount & remember who paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isSplitEnabled,
                            onCheckedChange = { isSplitEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("split_switch")
                        )
                    }

                    AnimatedVisibility(visible = isSplitEnabled) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Add Person Input
                            Text(
                                text = "Add People to this Expense",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (friends.isNotEmpty()) {
                                Text(
                                    text = "Quick add from saved friends:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    friends.forEach { friend ->
                                        val isAdded = participants.any { it.name.trim().equals(friend.name.trim(), ignoreCase = true) }
                                        FilterChip(
                                            selected = isAdded,
                                            onClick = {
                                                if (isAdded) {
                                                    participants.removeAll { it.name.trim().equals(friend.name.trim(), ignoreCase = true) }
                                                } else {
                                                    participants.add(ParticipantInputState(name = friend.name.trim()))
                                                }
                                            },
                                            label = { Text(friend.name) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newPersonName,
                                    onValueChange = {
                                        newPersonName = it
                                        if (splitError != null) splitError = null
                                    },
                                    placeholder = { Text("Friend's name (e.g. Rahul)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (newPersonName.isNotBlank()) {
                                                val trimmed = newPersonName.trim()
                                                if (!participants.any { it.name.equals(trimmed, ignoreCase = true) }) {
                                                    participants.add(ParticipantInputState(name = trimmed))
                                                    newPersonName = ""
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("split_person_input")
                                )

                                Button(
                                    onClick = {
                                        if (newPersonName.isNotBlank()) {
                                            val trimmed = newPersonName.trim()
                                            if (!participants.any { it.name.equals(trimmed, ignoreCase = true) }) {
                                                participants.add(ParticipantInputState(name = trimmed))
                                                newPersonName = ""
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("split_add_person_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }

                            // Suggestions Chips
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                suggestedFriends.forEach { name ->
                                    if (!participants.any { it.name.equals(name, ignoreCase = true) }) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.clickable {
                                                participants.add(ParticipantInputState(name = name))
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "+ $name", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            // Added People Chips
                            if (participants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "People in this split (${participants.size} friends + You):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    participants.forEachIndexed { index, person ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.testTag("participant_chip_${person.name.lowercase(Locale.getDefault())}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = person.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { participants.removeAt(index) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove ${person.name}",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Division Mode Selector
                                Text(
                                    text = "Division Method",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SplitAllocationMode.entries.forEach { mode ->
                                        val isSelected = splitMode == mode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                splitMode = mode
                                                splitError = null
                                            },
                                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("split_mode_${mode.name.lowercase(Locale.getDefault())}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val totalAmt = amount.toDoubleOrNull() ?: 0.0

                                // Mode Details: Equal Division
                                if (splitMode == SplitAllocationMode.EQUAL) {
                                    val countTotal = participants.size + 1
                                    val perPerson = if (countTotal > 0 && totalAmt > 0) totalAmt / countTotal else 0.0

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Your Share (You)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", perPerson)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.Divider()
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Friends' Shares (Check if they already paid):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            participants.forEachIndexed { idx, p ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Checkbox(
                                                            checked = p.isPaid,
                                                            onCheckedChange = { checked ->
                                                                participants[idx] = p.copy(isPaid = checked)
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                                            modifier = Modifier.testTag("checkbox_paid_${p.name.lowercase(Locale.getDefault())}")
                                                        )
                                                        Text(
                                                            text = p.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (p.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Text(
                                                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", perPerson)}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (p.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Mode Details: Percentage Division
                                if (splitMode == SplitAllocationMode.PERCENTAGE) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            var totalPct = 0.0
                                            participants.forEach { p ->
                                                totalPct += p.percentageText.toDoubleOrNull() ?: 0.0
                                            }
                                            val myPct = (100.0 - totalPct).coerceAtLeast(0.0)
                                            val myAmt = (totalAmt * myPct / 100.0)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Your Share (${String.format(Locale.US, "%.1f", myPct)}%)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", myAmt)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.Divider()
                                            Spacer(modifier = Modifier.height(8.dp))

                                            participants.forEachIndexed { idx, p ->
                                                val pctVal = p.percentageText.toDoubleOrNull() ?: 0.0
                                                val friendAmt = totalAmt * pctVal / 100.0

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = p.isPaid,
                                                        onCheckedChange = { checked ->
                                                            participants[idx] = p.copy(isPaid = checked)
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.testTag("checkbox_paid_${p.name.lowercase(Locale.getDefault())}")
                                                    )

                                                    Text(
                                                        text = p.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    OutlinedTextField(
                                                        value = p.percentageText,
                                                        onValueChange = { newPct ->
                                                            participants[idx] = p.copy(percentageText = newPct)
                                                            if (splitError != null) splitError = null
                                                        },
                                                        suffix = { Text("%") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .width(88.dp)
                                                            .testTag("percentage_input_${p.name.lowercase(Locale.getDefault())}")
                                                    )

                                                    Text(
                                                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", friendAmt)}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Mode Details: Manual Entry
                                if (splitMode == SplitAllocationMode.MANUAL) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            var totalManual = 0.0
                                            participants.forEach { p ->
                                                totalManual += p.manualAmountText.toDoubleOrNull() ?: 0.0
                                            }
                                            val myManual = (totalAmt - totalManual).coerceAtLeast(0.0)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Your Share (Remaining)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", myManual)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.Divider()
                                            Spacer(modifier = Modifier.height(8.dp))

                                            participants.forEachIndexed { idx, p ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = p.isPaid,
                                                        onCheckedChange = { checked ->
                                                            participants[idx] = p.copy(isPaid = checked)
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.testTag("checkbox_paid_${p.name.lowercase(Locale.getDefault())}")
                                                    )

                                                    Text(
                                                        text = p.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f)
                                                    )

                                                    OutlinedTextField(
                                                        value = p.manualAmountText,
                                                        onValueChange = { newAmt ->
                                                            participants[idx] = p.copy(manualAmountText = newAmt)
                                                            if (splitError != null) splitError = null
                                                        },
                                                        prefix = { Text(currencySymbol) },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier
                                                            .width(110.dp)
                                                            .testTag("manual_amount_input_${p.name.lowercase(Locale.getDefault())}")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (splitError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = splitError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Remarks / Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Remarks / Notes (Optional)") },
                placeholder = { Text("Add any extra details or split info") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input_field")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Action Button
            Button(
                onClick = {
                    val amtVal = amount.toDoubleOrNull()
                    if (amtVal == null || amtVal <= 0) {
                        amountError = "Enter a valid amount > 0"
                        return@Button
                    }
                    if (title.isBlank()) {
                        titleError = "Title cannot be blank"
                        return@Button
                    }

                    val finalSplits = mutableListOf<SplitDraftItem>()
                    if (isSplitEnabled && participants.isNotEmpty()) {
                        when (splitMode) {
                            SplitAllocationMode.EQUAL -> {
                                val count = participants.size + 1
                                val perPerson = amtVal / count
                                val pct = 100.0 / count
                                participants.forEach { p ->
                                    finalSplits.add(
                                        SplitDraftItem(
                                            id = p.id,
                                            personName = p.name,
                                            allocatedAmount = perPerson,
                                            percentage = pct,
                                            isPaid = p.isPaid
                                        )
                                    )
                                }
                            }
                            SplitAllocationMode.PERCENTAGE -> {
                                var totalPct = 0.0
                                for (p in participants) {
                                    val pct = p.percentageText.toDoubleOrNull()
                                    if (pct == null || pct <= 0) {
                                        splitError = "Enter a valid percentage for ${p.name}"
                                        return@Button
                                    }
                                    totalPct += pct
                                }
                                if (totalPct > 100.0) {
                                    splitError = "Total allocated percentage (${String.format(Locale.US, "%.1f", totalPct)}%) exceeds 100%"
                                    return@Button
                                }
                                participants.forEach { p ->
                                    val pct = p.percentageText.toDouble()
                                    finalSplits.add(
                                        SplitDraftItem(
                                            id = p.id,
                                            personName = p.name,
                                            allocatedAmount = (amtVal * pct / 100.0),
                                            percentage = pct,
                                            isPaid = p.isPaid
                                        )
                                    )
                                }
                            }
                            SplitAllocationMode.MANUAL -> {
                                var totalAllocated = 0.0
                                for (p in participants) {
                                    val alloc = p.manualAmountText.toDoubleOrNull()
                                    if (alloc == null || alloc <= 0) {
                                        splitError = "Enter a valid amount for ${p.name}"
                                        return@Button
                                    }
                                    totalAllocated += alloc
                                }
                                if (totalAllocated > amtVal) {
                                    splitError = "Allocated amount ($currencySymbol${String.format(Locale.US, "%,.2f", totalAllocated)}) exceeds total expense ($currencySymbol$amtVal)"
                                    return@Button
                                }
                                participants.forEach { p ->
                                    val alloc = p.manualAmountText.toDouble()
                                    val pct = if (amtVal > 0) (alloc / amtVal) * 100.0 else 0.0
                                    finalSplits.add(
                                        SplitDraftItem(
                                            id = p.id,
                                            personName = p.name,
                                            allocatedAmount = alloc,
                                            percentage = pct,
                                            isPaid = p.isPaid
                                        )
                                    )
                                }
                            }
                        }
                    }

                    val success = onSaveExpense(
                        title,
                        amount,
                        selectedCategoryId,
                        timestamp,
                        paymentMethod,
                        notes,
                        finalSplits
                    )
                    if (success) {
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_expense_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (expenseToEdit == null) "Save Expense" else "Update Expense",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
