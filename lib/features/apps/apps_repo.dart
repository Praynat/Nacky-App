import 'package:shared_preferences/shared_preferences.dart';

class AppsRepo {
  static const _k = 'blocked_apps_v1';

  Future<Set<String>> getBlocked() async {
    final sp = await SharedPreferences.getInstance();
    return (sp.getStringList(_k) ?? const <String>[]).toSet();
  }

  Future<void> setBlocked(Set<String> ids) async {
    final sp = await SharedPreferences.getInstance();
    await sp.setStringList(_k, ids.toList()..sort());
  }

  Future<void> block(String id) async {
    final s = await getBlocked(); s.add(id); await setBlocked(s);
  }

  Future<bool> unblock(String id) async {
    final s = await getBlocked();
    final ok = s.remove(id);
    if (ok) await setBlocked(s);
    return ok;
  }
}
