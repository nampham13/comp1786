
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../models/instance.dart';
import '../services/firestore_service.dart';
import '../services/buy_service.dart';
import '../services/billing_service.dart';


class CourseDetailScreen extends StatelessWidget {
  final String courseId;
  const CourseDetailScreen({required this.courseId, Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final firestoreService = FirestoreService();
    final user = FirebaseAuth.instance.currentUser;
    print('CourseDetailScreen: courseId=$courseId (type: ${courseId.runtimeType})');
    // courseId may be a string or int, depending on Firestore data structure
    
    return Scaffold(
      appBar: AppBar(title: const Text('Class Details')),
      body: StreamBuilder<DocumentSnapshot>(
        stream: FirebaseFirestore.instance.collection('yoga_courses').doc(courseId).snapshots(),
        builder: (context, courseSnap) {
          if (courseSnap.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          
          if (!courseSnap.hasData || !courseSnap.data!.exists) {
            return const Center(child: Text('Class not found.'));
          }
          
          final data = courseSnap.data!.data() as Map<String, dynamic>;
          
          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Course header with type and level
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).primaryColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        data['type'] ?? 'Untitled',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (data['level'] != null && data['level'] != '')
                        Chip(
                          label: Text(data['level'] ?? ''),
                          backgroundColor: Colors.amber.shade100,
                        ),
                    ],
                  ),
                ),

                const SizedBox(height: 16),

                // Course description
                if (data['description'] != null && data['description'] != '') ...[
                  Text(
                    'Description',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    data['description'],
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 16),
                ],

                // Course details
                Text(
                  'Class Details',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                _buildDetailRow(Icons.calendar_today, 'Day', data['dayOfWeek'] ?? 'Not specified'),
                _buildDetailRow(Icons.access_time, 'Time', data['time'] ?? 'Not specified'),
                _buildDetailRow(Icons.timelapse, 'Duration', '${data['duration'] ?? 'Not specified'} min'),
                _buildDetailRow(Icons.group, 'Capacity', data['capacity']?.toString() ?? 'Not specified'),
                _buildDetailRow(Icons.people, 'Enrolled', data['enrolled']?.toString() ?? '0'),
                _buildDetailRow(Icons.attach_money, 'Price', '\$${data['price'] ?? 'Not specified'}'),

                if (data['equipmentNeeded'] != null && data['equipmentNeeded'] != '')
                  _buildDetailRow(Icons.fitness_center, 'Equipment', data['equipmentNeeded']),

                const SizedBox(height: 24),

                // Purchase history removed as per requirements

                const SizedBox(height: 24),

                // Class instances section
                Text(
                  'Class Instances',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                // Stream of class instances
                StreamBuilder<List<Instance>>(
                  stream: firestoreService.instances(courseId),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(
                        child: Padding(
                          padding: EdgeInsets.all(16.0),
                          child: CircularProgressIndicator(),
                        ),
                      );
                    }
                    final instances = snapshot.data ?? [];
                    if (instances.isEmpty) {
                      return _CourseBuyCard(
                        courseId: courseId,
                        courseData: data,
                        userId: user?.uid,
                      );
                    }
                    return ListView.separated(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: instances.length,
                      separatorBuilder: (_, __) => const Divider(),
                      itemBuilder: (context, index) {
                        final instance = instances[index];
                        final instanceMap = instance.toJson();
                        return _InstanceCard(
                          instance: instanceMap,
                          instanceId: instance.id,
                          userId: user?.uid,
                        );
                      },
                    );
                  },
                ),
              ],
            ),
          );
      },
    ),
  );
  }

  // Helper method to build detail rows
  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: Row(
        children: [
          Icon(icon, size: 18, color: Colors.grey),
          const SizedBox(width: 8),
          Text(
            '$label: ',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

// Widget for each instance card with booking button
class _InstanceCard extends StatefulWidget {
  final Map<String, dynamic> instance;
  final String instanceId;
  final String? userId;
  const _InstanceCard({required this.instance, required this.instanceId, required this.userId});

  @override
  State<_InstanceCard> createState() => _InstanceCardState();
}

class _InstanceCardState extends State<_InstanceCard> {
  bool _loading = false;
  String? _message;

  Future<void> _buyCourse() async {
    setState(() { _loading = true; _message = null; });
    try {
      if (widget.userId == null) throw Exception('Not logged in');
      final buyService = BuyService();
      // You may want to pass more course data if available
      await buyService.buyCourse(
        courseId: widget.instance['courseId'] ?? '',
        instanceId: widget.instanceId,
        price: (widget.instance['price'] ?? 0.0) is num ? (widget.instance['price'] ?? 0.0).toDouble() : 0.0,
        courseData: widget.instance['courseData'] ?? {},
        instanceData: widget.instance,
      );
      setState(() { _message = 'Course purchased!'; });
    } catch (e) {
      setState(() { _message = 'Failed to buy: $e'; });
    } finally {
      setState(() { _loading = false; });
    }
  }

  Widget _instanceField(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('$label', style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(width: 8),
          Expanded(child: Text(value?.toString() ?? '', style: const TextStyle(color: Colors.black87))),
        ],
      ),
    );
  }

  Future<void> _addToCart() async {
    setState(() { _loading = true; _message = null; });
    try {
      final uid = widget.userId;
      if (uid == null) throw Exception('Not logged in');
      final cartRef = FirebaseFirestore.instance
        .collection('users').doc(uid)
        .collection('cart').doc(widget.instanceId);
      await cartRef.set(widget.instance);
      setState(() { _message = 'Added to cart!'; });
    } catch (e) {
      setState(() { _message = 'Failed to add to cart: $e'; });
    } finally {
      setState(() { _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final i = widget.instance;
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _instanceField('comments', i['comments']),
            _instanceField('date', i['date']),
            _instanceField('teacher', i['teacher']),
            const SizedBox(height: 8),
            if (_loading)
              const Center(child: CircularProgressIndicator())
            else
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _addToCart,
                    child: const Text('Add to Cart'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _buyCourse,
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
                    child: const Text('Buy Now'),
                  ),
                ),
              ],
            ),
            if (_message != null) ...[
              const SizedBox(height: 8),
              Text(_message!, style: TextStyle(color: _message!.contains('add') ? Colors.green : Colors.red)),
            ],
          ],
        ),
      ),
    );
  }
}

