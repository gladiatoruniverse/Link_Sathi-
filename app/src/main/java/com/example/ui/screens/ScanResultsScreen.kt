package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.PiiRedactor
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.ui.components.HeuristicDisclaimerCard
import com.example.ui.components.RiskLevelBadge
import com.example.ui.components.SignalExplanationCard
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRiskPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultsScreen(
    result: ScanResult?,
    isHistoryEnabled: Boolean,
    onSaveToHistory: () -> Unit,
    onCheckAnother: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    val riskPalette = LocalRiskPalette.current

    if (result == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No Active Scan Result",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(onClick = onNavigateHome) {
                    Text("Return to Dashboard")
                }
            }
        }
        return
    }

    val (bannerContainerColor, bannerBorderColor, bannerAccentColor) = when (result.riskLevel) {
        RiskLevel.HIGH -> Triple(
            riskPalette.highContainer,
            riskPalette.highBorder,
            riskPalette.highText
        )
        RiskLevel.MEDIUM -> Triple(
            riskPalette.mediumContainer,
            riskPalette.mediumBorder,
            riskPalette.mediumText
        )
        RiskLevel.LOW -> Triple(
            riskPalette.lowContainer,
            riskPalette.lowBorder,
            riskPalette.lowText
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("scan_results_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 820.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Primary Risk Classification Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_result_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = bannerContainerColor.copy(alpha = 0.72f)),
                border = BorderStroke(1.5.dp, bannerBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RiskLevelBadge(riskLevel = result.riskLevel, large = true)
                        Text(
                            text = "${result.scanType.displayName} • ${formatTimestamp(result.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = result.summaryTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = result.summaryReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 2. Primary Action Controls ("Check Another", "Save to History", "Share Safety Warning")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCheckAnother,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("check_another_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check Another",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSaveToHistory,
                    enabled = !result.isSavedToHistory && isHistoryEnabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_to_history_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (result.isSavedToHistory) {
                            Icons.Filled.BookmarkAdded
                        } else {
                            Icons.Outlined.BookmarkBorder
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            result.isSavedToHistory -> "Saved (Redacted)"
                            !isHistoryEnabled -> "History Disabled"
                            else -> "Save to History"
                        }
                    )
                }

                OutlinedButton(
                    onClick = {
                        val shareText = PiiRedactor.buildShareableWarningText(result)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "LinkSaathi Safety Advisory")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        val chooser = Intent.createChooser(sendIntent, "Share Safety Warning with Family or Friends")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(chooser) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_warning_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Defanged Safety Summary with Family")
                }
            }

            // 3. Inspected Content Preview Card (Sanitized & Redacted Preview)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "INSPECTED CONTENT (PII-REDACTED PREVIEW)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sanitized",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.redactedPreview,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = if (result.scanType == ScanType.URL) {
                                    JetBrainsMonoFontFamily
                                } else {
                                    MaterialTheme.typography.bodyMedium.fontFamily
                                }
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag("inspected_content_preview")
                        )
                    }
                }
            }

            // 4. Detected Warning Signals Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Detected Signals & Evidence (${result.signals.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (result.signals.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("no_signals_detected_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, bannerBorderColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = bannerAccentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "No Automated Heuristic Warnings Triggered",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (result.scanType == ScanType.URL) {
                                        "This URL uses standard syntax and did not trigger lookalike, shortener, or raw-IP rules. However, remember that HTTPS encryption does NOT mean a website is legitimate, and new phishing sites can look structurally normal."
                                    } else {
                                        "This message did not trigger rules for OTP solicitation, urgent payment demands, or unknown APK downloads. Still, always verify unexpected requests through official channels."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    result.signals.forEach { signal ->
                        SignalExplanationCard(signal = signal)
                    }
                }
            }

            // 5. Recommended Next Steps Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recommended_actions_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Recommended Safe Next Steps",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    result.recommendations.forEachIndexed { index, step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 6. Explicit Heuristic vs Threat-Intelligence Disclaimer
            HeuristicDisclaimerCard(text = result.disclaimer)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
