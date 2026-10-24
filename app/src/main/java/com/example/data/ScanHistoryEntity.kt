package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DetectedSignal
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.SignalSeverity
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val scanType: String,
    val riskLevel: String,
    val redactedPreview: String,
    val summaryTitle: String,
    val summaryReason: String,
    val signalsJson: String,
    val recommendationsJson: String
) {
    fun toDomainModel(): ScanResult {
        val parsedSignals = mutableListOf<DetectedSignal>()
        try {
            val arr = JSONArray(signalsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parsedSignals.add(
                    DetectedSignal(
                        id = obj.optString("id", "sig_$i"),
                        title = obj.optString("title", ""),
                        category = obj.optString("category", "General"),
                        severity = runCatching {
                            SignalSeverity.valueOf(obj.optString("severity", SignalSeverity.WARNING.name))
                        }.getOrDefault(SignalSeverity.WARNING),
                        explanation = obj.optString("explanation", ""),
                        matchedSnippet = obj.optString("matchedSnippet").takeIf { it.isNotEmpty() }
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore malformed JSON fallback
        }

        val parsedRecommendations = mutableListOf<String>()
        try {
            val recArr = JSONArray(recommendationsJson)
            for (i in 0 until recArr.length()) {
                parsedRecommendations.add(recArr.getString(i))
            }
        } catch (_: Exception) {
            // Ignore malformed JSON fallback
        }

        return ScanResult(
            id = id,
            scanType = runCatching { ScanType.valueOf(scanType) }.getOrDefault(ScanType.URL),
            rawInput = redactedPreview,
            sanitizedDisplayInput = redactedPreview,
            redactedPreview = redactedPreview,
            riskLevel = runCatching { RiskLevel.valueOf(riskLevel) }.getOrDefault(RiskLevel.LOW),
            summaryTitle = summaryTitle,
            summaryReason = summaryReason,
            signals = parsedSignals,
            recommendations = parsedRecommendations,
            timestamp = timestamp,
            isSavedToHistory = true
        )
    }

    companion object {
        fun fromDomainModel(result: ScanResult): ScanHistoryEntity {
            val signalsArray = JSONArray()
            result.signals.forEach { signal ->
                val obj = JSONObject().apply {
                    put("id", signal.id)
                    put("title", signal.title)
                    put("category", signal.category)
                    put("severity", signal.severity.name)
                    put("explanation", signal.explanation)
                    put("matchedSnippet", signal.matchedSnippet ?: "")
                }
                signalsArray.put(obj)
            }

            val recommendationsArray = JSONArray()
            result.recommendations.forEach { rec ->
                recommendationsArray.put(rec)
            }

            return ScanHistoryEntity(
                id = result.id,
                timestamp = result.timestamp,
                scanType = result.scanType.name,
                riskLevel = result.riskLevel.name,
                redactedPreview = result.redactedPreview,
                summaryTitle = result.summaryTitle,
                summaryReason = result.summaryReason,
                signalsJson = signalsArray.toString(),
                recommendationsJson = recommendationsArray.toString()
            )
        }
    }
}
