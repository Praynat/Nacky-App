import 'package:flutter/material.dart';
import 'package:nacky/core/platform/android_bridge.dart';
import 'package:nacky/features/settings/detection_settings.dart';

class DetectionSettingsScreen extends StatefulWidget {
  const DetectionSettingsScreen({super.key});
  @override
  State<DetectionSettingsScreen> createState() => _DetectionSettingsScreenState();
}

class _DetectionSettingsScreenState extends State<DetectionSettingsScreen> {
  DetectionSettings _settings = const DetectionSettings();
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final s = await DetectionSettings.load();
    setState(() { _settings = s; _loading = false; });
    await _push();
  }

  Future<void> _save(DetectionSettings s) async {
    setState(() { _settings = s; });
    await s.save();
    await _push();
  }

  Future<void> _push() async {
    await AndroidBridge.updateSettings(_settings.toMap());
  }

  void _update<T>(T value, DetectionSettings Function(DetectionSettings) apply) {
    _save(apply(_settings));
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Detection Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          SwitchListTile(
            title: const Text('Live detection enabled'),
            value: _settings.liveEnabled,
            onChanged: (v) => _update(v, (s) => s.copyWith(liveEnabled: v)),
          ),
          SwitchListTile(
            title: const Text('Monitoring enabled'),
            value: _settings.monitoringEnabled,
            onChanged: (v) => _update(v, (s) => s.copyWith(monitoringEnabled: v)),
          ),
          SwitchListTile(
            title: const Text('Block only HIGH severity'),
            subtitle: const Text('If off, all severities may block'),
            value: _settings.blockHighSeverityOnly,
            onChanged: (v) => _update(v, (s) => s.copyWith(blockHighSeverityOnly: v)),
          ),
          const SizedBox(height: 12),
          _IntSlider(
            label: 'Min occurrences (monitoring)',
            value: _settings.minOccurrences,
            min: 1,
            max: 10,
            onChanged: (v) => _update(v, (s) => s.copyWith(minOccurrences: v)),
          ),
          _IntSlider(
            label: 'Window seconds',
            value: _settings.windowSeconds,
            min: 10,
            max: 3600,
            divisions: 50,
            onChanged: (v) => _update(v, (s) => s.copyWith(windowSeconds: v)),
          ),
          _IntSlider(
            label: 'Cooldown (ms)',
            value: _settings.cooldownMs,
            min: 100,
            max: 60000,
            divisions: 100,
            onChanged: (v) => _update(v, (s) => s.copyWith(cooldownMs: v)),
          ),
          _IntSlider(
            label: 'Debounce (ms)',
            value: _settings.debounceMs,
            min: 50,
            max: 2000,
            divisions: 40,
            onChanged: (v) => _update(v, (s) => s.copyWith(debounceMs: v)),
          ),
          const SizedBox(height: 12),
          ListTile(
            title: const Text('Counting mode'),
            subtitle: Text(_settings.countMode),
            trailing: DropdownButton<String>(
              value: _settings.countMode,
              items: const [
                DropdownMenuItem(value: 'UNIQUE_PER_SNAPSHOT', child: Text('Unique per snapshot')),
                DropdownMenuItem(value: 'ALL_MATCHES', child: Text('All matches')),
              ],
              onChanged: (v) {
                if (v != null) _update(v, (s) => s.copyWith(countMode: v));
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _IntSlider extends StatelessWidget {
  final String label;
  final int value;
  final int min;
  final int max;
  final int? divisions;
  final ValueChanged<int> onChanged;
  const _IntSlider({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.onChanged,
    this.divisions,
  });
  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Expanded(child: Text(label)),
          Text(value.toString()),
        ]),
        Slider(
          value: value.toDouble(),
            min: min.toDouble(),
            max: max.toDouble(),
            divisions: divisions ?? (max - min),
            label: value.toString(),
            onChanged: (d) => onChanged(d.round()),
          ),
        const SizedBox(height: 8),
      ],
    );
  }
}
