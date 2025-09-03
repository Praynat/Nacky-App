import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

class SettingsMenuScreen extends StatelessWidget {
  const SettingsMenuScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Paramètres')),
      body: ListView(
        children: [
          ListTile(
            leading: const Icon(Icons.password),
            title: const Text('Mot de passe'),
            subtitle: const Text('Définir ou changer le code PIN gardien'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/settings/password'),
          ),
          const Divider(height: 1),
          // Tu pourras ajouter d’autres entrées ici (Thème, Permissions Android, etc.)
        ],
      ),
    );
  }
}
