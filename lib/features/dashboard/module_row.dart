import 'package:flutter/material.dart';
import '../../theme.dart';

class ModuleRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final bool enabled;
  const ModuleRow({super.key, required this.icon, required this.title, required this.enabled});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 12),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: NackyTheme.divider)),
      ),
      child: Row(
        children: [
          Icon(icon, size: 20),
          const SizedBox(width: 12),
          Expanded(child: Text(title, style: Theme.of(context).textTheme.bodyMedium)),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: (enabled ? NackyTheme.primary : NackyTheme.muted).withOpacity(0.12),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              enabled ? 'Actif' : 'Inactif',
              style: TextStyle(color: enabled ? NackyTheme.primary : NackyTheme.muted, fontWeight: FontWeight.w600),
            ),
          )
        ],
      ),
    );
  }
}
