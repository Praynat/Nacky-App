import 'package:flutter/services.dart' show rootBundle;
import 'package:shared_preferences/shared_preferences.dart';

class SitesRepo {
  static const _kUser = 'sites_user_v1';
  static const _seedAsset = 'assets/AdultWebsitesFilterList.txt';

  // --- Normalisation minimale d’un domaine/URL vers un hostname ---
  String normalizeHost(String input) {
    var s = input.trim().toLowerCase();
    if (s.isEmpty) return '';
    // enlève scheme
    if (s.startsWith('http://'))  s = s.substring(7);
    if (s.startsWith('https://')) s = s.substring(8);
    // enlève chemin/params
    final slash = s.indexOf('/');
    if (slash != -1) s = s.substring(0, slash);
    // enlève port
    final colon = s.indexOf(':');
    if (colon != -1) s = s.substring(0, colon);
    // enlève www.
    if (s.startsWith('www.')) s = s.substring(4);
    // simple validation : a.b, pas d’espace
    final re = RegExp(r'^[a-z0-9.-]+\.[a-z]{2,}$');
    return re.hasMatch(s) ? s : '';
  }

  Future<List<String>> _readSeed() async {
    final txt = await rootBundle.loadString(_seedAsset);
    final set = <String>{};
    for (final line in txt.split(RegExp(r'\r?\n'))) {
      final h = normalizeHost(line);
      if (h.isNotEmpty) set.add(h);
    }
    final list = set.toList()..sort();
    return list;
  }

  /// Pour le moteur de filtrage (seed + user), non affiché
  Future<List<String>> getAllForFilter() async {
    final seed = await _readSeed();
    final user = await getUserSites();
    return {...seed, ...user}.toList();
  }

  /// Pour l’UI manage (uniquement les ajouts user)
  Future<List<String>> getUserSites() async {
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    list.sort();
    return list;
  }

  Future<bool> add(String input) async {
    final host = normalizeHost(input);
    if (host.isEmpty) return false;
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    if (list.contains(host)) return false;
    list..add(host)..sort();
    return sp.setStringList(_kUser, list);
  }

  Future<bool> edit(String oldHost, String newInput) async {
    final oldN = normalizeHost(oldHost);
    final newN = normalizeHost(newInput);
    if (oldN.isEmpty || newN.isEmpty) return false;
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    final idx = list.indexOf(oldN);
    if (idx == -1) return false;
    if (list.contains(newN)) return false;
    list[idx] = newN;
    list.sort();
    return sp.setStringList(_kUser, list);
  }

  Future<bool> remove(String host) async {
    final h = normalizeHost(host);
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    final ok = list.remove(h);
    if (ok) await sp.setStringList(_kUser, list);
    return ok;
  }
}
