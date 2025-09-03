import 'package:flutter/material.dart';
import '../../theme.dart';

class ActivityItem {
  final String title;
  final String subtitle;
  final Color dot;
  const ActivityItem(this.title, this.subtitle, this.dot);
}


class ActivityList extends StatelessWidget {
  final List<ActivityItem> items;
  const ActivityList({super.key, required this.items});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: items.map((e) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            children: [
              Container(width: 10, height: 10, decoration: BoxDecoration(color: e.dot, shape: BoxShape.circle)),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(e.title, style: Theme.of(context).textTheme.bodyMedium),
                  const SizedBox(height: 2),
                  Text(e.subtitle, style: Theme.of(context).textTheme.bodySmall),
                ]),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}
