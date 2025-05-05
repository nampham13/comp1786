// Booking model stub
class Booking {
  final String id;
  final String instanceId;
  final String status;
  Booking({required this.id, required this.instanceId, required this.status});
  factory Booking.fromJson(Map<String, dynamic> json, {required String id}) => Booking(
    id: id,
    instanceId: json['instanceId'] ?? '',
    status: json['status'] ?? '',
  );
}
