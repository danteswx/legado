package io.legado.app.ci

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GithubWorkflowSigningTest {

    @Test
    fun releaseWorkflowFallsBackToStableCiSigningKey() {
        val workflow = repoFile(".github/workflows/release.yml").readText()
        val fallback = workflow
            .substringAfter("Release signing secrets are not configured")
            .substringBefore("fi")

        assertStableCiSigningFallback(fallback)
    }

    @Test
    fun testWorkflowUsesStableCiSigningKeyForInstallableArtifacts() {
        val workflow = repoFile(".github/workflows/test.yml").readText()

        assertStableCiSigningFallback(workflow)
    }

    private fun assertStableCiSigningFallback(script: String) {
        assertTrue(script.contains("RELEASE_STORE_FILE=./ci-debug.keystore"))
        assertTrue(script.contains("RELEASE_KEY_ALIAS=legado-ci-debug"))
        assertTrue(script.contains("RELEASE_STORE_PASSWORD=legado-ci-debug"))
        assertTrue(script.contains("RELEASE_KEY_PASSWORD=legado-ci-debug"))
        assertFalse(script.contains("keytool -genkeypair"))
        assertFalse(script.contains("RELEASE_STORE_FILE=./legado.jks"))
    }

    private fun repoFile(path: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, path) }
            .firstOrNull { it.exists() }
            ?: error("$path not found")
    }
}
