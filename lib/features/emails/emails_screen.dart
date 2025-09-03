import 'package:flutter/material.dart';
import 'emails_repo.dart';
import '../guardian/pin_dialog.dart';

class EmailsScreen extends StatefulWidget {
  const EmailsScreen({super.key});
  @override State<EmailsScreen> createState() => _EmailsScreenState();
}

class _EmailsScreenState extends State<EmailsScreen> {
  final _repo = EmailsRepo();
  final _c = TextEditingController();
  bool _adding = false;

  Future<bool> _requireGuardian() async {
  return await requireGuardian(context, title: 'Enter PIN to modify emails');
}

  Future<void> _add() async {
    final v = _c.text.trim();
    if (v.isEmpty || _adding) return;
    setState(() => _adding = true);
    final ok = await _repo.add(v);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? 'Ajouté.' : 'Email invalide ou déjà présent.')),
    );
    if (ok) _c.clear();
    setState(() => _adding = false);
  }

  Future<void> _edit(String oldEmail) async {
    if (!await _requireGuardian()) return;
    final c = TextEditingController(text: oldEmail);
    final result = await showDialog<String>(
      context: context,
      builder: (dCtx) => AlertDialog(
        title: const Text("Edit email"),
        content: TextField(
          controller: c,
          autofocus: true,
          keyboardType: TextInputType.emailAddress,
          decoration: const InputDecoration(border: OutlineInputBorder()),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dCtx).pop(), child: const Text("Cancel")),
          ElevatedButton(onPressed: () => Navigator.of(dCtx).pop(c.text.trim()), child: const Text("Save")),
        ],
      ),
    );
    if (result != null && result.isNotEmpty && result != oldEmail) {
      final ok = await _repo.edit(oldEmail, result);
      if (!mounted) return;
      if (!ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Édition impossible (invalide ou doublon).')),
        );
      }
      setState(() {});
    }
  }

  Future<void> _delete(String email) async {
    if (!await _requireGuardian()) return;
    final ok = await _repo.remove(email);
    if (!mounted) return;
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Suppression impossible.')),
      );
    }
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Références e-mail')),
      body: FutureBuilder<List<String>>(
        future: _repo.getUserEmails(),
        builder: (context, snap) {
          final data = snap.data ?? const <String>[];
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Row(children: [
                Expanded(child: TextField(
                  controller: _c,
                  keyboardType: TextInputType.emailAddress,
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => _add(),
                  decoration: const InputDecoration(
                    hintText: 'Ajouter une adresse e-mail',
                    border: OutlineInputBorder(),
                  ),
                )),
                const SizedBox(width: 8),
                FilledButton(onPressed: _adding ? null : _add, child: const Text('Ajouter')),
              ]),
              const SizedBox(height: 16),
              Text('${data.length} adresse(s)', style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 8),
              ...data.map((email) => ListTile(
                title: Text(email),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(icon: const Icon(Icons.edit), onPressed: () => _edit(email)),
                    IconButton(icon: const Icon(Icons.delete_outline), onPressed: () => _delete(email)),
                  ],
                ),
              )),
              if (data.isEmpty)
                const Padding(
                  padding: EdgeInsets.only(top: 32),
                  child: Center(child: Text('Aucune adresse ajoutée.')),
                ),
            ],
          );
        },
      ),
    );
  }
}
