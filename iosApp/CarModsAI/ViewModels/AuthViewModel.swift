import Foundation
import Combine

class AuthViewModel: ObservableObject {
    @Published var currentUser: User?
    @Published var isLoggedIn: Bool = false
    
    private let userDefaultsKey = "saved_user_data"
    
    init() {
        loadUser()
    }
    
    func login(email: String) {
        // In a real app, you'd verify with backend.
        // For this offline-first/local MVP, we just create/load the user.
        
        let newUser = User(
            email: email,
            phoneNumber: "",
            planName: "Free",
            isUnlimited: false,
            calculationsLeft: 3,
            dynoRunsLeft: 0,
            subscriptionExpiry: 0
        )
        
        self.currentUser = newUser
        self.isLoggedIn = true
        saveUser()
    }
    
    func logout() {
        self.currentUser = nil
        self.isLoggedIn = false
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
    }
    
    func updateUserPlan(planName: String, isUnlimited: Bool, calculations: Int, dynoRuns: Int) {
        guard var user = currentUser else { return }
        user.planName = planName
        user.isUnlimited = isUnlimited
        user.calculationsLeft = calculations
        user.dynoRunsLeft = dynoRuns
        
        self.currentUser = user
        saveUser()
    }
    
    private func saveUser() {
        if let user = currentUser,
           let encoded = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(encoded, forKey: userDefaultsKey)
        }
    }
    
    private func loadUser() {
        if let savedData = UserDefaults.standard.data(forKey: userDefaultsKey),
           let loadedUser = try? JSONDecoder().decode(User.self, from: savedData) {
            self.currentUser = loadedUser
            self.isLoggedIn = true
        }
    }
}
