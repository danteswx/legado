package io.legado.app.ui.main.explore

import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.BookSourceShelfStats
import io.legado.app.help.config.DiscoverSourceUseStats
import io.legado.app.help.source.BookSourcePrioritySorter

internal object DiscoverSourceSorter {

    fun sort(
        sources: List<BookSourcePart>,
        shelfStats: Collection<BookSourceShelfStats>,
        useStats: Collection<DiscoverSourceUseStats>,
        now: Long
    ): List<BookSourcePart> {
        return BookSourcePrioritySorter.sort(sources, shelfStats, useStats, now)
    }
}
