// CartService for managing user's cart in Firestore
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class CartService {
  final _db = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;

  String get _uid => _auth.currentUser!.uid;

  CollectionReference get _cartRef => _db.collection('users').doc(_uid).collection('cart');

  Stream<List<Map<String, dynamic>>> cartItems() {
    return _cartRef.snapshots().map((snap) =>
      snap.docs.map((doc) => {...doc.data() as Map<String, dynamic>, 'id': doc.id}).toList()
    );
  }

  Future<void> addToCart(Map<String, dynamic> item) async {
    // item should contain at least an 'instanceId' or 'classId'
    final id = item['instanceId'] ?? item['classId'];
    if (id == null) throw Exception('Missing instanceId or classId');
    await _cartRef.doc(id.toString()).set(item);
  }

  Future<void> removeFromCart(String id) async {
    await _cartRef.doc(id).delete();
  }

  Future<void> clearCart() async {
    final batch = _db.batch();
    final snap = await _cartRef.get();
    for (final doc in snap.docs) {
      batch.delete(doc.reference);
    }
    await batch.commit();
  }
}
