import SwiftUI

struct SubscriptionView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if let user = authViewModel.currentUser {
                    Text("Current Plan: \(user.planName)")
                        .font(.title)
                    
                    Text("Calculations Left: \(user.isUnlimited ? "Unlimited" : "\(user.calculationsLeft)")")
                    Text("Dyno Runs Left: \(user.isUnlimited ? "Unlimited" : "\(user.dynoRunsLeft)")")
                    
                    Button("Upgrade to Pro") {
                        // Trigger payment logic
                    }
                    .padding()
                    .background(Color.green)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                } else {
                    Text("Please log in to view subscription.")
                }
                
                Button("Logout") {
                    authViewModel.logout()
                }
                .foregroundColor(.red)
            }
            .navigationTitle("Subscription")
        }
    }
}
