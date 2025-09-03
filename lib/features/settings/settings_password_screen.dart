import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../guardian/guardian_repo.dart';

class SettingsPasswordScreen extends StatefulWidget {
  const SettingsPasswordScreen({super.key});
  @override
  State<SettingsPasswordScreen> createState() => _SettingsPasswordScreenState();
}

class _SettingsPasswordScreenState extends State<SettingsPasswordScreen> {
  bool _hasPin = false;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final has = await GuardianRepo.I.hasPin();
    if (!mounted) return;
    setState(() { _hasPin = has; _loading = false; });
  }

  Future<void> _openSetPin() async {
    await context.push('/settings/set-pin');
    _load(); // rafraîchit l’état des boutons au retour
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Mot de passe')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            elevation: 0,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  const Icon(Icons.admin_panel_settings),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _hasPin ? 'Mot de passe administrateur : défini'
                              : 'Mot de passe administrateur : non défini',
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: (_hasPin ? Colors.green : Colors.grey).withOpacity(0.12),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(
                      _hasPin ? 'ACTIF' : 'INACTIF',
                      style: TextStyle(
                        color: _hasPin ? Colors.green : Colors.grey,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _hasPin ? null : _openSetPin,
            icon: const Icon(Icons.lock),
            label: const Text('Définir mot de passe'),
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: _hasPin ? _openSetPin : null,
            icon: const Icon(Icons.lock_reset),
            label: const Text('Changer mot de passe'),
          ),
        ],
      ),
    );
  }
}
