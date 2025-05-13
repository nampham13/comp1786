// Course provider stub
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/course.dart';

class CourseProvider extends ChangeNotifier {
  List<Course> _allCourses = [];
  List<Course> _filteredCourses = [];
  String? _selectedDay;
  String? _selectedTime;
  String _searchQuery = '';
  bool _isLoading = false;
  String? _error;

  // Getters
  List<Course> get courses => _filteredCourses;
  bool get isLoading => _isLoading;
  String? get error => _error;
  String? get selectedDay => _selectedDay;
  String? get selectedTime => _selectedTime;
  bool get hasFilters => _selectedDay != null || _selectedTime != null || _searchQuery.isNotEmpty;

  String get searchQuery => _searchQuery;

  // Set all courses
  void setCourses(List<Course> courses) {
    _allCourses = courses;
    _applyFilters();
    notifyListeners();
  }

  // Set search query
  void setSearchQuery(String query) {
    _searchQuery = query.trim();
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
    _searchQuery = '';
    _applyFilters();
    notifyListeners();
  }

  // Apply both filters
  void setFilters({String? day, String? time, String? searchQuery}) {
    _selectedDay = day;
    _selectedTime = time;
    if (searchQuery != null) _searchQuery = searchQuery;
    _applyFilters();
    notifyListeners();
  }

  // Apply filters to the course list
  void _applyFilters() {
    List<Course> filtered = List.from(_allCourses);
    if (_selectedDay != null) {
      filtered = filtered.where((course) => course.dayOfWeek == _selectedDay).toList();
    }
    if (_selectedTime != null) {
      filtered = filtered.where((course) => course.time == _selectedTime).toList();
    }
    if (_searchQuery.isNotEmpty) {
      final q = _searchQuery.toLowerCase();
      filtered = filtered.where((course) {
        return (course.title?.toLowerCase().contains(q) ?? false) ||
               (course.type?.toLowerCase().contains(q) ?? false) ||
               (course.description?.toLowerCase().contains(q) ?? false);
      }).toList();
    }
    _filteredCourses = filtered;
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
