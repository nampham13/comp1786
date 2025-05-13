import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class BuyService {
  final _db = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;

  String get _uid => _auth.currentUser!.uid;

  Future<void> buyCourse({
    required String courseId,
    required String instanceId,
    required double price,
    required Map<String, dynamic> courseData,
    required Map<String, dynamic> instanceData,
  }) async {
    final orderRef = _db.collection('users').doc(_uid).collection('orders').doc();
    final batch = _db.batch();
    batch.set(orderRef, {
      'courseId': courseId,
      'instanceId': instanceId,
      'price': price,
      'courseData': courseData,
      'instanceData': instanceData,
      'timestamp': FieldValue.serverTimestamp(),
    });
    // Optionally, remove from cart after buying
    final cartRef = _db.collection('users').doc(_uid).collection('cart').doc(instanceId);
    batch.delete(cartRef);
    await batch.commit();
  }
}
