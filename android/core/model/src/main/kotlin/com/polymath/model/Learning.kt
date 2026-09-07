package com.polymath.model

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sqrt

enum class ContentKind { NEWS, KNOWLEDGE_PILL }
enum class NoteMode(val label: String) { THOUGHT("Thought"), RESEARCH_TOPIC("Research"), IDEA("Idea") }
enum class Judgment { SAVE, DISMISS }

data class Topic(val id: String, val title: String, val x: Float, val y: Float)
object Topics {
    val all = listOf(
        Topic("systems", "Systems thinking", .50f, .38f),
        Topic("ai", "Artificial intelligence", .25f, .20f),
        Topic("design", "Design", .78f, .25f),
        Topic("science", "Science", .22f, .58f),
        Topic("math", "Mathematics", .64f, .70f),
        Topic("craft", "Craft of work", .84f, .53f),
        Topic("history", "History", .13f, .82f),
        Topic("philosophy", "Philosophy", .44f, .88f),
        Topic("literature", "Literature & language", .80f, .88f),
        Topic("economics", "Economics", .92f, .10f),
        Topic("engineering", "Engineering", .09f, .38f),
        Topic("certifications", "Professional certifications", .55f, .08f),
    )
    // Editorial relatedness, never an inferred prerequisite or a mastery claim.
    val related = listOf("systems" to "ai", "systems" to "design", "systems" to "science",
        "systems" to "math", "systems" to "craft", "ai" to "math", "design" to "craft", "science" to "engineering", "craft" to "certifications",
        "history" to "literature", "philosophy" to "history", "economics" to "math", "philosophy" to "systems")
    fun title(id: String) = all.firstOrNull { it.id == id }?.title ?: id
}

data class Card(
    val id: String,
    val kind: ContentKind,
    val title: String,
    val summary: String,
    val body: String,
    val topicId: String,
    val publisher: String,
    val sourceUrl: String,
    val publishedAt: Long?,
    val fetchedAt: Long,
    val question: String? = null,
    val answers: List<String> = emptyList(),
    val correctAnswer: Int = -1,
    val explanation: String = "",
    val edition: Int = 1,
    val images: List<SourceImage> = emptyList(),
    val datasetId: String = "public",
)

data class SourceImage(val url: String, val alt: String = "Source illustration", val caption: String = "")

data class Preference(val topicId: String, val followed: Boolean, val muted: Boolean = false)
data class TrainingEvent(val id: String, val features: List<Double>, val judgment: Judgment)

/** Tiny online logistic model. Replay from zero makes an undo exact, including later events. */
object Recommender {
    const val VERSION = 2
    val dimensions = Topics.all.size + 2
    fun features(card: Card): List<Double> = List(dimensions) { index ->
        when {
            index < Topics.all.size -> if (Topics.all[index].id == card.topicId) 1.0 else 0.0
            index == Topics.all.size -> if (card.kind == ContentKind.NEWS) 1.0 else -1.0
            else -> 1.0
        }
    }
    fun replay(events: List<TrainingEvent>): List<Double> {
        val weights = DoubleArray(dimensions)
        events.forEach { event ->
            require(event.features.size == dimensions)
            val p = sigmoid(weights.indices.sumOf { weights[it] * event.features[it] })
            val target = if (event.judgment == Judgment.SAVE) 1.0 else 0.0
            val confidence = if (event.judgment == Judgment.SAVE) 1.0 else .3
            weights.indices.forEach { i -> weights[i] += .18 * confidence * (target - p) * event.features[i] }
        }
        return weights.toList()
    }
    fun rank(cards: List<Card>, preferences: List<Preference>, events: List<TrainingEvent>,
             seed: Long, now: Long): List<Card> {
        val allowed = cards.filter { card -> preferences.none { it.topicId == card.topicId && it.muted } }
        val weights = replay(events)
        val learnedMix = (events.size / 20.0).coerceAtMost(1.0)
        fun score(card: Card): Double {
            val prior = if (preferences.any { it.topicId == card.topicId && it.followed }) .75 else .35
            val prediction = sigmoid(features(card).indices.sumOf { weights[it] * features(card)[it] })
            val ageHours = card.publishedAt?.let { ((now - it).coerceAtLeast(0) / 3_600_000.0) } ?: 0.0
            val freshness = if (card.kind == ContentKind.NEWS) .08 * exp(-ageHours / 48.0) else .04
            return (1 - learnedMix) * prior + learnedMix * prediction + freshness
        }
        val pool = allowed.sortedWith(compareByDescending<Card> { score(it) }.thenBy { it.id }).toMutableList()
        val result = mutableListOf<Card>()
        while (pool.isNotEmpty()) {
            // Source diversity is a soft constraint, relaxed when inventory is narrow.
            val diverse = pool.filter { candidate -> result.takeLast(9).count { it.publisher == candidate.publisher } < 2 }
            val candidates = diverse.ifEmpty { pool }
            val chosen = if (result.size % 10 == 9) candidates.minBy { stableHash("$seed:${it.id}") }
                else candidates.first()
            result += chosen
            pool.remove(chosen)
        }
        return result
    }
    private fun stableHash(text: String): Long = text.fold(1125899906842597L) { a, c -> 31 * a + c.code }.ushr(1)
    private fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z.coerceIn(-20.0, 20.0)))
}

object LearningRules {
    const val DAY = 86_400_000L
    private val intervals = listOf(1, 3, 7, 14, 30)
    fun level(exp: Int): Int = floor(sqrt(exp.coerceAtLeast(0) / 100.0)).toInt()
    fun nextLevelAt(exp: Int): Int = (level(exp) + 1).let { it * it * 100 }
    fun nextReviewAt(now: Long, successfulReviews: Int, correct: Boolean): Long =
        now + DAY * if (correct) intervals[successfulReviews.coerceIn(0, intervals.lastIndex)] else 1
    fun award(firstCredit: Boolean, correct: Boolean) = if (!correct) 0 else if (firstCredit) 10 else 5
    fun applicationAward(alreadyEarnedToday: Int) = (40 - alreadyEarnedToday).coerceIn(0, 20)
}

data class PlanStep(val title: String, val acceptance: String, val dependsOn: Int? = null)
object PlanTemplates {
    fun forIdea(outcome: String) = listOf(
        PlanStep("Clarify the problem", "Write one observable problem and who experiences it."),
        PlanStep("Test the key assumption", "Collect evidence from one small experiment; record what would disprove the idea.", 0),
        PlanStep("Build the smallest useful version", "Demonstrate one complete use case toward: ${outcome.ifBlank { "the intended outcome" }}.", 1),
        PlanStep("Reflect and choose the next step", "Record the result, one limitation, and a concrete follow-up.", 2),
    )
}

/** Explicit capability boundary. No canned response may masquerade as LLM output. */
data class EvidenceExcerpt(val documentId: String, val title: String, val text: String, val sourceUrl: String?)
interface EmbeddingEngine {
    val modelId: String
    val dimensions: Int
    suspend fun embed(text: String): FloatArray
}
interface GroundedGenerator {
    suspend fun answer(question: String, evidence: List<EvidenceExcerpt>): String
}
