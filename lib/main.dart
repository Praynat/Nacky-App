import 'package:flutter/material.dart';
import 'theme.dart';
import 'app/router.dart';
import 'features/settings/detection_settings.dart';
import 'core/platform/android_bridge.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final settings = await DetectionSettings.load();
  await AndroidBridge.updateSettings(settings.toMap());
  runApp(const NackyApp());
}

class NackyApp extends StatelessWidget {
  const NackyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Nacky',
      theme: NackyTheme.light(),
      routerConfig: appRouter,
      debugShowCheckedModeBanner: false,
    );
  }
}
