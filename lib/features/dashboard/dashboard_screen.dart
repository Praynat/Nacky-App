import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../theme.dart';
import '../../models/status.dart';
import 'mascot_header.dart';
import 'kpi_card.dart';
import 'module_row.dart';
import 'activity_list.dart';
import 'quick_action_button.dart';
import '../../core/platform/android_bridge.dart';
import '../words/words_repo.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool filterOn = true;
  NackyStatus forcedStatus = NackyStatus.ok; // pour tests rapides via menu

  NackyStatus get status {
    if (!filterOn) return NackyStatus.off;
    return forcedStatus; // ok / minor / warn / critical
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NackyTheme.surface,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ===== Header (mascotte + état + toggle + menu test) =====
              MascotHeader(
                status: status,
                title: filterOn ? "Protection active" : "Protection désactivée",
                subtitle: "Nacky surveille et protège votre navigation",
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Switch(
                      value: filterOn,
                      onChanged: (v) => setState(() => filterOn = v),
                    ),
                    const SizedBox(width: 8),
                    PopupMenuButton<NackyStatus>(
                      tooltip: 'Changer l’état (test)',
                      icon: const Icon(Icons.more_vert),
                      onSelected: (s) => setState(() => forcedStatus = s),
                      itemBuilder: (_) => const [
                        PopupMenuItem(value: NackyStatus.ok,       child: Text('OK (vert)')),
                        PopupMenuItem(value: NackyStatus.minor,    child: Text('Minor (jaune)')),
                        PopupMenuItem(value: NackyStatus.warn,     child: Text('Warning (orange)')),
                        PopupMenuItem(value: NackyStatus.critical, child: Text('Critical (rouge)')),
                        PopupMenuItem(value: NackyStatus.off,      child: Text('OFF (gris)')),
                      ],
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 28),

              // ===== KPIs (responsive) =====
              LayoutBuilder(
                builder: (context, c) {
                  final isWide = c.maxWidth >= 720;
                  final kpis = const [
                    KpiCard(icon: Icons.gpp_good, value: "32", label: "Mots surveillés"),
                    KpiCard(icon: Icons.block,    value: "4",  label: "Apps bloquées"),
                    KpiCard(icon: Icons.email,    value: "2",  label: "Références e-mail"),
                  ];
                  if (isWide) {
                    return Row(
                      children: [
                        Expanded(child: kpis[0]),
                        const SizedBox(width: 12),
                        Expanded(child: kpis[1]),
                        const SizedBox(width: 12),
                        Expanded(child: kpis[2]),
                      ],
                    );
                  }
                  return Column(
                    children: [
                      kpis[0],
                      const SizedBox(height: 12),
                      kpis[1],
                      const SizedBox(height: 12),
                      kpis[2],
                    ],
                  );
                },
              ),

              const SizedBox(height: 28),

              // ===== Modules de protection =====
              Text("Modules de protection", style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: const Column(
                  children: [
                    ModuleRow(icon: Icons.keyboard_alt, title: "Filtrage à la saisie", enabled: true),
                    ModuleRow(icon: Icons.visibility,   title: "Détection visuelle",  enabled: true),
                    ModuleRow(icon: Icons.app_blocking, title: "Blocage d’apps",      enabled: true),
                  ],
                ),
              ),

              const SizedBox(height: 28),

              // ===== Activité récente =====
              Text("Activité récente", style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: ActivityList(items: [
                    ActivityItem("Mot interdit détecté", "“xxx” dans Chrome • il y a 2 min", NackyTheme.critical),
                    ActivityItem("Application fermée",   "TikTok • il y a 7 min",             NackyTheme.minor),
                    ActivityItem("Email d’alerte envoyé","Perte d’accessibilité • il y a 12 min", NackyTheme.warning),
                  ]),
                ),
              ),

              const SizedBox(height: 28),

              // ===== Actions rapides =====
              Text("Actions rapides", style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12, runSpacing: 12,
                children: [
                  QuickActionButton(
                    icon: Icons.add, label: "Ajouter un mot interdit",
                    onPressed: () => context.push('/words'),
                  ),
                  QuickActionButton(
                    icon: Icons.block, label: "Bloquer une app",
                    onPressed: () => context.push('/apps'),
                  ),
                  QuickActionButton(
                    icon: Icons.email, label: "Ajouter un e-mail",
                    onPressed: () => context.push('/emails'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
@override
void initState() {
  super.initState();
  _pushWordsToAndroid();
}

Future<void> _pushWordsToAndroid() async {
  try {
    final words = await WordsRepo().getAllForFilter(); // seed + user, déjà normalisés
    await AndroidBridge.sendWordList(words);
  } catch (_) {
    // silencieux
  }
}

}
