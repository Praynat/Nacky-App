import 'package:flutter/services.dart' show rootBundle;
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/normalize.dart';

class WordsRepo {
  static const _kUser = 'words_user_v1';
  static const _seedAsset = 'assets/DefaultWords.txt';

  Future<List<String>> _readSeed() async {
    final txt = await rootBundle.loadString(_seedAsset);
    return txt
        .split(RegExp(r'\r?\n'))
        .map((e) => normalizeWord(e))
        .where((e) => e.isNotEmpty)
        .toSet()
        .toList();
  }

  /// Pour le moteur de filtrage (seed + user), non affiché
  Future<List<String>> getAllForFilter() async {
    final seed = await _readSeed();
    final user = await getUserWords();
    return {...seed, ...user}.toList();
  }

  /// Pour l’affichage (uniquement mots ajoutés par l’utilisateur)
  Future<List<String>> getUserWords() async {
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    list.sort();
    return list;
  }

  Future<bool> add(String w) async {
    final word = normalizeWord(w);
    if (word.isEmpty) return false;
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    if (list.contains(word)) return false;
    list..add(word)..sort();
    return sp.setStringList(_kUser, list);
  }

  Future<bool> remove(String w) async {
    final word = normalizeWord(w);
    final sp = await SharedPreferences.getInstance();
    final list = sp.getStringList(_kUser) ?? <String>[];
    final ok = list.remove(word);
    if (ok) await sp.setStringList(_kUser, list);
    return ok;
  }
}
