import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'words_repo.dart';
import '../../core/platform/android_bridge.dart';


class WordsAddScreen extends StatefulWidget {
  const WordsAddScreen({super.key});
  @override State<WordsAddScreen> createState() => _WordsAddScreenState();
}

class _WordsAddScreenState extends State<WordsAddScreen> {
  final _repo = WordsRepo();
  final _c = TextEditingController();
  bool _adding = false;

  Future<void> _add() async {
    final v = _c.text.trim();
    if (v.isEmpty || _adding) return;
    setState(() => _adding = true);
    final ok = await _repo.add(v);
    if (!mounted) return;
    if (ok) {
    final words = await _repo.getAllForFilter(); 
    await AndroidBridge.sendWordList(words);    
  }                                             
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(ok ? 'Ajouté.' : 'Déjà présent ou invalide.'),
    ));
    _c.clear();
    setState(() => _adding = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mots interdits')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(children: [
              Expanded(child: TextField(
                controller: _c,
                textInputAction: TextInputAction.done,
                onSubmitted: (_) => _add(),
                decoration: const InputDecoration(
                  hintText: 'Ajouter un mot',
                  border: OutlineInputBorder(),
                ),
              )),
              const SizedBox(width: 8),
              FilledButton(
                onPressed: _adding ? null : _add,
                child: const Text('Ajouter'),
              ),
            ]),
            const SizedBox(height: 24),
            Center(
              child: OutlinedButton.icon(
                onPressed: () => context.push('/words/manage'),
                icon: const Icon(Icons.lock),
                label: const Text('Manage custom word list'),
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              "La liste par défaut est cachée. Vous ne voyez pas vos mots ici.\n"
              "Utilisez « Manage custom word list » pour consulter/supprimer/éditer.",
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
