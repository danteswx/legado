package io.legado.app.ui.book.read

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadMenuThemePresetTest {

    @Test
    fun defaultPresetsMatchReferenceSheet() {
        val presets = ReadMenuThemePreset.defaultPresets()

        assertEquals(6, presets.size)
        assertEquals("follow_system", presets[0].key)
        assertEquals("dark", presets[1].key)
        assertEquals("paper", presets[2].key)
        assertEquals("eye_green", presets[3].key)
        assertEquals("quiet_blue", presets[4].key)
        assertEquals("night", presets[5].key)
        assertTrue(presets.all { it.previewTitle.isNotBlank() })
        assertTrue(presets.all { it.previewBody.isNotBlank() })
    }

    @Test
    fun contrastProgressDarkensTextOnLightBackground() {
        val result = ReadMenuThemePreset.contrastTextColor(
            baseTextColor = 0xFF444444.toInt(),
            backgroundColor = 0xFFF6F6F6.toInt(),
            progress = 75
        )

        assertEquals(0xFF333333.toInt(), result)
    }

    @Test
    fun contrastProgressLightensTextOnDarkBackground() {
        val result = ReadMenuThemePreset.contrastTextColor(
            baseTextColor = 0xFFBBBBBB.toInt(),
            backgroundColor = 0xFF101010.toInt(),
            progress = 75
        )

        assertEquals(0xFFD2D2D2.toInt(), result)
    }
}
