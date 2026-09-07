package com.polymath.data

import androidx.room.*
import com.polymath.model.*
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter fun images(value: List<SourceImage>): String = JSONArray().apply {
        value.forEach { put(JSONObject().put("url", it.url).put("alt", it.alt).put("caption", it.caption)) }
    }.toString()
    @TypeConverter fun imageList(value: String): List<SourceImage> = JSONArray(value).let { a ->
        List(a.length()) { i -> a.getJSONObject(i).let { SourceImage(it.getString("url"), it.optString("alt"), it.optString("caption")) } }
    }
    @TypeConverter fun strings(value: List<String>): String = JSONArray(value).toString()
    @TypeConverter fun stringList(value: String): List<String> = JSONArray(value).let { a -> List(a.length()) { a.getString(it) } }
}

@Entity(tableName = "contents")
data class ContentEntity(
    @PrimaryKey val id: String,
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
    @ColumnInfo(defaultValue = "'[]'") val images: List<SourceImage> = emptyList(),
    @ColumnInfo(defaultValue = "'public'") val datasetId: String = "public",
) {
    fun card() = Card(id, kind, title, summary, body, topicId, publisher, sourceUrl, publishedAt, fetchedAt,
        question, answers, correctAnswer, explanation, edition, images, datasetId)
}

@Entity(tableName = "preferences")
data class PreferenceEntity(@PrimaryKey val topicId: String, val followed: Boolean, val muted: Boolean = false) {
    fun preference() = Preference(topicId, followed, muted)
}

@Entity(tableName = "swipes", indices = [Index(value = ["actionId"], unique = true), Index("contentId"), Index("reversalOf")])
data class SwipeEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    val actionId: String,
    val contentId: String,
    val action: String,
    val features: String,
    val createdAt: Long,
    val reversalOf: String? = null,
    val modelVersion: Int = Recommender.VERSION,
)

@Entity(tableName = "saves", foreignKeys = [ForeignKey(ContentEntity::class, ["id"], ["contentId"], onDelete = ForeignKey.CASCADE)])
data class SaveEntity(@PrimaryKey val contentId: String, val savedAt: Long, val originAction: String?)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val mode: NoteMode,
    val title: String,
    val body: String,
    val goal: String = "",
    val context: String = "",
    val topicId: String = "systems",
    val sourceContentId: String? = null,
    val revision: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "note_revisions", primaryKeys = ["noteId", "revision"], foreignKeys = [
    ForeignKey(NoteEntity::class, ["id"], ["noteId"], onDelete = ForeignKey.CASCADE)])
data class NoteRevisionEntity(val noteId: String, val revision: Int, val mode: NoteMode, val title: String,
    val body: String, val goal: String, val context: String, val createdAt: Long)

@Entity(tableName = "plans", indices = [Index("noteId")], foreignKeys = [
    ForeignKey(NoteEntity::class, ["id"], ["noteId"], onDelete = ForeignKey.CASCADE)])
data class PlanEntity(@PrimaryKey val id: String, val noteId: String, val sourceRevision: Int,
    val status: String = "DRAFT", val createdAt: Long)

@Entity(tableName = "tasks", indices = [Index("planId")], foreignKeys = [
    ForeignKey(PlanEntity::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)])
data class TaskEntity(@PrimaryKey val id: String, val planId: String, val position: Int,
    val title: String, val acceptance: String, val dependencyId: String?, val done: Boolean = false,
    val reflection: String = "", val completedAt: Long? = null)

@Entity(tableName = "attempts", indices = [Index(value = ["episodeKey"], unique = true)])
data class AttemptEntity(@PrimaryKey val id: String, val contentId: String, val episodeKey: String,
    val answer: Int, val correct: Boolean, val awardedExp: Int, val attemptedAt: Long)

@Entity(tableName = "reviews")
data class ReviewEntity(@PrimaryKey val contentId: String, val edition: Int, val episode: Int,
    val successfulReviews: Int, val hasFirstCredit: Boolean, val dueAt: Long)

@Entity(tableName = "exp_awards", indices = [Index(value = ["earningKey"], unique = true)])
data class ExpAwardEntity(@PrimaryKey val id: String, val earningKey: String, val topicId: String,
    val amount: Int, val reason: String, val createdAt: Long, val reversalOf: String? = null,
    val ruleVersion: Int = 1)

@Entity(tableName = "search_documents", indices = [Index(value = ["resourceKey"], unique = true)])
data class SearchDocument(@PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val rowId: Long = 0,
    val resourceKey: String, val kind: String, val resourceId: String, val title: String,
    val body: String, val topicId: String, val updatedAt: Long)

@Fts4(contentEntity = SearchDocument::class)
@Entity(tableName = "search_fts")
data class SearchFts(val title: String, val body: String)

@Entity(tableName = "datasets")
data class DatasetEntity(@PrimaryKey val id: String, val title: String, val importedAt: Long)

@Entity(tableName = "chat_messages", indices = [Index("scope")])
data class ChatMessageEntity(@PrimaryKey val id: String, val scope: String, val role: String,
    val text: String, val citations: String = "[]", val status: String = "answered", val createdAt: Long)

