// Booking workflow: add booking with transaction and listen for updates
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class BookingService {
  final _db = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;

  Future<void> addBooking(String instanceId) async {
    final uid = _auth.currentUser!.uid;
    final userBookings = _db.collection('users').doc(uid).collection('bookings');
    final instanceRef = _db.collection('instances').doc(instanceId);
    return _db.runTransaction((tx) async {
      final instSnap = await tx.get(instanceRef);
      final data = instSnap.data()!;
      final cap = data['capacity'] as int;
      final enrolled = data['enrolled'] as int;
      if (enrolled >= cap) {
        throw Exception('This class is full.');
      }
      tx.set(userBookings.doc(instanceId), {
        'instanceId': instanceId,
        'status': 'confirmed',
        'bookedAt': FieldValue.serverTimestamp(),
      });
      tx.update(instanceRef, {'enrolled': enrolled + 1});
    });
  }

  Stream<DocumentSnapshot> bookingStatus(String instanceId) {
    final uid = _auth.currentUser!.uid;
    return _db.collection('users').doc(uid).collection('bookings').doc(instanceId).snapshots();
  }
}
