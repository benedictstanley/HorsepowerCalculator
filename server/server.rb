require 'stripe'
require 'sinatra'
require 'json'

# This is your test secret API key.
# TODO: For Production, set the STRIPE_SECRET_KEY environment variable to your sk_live_... key
Stripe.api_key = ENV['STRIPE_SECRET_KEY']
# Use an older API version to avoid breaking changes with Invoice.payment_intent
Stripe.api_version = '2023-10-16'

set :static, true
set :port, 4242
# Bind to all interfaces so Emulator can access it via 10.0.2.2
set :bind, '0.0.0.0'

YOUR_DOMAIN = 'http://localhost:4242'

# Create a price for the plan if not passed? 
# In a real app, you'd map plan names to Stripe Price IDs.
# For demo, we'll create a price on the fly or use a known one.

post '/create-checkout-session' do
  content_type 'application/json'
  data = JSON.parse(request.body.read)
  
  # Map app plan names to Stripe Price IDs or create them dynamically
  # For this demo, we will create a one-time price or subscription price
  # In production, use existing Price IDs from Stripe Dashboard.
  
  price_amount = data['amount'] || 300 # Default 3.00
  plan_name = data['plan_name'] || 'Pro Plan'
  
  # Create a product/price on the fly for simplicity (NOT recommended for prod)
  # Or just use a hardcoded price logic.
  
  begin
    session = Stripe::Checkout::Session.create({
      payment_method_types: ['card'],
      mode: 'subscription',
      line_items: [{
        price_data: {
          currency: 'usd',
          product_data: {
            name: "CarModsAI #{plan_name}",
          },
          unit_amount: price_amount,
          recurring: {
            interval: 'month',
          },
        },
        quantity: 1,
      }],
      success_url: 'carmods://payment_success?session_id={CHECKOUT_SESSION_ID}',
      cancel_url: 'carmods://payment_cancel',
    })
    
    { url: session.url }.to_json
  rescue StandardError => e
    status 400
    { error: { message: e.message } }.to_json
  end
end

post '/payment-sheet' do
  content_type 'application/json'
  data = JSON.parse(request.body.read)
  
  price_amount = data['amount'] || 300 
  
  # Use an existing Customer ID if this is a returning customer.
  customer = Stripe::Customer.create()
  ephemeralKey = Stripe::EphemeralKey.create(
    {customer: customer.id},
    {stripe_version: '2023-10-16'}
  )
  
  paymentIntent = Stripe::PaymentIntent.create(
    amount: price_amount,
    currency: 'usd',
    customer: customer.id,
    automatic_payment_methods: {
      enabled: true,
    },
  )
  
  {
    paymentIntent: paymentIntent.client_secret,
    ephemeralKey: ephemeralKey.secret,
    customer: customer.id,
    publishableKey: ENV['STRIPE_PUBLISHABLE_KEY']
  }.to_json
end

post '/create-customer' do
  content_type 'application/json'
  data = JSON.parse(request.body.read)

  # Using the hardcoded address from the user request, but making email dynamic
  # In a real app, you would pass these details from the client
  customer = Stripe::Customer.create({
    email: data['email'],
    name: data['name'] || 'Customer Name', # Fallback if not provided
    shipping: {
      name: data['name'] || 'Customer Name',
      address: {
        city: 'Brothers',
        country: 'US',
        line1: '27 Fredrick Ave',
        postal_code: '97712',
        state: 'CA',
      },
    },
    address: {
      city: 'Brothers',
      country: 'US',
      line1: '27 Fredrick Ave',
      postal_code: '97712',
      state: 'CA',
    },
  })

  {
    customer: customer.id,
    message: 'Customer created successfully'
  }.to_json
rescue Stripe::StripeError => e
  status 400
  { error: { message: e.message } }.to_json
end

