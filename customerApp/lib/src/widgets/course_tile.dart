// Course tile widget stub
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../screens/course_detail_screen.dart';

class CourseTileList extends StatelessWidget {
  final String? dayOfWeek;
  final String? time;
  const CourseTileList({this.dayOfWeek, this.time, super.key});

  @override
  Widget build(BuildContext context) {
    Query query = FirebaseFirestore.instance.collection('yoga_courses');
    
    // Apply day filter if selected
    if (dayOfWeek != null && dayOfWeek!.isNotEmpty) {
      query = query.where('dayOfWeek', isEqualTo: dayOfWeek);
    }
    
    // Apply time filter if selected
    if (time != null && time!.isNotEmpty) {
      query = query.where('time', isEqualTo: time);
    }
    
    return StreamBuilder<QuerySnapshot>(
      stream: query.snapshots(),
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return Center(child: Text('Error: ${snapshot.error}'));
        }
        
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        
        final docs = snapshot.data?.docs ?? [];
        
        if (docs.isEmpty) {
          // Show appropriate message based on whether filters are applied
          if ((dayOfWeek != null && dayOfWeek!.isNotEmpty) || (time != null && time!.isNotEmpty)) {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.filter_list_off, size: 48, color: Colors.grey),
                    SizedBox(height: 16),
                    Text(
                      'No classes found for this filter.',
                      style: TextStyle(fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            );
          } else {
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.event_busy, size: 48, color: Colors.grey),
                    SizedBox(height: 16),
                    Text(
                      'No classes found.',
                      style: TextStyle(fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            );
          }
        }
        
        // Display the filtered results
        return ListView.builder(
          itemCount: docs.length,
          itemBuilder: (context, i) {
            final data = docs[i].data() as Map<String, dynamic>;
            return Card(
              margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              child: ListTile(
                contentPadding: const EdgeInsets.all(12),
                title: Text(
                  data['type'] ?? 'Untitled',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  'Level: ${data['level'] ?? ''}\nDay: ${data['dayOfWeek'] ?? ''} | Time: ${data['time'] ?? ''}',
                ),
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => CourseDetailScreen(courseId: docs[i].id),
                    ),
                  );
                },
              ),
            );
          },
        );
      },
    );
  }
}
