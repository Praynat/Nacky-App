package com.nacky.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.provider.Settings
import android.content.Intent
import android.util.Log
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.persistence.SettingsStore
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.Serializable

// Runtime settings updated from Flutter (no persistence on Android side yet)
@Serializable
data class DetectionSettings(
  val liveEnabled: Boolean = false,
  val monitoringEnabled: Boolean = true,
  val minOccurrences: Int = 3,
  val windowSeconds: Long = 300,
  val cooldownMs: Long = 1000,
  val countMode: String = "UNIQUE_PER_SNAPSHOT",
  val blockHighSeverityOnly: Boolean = false,
  val debounceMs: Long = 250,
) {
  fun normalized(): DetectionSettings {
    val cm = if (countMode == "ALL_MATCHES") "ALL_MATCHES" else "UNIQUE_PER_SNAPSHOT"
    return copy(
      minOccurrences = minOccurrences.coerceAtLeast(1),
      windowSeconds = windowSeconds.coerceAtLeast(1),
      cooldownMs = cooldownMs.coerceAtLeast(50),
      debounceMs = debounceMs.coerceAtLeast(50),
      countMode = cm
    )
  }
}

object DetectionSettingsStore { val current = AtomicReference(DetectionSettings()) }

class MainActivity: FlutterActivity() {
  private val CHANNEL = "nacky/android"

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)

    MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
      .setMethodCallHandler { call, result ->
        when (call.method) {
          "isAccessibilityEnabled" -> {
            val enabled = try {
              val enabledServices = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
              ) ?: ""
              // IMPORTANT: le nom complet du service
              enabledServices.contains("$packageName/com.nacky.app.NackyAccessibilityService") ||
              enabledServices.contains("$packageName/.NackyAccessibilityService")
            } catch (_: Throwable) { false }
            result.success(enabled)
          }
          "requestAccessibilityPermission" -> {
            try {
              startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              )
              result.success(null)
            } catch (e: Throwable) {
              result.error("OPEN_SETTINGS_FAILED", e.message, null)
            }
          }
          "sendWordList" -> {
            val list = (call.argument<List<String>>("words") ?: emptyList())
            ForbiddenStore.words = list.map { it.lowercase() }.toSet()
            result.success(null)
          }
          "updatePatterns" -> {
            val arg = call.arguments
            val update = PatternRepository.updateFromPayload(arg, applicationContext)
            if (update.ok) {
              Log.i(
                "Nacky",
                "updatePatterns: received payload patterns=${update.patternCount} items=${update.itemTotal} categories=${update.categories} severities=${update.severities}"
              )
            } else {
              Log.e("Nacky", "updatePatterns: parse failed ${update.error}")
            }
            // Legacy support: if a flat words list still arrives (old app version)
            val legacyWords = (if (arg is Map<*, *>) arg["words"] else null) as? List<*>
            if (legacyWords != null && legacyWords.isNotEmpty()) {
              ForbiddenStore.words = legacyWords.mapNotNull { it?.toString()?.lowercase() }.toSet()
            }
            result.success(null)
          }
          "updateSettings" -> {
            try {
              val map = call.arguments as? Map<*, *> ?: emptyMap<String, Any>()
              val s = DetectionSettings(
                liveEnabled = map["liveEnabled"] as? Boolean ?: false,
                monitoringEnabled = map["monitoringEnabled"] as? Boolean ?: true,
                minOccurrences = (map["minOccurrences"] as? Number)?.toInt() ?: 3,
                windowSeconds = (map["windowSeconds"] as? Number)?.toLong() ?: 300L,
                cooldownMs = (map["cooldownMs"] as? Number)?.toLong() ?: 1000L,
                countMode = map["countMode"]?.toString() ?: "UNIQUE_PER_SNAPSHOT",
                blockHighSeverityOnly = map["blockHighSeverityOnly"] as? Boolean ?: false,
                debounceMs = (map["debounceMs"] as? Number)?.toLong() ?: 250L,
              ).normalized()
              DetectionSettingsStore.current.set(s)
              Log.i("Nacky", "updateSettings: $s")
              try { SettingsStore.save(applicationContext, s) } catch (_: Throwable) {}
              // TODO: apply to running MonitoringEngine / LiveTypingDetector instances once central managers exist
              result.success(null)
            } catch (e: Throwable) {
              Log.e("Nacky", "updateSettings failed ${e.message}")
              result.error("UPDATE_SETTINGS_FAILED", e.message, null)
            }
          }
          else -> result.notImplemented()
        }
      }
  }
}
