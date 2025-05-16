import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/services.dart';
import '../services/cart_service.dart';
import '../services/billing_service.dart';

/// Shows a payment dialog for a single class and processes the payment.
Future<bool> payForOneClass(BuildContext context, Map<String, dynamic> item) async {
  final formKey = GlobalKey<FormState>();
  final cardHolderController = TextEditingController();
  final cardNumberController = TextEditingController();
  final expiryController = TextEditingController();
  final cvvController = TextEditingController();
  bool paid = false;
  final result = await showDialog(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Mock Payment'),
      content: Form(
        key: formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextFormField(
              controller: cardHolderController,
              decoration: const InputDecoration(labelText: 'Cardholder Name'),
              validator: (v) => v == null || v.isEmpty ? 'Required' : null,
            ),
            TextFormField(
              controller: cardNumberController,
              decoration: const InputDecoration(labelText: 'Card Number'),
              keyboardType: TextInputType.number,
              maxLength: 16,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              validator: (v) => v == null || v.length != 16 ? 'Enter 16 digits' : null,
            ),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: expiryController,
                    decoration: const InputDecoration(labelText: 'MM/YY', hintText: 'MM/YY'),
                    maxLength: 5,
                    keyboardType: TextInputType.number,
                    inputFormatters: [
                      LengthLimitingTextInputFormatter(5),
                      _ExpiryDateTextInputFormatter(),
                    ],
                    validator: (v) {
                      if (v == null || v.length != 5) return 'MM/YY';
                      final parts = v.split('/');
                      if (parts.length != 2) return 'MM/YY';
                      final mm = int.tryParse(parts[0]);
                      final yy = int.tryParse(parts[1]);
                      if (mm == null || yy == null || mm < 1 || mm > 12) return 'MM/YY';
                      return null;
                    },
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: TextFormField(
                    controller: cvvController,
                    decoration: const InputDecoration(labelText: 'CVV'),
                    keyboardType: TextInputType.number,
                    maxLength: 3,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    validator: (v) => v == null || v.length != 3 ? '3 digits' : null,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
        ElevatedButton(
          onPressed: () {
            if (formKey.currentState?.validate() ?? false) {
              paid = true;
              Navigator.pop(ctx, true);
            }
          },
          child: const Text('Pay'),
        ),
      ],
    ),
  );
  if (paid) {
    String cardLast4 = cardNumberController.text.length >= 4
        ? cardNumberController.text.substring(cardNumberController.text.length - 4)
        : 'mock';
    await confirmBookingsWithCardInfo(
      context,
      [item],
      cardLast4,
      cardHolderController.text,
      expiryController.text,
    );
    return true;
  }
  return false;
}

/// Confirms bookings and saves card info to Firestore.
Future<void> confirmBookingsWithCardInfo(
  BuildContext context,
  List<Map<String, dynamic>> items,
  String cardLast4,
  String cardHolder,
  String cardExpiry,
) async {
  final uid = FirebaseAuth.instance.currentUser?.uid;
  if (uid == null) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Not logged in.')));
    return;
  }
  final billingService = BillingService();
  final CartService _cartService = CartService();
  double totalAmount = 0.0;
  List<String> billingIds = [];
  List<String> failed = [];
  for (final item in items) {
    final billingId = item['id'].toString();
    debugPrint('Attempting to remove from cart: $billingId');
    final instanceRef = FirebaseFirestore.instance.collection('class_instances').doc(billingId);
    final instSnap = await instanceRef.get();
    final data = instSnap.data();
    if (data == null) {
      failed.add('Instance $billingId not found.');
      try {
        await _cartService.removeFromCart(billingId);
        debugPrint('Auto-removed invalid cart item: $billingId');
      } catch (e) {
        debugPrint('Failed to auto-remove invalid cart item: $billingId, error: $e');
      }
      continue;
    }
    final courseId = data['courseId']?.toString();
    if (courseId == null) {
      failed.add('No courseId for instance $billingId.');
      continue;
    }
    final courseRef = FirebaseFirestore.instance.collection('yoga_courses').doc(courseId);
    final courseSnap = await courseRef.get();
    final courseData = courseSnap.data();
    if (courseData == null) {
      failed.add('Course $courseId not found for instance $billingId.');
      continue;
    }
    final capacity = courseData['capacity'] as int? ?? 0;
    final enrolled = courseData['enrolled'] as int? ?? 0;
    if (enrolled >= capacity) {
      failed.add('Class $billingId is full.');
      continue;
    }
    final price = (courseData['price'] ?? 0.0) is num ? (courseData['price'] ?? 0.0).toDouble() : 0.0;
    totalAmount += price;
    billingIds.add(billingId);
    final billingRef = FirebaseFirestore.instance.collection('users').doc(uid).collection('billings').doc(billingId);
    await billingRef.set({
      'billingId': billingId,
      'status': 'confirmed',
      'billedAt': FieldValue.serverTimestamp(),
      'cardLast4': cardLast4,
      'cardHolder': cardHolder,
      'cardExpiry': cardExpiry,
    });
    await courseRef.update({'enrolled': enrolled + 1});
    try {
      await _cartService.removeFromCart(billingId);
      debugPrint('Removed from cart: $billingId');
    } catch (e) {
      debugPrint('Failed to remove from cart: $billingId, error: $e');
    }
  }
  if (billingIds.isNotEmpty) {
    await billingService.createBilling(
      amount: totalAmount,
      bookingIds: billingIds,
      paymentMethod: 'card',
      status: 'paid',
      extra: {
        'cardLast4': cardLast4,
        'cardHolder': cardHolder,
        'cardExpiry': cardExpiry,
        'paidAt': FieldValue.serverTimestamp(),
      },
    );
  }
  if (failed.isNotEmpty) {
    final filtered = failed.where((msg) => !msg.contains('Instance') || !msg.contains('not found')).toList();
    if (filtered.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Some billings failed:\n${filtered.join('\n')}')));
    }
  } else {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('All billings confirmed and payment processed!')));
  }
}

/// Helper for expiry date formatting
class _ExpiryDateTextInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    var text = newValue.text.replaceAll('/', '');
    if (text.length > 2) {
      text = text.substring(0, 2) + '/' + text.substring(2);
    }
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }
}