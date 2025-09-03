import 'package:flutter/material.dart';
import 'guardian_repo.dart';

class SetPinScreen extends StatefulWidget {
  const SetPinScreen({super.key});
  @override State<SetPinScreen> createState() => _SetPinScreenState();
}

class _SetPinScreenState extends State<SetPinScreen> {
  final _current = TextEditingController();
  final _pin1 = TextEditingController();
  final _pin2 = TextEditingController();
  bool _hasPin = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    GuardianRepo.I.hasPin().then((v) { if (mounted) setState(() => _hasPin = v); });
  }

  Future<void> _save() async {
    setState(() => _saving = true);

    if (_hasPin) {
      final ok = await GuardianRepo.I.verifyPin(_current.text.trim());
      if (!ok) {
        setState(() => _saving = false);
        _snack('Current PIN incorrect.');
        return;
      }
    }
    final p1 = _pin1.text.trim();
    final p2 = _pin2.text.trim();
    if (p1 != p2 || p1.length < 4 || p1.length > 8 || !_isDigits(p1)) {
      setState(() => _saving = false);
      _snack('PIN must be 4–8 digits and match.');
      return;
    }
    final ok2 = await GuardianRepo.I.setPin(p1);
    setState(() => _saving = false);
    if (ok2 && mounted) {
      _snack('PIN saved.');
      Navigator.of(context).maybePop();
    } else {
      _snack('Failed to save PIN.');
    }
  }

  bool _isDigits(String s) => s.runes.every((c) => c >= 0x30 && c <= 0x39);

  void _snack(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Set / Change PIN')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            if (_hasPin) ...[
              TextField(
                controller: _current,
                obscureText: true,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Current PIN',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 16),
            ],
            TextField(
              controller: _pin1,
              obscureText: true,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'New PIN (4–8 digits)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _pin2,
              obscureText: true,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Confirm PIN',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }
}