post '/create-subscription' do
  content_type 'application/json'
  
  body_content = request.body.read
  puts "Received body: #{body_content}"
  
  begin
    data = JSON.parse(body_content)
  rescue JSON::ParserError => e
    status 400
    return { error: { message: "Invalid JSON body: #{e.message}" } }.to_json
  end
  
  # App passes customerId in the body, not cookies
  customer_id = data['customerId']
  price_id = data['priceId']

  begin
    # Create the subscription. Note we're expanding the Subscription's
    # latest invoice and that invoice's payment_intent
    # so we can pass it to the front end to confirm the payment
    subscription = Stripe::Subscription.create({
      customer: customer_id,
      items: [{
        price: price_id,
      }],
      payment_behavior: 'default_incomplete',
      payment_settings: { save_default_payment_method: 'on_subscription' },
      # billing_mode: { type: 'flexible' }, # Commented out as it causes issues with standard API versions
    expand: ['latest_invoice.payment_intent'],
  })

  puts "Subscription created: #{subscription.id}"
  puts "Latest Invoice: #{subscription.latest_invoice}"

  # Handle breaking change where payment_intent is removed from Invoice
  # We pinned API version to 2023-10-16 to ensure payment_intent is returned
  invoice = subscription.latest_invoice
  puts "Invoice ID: #{invoice.id}"
  
  # Try to find payment_intent in the hash directly
  # Since we are using an older API version, it should be present in the JSON response
  # even if the Ruby SDK object wrapper doesn't expose it as a method.
  
  # Access via hash (handling symbol or string keys)
  invoice_hash = invoice.to_hash
  payment_intent = invoice_hash[:payment_intent] || invoice_hash['payment_intent']
  
  if payment_intent
      # If it's an object (Hash or PaymentIntent)
      if payment_intent.is_a?(Hash)
         # Hash keys are likely symbols if coming from to_hash
         client_secret = payment_intent[:client_secret] || payment_intent['client_secret']
      elsif payment_intent.is_a?(Stripe::PaymentIntent)
         # If it's a Stripe object, use method access (or [] if method missing)
         client_secret = payment_intent.respond_to?(:client_secret) ? payment_intent.client_secret : payment_intent['client_secret']
      else
         # It's an ID (String)
         # We might need to fetch it if expansion failed
         # puts "Payment Intent is ID: #{payment_intent}"
         pi = Stripe::PaymentIntent.retrieve(payment_intent)
         client_secret = pi.client_secret
      end
   else
     # puts "WARNING: No payment_intent found on invoice hash. Checking pending_setup_intent..."
     if subscription.pending_setup_intent
        # puts "Found pending_setup_intent: #{subscription.pending_setup_intent}"
        # We could handle setup intent here if needed
     end
     client_secret = nil
  end

  {
    subscriptionId: subscription.id,
    clientSecret: client_secret,
  }.to_json
  rescue Stripe::StripeError => e
    status 400
    { error: { message: e.message } }.to_json
  end
end

post '/cancel-subscription' do
  content_type 'application/json'
  data = JSON.parse request.body.read

  deleted_subscription = Stripe::Subscription.cancel(data['subscriptionId'])

  deleted_subscription.to_json
end

post '/webhook' do
  # You can use webhooks to receive information about asynchronous payment events.
  # For more about our webhook events check out https://stripe.com/docs/webhooks.
  webhook_secret = ENV['STRIPE_WEBHOOK_SECRET'] || 'whsec_d16575a0d083da0beab1ca3ad19abb0ef57b97951ef3b2c74b0bf7854eb4bdce'
  payload = request.body.read
  if !webhook_secret.empty?
    # Retrieve the event by verifying the signature using the raw body and secret if webhook signing is configured.
    sig_header = request.env['HTTP_STRIPE_SIGNATURE']
    event = nil

    begin
      event = Stripe::Webhook.construct_event(
        payload, sig_header, webhook_secret
      )
    rescue JSON::ParserError => e
      # Invalid payload
      status 400
      return
    rescue Stripe::SignatureVerificationError => e
      # Invalid signature
      puts '⚠️  Webhook signature verification failed.'
      status 400
      return
    end
  else
    data = JSON.parse(payload, symbolize_names: true)
    event = Stripe::Event.construct_from(data)
  end
  # Get the type of webhook event sent - used to check the status of PaymentIntents.
  event_type = event['type']
  data = event['data']
  data_object = data['object']

  if event_type == 'invoice.paid'
    # Used to provision services after the trial has ended.
    # The status of the invoice will show up as paid. Store the status in your
    # database to reference when a user accesses your service to avoid hitting rate
    # limits.
    puts "Invoice paid: #{data_object['id']}"
  end

  if event_type == 'invoice.payment_failed'
    # If the payment fails or the customer does not have a valid payment method,
    # an invoice.payment_failed event is sent, the subscription becomes past_due.
    # Use this webhook to notify your user that their payment has
    # failed and to retrieve new card details.
    puts "Invoice payment failed: #{data_object['id']}"
  end

  if event_type == 'customer.subscription.deleted'
    # handle subscription canceled automatically based
    # upon your subscription settings. Or if the user cancels it.
    puts "Subscription deleted: #{data_object['id']}"
  end

  content_type 'application/json'
  { status: 'success' }.to_json
end
