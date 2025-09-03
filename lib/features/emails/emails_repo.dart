import 'package:shared_preferences/shared_preferences.dart';

class EmailsRepo {
  static const _kUser = 'emails_user_v1';
  static final _re = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]{2,}$');

  bool isValid(String input) => _re.hasMatch(input.trim().toLowerCase());

  Future<List<String>> getUserEmails() async {
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    list.sort();
    return list;
  }

  Future<bool> add(String input) async {
    final v = input.trim().toLowerCase();
    if (!isValid(v)) return false;
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    if (list.contains(v)) return false;
    list..add(v)..sort();
    return sp.setStringList(_kUser, list);
  }

  Future<bool> edit(String oldEmail, String newEmail) async {
    final oldV = oldEmail.trim().toLowerCase();
    final newV = newEmail.trim().toLowerCase();
    if (!isValid(newV)) return false;
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    final idx = list.indexOf(oldV);
    if (idx == -1 || list.contains(newV)) return false;
    list[idx] = newV;
    list.sort();
    return sp.setStringList(_kUser, list);
  }

  Future<bool> remove(String email) async {
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    final ok = list.remove(email.trim().toLowerCase());
    if (ok) await sp.setStringList(_kUser, list);
    return ok;
  }
}
