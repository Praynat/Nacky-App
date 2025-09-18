import 'dart:io' show Platform;
import 'package:flutter/services.dart';

class AndroidBridge {
  static const _m = MethodChannel('nacky/android');

  static Future<bool> isAccessibilityEnabled() async {
    if (!Platform.isAndroid) return false;
    final v = await _m.invokeMethod<bool>('isAccessibilityEnabled');
    return v ?? false;
  }

  static Future<void> requestAccessibilityPermission() async {
    if (!Platform.isAndroid) return;
    await _m.invokeMethod('requestAccessibilityPermission');
  }

  static Future<void> sendWordList(List<String> words) async {
    if (!Platform.isAndroid) return;
    await _m.invokeMethod('sendWordList', {'words': words});
  }

  /// Temporary V2 placeholder: send structured pattern config to Android.
  /// Will evolve to include multi-phase detection metadata. For now just
  /// sends a stub payload so Android side can log receipt.
  static Future<void> updatePatterns({
    List<String>? words,
    Map<String, dynamic>? meta,
  }) async {
    if (!Platform.isAndroid) return;
    final payload = <String, dynamic>{
      'version': 1,
      'timestamp': DateTime.now().toIso8601String(),
      'words': words ?? <String>[],
      'meta': meta ?? <String, dynamic>{'phase': 'monitoring-draft'},
    };
    await _m.invokeMethod('updatePatterns', payload);
  }
}
