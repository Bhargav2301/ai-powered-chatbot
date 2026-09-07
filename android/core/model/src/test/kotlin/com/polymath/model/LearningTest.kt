package com.polymath.model

import org.junit.Assert.*
import org.junit.Test

class LearningTest {
    private fun card(id: String, topic: String = "ai", publisher: String = id) = Card(id, ContentKind.KNOWLEDGE_PILL,
        id, "summary", "body", topic, publisher, "https://example.org/$id", null, 0)
    @Test fun `mute is a hard exclusion including exploration`() {
        val result = Recommender.rank((0..30).map { card("$it") }, listOf(Preference("ai", true, true)), emptyList(), 4, 0)
        assertTrue(result.isEmpty())
    }
    @Test fun `positive feedback increases topic affinity`() {
        val item = card("a")
        val weights = Recommender.replay(listOf(TrainingEvent("1", Recommender.features(item), Judgment.SAVE)))
        assertTrue(weights[Topics.all.indexOfFirst { it.id == "ai" }] > 0)
    }
    @Test fun `dismissal is weaker than positive feedback`() {
        val f = Recommender.features(card("a"))
        val positive = Recommender.replay(listOf(TrainingEvent("1", f, Judgment.SAVE)))
        val negative = Recommender.replay(listOf(TrainingEvent("1", f, Judgment.DISMISS)))
        assertEquals(positive.last() * .3, -negative.last(), .000001)
    }
    @Test fun `replay after undo exactly matches never having trained that event`() {
        val a = TrainingEvent("a", Recommender.features(card("a")), Judgment.SAVE)
        val b = TrainingEvent("b", Recommender.features(card("b", "math")), Judgment.DISMISS)
        val c = TrainingEvent("c", Recommender.features(card("c")), Judgment.SAVE)
        val retained = listOf(a, b, c).filterNot { it.id == b.id }
        assertEquals(Recommender.replay(listOf(a, c)), Recommender.replay(retained))
    }
    @Test fun `cold start prioritizes explicit follows`() {
        val ranked = Recommender.rank(listOf(card("a"), card("z", "design")), listOf(Preference("design", true)), emptyList(), 1, 0)
        assertEquals("z", ranked.first().id)
    }
    @Test fun `rank is stable for a daily seed`() {
        val cards = (0..30).map { card("$it") }
        assertEquals(Recommender.rank(cards, emptyList(), emptyList(), 7, 0), Recommender.rank(cards, emptyList(), emptyList(), 7, 0))
    }
    @Test fun `levels reflect square thresholds`() {
        assertEquals(0, LearningRules.level(99)); assertEquals(1, LearningRules.level(100))
        assertEquals(1, LearningRules.level(399)); assertEquals(2, LearningRules.level(400))
        assertEquals(900, LearningRules.nextLevelAt(400))
    }
    @Test fun `incorrect recall earns nothing and returns tomorrow`() {
        assertEquals(0, LearningRules.award(true, false))
        assertEquals(100L + LearningRules.DAY, LearningRules.nextReviewAt(100, 4, false))
    }
    @Test fun `review schedule saturates at thirty days`() {
        assertEquals(30 * LearningRules.DAY, LearningRules.nextReviewAt(0, 99, true))
    }
    @Test fun `application EXP is capped at forty a day`() {
        assertEquals(20, LearningRules.applicationAward(0)); assertEquals(20, LearningRules.applicationAward(20))
        assertEquals(0, LearningRules.applicationAward(40))
    }
    @Test fun `template dependencies only refer to prior tasks`() {
        PlanTemplates.forIdea("test").forEachIndexed { index, step -> assertTrue(step.dependsOn == null || step.dependsOn < index) }
    }
}