// Widget for buying/adding to cart when there are no instances
class _CourseBuyCard extends StatefulWidget {
  final String courseId;
  final Map<String, dynamic> courseData;
  final String? userId;
  const _CourseBuyCard({required this.courseId, required this.courseData, required this.userId});

  @override
  State<_CourseBuyCard> createState() => _CourseBuyCardState();
}

class _CourseBuyCardState extends State<_CourseBuyCard> {
  bool _loading = false;
  String? _message;

  Future<void> _showCardInputAndBuy() async {
    final cardInfo = await showDialog<Map<String, String>>(
      context: context,
      builder: (context) {
        final cardNumberController = TextEditingController();
        final expiryController = TextEditingController();
        final cvvController = TextEditingController();
        String? errorText;
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: const Text('Enter Card Details'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(
                      controller: cardNumberController,
                      keyboardType: TextInputType.number,
                      inputFormatters: [
                        FilteringTextInputFormatter.digitsOnly,
                        LengthLimitingTextInputFormatter(16),
                      ],
                      decoration: const InputDecoration(labelText: 'Card Number (16 digits)'),
                    ),
                    TextField(
                      controller: expiryController,
                      keyboardType: TextInputType.number,
                      inputFormatters: [
                        FilteringTextInputFormatter.digitsOnly,
                        LengthLimitingTextInputFormatter(4),
                      ],
                      decoration: const InputDecoration(labelText: 'Expiry (MM/YY)', hintText: 'MM/YY'),
                    ),
                    TextField(
                      controller: cvvController,
                      keyboardType: TextInputType.number,
                      inputFormatters: [
                        FilteringTextInputFormatter.digitsOnly,
                        LengthLimitingTextInputFormatter(4),
                      ],
                      decoration: const InputDecoration(labelText: 'CVV'),
                      obscureText: true,
                    ),
                    if (errorText != null) ...[
                      const SizedBox(height: 8),
                      Text(errorText!, style: const TextStyle(color: Colors.red)),
                    ],
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    final cardNumber = cardNumberController.text.trim();
                    final expiry = expiryController.text.trim();
                    final cvv = cvvController.text.trim();
                    // Validate card number
                    if (cardNumber.isEmpty || expiry.isEmpty || cvv.isEmpty) {
                      setState(() { errorText = 'All card fields are required.'; });
                      return;
                    }
                    if (cardNumber.length != 16) {
                      setState(() { errorText = 'Card number must be exactly 16 digits.'; });
                      return;
                    }
                    // Validate expiry MM/YY and not in the past
                    if (expiry.length != 5 || expiry[2] != '/') {
                      setState(() { errorText = 'Expiry must be in MM/YY format.'; });
                      return;
                    }
                    final parts = expiry.split('/');
                    if (parts.length != 2) {
                      setState(() { errorText = 'Expiry must be in MM/YY format.'; });
                      return;
                    }
                    final mm = int.tryParse(parts[0]);
                    final yy = int.tryParse(parts[1]);
                    if (mm == null || yy == null || mm < 1 || mm > 12) {
                      setState(() { errorText = 'Invalid expiry month.'; });
                      return;
                    }
final now = DateTime.now();
final currentYear = now.year % 100;
final currentMonth = now.month;
if (yy < currentYear || (yy == currentYear && mm < currentMonth)) {
  setState(() { errorText = 'Card expiry is in the past.'; });
  return;
}
// Validate CVV: 3 or 4 digits
if (!(cvv.length == 3 || cvv.length == 4)) {
  setState(() { errorText = 'CVV must be 3 or 4 digits.'; });
  return;
}
                    if (!RegExp(r'^\d{3,4} ?$').hasMatch(cvv)) {
                      setState(() { errorText = 'CVV must be numeric.'; });
                      return;
                    }
                    Navigator.of(context).pop({
                      'cardNumber': cardNumber,
                      'expiry': expiry,
                      'cvv': cvv,
                    });
                  },
                  child: const Text('Pay'),
                ),
              ],
            );
          },
        );
      },
    );
    if (cardInfo == null) return;
    // Defensive: check all fields and card number length and logic again
    if ((cardInfo['cardNumber']?.isEmpty ?? true) ||
        (cardInfo['expiry']?.isEmpty ?? true) ||
        (cardInfo['cvv']?.isEmpty ?? true)) {
      setState(() { _message = 'All card fields are required.'; });
      return;
    }
    if ((cardInfo['cardNumber']?.length ?? 0) != 16) {
      setState(() { _message = 'Card number must be exactly 16 digits.'; });
      return;
    }
    // Validate expiry MM/YY and not in the past
    final expReg = RegExp(r'^(0[1-9]|1[0-2])/([0-9]{2}) ?$');
    final expiry = cardInfo['expiry']!;
    if (!expReg.hasMatch(expiry)) {
      setState(() { _message = 'Expiry must be in MM/YY format.'; });
      return;
    }
    final parts = expiry.split('/');
    final now = DateTime.now();
    int expMonth = int.tryParse(parts[0]) ?? 0;
    int expYear = int.tryParse(parts[1]) ?? 0;
    expYear += (expYear < 100) ? 2000 : 0;
    final expDate = DateTime(expYear, expMonth + 1, 0);
    if (expDate.isBefore(DateTime(now.year, now.month, 1))) {
      setState(() { _message = 'Card expiry is in the past.'; });
      return;
    }
    // Validate CVV: 3 or 4 digits
    final cvv = cardInfo['cvv']!;
    if (!(cvv.length == 3 || cvv.length == 4)) {
      setState(() { _message = 'CVV must be 3 or 4 digits.'; });
      return;
    }
    if (!RegExp(r'^\d{3,4} ?$').hasMatch(cvv)) {
      setState(() { _message = 'CVV must be numeric.'; });
      return;
    }
    await _buyCourse(cardInfo);
  }

  Future<void> _buyCourse(Map<String, String> cardInfo) async {
    setState(() { _loading = true; _message = null; });
    try {
      if (widget.userId == null) throw Exception('Not logged in');
      // Defensive: check card fields and card number length and logic again
      if ((cardInfo['cardNumber']?.isEmpty ?? true) ||
          (cardInfo['expiry']?.isEmpty ?? true) ||
          (cardInfo['cvv']?.isEmpty ?? true)) {
        setState(() { _message = 'All card fields are required.'; });
        return;
      }
      if ((cardInfo['cardNumber']?.length ?? 0) != 16) {
        setState(() { _message = 'Card number must be exactly 16 digits.'; });
        return;
      }
      // Validate expiry MM/YY and not in the past
      final expReg = RegExp(r'^(0[1-9]|1[0-2])/([0-9]{2})\u0000?$');
      final expiry = cardInfo['expiry']!;
      if (!expReg.hasMatch(expiry)) {
        setState(() { _message = 'Expiry must be in MM/YY format.'; });
        return;
      }
      final parts = expiry.split('/');
      final now = DateTime.now();
      int expMonth = int.tryParse(parts[0]) ?? 0;
      int expYear = int.tryParse(parts[1]) ?? 0;
      expYear += (expYear < 100) ? 2000 : 0;
      final expDate = DateTime(expYear, expMonth + 1, 0);
      if (expDate.isBefore(DateTime(now.year, now.month, 1))) {
        setState(() { _message = 'Card expiry is in the past.'; });
        return;
      }
      // Validate CVV: 3 or 4 digits
      final cvv = cardInfo['cvv']!;
      if (!(cvv.length == 3 || cvv.length == 4)) {
        setState(() { _message = 'CVV must be 3 or 4 digits.'; });
        return;
      }
      if (!RegExp(r'^\d{3,4}\u0000?$').hasMatch(cvv)) {
        setState(() { _message = 'CVV must be numeric.'; });
        return;
      }
      // Simulate a fake billing transaction
      final billingService = BillingService();
      final amount = (widget.courseData['price'] ?? 0.0) is num ? (widget.courseData['price'] ?? 0.0).toDouble() : 0.0;
      await billingService.createBilling(
        amount: amount,
        bookingIds: [widget.courseId],
        paymentMethod: 'mock_card',
        status: 'paid',
        extra: {
          'courseId': widget.courseId,
          'courseTitle': widget.courseData['title'] ?? '',
          'cardNumber': cardInfo['cardNumber'],
          'expiry': cardInfo['expiry'],
        },
      );
      final buyService = BuyService();
      await buyService.buyCourse(
        courseId: widget.courseId,
        instanceId: widget.courseId,
        price: amount,
        courseData: widget.courseData,
        instanceData: widget.courseData,
      );

      // Purchase history logic removed

      setState(() { _message = 'Course purchased (mock card)!'; });
    } catch (e) {
      setState(() { _message = 'Failed to buy: $e'; });
    } finally {
      setState(() { _loading = false; });
    }
  }

  Future<void> _addToCart() async {
    setState(() { _loading = true; _message = null; });
    try {
      final uid = widget.userId;
      if (uid == null) throw Exception('Not logged in');
      final cartRef = FirebaseFirestore.instance
          .collection('users').doc(uid)
          .collection('cart').doc(widget.courseId);
      await cartRef.set(widget.courseData);
      setState(() { _message = 'Added to cart!'; });
    } catch (e) {
      setState(() { _message = 'Failed to add to cart: $e'; });
    } finally {
      setState(() { _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('No specific class instances available. You can still add this course to your cart or buy it.'),
            const SizedBox(height: 8),
            if (_loading)
              const Center(child: CircularProgressIndicator())
            else
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _addToCart,
                      child: const Text('Add to Cart'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: _loading ? null : _showCardInputAndBuy,
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
                      child: const Text('Buy Now'),
                    ),
                  ),
                ],
            ),
            if (_message != null) ...[
              const SizedBox(height: 8),
              Text(_message!, style: TextStyle(color: _message!.contains('add') ? Colors.green : Colors.red)),
            ],
          ],
        ),
      ),
    );
  }
}