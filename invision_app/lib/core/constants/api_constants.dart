import 'package:flutter/foundation.dart';

class ApiConstants {
  static String get host {
    if (kIsWeb) {
      return 'localhost';
    }

    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return '10.0.2.2';
      default:
        return 'localhost';
    }
  }

  static String get baseUrl => 'http://$host:9000/api';
  static String get loginUrl => '$baseUrl/auth/login';
  static String get registerUrl => '$baseUrl/auth/register';
  static String profileUrl(int userId) => '$baseUrl/users/$userId/profile';
  static String ratingUrl(String role) => '$baseUrl/users/list?requesterRole=$role';
}
