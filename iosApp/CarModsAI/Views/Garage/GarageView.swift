import SwiftUI

struct GarageView: View {
    // Placeholder data
    @State private var builds: [CarBuild] = [
        CarBuild(year: "2020", make: "Toyota", model: "Supra", baseHp: 335, mods: "Tune, Exhaust", estimatedHp: 450)
    ]
    
    var body: some View {
        NavigationView {
            List(builds) { build in
                VStack(alignment: .leading) {
                    Text("\(build.year) \(build.make) \(build.model)")
                        .font(.headline)
                    Text("Est. HP: \(build.estimatedHp)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle("Garage")
            .toolbar {
                Button(action: {
                    // Add new car logic
                }) {
                    Image(systemName: "plus")
                }
            }
        }
    }
}
