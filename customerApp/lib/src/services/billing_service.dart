// BillingService for creating billing data in Firestore
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class BillingService {
  final _db = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;

  String get _uid => _auth.currentUser!.uid;

  CollectionReference get _billingRef => _db.collection('users').doc(_uid).collection('billings');

  Future<DocumentReference> createBilling({
    required double amount,
    required List<String> bookingIds,
    required String paymentMethod,
    required String status, // e.g. 'paid', 'pending', 'failed'
    Map<String, dynamic>? extra,
  }) async {
    final billingData = {
      'amount': amount,
      'bookingIds': bookingIds,
      'paymentMethod': paymentMethod,
      'status': status,
      'createdAt': FieldValue.serverTimestamp(),
      ...?extra,
    };
    return await _billingRef.add(billingData);
  }

  Stream<List<Map<String, dynamic>>> billingHistory() {
    return _billingRef.orderBy('createdAt', descending: true).snapshots().map(
      (snap) => snap.docs.map((doc) => {...doc.data() as Map<String, dynamic>, 'id': doc.id}).toList(),
    );
  }
}
