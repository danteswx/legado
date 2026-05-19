package io.legado.app.ui.book

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SearchBookOpenHelperTest {

    @Test
    fun openingSearchBookCarriesCoverUrlIntoBookInfoIntent() {
        val openHelper = repoFile(
            "app/src/main/java/io/legado/app/ui/book/SearchBookOpenHelper.kt"
        ).readText()
        val viewModel = repoFile(
            "app/src/main/java/io/legado/app/ui/book/info/BookInfoViewModel.kt"
        ).readText()

        assertTrue(openHelper.contains("""putExtra("coverUrl", book.coverUrl)"""))
        assertTrue(viewModel.contains("""val coverUrl = intent.getStringExtra("coverUrl")"""))
        assertTrue(viewModel.contains("book.coverUrl = coverUrl"))
        assertTrue(viewModel.contains("coverUrl = coverUrl"))
    }

    private fun repoFile(relativePath: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.exists() }
            ?: File(relativePath)
    }
}
