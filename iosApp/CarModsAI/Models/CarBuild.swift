import Foundation

struct CarBuild: Codable, Identifiable {
    var id: Int?
    var year: String
    var make: String
    var model: String
    var baseHp: Int
    var mods: String
    var estimatedHp: Int
    var timestamp: Int64
    
    init(id: Int? = nil, year: String, make: String, model: String, baseHp: Int, mods: String, estimatedHp: Int, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.year = year
        self.make = make
        self.model = model
        self.baseHp = baseHp
        self.mods = mods
        self.estimatedHp = estimatedHp
        self.timestamp = timestamp
    }
}
