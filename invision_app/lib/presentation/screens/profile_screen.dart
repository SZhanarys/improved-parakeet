import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/ui/theme_toggle_button.dart';
import '../../data/services/questionnaire_service.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key, required this.userId});

  final int userId;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  static const int _maxNameLength = 120;
  static const int _maxPhoneLength = 32;
  static const int _maxSkillsLength = 4000;
  static const int _maxValueLength = 5000;
  static const int _maxExperienceLength = 8000;
  static const int _maxProjectsLength = 5000;
  static const List<String> _aiStatuses = [
    'Идет магия ИИ...',
    'Анализируем ваш опыт...',
    'Нормализуем навыки...',
    'Собираем отчет для рекрутера...',
  ];

  final _formKey = GlobalKey<FormState>();
  final _questionnaireService = QuestionnaireService();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _skillsController = TextEditingController();
  final _valueController = TextEditingController();
  final _experienceController = TextEditingController();
  final _projectsController = TextEditingController();

  bool _isCheckingStatus = true;
  bool _isSubmitting = false;
  bool _hasExistingSubmission = false;
  int _statusIndex = 0;
  Timer? _statusTimer;

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  @override
  void dispose() {
    _statusTimer?.cancel();
    _firstNameController.dispose();
    _lastNameController.dispose();
    _phoneController.dispose();
    _skillsController.dispose();
    _valueController.dispose();
    _experienceController.dispose();
    _projectsController.dispose();
    super.dispose();
  }

  Future<void> _loadStatus() async {
    final status = await _questionnaireService.fetchStatus(widget.userId);
    if (!mounted) {
      return;
    }

    setState(() {
      _hasExistingSubmission = status['submitted'] == true;
      _isCheckingStatus = false;
    });
  }

  void _startStatusRotation() {
    _statusTimer?.cancel();
    _statusIndex = 0;
    _statusTimer = Timer.periodic(const Duration(milliseconds: 1500), (_) {
      if (!mounted || !_isSubmitting) {
        return;
      }
      setState(() {
        _statusIndex = (_statusIndex + 1) % _aiStatuses.length;
      });
    });
  }

  void _stopStatusRotation() {
    _statusTimer?.cancel();
    _statusTimer = null;
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
      _statusIndex = 0;
    });
    _startStatusRotation();

    final response = await _questionnaireService.submit(
      userId: widget.userId,
      firstName: _firstNameController.text.trim(),
      lastName: _lastNameController.text.trim(),
      phoneNumber: _phoneController.text.trim(),
      skillsInput: _skillsController.text.trim(),
      valueStatement: _valueController.text.trim(),
      experienceSummary: _experienceController.text.trim(),
      projectsInput: _projectsController.text.trim(),
    );

    if (!mounted) {
      return;
    }

    _stopStatusRotation();
    setState(() => _isSubmitting = false);

    final message = (response['message'] ?? 'Ошибка запроса').toString();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));

    if (response['ok'] == true) {
      setState(() => _hasExistingSubmission = true);
    }
  }

  String? _requiredValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Обязательное поле';
    }
    return null;
  }

  String? _phoneValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Обязательное поле';
    }

    final digits = value.replaceAll(RegExp(r'[^\d]'), '');
    if (digits.length < 10) {
      return 'Введите корректный номер телефона';
    }
    if (value.trim().length > _maxPhoneLength) {
      return 'Номер телефона слишком длинный';
    }

    return null;
  }

  String? _minLengthValidator(String? value, int min, String message) {
    if (value == null || value.trim().isEmpty) {
      return 'Обязательное поле';
    }
    if (value.trim().length < min) {
      return message;
    }
    return null;
  }

  String? _maxLengthValidator(String? value, int max, String message) {
    if (value == null || value.trim().isEmpty) {
      return null;
    }
    if (value.trim().length > max) {
      return message;
    }
    return null;
  }

  String? _combineValidators(
    String? value,
    List<String? Function(String?)> validators,
  ) {
    for (final validator in validators) {
      final result = validator(value);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Анкета кандидата'),
        actions: const [ThemeToggleButton()],
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFF5F7FB), Color(0xFFE9EEF5)],
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
          ),
        ),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 920),
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Card(
                elevation: 0,
                color: Theme.of(context).colorScheme.surface,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(24),
                  side: const BorderSide(color: Color(0xFFD6DEE8)),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(28),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Профессиональная анкета кандидата',
                          style: theme.textTheme.headlineSmall?.copyWith(
                            fontWeight: FontWeight.w700,
                            color: const Color(0xFF162033),
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _hasExistingSubmission
                              ? 'Анкета уже сохранена. Можно обновить данные и отправить повторно.'
                              : 'Заполните поля подробно: это поможет ИИ точнее собрать профиль и вердикт.',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: const Color(0xFF5C6778),
                          ),
                        ),
                        if (_isCheckingStatus) ...[
                          const SizedBox(height: 18),
                          const LinearProgressIndicator(minHeight: 3),
                          const SizedBox(height: 8),
                          Text(
                            'Проверяем статус анкеты...',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: const Color(0xFF6B7280),
                            ),
                          ),
                        ],
                        if (_isSubmitting) ...[
                          const SizedBox(height: 20),
                          _AiStatusBanner(
                            status: _aiStatuses[_statusIndex],
                            step: (_statusIndex + 1) / _aiStatuses.length,
                          ),
                        ],
                        const SizedBox(height: 28),
                        Row(
                          children: [
                            Expanded(
                              child: _buildField(
                                controller: _lastNameController,
                                label: 'Фамилия',
                                maxLength: _maxNameLength,
                                validator: (value) => _combineValidators(value, [
                                  _requiredValidator,
                                  (text) => _maxLengthValidator(
                                        text,
                                        _maxNameLength,
                                        'Фамилия слишком длинная',
                                      ),
                                ]),
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: _buildField(
                                controller: _firstNameController,
                                label: 'Имя',
                                maxLength: _maxNameLength,
                                validator: (value) => _combineValidators(value, [
                                  _requiredValidator,
                                  (text) => _maxLengthValidator(
                                        text,
                                        _maxNameLength,
                                        'Имя слишком длинное',
                                      ),
                                ]),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        _buildField(
                          controller: _phoneController,
                          label: 'Номер телефона',
                          hint: '+7 777 123 45 67',
                          keyboardType: TextInputType.phone,
                          maxLength: _maxPhoneLength,
                          validator: _phoneValidator,
                        ),
                        const SizedBox(height: 16),
                        _buildField(
                          controller: _skillsController,
                          label: 'Что умеет кандидат',
                          hint: 'Flutter, Java, Spring Boot, PostgreSQL',
                          minLines: 3,
                          maxLines: null,
                          maxLength: _maxSkillsLength,
                          validator: (value) => _combineValidators(value, [
                            _requiredValidator,
                            (text) => _maxLengthValidator(
                                  text,
                                  _maxSkillsLength,
                                  'Описание навыков слишком длинное',
                                ),
                          ]),
                        ),
                        const SizedBox(height: 16),
                        _buildField(
                          controller: _valueController,
                          label: 'Почему кандидат ценный',
                          hint: 'Опишите практическую ценность, сильные стороны и пользу для бизнеса.',
                          minLines: 4,
                          maxLines: null,
                          maxLength: _maxValueLength,
                          validator: (value) => _combineValidators(value, [
                            (text) => _minLengthValidator(
                                  text,
                                  20,
                                  'Опишите ценность кандидата подробнее',
                                ),
                            (text) => _maxLengthValidator(
                                  text,
                                  _maxValueLength,
                                  'Описание ценности слишком длинное',
                                ),
                          ]),
                        ),
                        const SizedBox(height: 16),
                        _buildField(
                          controller: _experienceController,
                          label: 'Опыт работы',
                          hint: 'Опишите обязанности, стек, роль и уровень участия.',
                          minLines: 5,
                          maxLines: null,
                          maxLength: _maxExperienceLength,
                          validator: (value) => _combineValidators(value, [
                            (text) => _minLengthValidator(
                                  text,
                                  30,
                                  'Опишите опыт работы подробнее',
                                ),
                            (text) => _maxLengthValidator(
                                  text,
                                  _maxExperienceLength,
                                  'Описание опыта слишком длинное',
                                ),
                          ]),
                        ),
                        const SizedBox(height: 16),
                        _buildField(
                          controller: _projectsController,
                          label: 'Проекты',
                          hint: 'Банковское мобильное приложение, HR-платформа, CRM dashboard',
                          minLines: 4,
                          maxLines: null,
                          maxLength: _maxProjectsLength,
                          validator: (value) => _combineValidators(value, [
                            _requiredValidator,
                            (text) => _maxLengthValidator(
                                  text,
                                  _maxProjectsLength,
                                  'Описание проектов слишком длинное',
                                ),
                          ]),
                        ),
                        const SizedBox(height: 24),
                        SizedBox(
                          width: double.infinity,
                          height: 54,
                          child: ElevatedButton(
                            onPressed: _isSubmitting || _isCheckingStatus ? null : _submit,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFF183153),
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(14),
                              ),
                            ),
                            child: Text(
                              _isSubmitting ? 'Сохраняем и анализируем...' : 'Отправить анкету',
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildField({
    required TextEditingController controller,
    required String label,
    required String? Function(String?) validator,
    String? hint,
    TextInputType? keyboardType,
    int minLines = 1,
    int? maxLines = 1,
    int? maxLength,
  }) {
    return TextFormField(
      controller: controller,
      validator: validator,
      keyboardType: keyboardType,
      minLines: minLines,
      maxLines: maxLines,
      maxLength: maxLength,
      maxLengthEnforcement: MaxLengthEnforcement.enforced,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        alignLabelWithHint: maxLines == null || (maxLines > 1),
        filled: true,
        fillColor: const Color(0xFFF8FAFC),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
        ),
      ),
    );
  }
}

class _AiStatusBanner extends StatelessWidget {
  const _AiStatusBanner({
    required this.status,
    required this.step,
  });

  final String status;
  final double step;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF0F172A), Color(0xFF2563EB)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.auto_awesome_rounded, color: Colors.white),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  status,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                      ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: step,
              minHeight: 8,
              backgroundColor: Colors.white.withValues(alpha: 0.20),
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          ),
        ],
      ),
    );
  }
}
