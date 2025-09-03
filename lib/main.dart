import 'package:flutter/material.dart';
import 'theme.dart';
import 'app/router.dart';

void main() => runApp(const NackyApp());

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
