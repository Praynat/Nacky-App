// lib/features/guardian/pin_dialog.dart
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'guardian_repo.dart';

/// Returns true if allowed (active session or correct PIN).
Future<bool> requireGuardian(BuildContext context, {String title = 'Enter PIN'}) async {
  if (!(await GuardianRepo.I.hasPin()) || GuardianRepo.I.isSessionActive()) return true;

  final c = TextEditingController();
  bool submitting = false;
  bool error = false;

  // Simple shake animation
  final controller = AnimationController(
    vsync: Navigator.of(context),
    duration: const Duration(milliseconds: 350),
  );
  final shake = controller.drive(
    Tween<double>(begin: 0, end: 1).chain(CurveTween(curve: Curves.easeOut)),
  );

  Future<void> _failFeedback(StateSetter setState) async {
    // Haptic + border turns red + shake
    HapticFeedback.mediumImpact();
    setState(() => error = true);
    await controller.forward(from: 0);
  }

  Future<void> _tryUnlock(StateSetter setState, BuildContext ctx) async {
    if (submitting) return;
    setState(() { submitting = true; error = false; });
    final ok = await GuardianRepo.I.verifyPin(c.text.trim());
    setState(() => submitting = false);
    if (ok) {
      Navigator.of(ctx).pop(true);
    } else {
      await _failFeedback(setState);
    }
  }

  final ok = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (ctx) {
      return StatefulBuilder(builder: (ctx, setState) {
        // small horizontal shake using Transform.translate
        final dx = shake.value == 0
            ? 0.0
            : math.sin(shake.value * math.pi * 6) * 8.0; // 3 cycles, ~8px

        return Transform.translate(
          offset: Offset(dx, 0),
          child: AlertDialog(
            title: Text(title),
            content: TextField(
              controller: c,
              autofocus: true,
              obscureText: true,
              keyboardType: TextInputType.number,
              maxLength: 8,
              onChanged: (_) { if (error) setState(() => error = false); },
              onSubmitted: (_) => _tryUnlock(setState, ctx),
              decoration: InputDecoration(
                hintText: '4–8 digits',
                counterText: '',
                border: const OutlineInputBorder(),
                enabledBorder: error
                    ? OutlineInputBorder(
                        borderSide: BorderSide(color: Theme.of(ctx).colorScheme.error, width: 2),
                      )
                    : const OutlineInputBorder(),
                focusedBorder: error
                    ? OutlineInputBorder(
                        borderSide: BorderSide(color: Theme.of(ctx).colorScheme.error, width: 2),
                      )
                    : null,
                errorText: error ? 'Incorrect PIN' : null,
              ),
            ),
            actions: [
              TextButton(
                onPressed: submitting ? null : () => Navigator.of(ctx).pop(false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: submitting ? null : () => _tryUnlock(setState, ctx),
                child: submitting
                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Unlock'),
              ),
            ],
          ),
        );
      });
    },
  );

  controller.dispose();
  return ok == true;
}
