package io.legado.app.ui.main.explore

import android.app.Application
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.DiscoverSourceUseConfig
import io.legado.app.help.source.SourceHelp
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    var sourcePart: BookSourcePart? = null
    var source: BookSource? = null
    var currentUrl: String? = null
    var page = 1
    var hasMore = true
    var majorGroup: String? = null
    var tagIndex = -1
    var urlIndex = -1
    var modeLoaded = false
    var scrollPosition = RecyclerView.NO_POSITION
    var scrollOffset = 0
    val sources = mutableListOf<BookSourcePart>()
    val allTagItems = mutableListOf<DiscoverTagItem>()
    val tagItems = mutableListOf<DiscoverTagItem>()
    val selectItems = mutableListOf<DiscoverTagItem>()
    val settingItems = mutableListOf<DiscoverTagItem>()
    val majorGroups = mutableListOf<String>()
    val bookshelf = linkedSetOf<String>()
    val books = linkedSetOf<SearchBook>()

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

    suspend fun sortDiscoverSources(list: List<BookSourcePart>): List<BookSourcePart> {
        if (list.size <= 1) {
            return list
        }
        return withContext(IO) {
            runCatching {
                val shelfStats = appDb.bookDao.getBookSourceShelfStats()
                val useStats = DiscoverSourceUseConfig.getUseStats(list.map { it.bookSourceUrl })
                DiscoverSourceSorter.sort(list, shelfStats, useStats.values, System.currentTimeMillis())
            }.getOrElse {
                list
            }
        }
    }

    fun recordDiscoverSourceUse(sourceUrl: String?, increment: Int = 1) {
        execute {
            DiscoverSourceUseConfig.addUse(sourceUrl, increment)
        }
    }

}
