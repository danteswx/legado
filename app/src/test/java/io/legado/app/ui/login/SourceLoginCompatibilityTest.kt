package io.legado.app.ui.login

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SourceLoginCompatibilityTest {

    @Test
    fun sourceLoginViewModelUpgradesLegacyUaaSourceBeforeReadingLoginInfo() {
        val viewModelSource = repoFile(
            "app/src/main/java/io/legado/app/ui/login/SourceLoginViewModel.kt"
        ).readText()

        val compatCall = viewModelSource.indexOf("applySourceLoginCompatIfNeeded()")
        val headerRead = viewModelSource.indexOf("source?.getHeaderMap(true)")

        assertTrue("UAA compat should run during login initialization", compatCall >= 0)
        assertTrue("UAA compat should run before login info is derived", compatCall < headerRead)
        assertTrue(viewModelSource.contains("UaaLoginCompat.apply(bookSource)"))
        assertTrue(viewModelSource.contains("appDb.bookSourceDao.update(bookSource)"))
    }

    @Test
    fun explorePageTreatsUaaAsLoginCapableEvenWhenSourcePartIsStale() {
        val exploreSource = repoFile(
            "app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt"
        ).readText()

        assertTrue(exploreSource.contains("UaaLoginCompat.isUaaSource"))
        assertTrue(exploreSource.contains("selectedDiscoverSourcePart?.canOpenSourceLogin() == true"))
    }

    @Test
    fun uaaCompatReplacesStaleVisibleCaptchaLoginUi() {
        val compatSource = repoFile(
            "app/src/main/java/io/legado/app/help/source/UaaLoginCompat.kt"
        ).readText()
        val loginUi = compatSource.substringAfter("val LOGIN_UI: String =")
            .substringBefore("val LOGIN_URL: String =")

        assertTrue(compatSource.contains("shouldReplaceLoginUi"))
        assertTrue(compatSource.contains("AI识别验证码"))
        assertTrue(compatSource.contains("验证码"))
        assertTrue(compatSource.contains("solveUaaCaptcha"))
        assertFalse(loginUi.contains("验证码"))
        assertFalse(loginUi.contains("AI识别验证码"))
        assertFalse(loginUi.contains("refreshCaptcha"))
    }

    private fun repoFile(path: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull { it.exists() }
            ?: error("$path not found")
    }
}
