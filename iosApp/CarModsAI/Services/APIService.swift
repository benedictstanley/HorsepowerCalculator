import Foundation

// MARK: - API Responses
struct CreateCustomerResponse: Codable {
    let customer: String
    let message: String?
}

struct SubscriptionResponse: Codable {
    let subscriptionId: String?
    let clientSecret: String?
    let status: String?
    
    enum CodingKeys: String, CodingKey {
        case subscriptionId = "subscriptionId"
        case clientSecret = "clientSecret"
        case status = "status"
    }
}

struct OpenAIResponse: Codable {
    struct Choice: Codable {
        struct Message: Codable {
            let content: String
        }
        let message: Message
    }
    let choices: [Choice]
}

struct OpenAIRequest: Codable {
    struct Message: Codable {
        let role: String
        let content: String
    }
    let model: String = "gpt-4o-mini" // or gpt-3.5-turbo
    let messages: [Message]
}

// MARK: - API Service
class APIService {
    static let shared = APIService()
    
    private let baseURL = "https://nmrncplnxsikhbzowrol.supabase.co/functions/v1/payment-api/"
    
    private init() {}
    
    // MARK: - OpenAI
    func getCompletion(prompt: String) async throws -> String {
        let url = URL(string: "https://api.openai.com/v1/chat/completions")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("Bearer \(Secrets.openAIKey)", forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let systemMessage = OpenAIRequest.Message(role: "system", content: "You are an expert automotive tuner. You output only raw JSON. Do not use Markdown code blocks.")
        let userMessage = OpenAIRequest.Message(role: "user", content: prompt)
        let payload = OpenAIRequest(messages: [systemMessage, userMessage])
        
        request.httpBody = try JSONEncoder().encode(payload)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "OpenAIError", code: 0, userInfo: [NSLocalizedDescriptionKey: "Failed to get response from OpenAI"])
        }
        
        let result = try JSONDecoder().decode(OpenAIResponse.self, from: data)
        return result.choices.first?.message.content ?? "{}"
    }
    
    // MARK: - Stripe
    func createCustomer(email: String) async throws -> String {
        let url = URL(string: baseURL + "create-customer")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = ["email": email]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            throw NSError(domain: "StripeError", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to create customer"])
        }
        
        let result = try JSONDecoder().decode(CreateCustomerResponse.self, from: data)
        return result.customer
    }
    
    func createSubscription(customerId: String, priceId: String) async throws -> SubscriptionResponse {
        let url = URL(string: baseURL + "create-subscription")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = ["customerId": customerId, "priceId": priceId]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            // Try to read error message
            if let errorJson = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                print("Subscription Error: \(errorJson)")
            }
            throw NSError(domain: "StripeError", code: 2, userInfo: [NSLocalizedDescriptionKey: "Failed to create subscription"])
        }
        
        return try JSONDecoder().decode(SubscriptionResponse.self, from: data)
    }
}
