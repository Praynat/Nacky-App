import 'package:flutter/material.dart';
import 'apps_repo.dart';
import '../guardian/pin_dialog.dart'; 

class AppsScreen extends StatefulWidget {
  const AppsScreen({super.key});
  @override
  State<AppsScreen> createState() => _AppsScreenState();
}

class _AppsScreenState extends State<AppsScreen> {
  final _repo = AppsRepo();
  final _q = TextEditingController();

  // Mock de quelques apps (remplacé plus tard par la vraie liste Android)
  final _all = const [
    ('com.google.android.youtube', 'YouTube'),
    ('com.zhiliaoapp.musically', 'TikTok'),
    ('com.instagram.android', 'Instagram'),
    ('org.mozilla.firefox', 'Firefox'),
    ('com.android.chrome', 'Chrome'),
  ];

  Set<String> _blocked = {};
  String get query => _q.text.trim().toLowerCase();

Future<bool> _requireGuardian() async {
  return await requireGuardian(context, title: 'Enter PIN to unblock');
}
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    _blocked = await _repo.getBlocked();
    if (mounted) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _all.where((e) {
      if (query.isEmpty) return true;
      return e.$1.contains(query) || e.$2.toLowerCase().contains(query);
    }).toList();

    return Scaffold(
      appBar: AppBar(title: const Text('Applications')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: _q,
              onChanged: (_) => setState(() {}),
              decoration: const InputDecoration(
                hintText: 'Rechercher…',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
            ),
          ),
          Expanded(
            child: ListView.separated(
              itemCount: filtered.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (_, i) {
                final (id, name) = filtered[i];
                final isBlocked = _blocked.contains(id);

                return ListTile(
                  title: Text(name),
                  subtitle: Text(
                    id,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  trailing: Switch(
                    value: isBlocked,

                    // Icon inside the thumb
                    thumbIcon: WidgetStateProperty.resolveWith<Icon?>((states) {
                      final on = states.contains(WidgetState.selected);
                      return Icon(
                        on ? Icons.lock : Icons.lock_open,
                        size: 16,
                        color: on ? Colors.red : Colors.grey[700],
                      );
                    }),

                    // Thumb always white
                    thumbColor: WidgetStateProperty.resolveWith<Color?>((states) {
                      return Colors.white;
                    }),

                    // Track red when ON, grey when OFF
                    trackColor: WidgetStateProperty.resolveWith<Color?>((states) {
                      return states.contains(WidgetState.selected)
                          ? Colors.red
                          : Colors.grey[400];
                    }),

                    onChanged: (v) async {
                      if (v) {
                        await _repo.block(id);
                        _blocked.add(id);
                      } else {
                        if (!await _requireGuardian()) return;
                        final ok = await _repo.unblock(id);
                        if (ok) _blocked.remove(id);
                      }
                      setState(() {});
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
