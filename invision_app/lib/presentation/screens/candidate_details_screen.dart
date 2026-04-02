import 'package:flutter/material.dart';

import '../../core/ui/responsive.dart';
import '../../core/ui/theme_toggle_button.dart';
import '../../data/services/user_service.dart';

class CandidateDetailsScreen extends StatefulWidget {
  const CandidateDetailsScreen({
    super.key,
    required this.userId,
    required this.requesterId,
  });

  final int userId;
  final int requesterId;

  @override
  State<CandidateDetailsScreen> createState() => _CandidateDetailsScreenState();
}

class _CandidateDetailsScreenState extends State<CandidateDetailsScreen> {
  final _userService = UserService();

  Map<String, dynamic>? _candidate;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadCandidate();
  }

  Future<void> _loadCandidate() async {
    final candidate = await _userService.fetchCandidateDetails(
      userId: widget.userId,
      requesterId: widget.requesterId,
    );

    if (!mounted) {
      return;
    }

    setState(() {
      _candidate = candidate;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final inset = AppResponsive.inset(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Детали кандидата'),
        actions: const [ThemeToggleButton()],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _candidate == null
              ? const Center(
                  child: Text(
                    'Не удалось загрузить данные кандидата или доступ запрещен.',
                  ),
                )
              : Center(
                  child: ConstrainedBox(
                    constraints: BoxConstraints(
                      maxWidth: AppResponsive.contentMaxWidth(context),
                    ),
                    child: ListView(
                      padding: EdgeInsets.all(inset),
                      children: [
                        _InfoCard(
                          title: 'Полное имя',
                          value: (_candidate!['fullName'] ?? '').toString(),
                        ),
                        _InfoCard(
                          title: 'Номер телефона',
                          value: (_candidate!['phoneNumber'] ?? '').toString(),
                        ),
                        _InfoCard(
                          title: 'Email',
                          value: (_candidate!['email'] ?? '').toString(),
                        ),
                        _InfoCard(
                          title: 'Сводка ценности',
                          value: (_candidate!['valueStatement'] ?? '').toString(),
                        ),
                        _InfoCard(
                          title: 'Выжимка опыта',
                          value: (_candidate!['experienceSummary'] ?? '').toString(),
                        ),
                        _InfoCard(
                          title: 'Оценка',
                          value: (_candidate!['aiScore'] ?? 0).toString(),
                        ),
                        _InfoCard(
                          title: 'Вердикт',
                          value: (_candidate!['aiVerdict'] ?? '').toString(),
                        ),
                        _ListCard(
                          title: 'Навыки',
                          items: (_candidate!['skills'] as List<dynamic>? ?? const [])
                              .map((item) => item.toString())
                              .toList(),
                        ),
                        _ListCard(
                          title: 'Проекты',
                          items: (_candidate!['projects'] as List<dynamic>? ?? const [])
                              .map((item) => item.toString())
                              .toList(),
                        ),
                      ],
                    ),
                  ),
                ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.title, required this.value});

  final String title;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: EdgeInsets.all(AppResponsive.isMobile(context) ? 14 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),
            Text(value.isEmpty ? '-' : value),
          ],
        ),
      ),
    );
  }
}

class _ListCard extends StatelessWidget {
  const _ListCard({required this.title, required this.items});

  final String title;
  final List<String> items;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: EdgeInsets.all(AppResponsive.isMobile(context) ? 14 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 10),
            if (items.isEmpty)
              const Text('-')
            else
              ...items.map(
                (item) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Text('- $item'),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
