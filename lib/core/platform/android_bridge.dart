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
}
