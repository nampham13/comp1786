// CartScreen to display and manage cart items
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/services.dart';

import '../services/cart_service.dart';
import '../services/billing_service.dart';
import '../services/buy_service.dart';


class CartScreen extends StatelessWidget {
  final CartService _cartService = CartService();

  CartScreen({super.key});

  Future<void> _confirmBookings(BuildContext context, List<Map<String, dynamic>> items, String cardLast4) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Not logged in.')));
      return;
    }
    final billingService = BillingService();
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
        // Auto-remove invalid cart item so user can't pay for it again
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
      // Calculate price (mock: use courseData['price'] or 0.0)
      final price = (courseData['price'] ?? 0.0) is num ? (courseData['price'] ?? 0.0).toDouble() : 0.0;
      totalAmount += price;
      billingIds.add(billingId);
      // Mark billing and update enrolled
      final billingRef = FirebaseFirestore.instance.collection('users').doc(uid).collection('billings').doc(billingId);
      await billingRef.set({
        'billingId': billingId,
        'status': 'confirmed',
        'billedAt': FieldValue.serverTimestamp(),
        // Add card info for each paid class
        'cardLast4': cardLast4,
      });
      await courseRef.update({'enrolled': enrolled + 1});
      try {
        await _cartService.removeFromCart(billingId);
        debugPrint('Removed from cart: $billingId');
      } catch (e) {
        debugPrint('Failed to remove from cart: $billingId, error: $e');
      }
    }
    // Create billing record (mock)
    if (billingIds.isNotEmpty) {
      await billingService.createBilling(
        amount: totalAmount,
        bookingIds: billingIds,
        paymentMethod: 'card',
        status: 'paid',
        extra: {
          'cardLast4': cardLast4,
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
      await _confirmBookings(context, items, cardLast4);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Payment cancelled.')));
    }
  }

  Future<void> _payForOneClass(BuildContext context, Map<String, dynamic> item) async {
    // Show payment dialog for single item
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
      // Pass card info to _confirmBookings
      await _confirmBookingsWithCardInfo(context, [item], cardLast4, cardHolderController.text, expiryController.text);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Payment cancelled.')));
    }
  }

  Future<void> _confirmBookingsWithCardInfo(BuildContext context, List<Map<String, dynamic>> items, String cardLast4, String cardHolder, String cardExpiry) async {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Not logged in.')));
      return;
    }
    final billingService = BillingService();
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

  Stream<List<Map<String, dynamic>>> _billingItemsStream() async* {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) {
      yield [];
      return;
    }
    final snapshots = FirebaseFirestore.instance
        .collection('users')
        .doc(uid)
        .collection('billings')
        .orderBy('billedAt', descending: true)
        .snapshots();
    await for (final snap in snapshots) {
      yield snap.docs.map((doc) => doc.data()).toList();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Cart'),
        leading: Navigator.canPop(context)
            ? IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: () => Navigator.of(context).pop(),
              )
            : null,
      ),
      body: Column(
        children: [
          Expanded(
            child: StreamBuilder<List<Map<String, dynamic>>>(
              stream: _cartService.cartItems(),
              builder: (context, snap) {
                if (snap.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }
                final items = snap.data ?? [];
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Padding(
                      padding: EdgeInsets.all(8.0),
                      child: Text('In Cart (Not Paid)', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    ),
                    if (items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.all(8.0),
                        child: Text('No items in cart.'),
                      )
                    else
                      ...items.map((item) => ListTile(
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
                      )),
                  ],
                );
              },
            ),
          ),
        ],
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
      drawer: Drawer(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const DrawerHeader(
              child: Text('Menu', style: TextStyle(fontSize: 24)),
            ),
            ListTile(
              leading: const Icon(Icons.history),
              title: const Text('Billing History'),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => _CartBillingHistoryScreen(billingItemsStream: _billingItemsStream()),
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

}


// Local BillingHistoryScreen for billing history navigation from cart
class _CartBillingHistoryScreen extends StatelessWidget {
  final Stream<List<Map<String, dynamic>>> billingItemsStream;
  const _CartBillingHistoryScreen({Key? key, required this.billingItemsStream}) : super(key: key);

  String _formatTimestamp(dynamic timestamp) {
    if (timestamp == null) return '';
    if (timestamp is Timestamp) {
      final dt = timestamp.toDate();
      return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    }
    return timestamp.toString();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Billing History')),
      body: StreamBuilder<List<Map<String, dynamic>>>(
        stream: billingItemsStream,
        builder: (context, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          final items = snap.data ?? [];
          if (items.isEmpty) {
            return const Center(child: Text('No billing history.'));
          }
          return ListView.separated(
            itemCount: items.length,
            separatorBuilder: (context, i) => const Divider(),
            itemBuilder: (context, i) {
              final item = items[i];
              final extra = item['extra'] as Map<String, dynamic>?;
              final cardLast4 = extra != null ? (extra['cardLast4']?.toString() ?? '') : '';
              final amount = item['amount'] ?? '';
              final bookingIds = item['bookingIds'] ?? [];
              final status = item['status'] ?? '';
              final paymentMethod = item['paymentMethod'] ?? '';
              final billedAt = _formatTimestamp(item['billedAt']);
              return ListTile(
                leading: const Icon(Icons.receipt_long),
                title: Text('Amount: \$${amount.toString()}'),
                subtitle: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (bookingIds is List && bookingIds.isNotEmpty)
                      Text('Classes: ${bookingIds.join(", ")}'),
                    if (cardLast4.isNotEmpty)
                      Text('Card: **** **** **** $cardLast4'),
                    if (billedAt.isNotEmpty)
                      Text('Date: $billedAt'),
                    if (status.isNotEmpty)
                      Text('Status: $status'),
                    if (paymentMethod.isNotEmpty)
                      Text('Payment: $paymentMethod'),
                  ],
                ),
                trailing: const Icon(Icons.check_circle, color: Colors.green),
              );
            },
          );
        },
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
