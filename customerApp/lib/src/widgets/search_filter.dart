// Search/filter widget stub
import 'package:flutter/material.dart';

class SearchFilter extends StatefulWidget {
  final void Function(String? day, String? time, String? searchQuery) onFilterChanged;
  const SearchFilter({required this.onFilterChanged, Key? key}) : super(key: key);

  @override
  State<SearchFilter> createState() => _SearchFilterState();
}

class _SearchFilterState extends State<SearchFilter> {
  // Search bar removed, handled by ModernSearchBar in HomeScreen
  String? selectedDay;
  TimeOfDay? selectedTime;
  final days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  // Convert TimeOfDay to a string format that matches the database
  String? _formatTimeForFilter(TimeOfDay? time) {
    if (time == null) return null;
    
    // Format as HH:MM AM/PM to match the expected format in Firestore
    final hour = time.hourOfPeriod == 0 ? 12 : time.hourOfPeriod;
    final minute = time.minute.toString().padLeft(2, '0');
    final period = time.period == DayPeriod.am ? 'AM' : 'PM';
    return '$hour:$minute $period';
  }

  void _applyFilters() {
    widget.onFilterChanged(selectedDay, _formatTimeForFilter(selectedTime), null);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          // Search bar removed from here; now handled by ModernSearchBar in HomeScreen
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: DropdownButton<String>(
                  value: selectedDay,
                  hint: const Text('Select Day'),
                  isExpanded: true,
                  items: [
                    const DropdownMenuItem<String>(
                      value: null,
                      child: Text('All Days'),
                    ),
                    ...days.map((d) => DropdownMenuItem(value: d, child: Text(d))).toList(),
                  ],
                  onChanged: (val) {
                    setState(() => selectedDay = val);
                    _applyFilters();
                  },
                ),
              ),
              const SizedBox(width: 8),
              ElevatedButton(
                onPressed: () async {
                  final picked = await showTimePicker(context: context, initialTime: TimeOfDay.now());
                  if (picked != null) {
                    setState(() => selectedTime = picked);
                    _applyFilters();
                  }
                },
                child: Text(selectedTime == null ? 'Pick Time' : selectedTime!.format(context)),
                style: ElevatedButton.styleFrom(
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                  backgroundColor: Colors.white,
                  foregroundColor: Colors.black87,
                  elevation: 0,
                  side: const BorderSide(color: Colors.grey, width: 0.5),
                ),
              ),
              if (selectedDay != null || selectedTime != null)
                IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () {
                    setState(() {
                      selectedDay = null;
                      selectedTime = null;
                    });
                    _applyFilters();
                  },
                  tooltip: 'Clear filters',
                ),
            ],
          ),
        ],
      ),
    );
  }
}
