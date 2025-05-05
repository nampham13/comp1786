import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../models/instance.dart';
import '../services/firestore_service.dart';

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
                      return Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Column(
                            children: [
                              Icon(
                                Icons.event_busy,
                                size: 48,
                                color: Colors.grey.shade400,
                              ),
                              const SizedBox(height: 16),
                              const Text(
                                'No instances found for this course.',
                                textAlign: TextAlign.center,
                              ),
                            ],
                          ),
                        ),
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
              ElevatedButton(
                onPressed: _addToCart,
                child: const Text('Add to Cart'),
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
