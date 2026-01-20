package com.carmodai.app

import android.content.Context

object SubscriptionManager {
    const val PLAN_FREE = "Free"
    const val PLAN_STARTER = "Starter" // $3
    const val PLAN_PRO = "Pro"         // $10
    const val PLAN_VIP = "VIP"         // $50

    // Live Mode Price IDs (Created on 2026-01-13)
    const val PRICE_ID_STARTER = "price_1SpGzkJuuVUrfrDtrnvukBfT" // $1.00
    const val PRICE_ID_PRO = "price_1SpGzuJuuVUrfrDtMQW2UQIN"     // $5.00
    const val PRICE_ID_VIP = "price_1SpGrCJuuVUrfrDtXebgX1iX"     // $50.00

    fun getPlanLimits(planName: String): PlanLimits {
        return when (planName) {
            PLAN_STARTER -> PlanLimits(calculations = 10, dynoRuns = 1, isUnlimited = false)
            PLAN_PRO -> PlanLimits(calculations = 999999, dynoRuns = 999999, isUnlimited = true)
            PLAN_VIP -> PlanLimits(calculations = 999999, dynoRuns = 999999, isUnlimited = true)
            else -> PlanLimits(calculations = 3, dynoRuns = 0, isUnlimited = false)
        }
    }
}

data class PlanLimits(
    val calculations: Int,
    val dynoRuns: Int,
    val isUnlimited: Boolean
)