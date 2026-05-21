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

    @Test
    fun javdbVideoSourceIncludesLoginAndDiscoverCategories() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val exploreUrl = fieldValue(javdbSource, "exploreUrl")
        val loginUrl = fieldValue(javdbSource, "loginUrl")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        assertTrue(loginUrl.contains("login"))
        listOf("censored", "uncensored", "western", "FC2", "anime").forEach {
            assertTrue("Missing JavDB primary category: $it", exploreUrl.contains(it))
        }
        listOf("vft=4&vst=3", "vft=5&vst=3", "vft=1", "vft=2", "vft=3").forEach {
            assertTrue("Missing JavDB secondary filter: $it", exploreUrl.contains(it))
        }
        listOf("LUXU", "VR", "jur", "snos", "mida", "start", "ipzz", "ntr", "abf").forEach {
            assertTrue("Missing JavDB tag: $it", exploreUrl.contains(it))
        }
    }

    @Test
    fun javdbVideoSourceUsesCurrentFilterRoutesAndExpandedTags() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val javdbSource = sourceObject(sourceText, """"bookSourceUrl": "https://javdb.com/"""")
        val exploreUrl = fieldValue(javdbSource, "exploreUrl")

        assertTrue("JavDB source should exist", javdbSource.isNotBlank())
        listOf("censored", "uncensored", "western", "fc2", "anime").forEach {
            assertTrue("Missing JavDB primary category route: $it", exploreUrl.contains(it))
        }
        listOf("vft=0", "vft=4&vst=3", "vft=5&vst=3", "vft=1", "vft=2", "vft=3").forEach {
            assertTrue("Missing JavDB current filter route: $it", exploreUrl.contains(it))
        }
        assertTrue(exploreUrl.contains("categories.filter"))
        assertTrue(exploreUrl.contains("filters.forEach"))
        listOf("rankings/playback", "rankings/top").forEach {
            assertTrue("Missing JavDB ranking category: $it", exploreUrl.contains(it))
        }
        listOf("p=daily&t=censored", "p=weekly&t=censored", "p=monthly&t=censored").forEach {
            assertTrue("Missing JavDB ranking period: $it", exploreUrl.contains("rankings/movies?$it&page={{page}}"))
        }
        listOf("深喉", "母子", "黑人", "start", "ipzz", "母乳", "ntr", "abf").forEach {
            assertTrue("Missing JavDB popular tag: $it", exploreUrl.contains(it))
        }
        assertFalse("JavDB discover should not use stale f=playable filter", exploreUrl.contains("f=playable"))
        assertFalse("JavDB discover should not use stale f=download filter", exploreUrl.contains("f=download"))
    }

    @Test
    fun hsexVideoSourceSearchUsesPagedSearchRoute() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val hsexSource = sourceObject(sourceText, """"bookSourceUrl": "https://hsex.icu/"""")
        val searchUrl = fieldValue(hsexSource, "searchUrl")

        assertTrue("好色TV source should exist", hsexSource.isNotBlank())
        assertTrue(searchUrl.contains("search-{{page}}.htm?search={{key}}&sort=new"))
        assertFalse("好色TV search should not stay on first page", searchUrl.contains("search.htm?search={{key}}&sort=new"))
    }

    @Test
    fun xmanSourceAddsNewMangaReleaseExploreCategory() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val xmanSource = sourceObject(sourceText, """"bookSourceUrl": "https://xman7.org"""")
        val exploreUrl = fieldValue(xmanSource, "exploreUrl")
        val bookListRule = fieldValue(xmanSource, "bookList")

        assertTrue("禁漫 source should exist", xmanSource.isNotBlank())
        assertTrue("禁漫 discover should include 新漫发布 category", exploreUrl.contains("新漫发布"))
        assertTrue("禁漫 新漫发布 should point to /nm", exploreUrl.contains("https://xman7.org/nm"))
        assertTrue("禁漫 explore parser should detect the /nm page", bookListRule.contains("/nm"))
        assertTrue("禁漫 新漫发布 should parse only the second /nm section", bookListRule.contains("sections.get(1)"))
        assertFalse("禁漫 explore parser should not depend on unavailable java.net.URL", bookListRule.contains("java.net.URL"))
    }

    @Test
    fun xmanSourceCollectsEveryLazyChapterImage() {
        val sourceText = repoFile("tests/shareBookSource.json").readText()
        val xmanSource = sourceObject(sourceText, """"bookSourceUrl": "https://xman7.org"""")
        val contentRule = fieldValue(xmanSource, "content")

        assertTrue("Xman source should exist", xmanSource.isNotBlank())
        assertTrue("Xman content should parse the chapter document", contentRule.contains("org.jsoup.Jsoup.parse(result)"))
        assertTrue("Xman content should read lazy image URLs", contentRule.contains("data-original"))
        assertTrue("Xman content should preserve all matching images", contentRule.contains("urls.push"))
        assertTrue("Xman content should return image tags for every collected URL", contentRule.contains("urls.map(function(u)"))
        assertTrue("Xman content should include image request headers", contentRule.contains("JSON.stringify(headers)"))
        assertFalse("Xman content should not keep the old single XPath image rule", contentRule.contains("//body/div[2]/center[3]/div/img"))
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
            """"$fieldName"\s*:\s*"((?:\\.|[^"\\])*)",""",
            RegexOption.DOT_MATCHES_ALL
        ).find(sourceText)?.groupValues?.get(1).orEmpty()
    }
}
