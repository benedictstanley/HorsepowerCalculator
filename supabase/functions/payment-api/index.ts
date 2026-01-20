// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import Stripe from "https://esm.sh/stripe@14.10.0?target=deno"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const supabaseUrl = 'https://nmrncplnxsikhbzowrol.supabase.co'
const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? Deno.env.get('SUPABASE_ANON_KEY') ?? ''
const supabase = createClient(supabaseUrl, supabaseKey)

const stripe = new Stripe(Deno.env.get('STRIPE_SECRET_KEY') ?? '', {
  // This is needed to use the Fetch API rather than Node's http client
  httpClient: Stripe.createFetchHttpClient(),
  apiVersion: '2023-10-16',
})

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  const url = new URL(req.url)
  const path = url.pathname.replace(/\/payment-api/, '') // Remove function name from path if needed

  try {
    if (path.endsWith('/payment-sheet') && req.method === 'POST') {
      const data = await req.json()
      
      // Default to $1.00 if not specified
      let amount = 100 

      // If plan_name is provided, use the correct amount
      if (data.plan_name) {
        const plan = data.plan_name.toLowerCase()
        if (plan.includes('vip')) {
             amount = 5000 // $50.00
        } else if (plan.includes('pro')) {
             amount = 500  // $5.00
        } else if (plan.includes('starter')) {
             amount = 100  // $1.00
        }
      }

      // Allow explicit amount override
      if (data.amount) {
        amount = data.amount
      }

      const customer = await stripe.customers.create()
      const ephemeralKey = await stripe.ephemeralKeys.create(
        { customer: customer.id },
        { stripe_version: '2023-10-16' }
      )
      
      const paymentIntent = await stripe.paymentIntents.create({
        amount: amount,
        currency: 'usd',
        customer: customer.id,
        automatic_payment_methods: {
          enabled: true,
        },
      })

      return new Response(
        JSON.stringify({
          paymentIntent: paymentIntent.client_secret,
          ephemeralKey: ephemeralKey.secret,
          customer: customer.id,
          publishableKey: Deno.env.get('STRIPE_PUBLISHABLE_KEY'),
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    if (path.endsWith('/create-subscription') && req.method === 'POST') {
      const data = await req.json()
      const customerId = data.customerId
      const priceId = data.priceId

      const subscription = await stripe.subscriptions.create({
        customer: customerId,
        items: [{ price: priceId }],
        payment_behavior: 'default_incomplete',
        payment_settings: { save_default_payment_method: 'on_subscription' },
        expand: ['latest_invoice.payment_intent'],
      })

      const invoice = subscription.latest_invoice as any
      let clientSecret = null

      if (invoice.payment_intent) {
         if (typeof invoice.payment_intent === 'string') {
             const pi = await stripe.paymentIntents.retrieve(invoice.payment_intent)
             clientSecret = pi.client_secret
         } else {
             clientSecret = invoice.payment_intent.client_secret
         }
      }

      return new Response(
        JSON.stringify({
          subscriptionId: subscription.id,
          clientSecret: clientSecret,
        }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      )
    }

    if (path.endsWith('/create-customer') && req.method === 'POST') {
        const data = await req.json()
        const customer = await stripe.customers.create({
            email: data.email,
            name: data.name || 'Customer Name',
            shipping: {
                name: data.name || 'Customer Name',
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

        return new Response(
            JSON.stringify({
                customer: customer.id,
                message: 'Customer created successfully'
            }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
    }

    return new Response(JSON.stringify({ error: 'Not Found' }), {
      status: 404,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  }
})
