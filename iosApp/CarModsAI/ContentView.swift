import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            GarageView()
                .tabItem {
                    Label("Garage", systemImage: "car.fill")
                }
            
            DynoChartView()
                .tabItem {
                    Label("Dyno", systemImage: "chart.xyaxis.line")
                }
            
            SubscriptionView()
                .tabItem {
                    Label("Plan", systemImage: "creditcard.fill")
                }
        }
    }
}
