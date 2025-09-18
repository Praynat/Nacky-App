package com.nacky.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.provider.Settings
import android.content.Intent
import android.util.Log

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
            // Placeholder handler for structured pattern configuration (V2).
            // We simply log receipt and (optionally) update words if provided.
            val words = call.argument<List<String>>("words") ?: emptyList()
            if (words.isNotEmpty()) {
              ForbiddenStore.words = words.map { it.lowercase() }.toSet()
            }
            val version = call.argument<Int>("version") ?: -1
            val phase = (call.argument<Map<String, Any>>("meta") ?: emptyMap())["phase"]
            Log.i("Nacky", "updatePatterns received (version=$version, words=${words.size}, phase=$phase)")
            result.success(null)
          }
          else -> result.notImplemented()
        }
      }
  }
}
