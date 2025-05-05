// Course model stub
class Course {
  final String id;
  final String title;
  final String type;
  final String level;
  final String dayOfWeek;
  final String time;
  final String description;
  final double price;
  
  Course({
    required this.id,
    required this.title,
    this.type = '',
    this.level = '',
    this.dayOfWeek = '',
    this.time = '',
    this.description = '',
    this.price = 0.0,
  });
  
  factory Course.fromJson(Map<String, dynamic> json, {required String id}) {
    return Course(
      id: id,
      title: json['title'] ?? '',
      type: json['type'] ?? '',
      level: json['level'] ?? '',
      dayOfWeek: json['dayOfWeek'] ?? '',
      time: json['time'] ?? '',
      description: json['description'] ?? '',
      price: (json['price'] ?? 0.0).toDouble(),
    );
  }
  
  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'type': type,
      'level': level,
      'dayOfWeek': dayOfWeek,
      'time': time,
      'description': description,
      'price': price,
    };
  }
}
