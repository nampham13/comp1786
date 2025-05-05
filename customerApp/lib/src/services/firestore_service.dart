// Firestore service stub
import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/course.dart';
import '../models/instance.dart';

class FirestoreService {
  final _db = FirebaseFirestore.instance;
  
  // Get all yoga courses
  Stream<List<Course>> courses() => 
    _db.collection('yoga_courses')
      .snapshots()
      .map((snap) => snap.docs
        .map((doc) => Course.fromJson(doc.data(), id: doc.id))
        .toList());
  
  // Updated: Use 'courseId' as the linking field, and handle int/string conversion
  Stream<List<Instance>> instances(String courseId) {
    // Try both int and string for compatibility
    final intCourseId = int.tryParse(courseId);
    final query = _db.collection('class_instances')
      .where('courseId', isEqualTo: intCourseId ?? courseId);
    return query.snapshots().map((snap) =>
      snap.docs.map((doc) => Instance.fromJson(doc.data(), id: doc.id)).toList()
    );
  }
  
  // Get instances filtered by day and time
  Stream<List<Instance>> filteredInstances({required String dayOfWeek, required String time}) {
    return _db.collection('class_instances')
      .where('dayOfWeek', isEqualTo: dayOfWeek)
      .where('time', isEqualTo: time)
      .snapshots()
      .map((snap) => snap.docs
        .map((doc) => Instance.fromJson(doc.data(), id: doc.id))
        .toList());
  }
  
  // Get instances for a specific day
  Stream<List<Instance>> instancesByDay(String dayOfWeek) {
    return _db.collection('class_instances')
      .where('dayOfWeek', isEqualTo: dayOfWeek)
      .snapshots()
      .map((snap) => snap.docs
        .map((doc) => Instance.fromJson(doc.data(), id: doc.id))
        .toList());
  }
  
  // Get instances for a specific time
  Stream<List<Instance>> instancesByTime(String time) {
    return _db.collection('class_instances')
      .where('time', isEqualTo: time)
      .snapshots()
      .map((snap) => snap.docs
        .map((doc) => Instance.fromJson(doc.data(), id: doc.id))
        .toList());
  }
  
  // Firestore indexing advice: 
  // 1. Create a composite index on (dayOfWeek ASC, time ASC) for the 'class_instances' collection
  // 2. Create a single field index on 'yogaCourseId' for the 'class_instances' collection
}
