import SwiftUI
// NOTE: You must add the Stripe iOS SDK via Swift Package Manager for this to work.
// Search for "https://github.com/stripe/stripe-ios" and add "StripePaymentSheet".
import StripePaymentSheet

struct SubscriptionView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    @State private var paymentSheet: PaymentSheet?
    @State private var errorMessage: String?
    @State private var isLoading = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if let user = authViewModel.currentUser {
                    Text("Current Plan: \(user.planName)")
                        .font(.title)
                    
                    Text("Calculations Left: \(user.isUnlimited ? "Unlimited" : "\(user.calculationsLeft)")")
                    Text("Dyno Runs Left: \(user.isUnlimited ? "Unlimited" : "\(user.dynoRunsLeft)")")
                    
                    if let error = errorMessage {
                        Text(error)
                            .foregroundColor(.red)
                            .padding()
                    }
                    
                    if isLoading {
                        ProgressView()
                    } else {
                        Button("Upgrade to Pro ($5.00)") {
                            preparePayment(amount: 500, planName: "Pro Plan")
                        }
                        .padding()
                        .background(Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                    }
                    
                    if let paymentSheet = paymentSheet {
                        PaymentSheet.PaymentButton(
                            paymentSheet: paymentSheet,
                            onCompletion: onPaymentCompletion
                        ) {
                            Text("Pay Now")
                                .bold()
                                .foregroundColor(.white)
                                .padding()
                                .frame(maxWidth: .infinity)
                                .background(Color.blue)
                                .cornerRadius(10)
                        }
                        .padding()
                    }
                    
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
    
    func preparePayment(amount: Int, planName: String) {
        isLoading = true
        errorMessage = nil
        
        APIService.shared.createPaymentSheet(amount: amount, planName: planName) { result in
            isLoading = false
            
            switch result {
            case .success(let json):
                guard let customerId = json["customer"] as? String,
                      let ephemeralKey = json["ephemeralKey"] as? String,
                      let clientSecret = json["paymentIntent"] as? String,
                      let publishableKey = json["publishableKey"] as? String else {
                    errorMessage = "Invalid response from server"
                    return
                }
                
                STPAPIClient.shared.publishableKey = publishableKey
                
                var configuration = PaymentSheet.Configuration()
                configuration.merchantDisplayName = "CarModsAI"
                configuration.customer = .init(id: customerId, ephemeralKeySecret: ephemeralKey)
                
                self.paymentSheet = PaymentSheet(paymentIntentClientSecret: clientSecret, configuration: configuration)
                
            case .failure(let error):
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func onPaymentCompletion(result: PaymentSheetResult) {
        switch result {
        case .completed:
            // Payment successful, update user plan locally
            // In a real app, you should verify via webhook or check status with backend
            authViewModel.updateUserPlan(
                planName: "Pro Plan",
                isUnlimited: true,
                calculations: 9999,
                dynoRuns: 9999
            )
            self.paymentSheet = nil
            print("Payment complete")
        case .canceled:
            print("Payment canceled")
        case .failed(let error):
            errorMessage = error.localizedDescription
        }
    }
}
