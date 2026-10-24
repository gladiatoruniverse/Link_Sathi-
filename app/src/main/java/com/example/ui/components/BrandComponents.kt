package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DetectedSignal
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.SignalSeverity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalRiskPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Official LinkSaathi Brand Logo rendered from the uploaded asset with 1:1 aspect ratio,
 * responsive sizing, zero distortion, and accessible alt text.
 */
@Composable
fun LinkSaathiShieldLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    contentDescription: String = "LinkSaathi — Your Personal Digital Safety Assistant official logo"
) {
    val cornerRadius = (size.value * 0.22f).dp
    val shape = RoundedCornerShape(cornerRadius)

    Image(
        painter = painterResource(id = R.drawable.img_linksaathi_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = shape
            )
            .testTag("linksaathi_brand_logo")
    )
}

@Composable
fun RiskLevelBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val riskPalette = LocalRiskPalette.current
    val (bgColor, borderColor, textColor, icon) = when (riskLevel) {
        RiskLevel.HIGH -> Quad(
            riskPalette.highContainer,
            riskPalette.highBorder,
            riskPalette.highText,
            Icons.Filled.Error
        )
        RiskLevel.MEDIUM -> Quad(
            riskPalette.mediumContainer,
            riskPalette.mediumBorder,
            riskPalette.mediumText,
            Icons.Filled.Warning
        )
        RiskLevel.LOW -> Quad(
            riskPalette.lowContainer,
            riskPalette.lowBorder,
            riskPalette.lowText,
            Icons.Filled.CheckCircle
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        modifier = modifier.testTag("risk_badge_${riskLevel.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = if (large) 16.dp else 10.dp,
                vertical = if (large) 8.dp else 4.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = riskLevel.label,
                tint = textColor,
                modifier = Modifier.size(if (large) 20.dp else 14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = riskLevel.badgeText,
                style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun SignalExplanationCard(
    signal: DetectedSignal,
    modifier: Modifier = Modifier
) {
    val riskPalette = LocalRiskPalette.current
    val (accentColor, containerTint, icon) = when (signal.severity) {
        SignalSeverity.CRITICAL -> Triple(
            riskPalette.highText,
            riskPalette.highContainer.copy(alpha = 0.45f),
            Icons.Filled.Error
        )
        SignalSeverity.WARNING -> Triple(
            riskPalette.mediumText,
            riskPalette.mediumContainer.copy(alpha = 0.45f),
            Icons.Filled.Warning
        )
        SignalSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            Icons.Filled.Info
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signal_card_${signal.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(containerTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = signal.severity.label,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = signal.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = signal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = signal.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!signal.matchedSnippet.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Detected pattern: ${signal.matchedSnippet}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ScanHistoryItemCard(
    record: ScanResult,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_card_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (record.scanType == ScanType.URL) {
                                Icons.Outlined.Link
                            } else {
                                Icons.Outlined.Sms
                            },
                            contentDescription = record.scanType.displayName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.scanType.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatTimestamp(record.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RiskLevelBadge(riskLevel = record.riskLevel)
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_history_item_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete scan record",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = record.redactedPreview,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (record.scanType == ScanType.URL) {
                        JetBrainsMonoFontFamily
                    } else {
                        MaterialTheme.typography.bodyMedium.fontFamily
                    }
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${record.signals.size} indicator(s) • Tap to view full assessment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HeuristicDisclaimerCard(
    modifier: Modifier = Modifier,
    text: String = ScanResult.DEFAULT_HEURISTIC_DISCLAIMER
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("heuristic_disclaimer_card"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "On-device heuristic notice",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Important: Heuristic vs. Verified Threat Intelligence",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FeedbackToastBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feedback_toast_banner"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss notification",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun formatTimestamp(timestampMs: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    return formatter.format(Date(timestampMs))
}
