import SwiftUI
import StripePaymentSheet

struct SubscriptionView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var paymentSheet: PaymentSheet?
    @State private var paymentResult: PaymentSheetResult?
    @State private var isLoading = false
    @State private var errorMessage: String?
    
    // Price IDs (Replace with your actual Stripe Price IDs if different)
    let priceIdPro = "price_1Qj2..." // TODO: Add real Price ID for Pro
    let priceIdVIP = "price_1Qj2..." // TODO: Add real Price ID for VIP
    
    var body: some View {
        VStack(spacing: 20) {
            Text("Upgrade Your Plan")
                .font(.largeTitle)
                .bold()
            
            if let user = authViewModel.currentUser {
                Text("Current Plan: \(user.planName)")
                    .foregroundColor(.gray)
                
                if !user.isUnlimited {
                    Button(action: {
                        preparePayment(plan: "Pro")
                    }) {
                        Text("Upgrade to Pro ($5/mo)")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    .disabled(isLoading)
                } else {
                    Text("You are on an Unlimited Plan!")
                        .foregroundColor(.green)
                }
            }
            
            if isLoading {
                ProgressView()
            }
            
            if let error = errorMessage {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
            }
            
            if let result = paymentResult {
                switch result {
                case .completed:
                    Text("Payment Complete!").foregroundColor(.green)
                case .canceled:
                    Text("Payment Canceled").foregroundColor(.orange)
                case .failed(let error):
                    Text("Payment Failed: \(error.localizedDescription)").foregroundColor(.red)
                }
            }
        }
        .padding()
        // Present the Payment Sheet
        .paymentSheet(isPresented: $paymentSheet, onCompletion: { result in
            paymentResult = result
            if case .completed = result {
                authViewModel.upgradePlan(to: "Pro") // Update local state
            }
        })
    }
    
    func preparePayment(plan: String) {
        guard let email = authViewModel.currentUser?.email else { return }
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                // 1. Create Customer
                let customerId = try await APIService.shared.createCustomer(email: email)
                
                // 2. Create Subscription (Get Client Secret)
                // Using a placeholder Price ID here. You must update this!
                let priceId = (plan == "Pro") ? "price_1Qj2K0H5X7..." : "price_vip..." 
                let response = try await APIService.shared.createSubscription(customerId: customerId, priceId: priceId)
                
                if let clientSecret = response.clientSecret {
                    // 3. Configure Payment Sheet
                    var configuration = PaymentSheet.Configuration()
                    configuration.merchantDisplayName = "Car Mods AI"
                    configuration.allowsDelayedPaymentMethods = true
                    
                    DispatchQueue.main.async {
                        self.paymentSheet = PaymentSheet(paymentIntentClientSecret: clientSecret, configuration: configuration)
                        self.isLoading = false
                    }
                } else {
                    throw NSError(domain: "App", code: 0, userInfo: [NSLocalizedDescriptionKey: "No client secret returned"])
                }
                
            } catch {
                DispatchQueue.main.async {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }
}
