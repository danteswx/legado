package io.legado.app.model

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadMangaChapterStateTest {

    @After
    fun tearDown() {
        ReadManga.book = null
    }

    @Test
    fun `chapter is accepted only when it belongs to current book`() {
        ReadManga.book = Book(bookUrl = "current-url")

        assertTrue(ReadManga.isCurrentBookChapter(BookChapter(bookUrl = "current-url")))
        assertFalse(ReadManga.isCurrentBookChapter(BookChapter(bookUrl = "old-url")))
    }
}
