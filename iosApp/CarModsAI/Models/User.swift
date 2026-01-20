import Foundation

struct User: Codable, Identifiable {
    var id: String { email }
    let email: String
    var phoneNumber: String
    var planName: String
    var isUnlimited: Bool
    var calculationsLeft: Int
    var dynoRunsLeft: Int
    var subscriptionExpiry: Int64
    
    init(email: String, phoneNumber: String = "", planName: String = "Free", isUnlimited: Bool = false, calculationsLeft: Int = 3, dynoRunsLeft: Int = 0, subscriptionExpiry: Int64 = 0) {
        self.email = email
        self.phoneNumber = phoneNumber
        self.planName = planName
        self.isUnlimited = isUnlimited
        self.calculationsLeft = calculationsLeft
        self.dynoRunsLeft = dynoRunsLeft
        self.subscriptionExpiry = subscriptionExpiry
    }
}
