import 'package:flutter/material.dart';
import '../services/billing_service.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

class BillingHistoryScreen extends StatelessWidget {
  const BillingHistoryScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final billingService = BillingService();
    // Force Firestore to fetch from server, not cache, to avoid showing deleted/mocked data
    return FutureBuilder(
      future: FirebaseFirestore.instance.clearPersistence(),
      builder: (context, snapshotClear) {
        // Wait for cache to clear before showing the real stream
        if (snapshotClear.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        return Scaffold(
          appBar: AppBar(title: const Text('Billing History')),
          body: StreamBuilder<List<Map<String, dynamic>>>(
            stream: billingService.billingHistory(),
            builder: (ctx, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return Center(child: Text('Error: ${snapshot.error}'));
              }
              final bills = snapshot.data;
              if (bills == null || bills.isEmpty) {
                return const Center(child: Text('No billing history found.'));
              }
              return ListView.builder(
                itemCount: bills.length,
                itemBuilder: (ctx, index) {
                  final bill = bills[index];
                  final createdAt = bill['createdAt'] as Timestamp?;
                  final date = createdAt != null ? createdAt.toDate() : null;
                  return ListTile(
                    leading: const Icon(Icons.payment),
                    title: Text('\$${bill['amount'].toStringAsFixed(2)}'),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Method: ${bill['paymentMethod']}'),
                        if (date != null) Text('Date: ${date.toLocal().toString()}'),
                        Text('Status: ${bill['status']}'),
                      ],
                    ),
                  );
                },
              );
            },
          ),
        );
      },
    );
  }
}
