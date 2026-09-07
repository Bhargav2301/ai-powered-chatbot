package com.polymath.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polymath.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private lateinit var db: FolioDatabase
    private lateinit var repository: FolioRepository
    private var time = 10 * LearningRules.DAY
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FolioDatabase::class.java).allowMainThreadQueries().build()
        repository = FolioRepository(db) { time }
        repository.initialize()
    }
    @After fun tearDown() { db.close() }
    private fun idea() = NoteEntity("idea", NoteMode.IDEA, "A recall tool", "Help learners bring useful ideas back.",
        goal = "One usable lesson", topicId = "ai", createdAt = time, updatedAt = time)

    @Test fun `save updates FTS in the same transaction and removal removes the result`() = runBlocking {
        assertTrue(repository.search("retrieving").isEmpty())
        repository.save("starter:retrieval")
        assertEquals("starter:retrieval", repository.search("retriev").single().resourceId)
        repository.removeSave("starter:retrieval")
        assertTrue(repository.search("retriev").isEmpty())
    }
    @Test fun `duplicate swipe only trains once and undo creates a reversal`() = runBlocking {
        assertTrue(repository.judge("starter:retrieval", Judgment.SAVE, "action"))
        assertFalse(repository.judge("starter:retrieval", Judgment.SAVE, "action"))
        assertEquals(1, repository.snapshot().activeSwipes.size)
        assertEquals(0, repository.snapshot().totalExp)
        time += 100
        assertTrue(repository.undo("action"))
        assertEquals(2, repository.snapshot().swipes.size)
        assertTrue(repository.snapshot().activeSwipes.isEmpty())
        assertTrue(repository.snapshot().saves.isEmpty())
        assertFalse(repository.undo("action"))
    }
    @Test fun `undo cannot delete a later explicit save`() = runBlocking {
        repository.judge("starter:retrieval", Judgment.SAVE, "action")
        repository.save("starter:retrieval")
        assertTrue(repository.undo("action"))
        assertEquals(1, repository.snapshot().saves.size)
        assertEquals(1, repository.search("retriev").size)
    }
    @Test fun `undo expires after five seconds`() = runBlocking {
        repository.judge("starter:retrieval", Judgment.DISMISS, "action")
        time += 5_001
        assertFalse(repository.undo("action"))
        assertEquals(1, repository.snapshot().activeSwipes.size)
    }
    @Test fun `note mode conversion preserves revisions and FTS replaces old words`() = runBlocking {
        val first = repository.saveNote(idea().copy(body = "oldkeyword"))
        val second = repository.saveNote(first.copy(mode = NoteMode.RESEARCH_TOPIC, body = "newkeyword"))
        assertEquals(2, second.revision)
        assertEquals(listOf(NoteMode.IDEA, NoteMode.RESEARCH_TOPIC), db.folio().revisions("idea").map { it.mode })
        assertTrue(repository.search("oldkeyword").isEmpty())
        assertEquals(1, repository.search("newkeyword").size)
        repository.deleteNote("idea")
        assertTrue(repository.search("newkeyword").isEmpty())
    }
    @Test fun `stale note update is rejected`() = runBlocking {
        val original = repository.saveNote(idea())
        repository.saveNote(original.copy(body = "Newer changes"))
        assertTrue(runCatching { repository.saveNote(original.copy(body = "Stale changes")) }.isFailure)
        assertEquals("Newer changes", repository.snapshot().notes.single().body)
    }
    @Test fun `duplicate recall submission cannot double award EXP`() = runBlocking {
        val first = repository.answer("starter:retrieval", 1, 0)
        val duplicate = repository.answer("starter:retrieval", 0, 0)
        assertEquals(first, duplicate)
        assertEquals(10, repository.snapshot().totalExp)
        assertEquals(1, repository.snapshot().attempts.size)
        assertTrue(runCatching { repository.answer("starter:retrieval", 1, 1) }.isFailure)
    }
    @Test fun `incorrect first attempt earns zero and successful later recall earns first credit`() = runBlocking {
        repository.answer("starter:retrieval", 0, 0)
        assertEquals(0, repository.snapshot().totalExp)
        time += LearningRules.DAY
        assertEquals(10, repository.answer("starter:retrieval", 1, 1).awardedExp)
        time += LearningRules.DAY
        assertEquals(5, repository.answer("starter:retrieval", 1, 2).awardedExp)
        assertEquals(15, repository.snapshot().totalExp)
    }
    @Test fun `plan acceptance validates the idea revision`() = runBlocking {
        val note = repository.saveNote(idea())
        val plan = repository.makePlan(note.id)
        repository.saveNote(note.copy(goal = "A changed outcome"))
        assertTrue(runCatching { repository.acceptPlan(plan) }.isFailure)
        repository.discardDraft(plan)
        assertTrue(repository.snapshot().tasks.isEmpty())
        assertTrue(repository.snapshot().plans.isEmpty())
    }
    @Test fun `task dependencies and daily EXP cap are enforced inside the transaction`() = runBlocking {
        repository.saveNote(idea())
        val plan = repository.makePlan("idea")
        repository.acceptPlan(plan)
        val tasks = repository.snapshot().tasks
        assertTrue(runCatching { repository.completeTask(tasks[1].id, "I tested the assumption.") }.isFailure)
        tasks.forEach { repository.completeTask(it.id, "I completed the task and recorded the result.") }
        assertEquals(40, repository.snapshot().totalExp)
        assertEquals(4, repository.snapshot().awards.size)
        assertTrue(runCatching { repository.reopenTask(tasks[0].id) }.isFailure)
    }
    @Test fun `reopening reverses the award and completion cannot farm EXP`() = runBlocking {
        repository.saveNote(idea())
        val plan = repository.makePlan("idea")
        repository.acceptPlan(plan)
        val task = repository.snapshot().tasks.first()
        repository.completeTask(task.id, "I documented the result.")
        assertEquals(20, repository.snapshot().totalExp)
        repository.reopenTask(task.id)
        assertEquals(0, repository.snapshot().totalExp)
        repository.completeTask(task.id, "I documented the updated result.")
        assertEquals(0, repository.snapshot().totalExp)
        repository.reopenTask(task.id)
        assertEquals(0, repository.snapshot().totalExp)
    }
    @Test fun `FTS syntax is treated as text rather than executable query syntax`() = runBlocking {
        repository.save("starter:retrieval")
        assertTrue(repository.search("\" OR * NEAR() -").isEmpty())
        assertTrue(repository.search("***").isEmpty())
    }
    @Test fun `disk database retains the folio across a repository restart`() = runBlocking {
        db.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("restart-test.db")
        db = Room.databaseBuilder(context, FolioDatabase::class.java, "restart-test.db").allowMainThreadQueries().build()
        repository = FolioRepository(db) { time }
        repository.initialize()
        repository.saveNote(idea())
        repository.judge("starter:retrieval", Judgment.SAVE, "persistent-action")
        db.close()
        db = Room.databaseBuilder(context, FolioDatabase::class.java, "restart-test.db").allowMainThreadQueries().build()
        repository = FolioRepository(db) { time }
        assertEquals(1, repository.snapshot().notes.size)
        assertEquals(1, repository.snapshot().saves.size)
        assertEquals(1, repository.snapshot().activeSwipes.size)
        assertEquals(2, repository.search("recall").size)
    }
}
