import 'package:flutter/material.dart';

class NackyTheme {
  static const primary  = Color(0xFF28C76F); // OK
  static const warning  = Color(0xFFFFB547);
  static const minor    = Color(0xFFFF7F50);
  static const critical = Color(0xFFEA5455);
  static const ink      = Color(0xFF0E1726);
  static const muted    = Color(0xFF5E6B7E);
  static const surface  = Color(0xFFFFFFFF);
  static const card     = Color(0xFFF6F8FB);
  static const divider  = Color(0xFFE6EAF0);

  static ThemeData light() {
    final base = ThemeData.light(useMaterial3: true);
    return base.copyWith(
      colorScheme: base.colorScheme.copyWith(
        primary: primary, error: critical, surface: surface, onSurface: ink,
      ),
      scaffoldBackgroundColor: surface,
      cardColor: card,
      dividerColor: divider,
      textTheme: base.textTheme.copyWith(
        headlineMedium: const TextStyle(fontSize: 28, fontWeight: FontWeight.w700, color: ink),
        titleLarge:   const TextStyle(fontSize: 22, fontWeight: FontWeight.w700, color: ink),
        bodyMedium:   const TextStyle(fontSize: 16, color: ink),
        bodySmall:    const TextStyle(fontSize: 13, color: muted),
      ),
    );
  }
}
