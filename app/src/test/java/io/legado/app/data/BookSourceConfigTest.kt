package io.legado.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BookSourceConfigTest {

    @Test
    fun uaaSourceDetectsCloudflareAndOpensBrowserVerification() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val uaaSource = sourceObject(sourceText, """"bookSourceUrl": "https://www.uaa.com"""")
        val loginCheckJs = fieldValue(uaaSource, "loginCheckJs")

        assertTrue("UAA source should exist", uaaSource.isNotBlank())
        assertTrue(uaaSource.contains(""""concurrentRate": "1/3000""""))
        assertFalse("UAA loginCheckJs should not be blank", loginCheckJs.isBlank())
        assertTrue(loginCheckJs.contains("startBrowserAwait"))
        assertTrue(loginCheckJs.contains("source.getKey()"))
        assertTrue(loginCheckJs.contains("cf-mitigated"))
        assertTrue(loginCheckJs.contains("_cf_chl_opt"))
        assertTrue(loginCheckJs.contains("cf_chl"))
        assertTrue(loginCheckJs.contains("Just a moment"))
    }

    @Test
    fun uaaBookInfoCoverUsesLazyImageAndFallsBackToListCover() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val uaaSource = sourceObject(sourceText, """"bookSourceUrl": "https://www.uaa.com"""")
        val coverUrlRule = fieldValue(uaaSource, "coverUrl")

        assertTrue("UAA source should exist", uaaSource.isNotBlank())
        assertTrue(coverUrlRule.contains("data-src"))
        assertTrue(coverUrlRule.contains("data-original"))
        assertTrue(coverUrlRule.contains("book.coverUrl"))
        assertTrue(coverUrlRule.contains("data:image"))
        assertTrue(coverUrlRule.contains("placeholder"))
        assertTrue(coverUrlRule.contains("src ||"))
        assertTrue(coverUrlRule.contains("og:image"))
        assertTrue(coverUrlRule.contains("coverUrl"))
    }

    private fun repoFile(relativePath: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.exists() }
            ?: File(relativePath)
    }

    private fun sourceObject(sourceText: String, marker: String): String {
        val markerIndex = sourceText.indexOf(marker)
        if (markerIndex < 0) return ""

        val start = sourceText.lastIndexOf("\n  {", markerIndex)
        val end = sourceText.indexOf("\n  }", markerIndex)
        if (start < 0 || end < 0) return ""

        return sourceText.substring(start, end + "\n  }".length)
    }

    private fun fieldValue(sourceText: String, fieldName: String): String {
        return Regex(
            """"$fieldName"\s*:\s*"(.*?)",""",
            RegexOption.DOT_MATCHES_ALL
        ).find(sourceText)?.groupValues?.get(1).orEmpty()
    }
}
