// Course provider stub
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/course.dart';

class CourseProvider extends ChangeNotifier {
  List<Course> _allCourses = [];
  List<Course> _filteredCourses = [];
  String? _selectedDay;
  String? _selectedTime;
  bool _isLoading = false;
  String? _error;

  // Getters
  List<Course> get courses => _filteredCourses;
  bool get isLoading => _isLoading;
  String? get error => _error;
  String? get selectedDay => _selectedDay;
  String? get selectedTime => _selectedTime;
  bool get hasFilters => _selectedDay != null || _selectedTime != null;

  // Set all courses
  void setCourses(List<Course> courses) {
    _allCourses = courses;
    _applyFilters();
    notifyListeners();
  }

  // Apply day filter
  void setDayFilter(String? day) {
    _selectedDay = day;
    _applyFilters();
    notifyListeners();
  }

  // Apply time filter
  void setTimeFilter(String? time) {
    _selectedTime = time;
    _applyFilters();
    notifyListeners();
  }

  // Clear all filters
  void clearFilters() {
    _selectedDay = null;
    _selectedTime = null;
    _applyFilters();
    notifyListeners();
  }

  // Apply both filters
  void setFilters({String? day, String? time}) {
    _selectedDay = day;
    _selectedTime = time;
    _applyFilters();
    notifyListeners();
  }

  // Apply filters to the course list
  void _applyFilters() {
    if (_selectedDay == null && _selectedTime == null) {
      // No filters, show all courses
      _filteredCourses = List.from(_allCourses);
      return;
    }

    _filteredCourses = _allCourses.where((course) {
      bool matchesDay = _selectedDay == null || course.dayOfWeek == _selectedDay;
      bool matchesTime = _selectedTime == null || course.time == _selectedTime;
      return matchesDay && matchesTime;
    }).toList();
  }

  // Fetch courses from Firestore
  Future<void> fetchCourses() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final snapshot = await FirebaseFirestore.instance.collection('yoga_courses').get();
      final courses = snapshot.docs.map((doc) {
        return Course.fromJson(doc.data(), id: doc.id);
      }).toList();
      
      _allCourses = courses;
      _applyFilters();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }
}
