import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../../core/constants/api_constants.dart';

class RatingScreen extends StatefulWidget {
  const RatingScreen({super.key, required this.role});

  final String role;

  @override
  State<RatingScreen> createState() => _RatingScreenState();
}

class _RatingScreenState extends State<RatingScreen> {
  List<dynamic> users = [];
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    fetch();
  }

  Future<void> fetch() async {
    try {
      final response = await http.get(Uri.parse(ApiConstants.ratingUrl(widget.role)));
      if (!mounted) {
        return;
      }

      if (response.statusCode == 200) {
        setState(() {
          users = jsonDecode(response.body) as List<dynamic>;
          isLoading = false;
        });
      } else {
        setState(() => isLoading = false);
      }
    } catch (_) {
      if (!mounted) {
        return;
      }
      setState(() => isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Рейтинг')),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              itemCount: users.length,
              itemBuilder: (context, index) {
                final user = users[index] as Map<String, dynamic>;
                final title = widget.role == 'ADMIN'
                    ? (user['fullName'] ?? 'N/A').toString()
                    : (user['name'] ?? 'N/A').toString();
                final score = (user['aiScore'] ?? user['score'] ?? 0).toString();

                return ListTile(
                  leading: CircleAvatar(child: Text('${index + 1}')),
                  title: Text(title),
                  subtitle: Text('Score: $score'),
                );
              },
            ),
    );
  }
}
