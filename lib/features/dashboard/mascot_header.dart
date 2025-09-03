import 'package:flutter/material.dart';
import '../../models/status.dart';
import '../../theme.dart';

class MascotHeader extends StatelessWidget {
  final NackyStatus status;
  final String title;
  final String subtitle;
  final Widget? trailing;

  const MascotHeader({
    super.key,
    required this.status,
    required this.title,
    required this.subtitle,
    this.trailing,
  });

  String _assetFor(NackyStatus s) {
    switch (s) {
      case NackyStatus.ok:       return 'assets/mascot_ok.png';
      case NackyStatus.minor:    return 'assets/mascot_minor.png';    // jaune (faible)
      case NackyStatus.warn:     return 'assets/mascot_warn.png';     // orange (plus grave)
      case NackyStatus.critical: return 'assets/mascot_critical.png'; // rouge
      case NackyStatus.off:      return 'assets/mascot_off.png';      // gris
    }
  }

  Color _ringColor(NackyStatus s) {
    switch (s) {
      case NackyStatus.ok:       return NackyTheme.primary;
      case NackyStatus.minor:    return NackyTheme.warning;
      case NackyStatus.warn:     return NackyTheme.minor;
      case NackyStatus.critical: return NackyTheme.critical;
      case NackyStatus.off:      return NackyTheme.muted;
    }
  }

  @override
  Widget build(BuildContext context) {
    final img = Image.asset(_assetFor(status), width: 88, height: 88);
    final ring = _ringColor(status);

    return Column(
      children: [
        Container(
          width: 104, height: 104,
          decoration: BoxDecoration(
            color: ring.withOpacity(0.14),
            shape: BoxShape.circle,
          ),
          alignment: Alignment.center,
          child: img,
        ),
        const SizedBox(height: 16),
        Text(title, style: Theme.of(context).textTheme.headlineMedium, textAlign: TextAlign.center),
        const SizedBox(height: 6),
        Text(subtitle, style: Theme.of(context).textTheme.bodySmall, textAlign: TextAlign.center),
        const SizedBox(height: 12),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _StatusBadge(
              label: status == NackyStatus.ok
                  ? 'ON'
                  : status == NackyStatus.off
                      ? 'OFF'
                      : 'ALERTE',
              color: ring,
            ),
            if (trailing != null) ...[const SizedBox(width: 12), trailing!],
          ],
        ),
      ],
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String label;
  final Color color;
  const _StatusBadge({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(label, style: TextStyle(color: color, fontWeight: FontWeight.w600)),
    );
  }
}
