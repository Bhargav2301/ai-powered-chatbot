package com.polymath.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @Test fun `v1 on disk upgrades with original notes saves EXP and swipe history intact`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath("migration-v1.db")
        context.deleteDatabase(path.name); path.parentFile?.mkdirs()
        val schema = javaClass.classLoader!!.getResourceAsStream("com.polymath.data.FolioDatabase/1.json")!!.bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val name = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", name))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name))
            }
            val triggers = entities.getJSONObject(entities.length() - 1).getJSONArray("contentSyncTriggers")
            for (i in 0 until triggers.length()) old.execSQL(triggers.getString(i))
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO contents VALUES ('old','KNOWLEDGE_PILL','Old source','Summary','Body','systems','Publisher','https://example.org',NULL,1,NULL,'[]',-1,'',1)")
            old.execSQL("INSERT INTO notes VALUES ('note','THOUGHT','A thought','Keep this body','','','systems','old',1,1,1)")
            old.execSQL("INSERT INTO saves VALUES ('old',1,NULL)")
            old.execSQL("INSERT INTO swipes VALUES (1,'swipe','old','SAVE','1,0,0,0,0,0,-1,1',1,NULL,1)")
            old.execSQL("INSERT INTO exp_awards VALUES ('exp','recall:old','systems',10,'RECALL',1,NULL,1)")
            old.version = 1
        }
        val db = Room.databaseBuilder(context, FolioDatabase::class.java, path.name).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val snapshot = FolioRepository(db).snapshot() // Opening triggers Room schema validation.
            assertEquals("Keep this body", snapshot.notes.single().body)
            assertEquals("old", snapshot.saves.single().contentId)
            assertEquals(10, snapshot.totalExp)
            assertEquals(1, snapshot.swipes.single().modelVersion)
            assertTrue(snapshot.cards.single().images.isEmpty())
            assertEquals("public", snapshot.cards.single().datasetId)
            assertTrue(snapshot.datasets.isEmpty())
        } finally { db.close(); context.deleteDatabase(path.name) }
    }
}
