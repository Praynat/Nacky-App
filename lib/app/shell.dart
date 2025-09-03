import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

enum AppDest { home, words, apps, sites, emails, settings }

class AppShell extends StatelessWidget {
  final Widget child;
  final AppDest current;
  const AppShell({super.key, required this.child, required this.current});

  void _go(BuildContext context, AppDest d) {
    switch (d) {
      case AppDest.home:    context.go('/'); break;
      case AppDest.words:   context.go('/words'); break;
      case AppDest.apps:    context.go('/apps'); break;
      case AppDest.sites:   context.go('/sites'); break;
      case AppDest.emails:  context.go('/emails'); break;
      case AppDest.settings:context.go('/settings'); break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      final w = c.maxWidth;
      final isSmall = w < 840;
      final isLarge = w >= 1200;

      // --- Drawer (petit écran) ---
      if (isSmall) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Nacky'),
            leading: Builder(
              builder: (ctx) => IconButton(
                icon: const Icon(Icons.menu),
                onPressed: () => Scaffold.of(ctx).openDrawer(),
              ),
            ),
          ),
          drawer: _DrawerMenu(current: current, onTap: (d) => _go(context, d)),
          body: child,
        );
      }

      // --- NavigationRail (moyen/large) ---
      return Scaffold(
        body: Row(
          children: [
            NavigationRail(
              extended: isLarge,
              selectedIndex: current.index,
              onDestinationSelected: (i) => _go(context, AppDest.values[i]),
              labelType: isLarge ? NavigationRailLabelType.none : NavigationRailLabelType.all,
              destinations: const [
                NavigationRailDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: Text('Accueil')),
                NavigationRailDestination(icon: Icon(Icons.shield),        selectedIcon: Icon(Icons.shield), label: Text('Mots')),
                NavigationRailDestination(icon: Icon(Icons.apps),          selectedIcon: Icon(Icons.apps),   label: Text('Applications')),
                NavigationRailDestination(icon: Icon(Icons.public),        selectedIcon: Icon(Icons.public), label: Text('Sites')),
                NavigationRailDestination(icon: Icon(Icons.email),         selectedIcon: Icon(Icons.email),  label: Text('Emails')),
                NavigationRailDestination(icon: Icon(Icons.settings),      selectedIcon: Icon(Icons.settings),label: Text('Paramètres')),
              ],
            ),
            const VerticalDivider(width: 1),
            Expanded(child: child),
          ],
        ),
      );
    });
  }
}

class _DrawerMenu extends StatelessWidget {
  final AppDest current;
  final ValueChanged<AppDest> onTap;
  const _DrawerMenu({required this.current, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: ListView(padding: EdgeInsets.zero, children: [
        const DrawerHeader(child: Text('Nacky')),
        _tile(context, AppDest.home,    Icons.home,      'Accueil'),
        _tile(context, AppDest.words,   Icons.shield,    'Mots interdits'),
        _tile(context, AppDest.apps,    Icons.apps,      'Applications'),
        _tile(context, AppDest.sites,   Icons.public,    'Sites Interdits'),
        _tile(context, AppDest.emails,  Icons.email,     'Références e-mail'),
        const Divider(),
        _tile(context, AppDest.settings,Icons.settings,  'Paramètres'),
      ]),
    );
  }

  Widget _tile(BuildContext context, AppDest d, IconData ic, String label) {
    final selected = d == current;
    return ListTile(
      leading: Icon(ic),
      title: Text(label),
      selected: selected,
      onTap: () { Navigator.of(context).pop(); onTap(d); },
    );
  }
}
