package io.legado.app.ui.main.explore

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ExploreFragmentLifecycleTest {

    @Test
    fun modernDiscoveryReinitializesCurrentViewAfterRecreation() {
        val source = repoFile("app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt").readText()
        val onResume = source.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")

        assertTrue(onResume.contains("val needsViewInitialization"))
        assertTrue(onResume.contains("!modernModeInitialized"))
        assertTrue(onResume.contains("!oldModeInitialized"))
        assertTrue(onResume.contains("|| needsViewInitialization"))
        assertTrue(onResume.contains("applyDiscoveryMode(loadData = true)"))
    }

    @Test
    fun modernDiscoveryShowsFriendlyRuleErrorsInsteadOfRawRhinoToast() {
        val source = repoFile("app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt").readText()
        val urlScriptBlock = source.substringAfter("private fun executeDiscoverUrlScriptIfNeeded(")
            .substringBefore("private fun extractDiscoverUrlScript")
        val buttonBlock = source.substringAfter("private fun handleDiscoverButtonTag(")
            .substringBefore("private fun applyDiscoverButtonResult")
        val selectBlock = source.substringAfter("private fun handleDiscoverSelectValue(")
            .substringBefore("private fun selectDiscoverTabByCode")

        assertTrue(source.contains("private fun showDiscoverRuleExecutionError(label: String)"))
        assertTrue(source.contains("R.string.discover_rule_error"))
        assertTrue(urlScriptBlock.contains("showDiscoverRuleExecutionError(item.text)"))
        assertTrue(buttonBlock.contains("showDiscoverRuleExecutionError(item.text)"))
        assertTrue(selectBlock.contains("showDiscoverRuleExecutionError(item.text)"))
        assertTrue(selectBlock.contains("catch (_: CancellationException)"))
        assertTrue(urlScriptBlock.contains("AppLog.put("))
        assertTrue(buttonBlock.contains("AppLog.put("))
        assertTrue(source.contains("binding.tvDiscoverEmpty.text = getString(R.string.discover_rule_error"))
        assertFalse(urlScriptBlock.contains("toastOnUi(it.localizedMessage"))
        assertFalse(buttonBlock.contains("toastOnUi(it.localizedMessage"))
    }

    private fun repoFile(relativePath: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.exists() }
            ?: File(relativePath)
    }
}
