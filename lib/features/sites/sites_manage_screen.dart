import 'package:flutter/material.dart';
import 'sites_repo.dart';
import '../guardian/pin_dialog.dart';

class SitesManageScreen extends StatefulWidget {
  const SitesManageScreen({super.key});
  @override State<SitesManageScreen> createState() => _SitesManageScreenState();
}

class _SitesManageScreenState extends State<SitesManageScreen> {
  final _repo = SitesRepo();

  Future<bool> _requireGuardian() async {
  return await requireGuardian(context, title: 'Enter PIN to manage words');
}

  Future<void> _editSite(String oldHost) async {
    if (!await _requireGuardian()) return;
    final controller = TextEditingController(text: oldHost);
    final result = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text("Edit site"),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(border: OutlineInputBorder()),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(dialogContext).pop(controller.text.trim()),
            child: const Text("Save"),
          ),
        ],
      ),
    );
    if (result != null && result.isNotEmpty && result != oldHost) {
      final ok = await _repo.edit(oldHost, result);
      if (!mounted) return;
      if (!ok) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Édition impossible (doublon ou invalide).')),
        );
      }
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _requireGuardian(),
      builder: (context, s) {
        if (s.connectionState != ConnectionState.done) {
          return const Scaffold(body: Center(child: CircularProgressIndicator()));
        }
        if (s.data != true) {
          return const Scaffold(body: Center(child: Text('Accès refusé.')));
        }
        return Scaffold(
          appBar: AppBar(title: const Text('Manage custom blocked sites')),
          body: FutureBuilder<List<String>>(
            future: _repo.getUserSites(),
            builder: (context, snap) {
              final data = snap.data ?? const <String>[];
              if (data.isEmpty) {
                return const Center(child: Text('Aucun site ajouté pour l’instant.'));
              }
              return ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: data.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (_, i) {
                  final host = data[i];
                  return ListTile(
                    title: Text(host),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(icon: const Icon(Icons.edit), onPressed: () => _editSite(host)),
                        IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () async {
                            final ok = await _repo.remove(host);
                            if (!mounted) return;
                            if (!ok) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('Suppression impossible.')),
                              );
                            }
                            setState(() {});
                          },
                        ),
                      ],
                    ),
                  );
                },
              );
            },
          ),
        );
      },
    );
  }
}
