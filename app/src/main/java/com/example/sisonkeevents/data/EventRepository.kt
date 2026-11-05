package com.example.sisonkeevents.data


import android.content.Context
import android.util.Log
import com.example.sisonkeevents.event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EventsResult(
    val events: List<event>,
    val lastUpdated: Long?,
    val fromCache: Boolean
)

class EventRepository(
    context: Context,
    private val api: EventApi
) {
    private val appContext = context.applicationContext
    private val db = OfflineDatabaseHelper(appContext)
    private val network = NetworkMonitor(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "EventRepository"
        private const val CACHE_STALE_MS = 15 * 60 * 1000L
        private const val CACHE_MAX_AGE_DAYS = 30
    }

    init {
        scope.launch {
            network.observe().collect { online ->
                if (online) {
                    runCatching { syncPendingActions() }
                        .onFailure { Log.w(TAG, "Auto-sync failed", it) }
                }
            }
        }
    }

    suspend fun getEventsForCategory(
        categoryId: String,
        forceRefresh: Boolean = false
    ): EventsResult = withContext(Dispatchers.IO) {
        fetchOrCache(
            cacheKey = categoryId,
            forceRefresh = forceRefresh,
            remote = { api.fetchEventsForCategory(categoryId) }
        )
    }

    suspend fun getWeekendEvents(
        countryCode: String = "ZA",
        forceRefresh: Boolean = false
    ): EventsResult = withContext(Dispatchers.IO) {
        fetchOrCache(
            cacheKey = "weekend_$countryCode",
            forceRefresh = forceRefresh,
            remote = { api.fetchWeekendEvents(countryCode) }
        )
    }

    private suspend fun fetchOrCache(
        cacheKey: String,
        forceRefresh: Boolean,
        remote: suspend () -> List<event>
    ): EventsResult {
        val online = network.isOnline()
        val lastUpdated = db.getCategoryLastUpdated(cacheKey)
        val stale = lastUpdated == null ||
                (System.currentTimeMillis() - lastUpdated) > CACHE_STALE_MS

        if (online && (forceRefresh || stale)) {
            try {
                val remoteEvents = remote()
                db.upsertEvents(remoteEvents)
                db.linkEventsToCategory(cacheKey, remoteEvents.map { it.id })
                db.setCategoryLastUpdated(cacheKey, System.currentTimeMillis())
                return EventsResult(
                    events = remoteEvents,
                    lastUpdated = System.currentTimeMillis(),
                    fromCache = false
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Remote fetch failed, falling back to cache", t)
            }
        }

        val cached = db.loadEventsForCategory(cacheKey)
        return EventsResult(cached, lastUpdated, fromCache = true)
    }

    /**
     * Returns true if the action was queued (device offline or API failed).
     * Returns false if it was sent immediately (online, API ok).
     * Callers should still persist locally regardless — this only tracks
     * the backend-sync queue state.
     */
    suspend fun markInterested(eventId: String): Boolean = withContext(Dispatchers.IO) {
        if (network.isOnline()) {
            try {
                api.markInterested(eventId)
                return@withContext false
            } catch (t: Throwable) {
                Log.w(TAG, "markInterested online failed, queuing", t)
            }
        }
        db.enqueueAction(actionType = "INTERESTED", eventId = eventId)
        true
    }

    suspend fun syncPendingActions() = withContext(Dispatchers.IO) {
        if (!network.isOnline()) return@withContext
        val actions = db.getPendingActions()
        if (actions.isEmpty()) return@withContext
        Log.d(TAG, "Syncing ${actions.size} pending action(s)")
        for (action in actions) {
            try {
                when (action.actionType) {
                    "INTERESTED" -> api.markInterested(action.eventId)
                    else -> Unit
                }
                db.deletePendingAction(action.id)
            } catch (t: Throwable) {
                Log.w(TAG, "Sync stopped at action ${action.id}", t)
                break
            }
        }
    }

    suspend fun evictStaleCache() = withContext(Dispatchers.IO) {
        db.evictEventsOlderThan(CACHE_MAX_AGE_DAYS)
    }

    fun evictStaleCacheAsync() {
        scope.launch { runCatching { evictStaleCache() } }
    }

    fun isOnline(): Boolean = network.isOnline()
}