@Dao
interface FolioDao {
    @Query("SELECT * FROM datasets ORDER BY title") suspend fun datasets(): List<DatasetEntity>
    @Upsert suspend fun dataset(value: DatasetEntity)
    @Query("DELETE FROM datasets WHERE id = :id") suspend fun deleteDataset(id: String)
    @Query("DELETE FROM contents WHERE datasetId = :id") suspend fun deleteDatasetContents(id: String)
    @Query("SELECT * FROM chat_messages ORDER BY createdAt, id") suspend fun messages(): List<ChatMessageEntity>
    @Insert suspend fun message(value: ChatMessageEntity)
    @Query("DELETE FROM chat_messages") suspend fun clearMessages()
    @Query("DELETE FROM chat_messages WHERE scope = :scope") suspend fun clearMessages(scope: String)

    @Query("SELECT * FROM contents") suspend fun contents(): List<ContentEntity>
    @Query("SELECT * FROM contents WHERE id = :id") suspend fun content(id: String): ContentEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertContent(content: List<ContentEntity>)
    @Query("UPDATE contents SET images = :images WHERE id = :id") suspend fun contentImages(id: String, images: List<SourceImage>)
    @Query("SELECT * FROM preferences") suspend fun preferences(): List<PreferenceEntity>
    @Upsert suspend fun preference(value: PreferenceEntity)
    @Query("SELECT * FROM swipes ORDER BY sequence") suspend fun swipes(): List<SwipeEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun swipe(value: SwipeEntity): Long
    @Query("SELECT * FROM saves ORDER BY savedAt DESC") suspend fun saves(): List<SaveEntity>
    @Query("SELECT * FROM saves WHERE contentId = :id") suspend fun save(id: String): SaveEntity?
    @Upsert suspend fun save(value: SaveEntity)
    @Query("DELETE FROM saves WHERE contentId = :id") suspend fun deleteSave(id: String)
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC") suspend fun notes(): List<NoteEntity>
    @Query("SELECT * FROM notes WHERE id = :id") suspend fun note(id: String): NoteEntity?
    @Upsert suspend fun note(value: NoteEntity)
    @Insert suspend fun revision(value: NoteRevisionEntity)
    @Query("SELECT * FROM note_revisions WHERE noteId = :id ORDER BY revision") suspend fun revisions(id: String): List<NoteRevisionEntity>
    @Query("DELETE FROM notes WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("SELECT * FROM plans ORDER BY createdAt") suspend fun plans(): List<PlanEntity>
    @Query("SELECT * FROM plans WHERE id = :id") suspend fun plan(id: String): PlanEntity?
    @Upsert suspend fun plan(value: PlanEntity)
    @Query("DELETE FROM plans WHERE id = :id") suspend fun deletePlan(id: String)
    @Query("SELECT * FROM tasks ORDER BY position") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun task(id: String): TaskEntity?
    @Upsert suspend fun tasks(values: List<TaskEntity>)
    @Query("SELECT * FROM attempts ORDER BY attemptedAt") suspend fun attempts(): List<AttemptEntity>
    @Query("SELECT * FROM attempts WHERE episodeKey = :key") suspend fun attempt(key: String): AttemptEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun attempt(value: AttemptEntity): Long
    @Query("SELECT * FROM reviews") suspend fun reviews(): List<ReviewEntity>
    @Query("SELECT * FROM reviews WHERE contentId = :id") suspend fun review(id: String): ReviewEntity?
    @Upsert suspend fun review(value: ReviewEntity)
    @Query("SELECT * FROM exp_awards ORDER BY createdAt") suspend fun awards(): List<ExpAwardEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun award(value: ExpAwardEntity): Long
    @Query("SELECT * FROM search_documents WHERE resourceKey = :key") suspend fun searchDocument(key: String): SearchDocument?
    @Upsert suspend fun searchDocument(value: SearchDocument)
    @Query("DELETE FROM search_documents WHERE resourceKey = :key") suspend fun deleteDocument(key: String)
    @Query("SELECT search_documents.* FROM search_documents JOIN search_fts ON search_documents.rowid = search_fts.rowid WHERE search_fts MATCH :query ORDER BY updatedAt DESC LIMIT 40")
    suspend fun search(query: String): List<SearchDocument>
    @Query("SELECT * FROM search_documents ORDER BY updatedAt DESC LIMIT 40") suspend fun recentDocuments(): List<SearchDocument>
}

@Database(entities = [ContentEntity::class, PreferenceEntity::class, SwipeEntity::class, SaveEntity::class,
    NoteEntity::class, NoteRevisionEntity::class, PlanEntity::class, TaskEntity::class, AttemptEntity::class,
    ReviewEntity::class, ExpAwardEntity::class, SearchDocument::class, SearchFts::class, DatasetEntity::class, ChatMessageEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class FolioDatabase : RoomDatabase() { abstract fun folio(): FolioDao }

/** Additive upgrade: original notes, swipes, saves and EXP remain intact. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contents ADD COLUMN images TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE contents ADD COLUMN datasetId TEXT NOT NULL DEFAULT 'public'")
        db.execSQL("CREATE TABLE IF NOT EXISTS datasets (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, importedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id TEXT NOT NULL PRIMARY KEY, scope TEXT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, citations TEXT NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_scope ON chat_messages (scope)")
    }
}
