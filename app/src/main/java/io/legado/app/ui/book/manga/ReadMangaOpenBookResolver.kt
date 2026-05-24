package io.legado.app.ui.book.manga

import io.legado.app.data.entities.Book

internal object ReadMangaOpenBookResolver {

    fun resolve(
        requestedBookUrl: String?,
        findLastReadBook: () -> Book?,
        findBookByUrl: (String) -> Book?,
        currentBook: () -> Book?
    ): Book? {
        return if (requestedBookUrl.isNullOrEmpty()) {
            findLastReadBook() ?: currentBook()
        } else {
            findBookByUrl(requestedBookUrl)
        }
    }
}
