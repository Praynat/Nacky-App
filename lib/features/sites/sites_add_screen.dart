import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'sites_repo.dart';

class SitesAddScreen extends StatefulWidget {
  const SitesAddScreen({super.key});
  @override State<SitesAddScreen> createState() => _SitesAddScreenState();
}

class _SitesAddScreenState extends State<SitesAddScreen> {
  final _repo = SitesRepo();
  final _c = TextEditingController();
  bool _adding = false;

  Future<void> _add() async {
    final v = _c.text.trim();
    if (v.isEmpty || _adding) return;
    setState(() => _adding = true);
    final ok = await _repo.add(v);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? 'Ajouté.' : 'Déjà présent ou invalide.'))
    );
    _c.clear();
    setState(() => _adding = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sites interdits')),
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
                  hintText: 'Ajouter un site (domaine ou URL)',
                  border: OutlineInputBorder(),
                ),
              )),
              const SizedBox(width: 8),
              FilledButton(onPressed: _adding ? null : _add, child: const Text('Ajouter')),
            ]),
            const SizedBox(height: 24),
            Center(
              child: OutlinedButton.icon(
                onPressed: () => context.push('/sites/manage'),
                icon: const Icon(Icons.lock),
                label: const Text('Manage custom blocked sites'),
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              "La liste par défaut est cachée. Vous ne voyez pas vos sites ici.\n"
              "Utilisez « Manage custom blocked sites » pour consulter/éditer/supprimer.",
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
