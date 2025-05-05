// Widget to listen for booking status in real time
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class BookingStatusIndicator extends StatelessWidget {
  final String instanceId;
  const BookingStatusIndicator({required this.instanceId, super.key});

  @override
  Widget build(BuildContext context) {
    final uid = FirebaseAuth.instance.currentUser!.uid;
    return StreamBuilder<DocumentSnapshot>(
      stream: FirebaseFirestore.instance
        .collection('users')
        .doc(uid)
        .collection('bookings')
        .doc(instanceId)
        .snapshots(),
      builder: (ctx, snap) {
        if (snap.connectionState == ConnectionState.waiting) {
          return const CircularProgressIndicator();
        }
        if (snap.hasError) {
          return Text('Error: ${snap.error}');
        }
        if (!snap.hasData || !snap.data!.exists) {
          return const Text('No booking');
        }
        final data = snap.data!.data() as Map<String, dynamic>?;
        final status = data?['status'] as String?;
        final bookedAtRaw = data?['bookedAt'];
        String? bookedAtStr;
        if (bookedAtRaw != null) {
          if (bookedAtRaw is Timestamp) {
            bookedAtStr = bookedAtRaw.toDate().toLocal().toString();
          } else if (bookedAtRaw is String) {
            bookedAtStr = bookedAtRaw;
          } else {
            bookedAtStr = bookedAtRaw.toString();
          }
        }
        if (status == null) return const Text('No booking');
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Booking status: $status'),
            if (bookedAtStr != null) Text('Booked at: $bookedAtStr'),
          ],
        );
      },
    );
  }
}
