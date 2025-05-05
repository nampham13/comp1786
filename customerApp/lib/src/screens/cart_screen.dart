// CartScreen to display and manage cart items
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/services.dart';
import '../services/cart_service.dart';

class CartScreen extends StatelessWidget {
  final CartService _cartService = CartService();

  CartScreen({super.key});

  Future<void> _confirmBookings(BuildContext context, List<Map<String, dynamic>> items) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Not logged in.')));
      return;
    }
    final batch = FirebaseFirestore.instance.batch();
    bool anyFailed = false;
    String errorMsg = '';
    for (final item in items) {
      final instanceId = item['id'].toString();
      final instanceRef = FirebaseFirestore.instance.collection('class_instances').doc(instanceId);
      final bookingRef = FirebaseFirestore.instance.collection('users').doc(uid).collection('bookings').doc(instanceId);
      final instSnap = await instanceRef.get();
      final data = instSnap.data();
      if (data == null) {
        anyFailed = true;
        errorMsg += 'Instance $instanceId not found.\n';
        continue;
      }
      // Find the parent yoga course for this instance
      final courseId = data['courseId']?.toString();
      if (courseId == null) {
        anyFailed = true;
        errorMsg += 'No courseId for instance $instanceId.\n';
        continue;
      }
      final courseRef = FirebaseFirestore.instance.collection('yoga_courses').doc(courseId);
      final courseSnap = await courseRef.get();
      final courseData = courseSnap.data();
      if (courseData == null) {
        anyFailed = true;
        errorMsg += 'Course $courseId not found for instance $instanceId.\n';
        continue;
      }
      final capacity = courseData['capacity'] as int? ?? 0;
      final enrolled = courseData['enrolled'] as int? ?? 0;
      if (enrolled >= capacity) {
        anyFailed = true;
        errorMsg += 'Class $instanceId is full.\n';
        continue;
      }
      batch.set(bookingRef, {
        'instanceId': instanceId,
        'status': 'confirmed',
        'bookedAt': FieldValue.serverTimestamp(),
      });
      batch.update(courseRef, {'enrolled': enrolled + 1});
      await _cartService.removeFromCart(instanceId);
    }
    await batch.commit();
    if (anyFailed) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Some bookings failed:\n$errorMsg')));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('All bookings confirmed!')));
    }
  }

  Future<void> _proceedToPayment(BuildContext context, List<Map<String, dynamic>> items) async {
    final formKey = GlobalKey<FormState>();
    final cardHolderController = TextEditingController();
    final cardNumberController = TextEditingController();
    final expiryController = TextEditingController();
    final cvvController = TextEditingController();
    bool paid = false;
    await showDialog(
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
                        FilteringTextInputFormatter.digitsOnly,
                        LengthLimitingTextInputFormatter(4),
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
      await _confirmBookings(context, items);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Payment cancelled.')));
    }
  }

  Future<void> _payForOneClass(BuildContext context, Map<String, dynamic> item) async {
    await _proceedToPayment(context, [item]);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('My Cart')),
      body: StreamBuilder<List<Map<String, dynamic>>>(
        stream: _cartService.cartItems(),
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          final items = snap.data ?? [];
          if (items.isEmpty) {
            return const Center(child: Text('Your cart is empty.'));
          }
          return ListView.builder(
            itemCount: items.length,
            itemBuilder: (context, i) {
              final item = items[i];
              return ListTile(
                title: Text(item['type'] ?? 'Class'),
                subtitle: Text('Day: ${item['dayOfWeek'] ?? ''} | Time: ${item['time'] ?? ''}'),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.delete),
                      onPressed: () => _cartService.removeFromCart(item['id'].toString()),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      onPressed: () => _payForOneClass(context, item),
                      child: const Text('Pay for this class'),
                    ),
                  ],
                ),
              );
            },
          );
        },
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.all(16.0),
        child: StreamBuilder<List<Map<String, dynamic>>>(
          stream: _cartService.cartItems(),
          builder: (context, snap) {
            final items = snap.data ?? [];
            return ElevatedButton(
              onPressed: items.isEmpty ? null : () => _proceedToPayment(context, items),
              child: const Text('Pay for all classes'),
            );
          },
        ),
      ),
    );
  }
}

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
