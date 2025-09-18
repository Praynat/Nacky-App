import 'package:flutter/material.dart';
import 'words_repo.dart';
import '../guardian/pin_dialog.dart';
import '../../core/platform/android_bridge.dart';

class WordsManageScreen extends StatefulWidget {
  const WordsManageScreen({super.key});
  @override
  State<WordsManageScreen> createState() => _WordsManageScreenState();
}

class _WordsManageScreenState extends State<WordsManageScreen> {
  final _repo = WordsRepo();

  Future<bool> _requireGuardian() async {
    return await requireGuardian(context, title: 'Enter PIN to manage words');
  }

  Future<void> _editWord(String oldWord) async {
    if (!await _requireGuardian()) return;
    final controller = TextEditingController(text: oldWord);
    final result = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text("Edit word"),
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
            onPressed: () =>
                Navigator.of(dialogContext).pop(controller.text.trim()),
            child: const Text("Save"),
          ),
        ],
      ),
    );

    if (result != null && result.isNotEmpty && result != oldWord) {
      await _repo.remove(oldWord);
      await _repo.add(result);
      final words = await _repo.getAllForFilter();
      await AndroidBridge.sendWordList(words);
      final patterns = await _repo.buildPatterns();
      await AndroidBridge.updatePatterns(
        words: words,
        meta: {
          'source': 'words_manage_screen',
          'reason': 'edit_word',
          'patterns_count': patterns.length,
          'items_total': words.length,
        },
      );
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _requireGuardian(),
      builder: (context, s) {
        if (s.connectionState != ConnectionState.done) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        if (s.data != true) {
          return const Scaffold(body: Center(child: Text('Accès refusé.')));
        }
        return Scaffold(
          appBar: AppBar(title: const Text('Manage custom word list')),
          body: FutureBuilder<List<String>>(
            future: _repo.getUserWords(),
            builder: (context, snap) {
              final data = snap.data ?? const <String>[];
              if (data.isEmpty) {
                return const Center(
                  child: Text('Aucun mot ajouté pour l’instant.'),
                );
              }
              return ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: data.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (_, i) {
                  final w = data[i];
                  return ListTile(
                    title: Text(w),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          icon: const Icon(Icons.edit),
                          onPressed: () => _editWord(w),
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () async {
                            final ok = await _repo.remove(w);
                            if (!mounted) return;
                            if (!ok) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('Suppression impossible.'),
                                ),
                              );
                            } else {
                              final words = await _repo.getAllForFilter();
                              await AndroidBridge.sendWordList(words);
                              final patterns = await _repo.buildPatterns();
                              await AndroidBridge.updatePatterns(
                                words: words,
                                meta: {
                                  'source': 'words_manage_screen',
                                  'reason': 'delete_word',
                                  'patterns_count': patterns.length,
                                  'items_total': words.length,
                                },
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
