// Home screen stub
import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
// import '../widgets/course_tile.dart';
import '../widgets/search_filter.dart';
import '../widgets/search_bar.dart';
import 'package:provider/provider.dart';
import '../providers/course_provider.dart';
import '../widgets/cart_icon_button.dart';
import '../screens/cart_screen.dart';
import '../screens/course_detail_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => context.read<CourseProvider>().fetchCourses());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Classes'),
        actions: [
          CartIconButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => CartScreen()),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Log out',
            onPressed: () async {
              await FirebaseAuth.instance.signOut();
            },
          ),
        ],
      ),
      body: Consumer<CourseProvider>(
        builder: (context, provider, _) {
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
                child: ModernSearchBar(
                  value: provider.searchQuery,
                  onChanged: (q) => provider.setSearchQuery(q),
                  onClear: () => provider.setSearchQuery(''),
                ),
              ),
              SearchFilter(
                onFilterChanged: (day, time, _) {
                  provider.setDayFilter(day);
                  provider.setTimeFilter(time);
                },
              ),
              if (provider.hasFilters)
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Filtered by: '
                          '${provider.selectedDay ?? ''} '
                          '${provider.selectedTime != null ? (provider.selectedDay != null ? 'at ' : '') + provider.selectedTime! : ''} '
                          '${provider.searchQuery.isNotEmpty ? 'Search: "${provider.searchQuery}"' : ''}',
                          style: const TextStyle(fontStyle: FontStyle.italic),
                        ),
                      ),
                    ],
                  ),
                ),
              if (provider.isLoading)
                const Expanded(child: Center(child: CircularProgressIndicator())),
              if (provider.error != null)
                Expanded(child: Center(child: Text('Error: ${provider.error}'))),
              if (!provider.isLoading && provider.error == null)
                Expanded(
                  child: ListView.builder(
                    itemCount: provider.courses.length,
                    itemBuilder: (context, i) {
                      final course = provider.courses[i];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        child: ListTile(
                          contentPadding: const EdgeInsets.all(12),
                          title: Text(
                            course.type.isNotEmpty ? course.type : course.title,
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                          subtitle: Text(
                            'Level: ${course.level}\nDay: ${course.dayOfWeek} | Time: ${course.time}',
                          ),
                          onTap: () {
                            Navigator.of(context).push(
                              MaterialPageRoute(
                                builder: (_) => CourseDetailScreen(courseId: course.id),
                              ),
                            );
                          },
                        ),
                      );
                    },
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}