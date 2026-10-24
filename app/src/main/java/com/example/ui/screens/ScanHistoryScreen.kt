package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.ui.components.ScanHistoryItemCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanHistoryScreen(
    historyList: List<ScanResult>,
    typeFilter: ScanType?,
    riskFilter: RiskLevel?,
    isHistoryEnabled: Boolean,
    onSelectTypeFilter: (ScanType?) -> Unit,
    onSelectRiskFilter: (RiskLevel?) -> Unit,
    onInspectRecord: (ScanResult) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredHistory = remember(historyList, typeFilter, riskFilter) {
        historyList.filter { item ->
            val matchesType = typeFilter == null || item.scanType == typeFilter
            val matchesRisk = riskFilter == null || item.riskLevel == riskFilter
            matchesType && matchesRisk
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Entire Scan History?") },
            text = {
                Text("This will permanently delete all ${historyList.size} saved scan records from your device storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("scan_history_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + Clear All Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local Scan History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Stored on-device with automatic OTP, PIN, and phone number redaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_all_history_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear all history",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All")
                    }
                }
            }

            // Banner if history storage is currently disabled
            if (!isHistoryEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_disabled_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HistoryToggleOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scan History Storage is Paused",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "New scans will not be saved until you re-enable History in Settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("history_open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings")
                        }
                    }
                }
            }

            // Filters Card
            if (historyList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Filter by Scan Type & Risk Classification",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = typeFilter == null,
                                onClick = { onSelectTypeFilter(null) },
                                label = { Text("All Types") }
                            )
                            FilterChip(
                                selected = typeFilter == ScanType.URL,
                                onClick = { onSelectTypeFilter(ScanType.URL) },
                                label = { Text("URLs Only") }
                            )
                            FilterChip(
                                selected = typeFilter == ScanType.MESSAGE,
                                onClick = { onSelectTypeFilter(ScanType.MESSAGE) },
                                label = { Text("Messages Only") }
                            )
                            FilterChip(
                                selected = riskFilter == RiskLevel.HIGH,
                                onClick = {
                                    onSelectRiskFilter(if (riskFilter == RiskLevel.HIGH) null else RiskLevel.HIGH)
                                },
                                label = { Text("High Risk") }
                            )
                            FilterChip(
                                selected = riskFilter == RiskLevel.MEDIUM,
                                onClick = {
                                    onSelectRiskFilter(if (riskFilter == RiskLevel.MEDIUM) null else RiskLevel.MEDIUM)
                                },
                                label = { Text("Medium Risk") }
                            )
                            FilterChip(
                                selected = riskFilter == RiskLevel.LOW,
                                onClick = {
                                    onSelectRiskFilter(if (riskFilter == RiskLevel.LOW) null else RiskLevel.LOW)
                                },
                                label = { Text("Low Risk") }
                            )
                        }
                    }
                }
            }

            // Records List or Empty State
            if (filteredHistory.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_empty_state_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (historyList.isEmpty()) {
                                "Your Scan History is Empty"
                            } else {
                                "No Records Match Your Selected Filters"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (historyList.isEmpty()) {
                                "When you check a URL or analyze a message and tap 'Save to History', it will appear here with sensitive numbers automatically masked."
                            } else {
                                "Try resetting the type or risk level filters above to view all ${historyList.size} saved scan(s)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                filteredHistory.forEach { record ->
                    ScanHistoryItemCard(
                        record = record,
                        onClick = { onInspectRecord(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
