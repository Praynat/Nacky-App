import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'shell.dart';
import '../features/dashboard/dashboard_screen.dart';
import '../features/apps/apps_screen.dart';
import '../features/sites/sites_add_screen.dart';
import '../features/sites/sites_manage_screen.dart';
import '../features/emails/emails_screen.dart';
import '../features/settings/settings_menu_screen.dart';
import '../features/settings/settings_password_screen.dart';
import '../features/guardian/set_pin_screen.dart';
import '../features/words/words_add_screen.dart';
import '../features/words/words_manage_screen.dart';
import '../features/settings/detection_settings_screen.dart';

AppDest _routeToDest(String p) {
  return switch (p) {
    '/' => AppDest.home,
    '/words' => AppDest.words,
    '/apps' => AppDest.apps,
    '/sites' => AppDest.sites,
    '/emails' => AppDest.emails,
    '/settings' => AppDest.settings,
    _ => AppDest.home,
  };
}

final appRouter = GoRouter(
  initialLocation: '/',
  routes: [
    ShellRoute(
      builder: (context, state, child) {
        final dest = _routeToDest(state.uri.toString());
        return AppShell(current: dest, child: child);
      },
      routes: [
        GoRoute(path: '/', builder: (_, __) => const DashboardScreen()),
        GoRoute(path: '/words', builder: (_, __) => const WordsAddScreen()),
        GoRoute(
          path: '/words/manage',
          builder: (_, __) => const WordsManageScreen(),
        ),
        GoRoute(path: '/sites', builder: (_, __) => const SitesAddScreen()),
        GoRoute(
          path: '/sites/manage',
          builder: (_, __) => const SitesManageScreen(),
        ),
        GoRoute(path: '/apps', builder: (_, __) => const AppsScreen()),
        GoRoute(path: '/emails', builder: (_, __) => const EmailsScreen()),
       GoRoute(path: '/settings',           builder: (_, __) => const SettingsMenuScreen()),
GoRoute(path: '/settings/password',  builder: (_, __) => const SettingsPasswordScreen()),
GoRoute(path: '/settings/set-pin',   builder: (_, __) => const SetPinScreen()),
GoRoute(path: '/settings/detection', builder: (_, __) => const DetectionSettingsScreen()),
      ],
    ),
  ],
);
