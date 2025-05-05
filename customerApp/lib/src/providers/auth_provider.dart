// Auth provider stub
import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
class AuthProvider extends ChangeNotifier {
  User? user;
  void setUser(User? u) {
    user = u;
    notifyListeners();
  }
}
