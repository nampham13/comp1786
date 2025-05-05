// CartIconButton widget to show cart and item count
import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class CartIconButton extends StatelessWidget {
  final VoidCallback? onPressed;
  const CartIconButton({this.onPressed, super.key});

  @override
  Widget build(BuildContext context) {
    final uid = FirebaseAuth.instance.currentUser?.uid;
    if (uid == null) return IconButton(icon: const Icon(Icons.shopping_cart), onPressed: onPressed);
    return StreamBuilder<QuerySnapshot>(
      stream: FirebaseFirestore.instance.collection('users').doc(uid).collection('cart').snapshots(),
      builder: (context, snap) {
        int count = 0;
        if (snap.hasData) count = snap.data!.docs.length;
        return Stack(
          alignment: Alignment.center,
          children: [
            IconButton(icon: const Icon(Icons.shopping_cart), onPressed: onPressed),
            if (count > 0)
              Positioned(
                right: 6,
                top: 6,
                child: Container(
                  padding: const EdgeInsets.all(2),
                  decoration: BoxDecoration(color: Colors.red, borderRadius: BorderRadius.circular(10)),
                  constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                  child: Text('$count', style: const TextStyle(color: Colors.white, fontSize: 10), textAlign: TextAlign.center),
                ),
              ),
          ],
        );
      },
    );
  }
}
