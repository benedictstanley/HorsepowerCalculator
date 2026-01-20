import Foundation
import Combine

class AuthViewModel: ObservableObject {
    @Published var currentUser: User?
    @Published var isLoggedIn: Bool = false
    
    // Placeholder for actual authentication logic
    func login(email: String) {
        // Simulating login for now
        self.currentUser = User(email: email, planName: "Free")
        self.isLoggedIn = true
    }
    
    func logout() {
        self.currentUser = nil
        self.isLoggedIn = false
    }
}
