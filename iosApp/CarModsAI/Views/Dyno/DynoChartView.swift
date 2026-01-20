import SwiftUI
import Charts

struct DynoDataPoint: Identifiable {
    let id = UUID()
    let rpm: Int
    let hp: Int
    let torque: Int
}

struct DynoChartView: View {
    let data: [DynoDataPoint] = [
        DynoDataPoint(rpm: 2000, hp: 150, torque: 200),
        DynoDataPoint(rpm: 3000, hp: 220, torque: 280),
        DynoDataPoint(rpm: 4000, hp: 300, torque: 320),
        DynoDataPoint(rpm: 5000, hp: 380, torque: 350),
        DynoDataPoint(rpm: 6000, hp: 420, torque: 340),
        DynoDataPoint(rpm: 7000, hp: 450, torque: 310)
    ]
    
    var body: some View {
        NavigationView {
            VStack {
                Text("Dyno Graph")
                    .font(.title2)
                    .padding()
                
                if #available(iOS 16.0, *) {
                    Chart(data) { point in
                        LineMark(
                            x: .value("RPM", point.rpm),
                            y: .value("HP", point.hp)
                        )
                        .foregroundStyle(.red)
                        .interpolationMethod(.catmullRom)
                        
                        LineMark(
                            x: .value("RPM", point.rpm),
                            y: .value("Torque", point.torque)
                        )
                        .foregroundStyle(.blue)
                        .interpolationMethod(.catmullRom)
                    }
                    .padding()
                } else {
                    Text("Charts require iOS 16+")
                }
                
                Spacer()
            }
            .navigationTitle("Dyno")
        }
    }
}
