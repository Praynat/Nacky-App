package com.nacky.app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.provider.Settings
import android.content.Intent
import android.util.Log
import com.nacky.app.patterns.PatternRepository

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
          "updatePatterns" -> {
            val arg = call.arguments
            val update = PatternRepository.updateFromPayload(arg)
            if (update.ok) {
              Log.i(
                "Nacky",
                "updatePatterns: received payload patterns=${update.patternCount} items=${update.itemTotal} categories=${update.categories} severities=${update.severities}"
              )
            } else {
              Log.e("Nacky", "updatePatterns: parse failed ${update.error}")
            }
            result.success(null)
          }
          else -> result.notImplemented()
        }
      }
  }
}
