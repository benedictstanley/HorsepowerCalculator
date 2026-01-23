import Foundation
import SwiftUI
import Combine

// MARK: - ViewModel
class AuthViewModel: ObservableObject {
    @Published var currentUser: User?
    @Published var isLoggedIn: Bool = false
    @Published var errorMessage: String?
    
    private let userDefaultsKey = "saved_user"
    
    init() {
        loadUser()
    }
    
    func login(email: String) {
        // In a real app with backend auth, you would verify credentials here.
        // For this port (matching Android), we simulate local login.
        
        // Check if we have a saved user with this email, else create new
        if let saved = loadUserFromDisk(email: email) {
            self.currentUser = saved
        } else {
            // New User
            // Note: Order of arguments matches Models/User.swift
            self.currentUser = User(
                email: email, 
                planName: "Free", 
                isUnlimited: false, 
                calculationsLeft: 3
            )
        }
        
        self.isLoggedIn = true
        saveUser()
    }
    
    func logout() {
        self.currentUser = nil
        self.isLoggedIn = false
        UserDefaults.standard.removeObject(forKey: userDefaultsKey)
    }
    
    func upgradePlan(to planName: String) {
        guard var user = currentUser else { return }
        user.planName = planName
        user.isUnlimited = true
        user.calculationsLeft = 999999
        self.currentUser = user
        saveUser()
    }
    
    func decrementCalculation() {
        guard var user = currentUser, !user.isUnlimited else { return }
        if user.calculationsLeft > 0 {
            user.calculationsLeft -= 1
            self.currentUser = user
            saveUser()
        }
    }
    
    // MARK: - Persistence
    private func saveUser() {
        if let user = currentUser, let data = try? JSONEncoder().encode(user) {
            UserDefaults.standard.set(data, forKey: userDefaultsKey)
        }
    }
    
    private func loadUser() {
        if let data = UserDefaults.standard.data(forKey: userDefaultsKey),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            self.currentUser = user
            self.isLoggedIn = true
        }
    }
    
    private func loadUserFromDisk(email: String) -> User? {
        // This is where you would normally look up in a database.
        // For now, we just return the current loaded user if emails match, 
        // effectively supporting single-user session on iOS for simplicity.
        if let current = currentUser, current.email == email {
            return current
        }
        return nil
    }
}
