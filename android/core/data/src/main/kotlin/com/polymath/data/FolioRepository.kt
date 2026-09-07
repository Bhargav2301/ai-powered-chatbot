package com.polymath.data

import androidx.room.withTransaction
import com.polymath.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class FolioSnapshot(
    val cards: List<Card> = emptyList(), val preferences: List<Preference> = emptyList(),
    val swipes: List<SwipeEntity> = emptyList(), val saves: List<SaveEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(), val plans: List<PlanEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(), val attempts: List<AttemptEntity> = emptyList(),
    val reviews: List<ReviewEntity> = emptyList(), val awards: List<ExpAwardEntity> = emptyList(),
) {
    val activeSwipes: List<SwipeEntity> get() {
        val reversed = swipes.mapNotNull { it.reversalOf }.toSet()
        return swipes.filter { it.reversalOf == null && it.actionId !in reversed }
    }
    val totalExp get() = awards.sumOf { it.amount }.coerceAtLeast(0)
    fun topicExp(id: String) = awards.filter { it.topicId == id }.sumOf { it.amount }.coerceAtLeast(0)
    fun deck(now: Long): List<Card> {
        val seen = activeSwipes.map { it.contentId }.toSet()
        val day = LocalDate.ofEpochDay(now / LearningRules.DAY)
        val start = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val remaining = (20 - activeSwipes.count { it.createdAt >= start }).coerceAtLeast(0)
        val events = activeSwipes.filter { it.modelVersion == Recommender.VERSION }.map {
            TrainingEvent(it.actionId, it.features.split(',').map(String::toDouble), Judgment.valueOf(it.action))
        }
        return Recommender.rank(cards.filter { it.id !in seen }, preferences, events, now / LearningRules.DAY, now).take(remaining)
    }
}

