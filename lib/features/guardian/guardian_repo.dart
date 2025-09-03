import 'package:shared_preferences/shared_preferences.dart';

class GuardianRepo {
  static const _kPin = 'guardian_pin_v1';
  static const _kSessionMinutes = 10;

  static final GuardianRepo I = GuardianRepo._();
  GuardianRepo._();

  DateTime? _sessionUntil; // en mémoire

  Future<bool> hasPin() async {
    final sp = await SharedPreferences.getInstance();
    return (sp.getString(_kPin) ?? '').isNotEmpty;
  }

  Future<bool> setPin(String pin) async {
    if (!_isValidPin(pin)) return false;
    final sp = await SharedPreferences.getInstance();
    return sp.setString(_kPin, pin);
  }

  Future<bool> verifyPin(String pin) async {
    final sp = await SharedPreferences.getInstance();
    final saved = sp.getString(_kPin) ?? '';
    final ok = saved.isNotEmpty ? saved == pin : true; // s’il n’y a pas de PIN, autoriser
    if (ok) _extendSession();
    return ok;
  }

  bool isSessionActive() {
    final now = DateTime.now();
    return _sessionUntil != null && now.isBefore(_sessionUntil!);
  }

  void _extendSession() {
    _sessionUntil = DateTime.now().add(const Duration(minutes: _kSessionMinutes));
  }

  bool _isValidPin(String pin) {
    final p = pin.trim();
    if (p.length < 4 || p.length > 8) return false;
    for (final ch in p.runes) {
      if (ch < 0x30 || ch > 0x39) return false; // digits only
    }
    return true;
  }
}
