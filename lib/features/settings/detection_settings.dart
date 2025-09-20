import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// Detection feature settings exposed to UI and bridged to Android.
class DetectionSettings {
  final bool liveEnabled;
  final bool monitoringEnabled;
  final int minOccurrences;
  final int windowSeconds;
  final int cooldownMs;
  final String countMode; // UNIQUE_PER_SNAPSHOT | ALL_MATCHES
  final bool blockHighSeverityOnly;
  final int debounceMs;

  const DetectionSettings({
    this.liveEnabled = false,
    this.monitoringEnabled = true,
    this.minOccurrences = 3,
    this.windowSeconds = 300,
    this.cooldownMs = 1000,
    this.countMode = 'UNIQUE_PER_SNAPSHOT',
    this.blockHighSeverityOnly = false,
    this.debounceMs = 250,
  });

  DetectionSettings copyWith({
    bool? liveEnabled,
    bool? monitoringEnabled,
    int? minOccurrences,
    int? windowSeconds,
    int? cooldownMs,
    String? countMode,
    bool? blockHighSeverityOnly,
    int? debounceMs,
  }) => DetectionSettings(
        liveEnabled: liveEnabled ?? this.liveEnabled,
        monitoringEnabled: monitoringEnabled ?? this.monitoringEnabled,
        minOccurrences: minOccurrences ?? this.minOccurrences,
        windowSeconds: windowSeconds ?? this.windowSeconds,
        cooldownMs: cooldownMs ?? this.cooldownMs,
        countMode: countMode ?? this.countMode,
        blockHighSeverityOnly: blockHighSeverityOnly ?? this.blockHighSeverityOnly,
        debounceMs: debounceMs ?? this.debounceMs,
      );

  Map<String, dynamic> toMap() => {
        'liveEnabled': liveEnabled,
        'monitoringEnabled': monitoringEnabled,
        'minOccurrences': minOccurrences,
        'windowSeconds': windowSeconds,
        'cooldownMs': cooldownMs,
        'countMode': countMode,
        'blockHighSeverityOnly': blockHighSeverityOnly,
        'debounceMs': debounceMs,
      };

  static DetectionSettings fromMap(Map<dynamic, dynamic>? map) {
    if (map == null) return const DetectionSettings();
    String cm = (map['countMode'] as String?) ?? 'UNIQUE_PER_SNAPSHOT';
    if (cm != 'UNIQUE_PER_SNAPSHOT' && cm != 'ALL_MATCHES') cm = 'UNIQUE_PER_SNAPSHOT';
    int clampPosInt(dynamic v, int min, int def) {
      final n = (v is num) ? v.toInt() : def;
      return n < min ? min : n;
    }
    return DetectionSettings(
      liveEnabled: (map['liveEnabled'] as bool?) ?? false,
      monitoringEnabled: (map['monitoringEnabled'] as bool?) ?? true,
      minOccurrences: clampPosInt(map['minOccurrences'], 1, 3),
      windowSeconds: clampPosInt(map['windowSeconds'], 1, 300),
      cooldownMs: clampPosInt(map['cooldownMs'], 50, 1000),
      countMode: cm,
      blockHighSeverityOnly: (map['blockHighSeverityOnly'] as bool?) ?? false,
      debounceMs: clampPosInt(map['debounceMs'], 50, 250),
    );
  }

  String toJson() => jsonEncode(toMap());
  static DetectionSettings fromJson(String? json) {
    if (json == null || json.isEmpty) return const DetectionSettings();
    try { return fromMap(jsonDecode(json) as Map<String, dynamic>); } catch (_) { return const DetectionSettings(); }
  }

  static const _prefsKey = 'detection_settings_v1';

  static Future<DetectionSettings> load() async {
    final prefs = await SharedPreferences.getInstance();
    return fromJson(prefs.getString(_prefsKey));
  }

  Future<void> save() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefsKey, toJson());
  }
}
