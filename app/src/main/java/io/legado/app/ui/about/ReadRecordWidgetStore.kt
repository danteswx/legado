package io.legado.app.ui.about

import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReadRecordShow
import io.legado.app.utils.GSON
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx

data class ReadRecentVisualSnapshot(
    val bookUrl: String,
    val name: String,
    val author: String = "",
    val coverUrl: String? = null,
    val customCoverUrl: String? = null,
    val lastRead: Long = System.currentTimeMillis()
) {
    fun displayCover(): String? = if (customCoverUrl.isNullOrBlank()) coverUrl else customCoverUrl
}

data class ReadRecentVisualItem(
    val snapshot: ReadRecentVisualSnapshot,
    val book: Book?
)

data class ReadRecordGoalConfig(
    val avatar: String? = null,
    val dailyGoalMinutes: Int = 120
)

data class ReadRecordRankItem(
    val book: Book?,
    val snapshot: ReadRecentVisualSnapshot?,
    val readTime: Long
)

object ReadRecordWidgetStore {

    private const val MAX_SNAPSHOTS = 24

    fun updateRecentSnapshot(book: Book, lastRead: Long) {
        val current = loadRecentSnapshots().toMutableList()
        current.removeAll { it.bookUrl == book.bookUrl }
        current.add(
            0,
            ReadRecentVisualSnapshot(
                bookUrl = book.bookUrl,
                name = book.name,
                author = book.author,
                coverUrl = book.coverUrl,
                customCoverUrl = book.customCoverUrl,
                lastRead = lastRead
            )
        )
        saveRecentSnapshots(current.take(MAX_SNAPSHOTS))
    }

    fun loadRecentSnapshots(): List<ReadRecentVisualSnapshot> {
        val raw = appCtx.getPrefString(PreferKey.readRecordRecentSnapshots).orEmpty()
        if (raw.isBlank()) return emptyList()
        return GSON.fromJsonArray<ReadRecentVisualSnapshot>(raw).getOrDefault(emptyList())
    }

    private fun saveRecentSnapshots(items: List<ReadRecentVisualSnapshot>) {
        appCtx.putPrefString(PreferKey.readRecordRecentSnapshots, GSON.toJson(items))
    }

    fun loadRecentVisualItems(limit: Int): List<ReadRecentVisualItem> {
        val booksByUrl = appDb.bookDao.all.associateBy { it.bookUrl }
        return loadRecentSnapshots()
            .sortedByDescending { it.lastRead }
            .take(limit)
            .map { ReadRecentVisualItem(it, booksByUrl[it.bookUrl]) }
    }

    fun loadGoalConfig(): ReadRecordGoalConfig {
        val raw = appCtx.getPrefString(PreferKey.readRecordGoalConfig).orEmpty()
        if (raw.isBlank()) return ReadRecordGoalConfig()
        return GSON.fromJsonObject<ReadRecordGoalConfig>(raw).getOrDefault(ReadRecordGoalConfig())
    }

    fun saveGoalConfig(config: ReadRecordGoalConfig) {
        appCtx.putPrefString(PreferKey.readRecordGoalConfig, GSON.toJson(config))
    }

    fun buildRankItems(limit: Int? = null): List<ReadRecordRankItem> {
        val readRecords = appDb.readRecordDao.allShow.sortedByDescending { it.readTime }
        val booksByName = appDb.bookDao.all.groupBy { it.name }.mapValues { entry ->
            entry.value.maxByOrNull { it.durChapterTime }
        }
        val snapshotsByName = loadRecentSnapshots()
            .sortedByDescending { it.lastRead }
            .associateBy { it.name }
        val result = readRecords.map { record: ReadRecordShow ->
            ReadRecordRankItem(
                book = booksByName[record.bookName],
                snapshot = snapshotsByName[record.bookName],
                readTime = record.readTime
            )
        }
        return if (limit != null) result.take(limit) else result
    }
}
