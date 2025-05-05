// Instance model stub
class Instance {
  final String id;
  final String yogaCourseId;
  final String dayOfWeek;
  final String time;
  final String date;
  final int capacity;
  final int enrolled;
  final String? comments;
  final String? teacher;
  
  Instance({
    required this.id,
    required this.yogaCourseId,
    required this.dayOfWeek,
    required this.time,
    required this.date,
    required this.capacity,
    required this.enrolled,
    this.comments,
    this.teacher,
  });
  
  factory Instance.fromJson(Map<String, dynamic> json, {required String id}) => Instance(
    id: id,
    yogaCourseId: json['yogaCourseId'] ?? '',
    dayOfWeek: json['dayOfWeek'] ?? '',
    time: json['time'] ?? '',
    date: json['date'] ?? '',
    capacity: json['capacity'] ?? 0,
    enrolled: json['enrolled'] ?? 0,
    comments: json['comments'],
    teacher: json['teacher'],
  );
  
  Map<String, dynamic> toJson() => {
    'yogaCourseId': yogaCourseId,
    'dayOfWeek': dayOfWeek,
    'time': time,
    'date': date,
    'capacity': capacity,
    'enrolled': enrolled,
    'comments': comments,
    'teacher': teacher,
  };
  
  // Helper method to check if the instance is full
  bool get isFull => enrolled >= capacity;
  
  // Helper method to get available spots
  int get availableSpots => capacity - enrolled;
}
