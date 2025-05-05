// Home screen stub
import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../widgets/course_tile.dart';
import '../widgets/search_filter.dart';
import '../widgets/cart_icon_button.dart';
import '../screens/cart_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String? _selectedDay;
  String? _selectedTime;

  void _handleFilterChanged(String? day, String? time) {
    setState(() {
      _selectedDay = day;
      _selectedTime = time;
    });
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
      body: Column(
        children: [
          SearchFilter(onFilterChanged: _handleFilterChanged),
          if (_selectedDay != null || _selectedTime != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      'Filtered by: ${_selectedDay ?? ''} ${_selectedTime != null ? (_selectedDay != null ? 'at ' : '') + _selectedTime! : ''}',
                      style: const TextStyle(fontStyle: FontStyle.italic),
                    ),
                  ),
                ],
              ),
            ),
          Expanded(
            child: CourseTileList(
              dayOfWeek: _selectedDay,
              time: _selectedTime,
            ),
          ),
        ],
      ),
    );
  }
}
