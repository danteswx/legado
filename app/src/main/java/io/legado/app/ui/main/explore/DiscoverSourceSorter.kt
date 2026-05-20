package io.legado.app.ui.main.explore

import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.BookSourceShelfStats
import io.legado.app.help.config.DiscoverSourceUseStats
import java.util.concurrent.TimeUnit

internal object DiscoverSourceSorter {

    fun sort(
        sources: List<BookSourcePart>,
        shelfStats: Collection<BookSourceShelfStats>,
        useStats: Collection<DiscoverSourceUseStats>,
        now: Long
    ): List<BookSourcePart> {
        val shelfStatsByUrl = shelfStats.associateBy { it.origin }
        val useStatsByUrl = useStats.associateBy { it.bookSourceUrl }

        return sources.sortedWith(
            compareByDescending<BookSourcePart> { source ->
                val sourceUrl = source.bookSourceUrl.trim()
                score(source, shelfStatsByUrl[sourceUrl], useStatsByUrl[sourceUrl], now)
            }
                .thenBy { it.customOrder }
                .thenBy { it.bookSourceName }
        )
    }

    private fun score(
        source: BookSourcePart,
        shelfStats: BookSourceShelfStats?,
        useStats: DiscoverSourceUseStats?,
        now: Long
    ): Int {
        val shelfScore = (shelfStats?.shelfCount ?: 0).coerceAtMost(20) * 100
        val readRecencyScore = recencyScore(shelfStats?.lastReadTime ?: 0, now)
        val useScore = (useStats?.useCount ?: 0).coerceAtMost(50) * 20
        val useRecencyScore = recencyScore(useStats?.lastUseTime ?: 0, now)
        val weightScore = if (hasBehaviorSignal(shelfStats, useStats, now)) {
            source.weight.coerceIn(-20, 20)
        } else {
            0
        }

        return shelfScore + readRecencyScore + useScore + useRecencyScore + weightScore
    }

    private fun hasBehaviorSignal(
        shelfStats: BookSourceShelfStats?,
        useStats: DiscoverSourceUseStats?,
        now: Long
    ): Boolean {
        return (shelfStats?.shelfCount ?: 0) > 0 ||
            isValidTime(shelfStats?.lastReadTime ?: 0, now) ||
            (useStats?.useCount ?: 0) > 0 ||
            isValidTime(useStats?.lastUseTime ?: 0, now)
    }

    private fun recencyScore(time: Long, now: Long): Int {
        if (!isValidTime(time, now)) {
            return 0
        }

        val ageDays = TimeUnit.MILLISECONDS.toDays(now - time)
        return when {
            ageDays <= 7 -> 80
            ageDays <= 30 -> 40
            ageDays <= 90 -> 15
            else -> 0
        }
    }

    private fun isValidTime(time: Long, now: Long): Boolean {
        return time > 0 && time <= now
    }
}
