package com.example.expencetrackerapp.ml

import android.content.Context

/**
 * Hybrid SMS classifier that combines rule-based scoring with ML predictions.
 * 
 * This provides the best of both worlds:
 * 1. Rule-based: Accurate for known patterns, works immediately
 * 2. ML-based: Learns from user behavior, improves over time
 * 
 * The final decision uses weighted combination based on ML confidence.
 */
class HybridSmsClassifier(context: Context) {
    
    private val mlClassifier = SmsClassifier(context)
    
    companion object {
        // Weight thresholds
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.6f
    }
    
    /**
     * Classify an SMS using hybrid approach.
     * 
     * @param smsBody The SMS text to classify
     * @param ruleBasedSpamScore Score from rule-based system (higher = more spam-like)
     * @param ruleBasedTransactionScore Score from rule-based system (higher = more transaction-like)
     * @return HybridResult with final decision and reasoning
     */
    fun classify(
        smsBody: String,
        ruleBasedSpamScore: Int,
        ruleBasedTransactionScore: Int
    ): HybridResult {
        // Get ML prediction
        val mlResult = mlClassifier.classify(smsBody)
        
        // Calculate rule-based decision
        val ruleBasedIsSpam = ruleBasedSpamScore > ruleBasedTransactionScore + 5
        val ruleBasedConfidence = calculateRuleConfidence(ruleBasedSpamScore, ruleBasedTransactionScore)
        
        // Combine decisions
        val finalDecision: Boolean
        val usedML: Boolean
        val confidence: Float
        val reason: String
        
        when {
            // If ML is ready and highly confident, prefer ML
            mlResult.usedML && mlResult.confidence >= HIGH_CONFIDENCE_THRESHOLD -> {
                finalDecision = mlResult.prediction == SmsClassifier.CLASS_SPAM
                usedML = true
                confidence = mlResult.confidence
                reason = "ML prediction (high confidence: ${(confidence * 100).toInt()}%)"
            }
            
            // If rule-based is highly confident, prefer rules
            ruleBasedConfidence >= HIGH_CONFIDENCE_THRESHOLD -> {
                finalDecision = ruleBasedIsSpam
                usedML = false
                confidence = ruleBasedConfidence
                reason = "Rule-based (high confidence: ${(confidence * 100).toInt()}%)"
            }
            
            // If ML is ready and both have moderate confidence, use weighted average
            mlResult.usedML && mlResult.confidence >= LOW_CONFIDENCE_THRESHOLD -> {
                val mlWeight = 0.6f
                val ruleWeight = 0.4f
                
                val mlSpamProb = if (mlResult.prediction == SmsClassifier.CLASS_SPAM) 
                    mlResult.confidence else 1f - mlResult.confidence
                val ruleSpamProb = if (ruleBasedIsSpam)
                    ruleBasedConfidence else 1f - ruleBasedConfidence
                
                val combinedSpamProb = mlSpamProb * mlWeight + ruleSpamProb * ruleWeight
                finalDecision = combinedSpamProb > 0.5f
                usedML = true
                confidence = combinedSpamProb
                reason = "Hybrid (ML: ${(mlSpamProb * 100).toInt()}%, Rules: ${(ruleSpamProb * 100).toInt()}%)"
            }
            
            // Default to rule-based
            else -> {
                finalDecision = ruleBasedIsSpam
                usedML = false
                confidence = ruleBasedConfidence
                reason = "Rule-based (default)"
            }
        }
        
        return HybridResult(
            isSpam = finalDecision,
            confidence = confidence,
            usedML = usedML,
            mlPrediction = mlResult.prediction,
            mlConfidence = mlResult.confidence,
            ruleScore = ruleBasedSpamScore - ruleBasedTransactionScore,
            reason = reason
        )
    }
    
    /**
     * Train the ML model with user feedback.
     */
    suspend fun train(smsBody: String, isSpam: Boolean) {
        val label = if (isSpam) SmsClassifier.CLASS_SPAM else SmsClassifier.CLASS_TRANSACTION
        mlClassifier.train(smsBody, label)
    }
    
    /**
     * Get model statistics for UI display.
     */
    fun getStats(): SmsClassifier.ModelStats = mlClassifier.getStats()
    
    private fun calculateRuleConfidence(spamScore: Int, transactionScore: Int): Float {
        val scoreDiff = kotlin.math.abs(spamScore - transactionScore)
        return when {
            scoreDiff >= 15 -> 0.95f
            scoreDiff >= 10 -> 0.85f
            scoreDiff >= 5 -> 0.7f
            scoreDiff >= 2 -> 0.55f
            else -> 0.5f
        }
    }
    
    data class HybridResult(
        val isSpam: Boolean,
        val confidence: Float,
        val usedML: Boolean,
        val mlPrediction: String,
        val mlConfidence: Float,
        val ruleScore: Int,
        val reason: String
    )
}
