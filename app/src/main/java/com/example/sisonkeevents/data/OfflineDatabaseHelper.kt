package com.example.sisonkeevents.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sisonkeevents.event

class OfflineDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "sisonke_offline.db"
        private const val DB_VERSION = 3 // Bumped to 3 for Auth Provider

        @Volatile private var INSTANCE: OfflineDatabaseHelper? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = OfflineDatabaseHelper(context.applicationContext)
                    }
                }
            }
        }

        fun get(): OfflineDatabaseHelper =
            INSTANCE ?: error("OfflineDatabaseHelper not initialized. Call init() in SisonkeApp.")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE events (
                id              TEXT PRIMARY KEY NOT NULL,
                name            TEXT NOT NULL,
                date            TEXT,
                location        TEXT,
                imageUrl        TEXT,
                interestedCount INTEGER NOT NULL DEFAULT 0,
                time            TEXT,
                description     TEXT,
                category        TEXT,
                ticketUrl       TEXT,
                age             TEXT,
                parking         TEXT,
                lastAccessedAt  INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE category_cache (
                categoryId  TEXT PRIMARY KEY NOT NULL,
                lastUpdated INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE category_event_ref (
                categoryId TEXT NOT NULL,
                eventId    TEXT NOT NULL,
                PRIMARY KEY (categoryId, eventId),
                FOREIGN KEY (eventId) REFERENCES events(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_ref_event ON category_event_ref(eventId)")
        db.execSQL(
            """
            CREATE TABLE pending_actions (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                actionType TEXT NOT NULL,
                eventId    TEXT NOT NULL,
                payload    TEXT,
                createdAt  INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_pending_created ON pending_actions(createdAt)")

        db.execSQL(
            """
            CREATE TABLE user_account (
                id           INTEGER PRIMARY KEY CHECK (id = 1),
                firstName    TEXT,
                lastName     TEXT,
                email        TEXT,
                phone        TEXT,
                password     TEXT, 
                authProvider TEXT DEFAULT 'LOCAL'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE session (
                id               INTEGER PRIMARY KEY CHECK (id = 1),
                loggedIn         INTEGER NOT NULL DEFAULT 0,
                lastPage         TEXT,
                lastCategoryName TEXT,
                lastCategoryId   TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE interested_events (
                eventId TEXT PRIMARY KEY NOT NULL,
                savedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE recently_viewed (
                eventId  TEXT PRIMARY KEY NOT NULL,
                viewedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pending_actions")
        db.execSQL("DROP TABLE IF EXISTS category_event_ref")
        db.execSQL("DROP TABLE IF EXISTS category_cache")
        db.execSQL("DROP TABLE IF EXISTS events")
        db.execSQL("DROP TABLE IF EXISTS interested_events")
        db.execSQL("DROP TABLE IF EXISTS recently_viewed")
        db.execSQL("DROP TABLE IF EXISTS user_account")
        db.execSQL("DROP TABLE IF EXISTS session")
        onCreate(db)
    }

    fun upsertEvents(events: List<event>) {
        if (events.isEmpty()) return
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val stmt = db.compileStatement(
            """
            INSERT OR REPLACE INTO events
                (id, name, date, location, imageUrl, interestedCount,
                 time, description, category, ticketUrl, age, parking, lastAccessedAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        )
        db.beginTransaction()
        try {
            for (e in events) {
                stmt.clearBindings()
                stmt.bindString(1, e.id)
                stmt.bindString(2, e.name)
                stmt.bindString(3, e.date)
                stmt.bindString(4, e.location)
                if (e.imageUrl == null) stmt.bindNull(5) else stmt.bindString(5, e.imageUrl)
                stmt.bindLong(6, e.interestedCount.toLong())
                stmt.bindString(7, e.time)
                stmt.bindString(8, e.description)
                stmt.bindString(9, e.category)
                stmt.bindString(10, e.ticketUrl)
                stmt.bindString(11, e.age)
                stmt.bindString(12, e.parking)
                stmt.bindLong(13, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun linkEventsToCategory(categoryId: String, eventIds: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("category_event_ref", "categoryId = ?", arrayOf(categoryId))
            val stmt = db.compileStatement(
                "INSERT INTO category_event_ref (categoryId, eventId) VALUES (?, ?)"
            )
            for (id in eventIds) {
                stmt.clearBindings()
                stmt.bindString(1, categoryId)
                stmt.bindString(2, id)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadEventsForCategory(categoryId: String): List<event> {
        val sql = """
            SELECT e.id, e.name, e.date, e.location, e.imageUrl, e.interestedCount,
                   e.time, e.description, e.category, e.ticketUrl, e.age, e.parking
            FROM events e
            INNER JOIN category_event_ref r ON r.eventId = e.id
            WHERE r.categoryId = ?
        """.trimIndent()
        return readableDatabase.rawQuery(sql, arrayOf(categoryId)).use { c ->
            buildList { while (c.moveToNext()) add(c.readEvent()) }
        }
    }

    fun evictEventsOlderThan(maxAgeDays: Int) {
        val cutoff = System.currentTimeMillis() - maxAgeDays * 24L * 60L * 60L * 1000L
        writableDatabase.execSQL(
            """
            DELETE FROM events
            WHERE lastAccessedAt < ?
              AND id NOT IN (SELECT eventId FROM interested_events)
              AND id NOT IN (SELECT eventId FROM recently_viewed)
              AND id NOT IN (SELECT eventId FROM pending_actions)
            """.trimIndent(),
            arrayOf<Any>(cutoff)
        )
    }

    fun setCategoryLastUpdated(categoryId: String, timestamp: Long) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO category_cache (categoryId, lastUpdated) VALUES (?, ?)",
            arrayOf<Any>(categoryId, timestamp)
        )
    }

    fun getCategoryLastUpdated(categoryId: String): Long? =
        readableDatabase
            .rawQuery("SELECT lastUpdated FROM category_cache WHERE categoryId = ?", arrayOf(categoryId))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    fun enqueueAction(actionType: String, eventId: String, payload: String? = null) {
        writableDatabase.execSQL(
            "INSERT INTO pending_actions (actionType, eventId, payload, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(actionType, eventId, payload, System.currentTimeMillis())
        )
    }

    fun getPendingActions(): List<PendingAction> {
        val sql = "SELECT id, actionType, eventId, payload, createdAt FROM pending_actions ORDER BY createdAt ASC"
        return readableDatabase.rawQuery(sql, null).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        PendingAction(
                            id = c.getLong(0),
                            actionType = c.getString(1),
                            eventId = c.getString(2),
                            payload = if (c.isNull(3)) null else c.getString(3),
                            createdAt = c.getLong(4)
                        )
                    )
                }
            }
        }
    }

    fun deletePendingAction(id: Long) {
        writableDatabase.delete("pending_actions", "id = ?", arrayOf(id.toString()))
    }

    fun saveInterestedEvent(e: event) {
        upsertEvents(listOf(e))
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO interested_events (eventId, savedAt) VALUES (?, ?)",
            arrayOf<Any>(e.id, System.currentTimeMillis())
        )
    }

    fun loadInterestedEvents(): List<event> {
        val sql = """
            SELECT e.id, e.name, e.date, e.location, e.imageUrl, e.interestedCount,
                   e.time, e.description, e.category, e.ticketUrl, e.age, e.parking
            FROM events e
            INNER JOIN interested_events i ON i.eventId = e.id
            ORDER BY i.savedAt DESC
        """.trimIndent()
        return readableDatabase.rawQuery(sql, null).use { c ->
            buildList { while (c.moveToNext()) add(c.readEvent()) }
        }
    }

    fun saveViewedEvent(e: event) {
        upsertEvents(listOf(e))
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO recently_viewed (eventId, viewedAt) VALUES (?, ?)",
            arrayOf<Any>(e.id, System.currentTimeMillis())
        )
        writableDatabase.execSQL(
            """
            DELETE FROM recently_viewed
            WHERE eventId NOT IN (
                SELECT eventId FROM recently_viewed ORDER BY viewedAt DESC LIMIT 50
            )
            """.trimIndent()
        )
    }

    fun loadRecentlyViewedEvents(): List<event> {
        val sql = """
            SELECT e.id, e.name, e.date, e.location, e.imageUrl, e.interestedCount,
                   e.time, e.description, e.category, e.ticketUrl, e.age, e.parking
            FROM events e
            INNER JOIN recently_viewed r ON r.eventId = e.id
            ORDER BY r.viewedAt DESC
        """.trimIndent()
        return readableDatabase.rawQuery(sql, null).use { c ->
            buildList { while (c.moveToNext()) add(c.readEvent()) }
        }
    }

    fun loadUserAccount(): UserAccountRow? {
        readableDatabase.rawQuery(
            "SELECT firstName, lastName, email, phone, password, authProvider FROM user_account WHERE id = 1",
            null
        ).use { c ->
            if (!c.moveToFirst()) return null
            return UserAccountRow(
                firstName = if (c.isNull(0)) null else c.getString(0),
                lastName = if (c.isNull(1)) null else c.getString(1),
                email = if (c.isNull(2)) null else c.getString(2),
                phone = if (c.isNull(3)) null else c.getString(3),
                password = if (c.isNull(4)) null else c.getString(4),
                authProvider = if (c.isNull(5)) "LOCAL" else c.getString(5)
            )
        }
    }

    fun saveUserAccount(row: UserAccountRow) {
        val cv = ContentValues().apply {
            put("id", 1)
            if (row.firstName == null) putNull("firstName") else put("firstName", row.firstName)
            if (row.lastName == null) putNull("lastName") else put("lastName", row.lastName)
            if (row.email == null) putNull("email") else put("email", row.email)
            if (row.phone == null) putNull("phone") else put("phone", row.phone)
            if (row.password == null) putNull("password") else put("password", row.password)
            put("authProvider", row.authProvider)
        }
        writableDatabase.insertWithOnConflict(
            "user_account", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun isLoggedIn(): Boolean =
        readableDatabase.rawQuery("SELECT loggedIn FROM session WHERE id = 1", null).use {
            it.moveToFirst() && it.getInt(0) == 1
        }

    fun setLoggedIn(loggedIn: Boolean) {
        val current = loadSessionState()
        saveSessionState(
            loggedIn = loggedIn,
            page = if (loggedIn) (current?.page ?: "home") else null,
            categoryName = if (loggedIn) current?.categoryName else null,
            categoryId = if (loggedIn) current?.categoryId else null
        )
    }

    fun saveSessionState(
        loggedIn: Boolean = true,
        page: String?,
        categoryName: String?,
        categoryId: String?
    ) {
        val cv = ContentValues().apply {
            put("id", 1)
            put("loggedIn", if (loggedIn) 1 else 0)
            if (page == null) putNull("lastPage") else put("lastPage", page)
            if (categoryName == null) putNull("lastCategoryName") else put("lastCategoryName", categoryName)
            if (categoryId == null) putNull("lastCategoryId") else put("lastCategoryId", categoryId)
        }
        writableDatabase.insertWithOnConflict(
            "session", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun loadSessionState(): SessionState? {
        readableDatabase.rawQuery(
            "SELECT loggedIn, lastPage, lastCategoryName, lastCategoryId FROM session WHERE id = 1",
            null
        ).use { c ->
            if (!c.moveToFirst()) return null
            return SessionState(
                loggedIn = c.getInt(0) == 1,
                page = if (c.isNull(1)) null else c.getString(1),
                categoryName = if (c.isNull(2)) null else c.getString(2),
                categoryId = if (c.isNull(3)) null else c.getString(3)
            )
        }
    }

    private fun Cursor.readEvent() = event(
        id = getString(0),
        name = getString(1),
        date = if (isNull(2)) "" else getString(2),
        location = if (isNull(3)) "" else getString(3),
        imageUrl = if (isNull(4)) null else getString(4),
        interestedCount = if (isNull(5)) (50..500).random() else getInt(5),
        time = if (isNull(6)) "19:00" else getString(6),
        description = if (isNull(7)) "No description available." else getString(7),
        category = if (isNull(8)) "General" else getString(8),
        ticketUrl = if (isNull(9)) "" else getString(9),
        age = if (isNull(10)) "All Ages" else getString(10),
        parking = if (isNull(11)) "Available" else getString(11)
    )
}

data class PendingAction(
    val id: Long,
    val actionType: String,
    val eventId: String,
    val payload: String?,
    val createdAt: Long
)

data class UserAccountRow(
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val password: String?,
    val authProvider: String = "LOCAL"
)

data class SessionState(
    val loggedIn: Boolean,
    val page: String?,
    val categoryName: String?,
    val categoryId: String?
)