class FolioRepository(private val db: FolioDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.folio()
    val snapshots: Flow<FolioSnapshot> = db.invalidationTracker.createFlow(
        "contents", "preferences", "swipes", "saves", "notes", "plans", "tasks", "attempts", "reviews", "exp_awards"
    ).map { snapshot() }

    suspend fun snapshot(): FolioSnapshot = db.withTransaction {
        FolioSnapshot(dao.contents().map { it.card() }, dao.preferences().map { it.preference() }, dao.swipes(),
            dao.saves(), dao.notes(), dao.plans(), dao.tasks(), dao.attempts(), dao.reviews(), dao.awards())
    }

    suspend fun initialize() = db.withTransaction {
        dao.insertContent(StarterContent.items())
        val existing = dao.preferences().map { it.topicId }.toSet()
        Topics.all.filter { it.id !in existing }.forEach { dao.preference(PreferenceEntity(it.id, false)) }
    }

    suspend fun preferences(followed: Set<String>) = db.withTransaction {
        Topics.all.forEach { dao.preference(PreferenceEntity(it.id, it.id in followed)) }
    }

    suspend fun preference(topic: String, followed: Boolean, muted: Boolean) {
        require(Topics.all.any { it.id == topic })
        dao.preference(PreferenceEntity(topic, followed && !muted, muted))
    }

    suspend fun judge(contentId: String, judgment: Judgment, actionId: String): Boolean = db.withTransaction {
        val current = snapshot()
        if (current.activeSwipes.any { it.contentId == contentId }) return@withTransaction false
        val card = dao.content(contentId)?.card() ?: return@withTransaction false
        val event = SwipeEntity(actionId = actionId, contentId = contentId, action = judgment.name,
            features = Recommender.features(card).joinToString(","), createdAt = now())
        if (dao.swipe(event) == -1L) return@withTransaction false
        if (judgment == Judgment.SAVE && dao.save(contentId) == null) {
            dao.save(SaveEntity(contentId, now(), actionId))
            index(card)
        }
        true
    }

    suspend fun undo(actionId: String): Boolean = db.withTransaction {
        val all = dao.swipes()
        val event = all.firstOrNull { it.actionId == actionId && it.reversalOf == null } ?: return@withTransaction false
        if (now() - event.createdAt !in 0..5_000 || all.any { it.reversalOf == actionId }) return@withTransaction false
        dao.swipe(SwipeEntity(actionId = "undo:$actionId", contentId = event.contentId, action = "UNDO",
            features = "", createdAt = now(), reversalOf = actionId))
        if (dao.save(event.contentId)?.originAction == actionId) {
            dao.deleteSave(event.contentId)
            dao.deleteDocument("content:${event.contentId}")
        }
        true
    }

    /** An explicit save owns its lifetime, even if a previous swipe is later undone. */
    suspend fun save(contentId: String) = db.withTransaction {
        val card = requireNotNull(dao.content(contentId)).card()
        dao.save(SaveEntity(contentId, dao.save(contentId)?.savedAt ?: now(), null))
        index(card)
    }

    suspend fun removeSave(contentId: String) = db.withTransaction {
        dao.deleteSave(contentId)
        dao.deleteDocument("content:$contentId")
    }

    suspend fun saveNote(draft: NoteEntity): NoteEntity = db.withTransaction {
        require(draft.title.isNotBlank() || draft.body.isNotBlank()) { "Write a thought or title first." }
        val previous = dao.note(draft.id)
        if (previous != null && draft.revision < previous.revision) error("This note changed. Reopen it before editing.")
        val saved = draft.copy(revision = (previous?.revision ?: 0) + 1,
            createdAt = previous?.createdAt ?: now(), updatedAt = now())
        dao.note(saved)
        dao.revision(NoteRevisionEntity(saved.id, saved.revision, saved.mode, saved.title, saved.body, saved.goal, saved.context, now()))
        indexDocument("note:${saved.id}", saved.mode.name, saved.id, saved.title.ifBlank { saved.body.take(60) },
            "${saved.body}\n${saved.goal}\n${saved.context}", saved.topicId)
        saved
    }

    suspend fun deleteNote(id: String) = db.withTransaction {
        dao.deleteDocument("note:$id")
        dao.deleteNote(id)
    }

    suspend fun makePlan(noteId: String): String = db.withTransaction {
        val note = requireNotNull(dao.note(noteId))
        require(note.mode == NoteMode.IDEA) { "Plans belong to ideas." }
        dao.plans().firstOrNull { it.noteId == noteId }?.let { return@withTransaction it.id }
        val planId = uuid()
        dao.plan(PlanEntity(planId, note.id, note.revision, createdAt = now()))
        val template = PlanTemplates.forIdea(note.goal)
        val ids = template.map { uuid() }
        dao.tasks(template.mapIndexed { index, step -> TaskEntity(ids[index], planId, index, step.title,
            step.acceptance, step.dependsOn?.let { ids[it] }) })
        planId
    }

    suspend fun acceptPlan(planId: String) = db.withTransaction {
        val plan = requireNotNull(dao.plan(planId))
        val note = requireNotNull(dao.note(plan.noteId))
        require(note.revision == plan.sourceRevision) { "The idea changed. Discard the draft and create a new plan." }
        dao.plan(plan.copy(status = "ACTIVE"))
    }

    suspend fun discardDraft(planId: String) = db.withTransaction {
        require(dao.plan(planId)?.status == "DRAFT") { "Only a draft can be discarded." }
        dao.deletePlan(planId)
    }

    suspend fun completeTask(id: String, reflection: String) = db.withTransaction {
        val task = requireNotNull(dao.task(id))
        if (task.done) return@withTransaction
        require(reflection.trim().length >= 12) { "Add a short reflection on what you did and learned." }
        val plan = requireNotNull(dao.plan(task.planId))
        require(plan.status == "ACTIVE") { "Accept the plan first." }
        require(task.dependencyId == null || dao.task(task.dependencyId)?.done == true) { "Complete the previous task first." }
        val note = requireNotNull(dao.note(plan.noteId))
        dao.tasks(listOf(task.copy(done = true, reflection = reflection.trim(), completedAt = now())))
        val startOfDay = now() / LearningRules.DAY * LearningRules.DAY
        val earned = dao.awards().filter { it.reason == "APPLICATION" && it.createdAt >= startOfDay }.sumOf { it.amount }
        val amount = LearningRules.applicationAward(earned)
        // Record a zero award too: completing again tomorrow must not evade the daily cap.
        dao.award(ExpAwardEntity(uuid(), "task:$id", note.topicId, amount, "APPLICATION", now()))
    }

    suspend fun reopenTask(id: String) = db.withTransaction {
        val task = requireNotNull(dao.task(id))
        require(dao.tasks().none { it.dependencyId == id && it.done }) { "Reopen dependent tasks first." }
        dao.tasks(listOf(task.copy(done = false, completedAt = null)))
        dao.awards().firstOrNull { it.earningKey == "task:$id" }?.let { award ->
            dao.award(ExpAwardEntity(uuid(), "reverse:task:$id", award.topicId, -award.amount,
                "REVERSAL", now(), award.id))
        }
    }

    suspend fun answer(contentId: String, selected: Int, expectedEpisode: Int): AttemptEntity = db.withTransaction {
        val content = requireNotNull(dao.content(contentId))
        require(content.question != null && selected in content.answers.indices)
        val schedule = dao.review(contentId)
        val key = "$contentId:${content.edition}:$expectedEpisode"
        dao.attempt(key)?.let { return@withTransaction it }
        require((schedule?.episode ?: 0) == expectedEpisode) { "This review has already changed." }
        require(schedule == null || now() >= schedule.dueAt) { "Your next recall is not due yet." }
        val correct = selected == content.correctAnswer
        val firstCredit = schedule?.hasFirstCredit != true
        val amount = LearningRules.award(firstCredit, correct)
        val attempt = AttemptEntity(uuid(), contentId, key, selected, correct, amount, now())
        dao.attempt(attempt)
        if (amount > 0) dao.award(ExpAwardEntity(uuid(), "recall:$key", content.topicId, amount, "RECALL", now()))
        val successes = if (correct) (schedule?.successfulReviews ?: 0) + 1 else 0
        dao.review(ReviewEntity(contentId, content.edition, expectedEpisode + 1, successes,
            !firstCredit || correct, LearningRules.nextReviewAt(now(), (successes - 1).coerceAtLeast(0), correct)))
        attempt
    }

    suspend fun ingest(items: List<ContentEntity>) = dao.insertContent(items)
    suspend fun search(text: String): List<SearchDocument> = withContext(Dispatchers.IO) {
        // FTS4 prefix markers belong inside the phrase quotes (FTS5 differs).
        val tokens = Regex("[\\p{L}\\p{N}]+").findAll(text).map { "\"${it.value}*\"" }.take(12).toList()
        if (text.isBlank()) dao.recentDocuments()
        else if (tokens.isEmpty()) emptyList() else dao.search(tokens.joinToString(" AND "))
    }
    private suspend fun index(card: Card) = indexDocument("content:${card.id}", card.kind.name, card.id,
        card.title, "${card.summary}\n${card.body}", card.topicId)
    private suspend fun indexDocument(key: String, kind: String, id: String, title: String, body: String, topic: String) {
        dao.searchDocument(SearchDocument(dao.searchDocument(key)?.rowId ?: 0, key, kind, id, title, body, topic, now()))
    }
    companion object { fun uuid(): String = UUID.randomUUID().toString() }
}
