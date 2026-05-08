package io.legado.app.ui.book.read

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ReadMenuLayoutTest {

    @Test
    fun expandedPanelIsDockedToBottomTabBar() {
        val layout = readMenuLayout()
        val expandedPanel = layout.elementById("fl_expanded_panel")
        val bottomTabBar = layout.elementById("bottom_tab_bar")

        assertEquals(
            bottomTabBar.androidAttr("layout_height"),
            expandedPanel.androidAttr("layout_marginBottom")
        )
    }

    @Test
    fun themePanelKeepsVerticalScrollAvailable() {
        val layout = readMenuLayout()
        val themePanel = layout.elementById("panel_theme")
        val layoutPanel = layout.elementById("panel_layout")
        val layoutScroll = layout.elementById("panel_layout_scroll")

        assertEquals("vertical", themePanel.androidAttr("scrollbars"))
        assertEquals("false", themePanel.androidAttr("fadeScrollbars"))
        assertEquals("outsideOverlay", themePanel.androidAttr("scrollbarStyle"))
        assertEquals("right", themePanel.androidAttr("verticalScrollbarPosition"))
        assertEquals("LinearLayout", layoutPanel.tagName)
        assertEquals("vertical", layoutScroll.androidAttr("scrollbars"))
        assertEquals("false", layoutScroll.androidAttr("fadeScrollbars"))
        assertEquals("outsideOverlay", layoutScroll.androidAttr("scrollbarStyle"))
        assertEquals("right", layoutScroll.androidAttr("verticalScrollbarPosition"))
    }

    @Test
    fun themePresetsAreInSingleHorizontalRow() {
        val layout = readMenuLayout()
        val presetScroller = layout.elementById("hsv_theme_presets")
        val presetRow = layout.elementById("ll_theme_preset_row")
        val presetCards = listOf(
            "theme_card_follow_system",
            "theme_card_dark",
            "theme_card_paper",
            "theme_card_eye_green",
            "theme_card_quiet_blue",
            "theme_card_night"
        ).map { id -> layout.elementById(id) }

        assertEquals("horizontal", presetRow.androidAttr("orientation"))
        presetCards.forEach { card ->
            assertTrue(card.hasAncestor(presetRow))
            assertTrue(card.hasAncestor(presetScroller))
        }
    }

    @Test
    fun fontControlsBelongToLayoutPanelOnly() {
        val layout = readMenuLayout()
        val layoutPanel = layout.elementById("panel_layout")
        val themePanel = layout.elementById("panel_theme")
        val fontControls = listOf(
            "tv_theme_font_label",
            "font_option_source",
            "font_option_sans",
            "font_option_art",
            "font_option_custom",
            "seek_theme_font_weight",
            "seek_theme_text_size"
        ).map { id -> layout.elementById(id) }

        fontControls.forEach { control ->
            assertTrue(control.hasAncestor(layoutPanel))
            assertFalse(control.hasAncestor(themePanel))
        }
    }

    @Test
    fun layoutPanelDefaultsToSpacingControlsFromReference() {
        val layout = readMenuLayout()
        val layoutPanel = layout.elementById("panel_layout")
        val layoutScroll = layout.elementById("panel_layout_scroll")
        val spacingPanel = layout.elementById("panel_layout_spacing")
        val fontPanel = layout.elementById("panel_layout_font")
        val stylePanel = layout.elementById("panel_layout_style")
        val expectedControls = listOf(
            "seek_layout_letter_spacing",
            "seek_layout_line_spacing",
            "seek_layout_paragraph_spacing",
            "seek_layout_padding_top",
            "seek_layout_padding_bottom",
            "seek_layout_padding_left",
            "seek_layout_padding_right",
            "seek_layout_header_spacing",
            "seek_layout_footer_spacing"
        ).map { id -> layout.elementById(id) }

        assertTrue(spacingPanel.hasAncestor(layoutPanel))
        assertTrue(spacingPanel.hasAncestor(layoutScroll))
        assertEquals("", spacingPanel.androidAttr("visibility"))
        assertEquals("gone", fontPanel.androidAttr("visibility"))
        assertEquals("gone", stylePanel.androidAttr("visibility"))
        expectedControls.forEach { control ->
            assertTrue(control.hasAncestor(spacingPanel))
            assertEquals("40dp", control.androidAttr("layout_height"))
        }
    }

    @Test
    fun layoutPanelUsesCompactReferenceSizing() {
        val layout = readMenuLayout()

        assertEquals("18sp", layout.elementById("tv_panel_layout_title").androidAttr("textSize"))
        assertEquals("38dp", layout.elementById("ll_layout_tabs").androidAttr("layout_height"))
        assertEquals("14sp", layout.elementById("tv_layout_letter_spacing_label").androidAttr("textSize"))
        assertEquals("14sp", layout.elementById("tv_layout_padding_top_label").androidAttr("textSize"))
    }

    @Test
    fun layoutSubTabsAreDockedAboveMainTabBarOutsideScrollableContent() {
        val layout = readMenuLayout()
        val layoutPanel = layout.elementById("panel_layout")
        val layoutScroll = layout.elementById("panel_layout_scroll")
        val layoutTabs = layout.elementById("ll_layout_tabs")

        assertTrue(layoutTabs.hasAncestor(layoutPanel))
        assertFalse(layoutTabs.hasAncestor(layoutScroll))
        assertTrue(layoutScroll.isBefore(layoutTabs))
    }

    @Test
    fun expandedPanelHeightAdaptsToContentBeforeCapping() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("applyAdaptivePanelHeight"))
        assertFalse(readMenu.contains("val targetHeight = if (tab == BottomTab.Layout || tab == BottomTab.Theme)"))
    }

    @Test
    fun expandedPanelDoesNotInsetScrollbarsWithBackgroundPadding() {
        val panelBackground = drawableRoot("bg_read_menu_panel")

        assertEquals("", panelBackground.elementByName("padding")?.androidAttr("right").orEmpty())
    }

    @Test
    fun themeCardsUseCompactPreviewSizing() {
        val cardLayout = parseXml(
            findRepoFile("app/src/main/res/layout/view_read_theme_card.xml")
                ?: findRepoFile("src/main/res/layout/view_read_theme_card.xml")
                ?: error("view_read_theme_card.xml not found")
        )

        assertEquals("70dp", cardLayout.elementById("theme_card_preview").androidAttr("layout_height"))
    }

    @Test
    fun themePresetCardsFitThreeFullItemsOnPhoneWidth() {
        val layout = readMenuLayout()
        val cards = listOf(
            "theme_card_follow_system",
            "theme_card_dark",
            "theme_card_paper"
        ).map { id -> layout.elementById(id) }

        cards.forEach { card ->
            assertEquals("96dp", card.androidAttr("layout_width"))
        }
    }

    @Test
    fun themePanelUsesCompactReferenceSizing() {
        val layout = readMenuLayout()

        assertEquals("18sp", layout.elementById("tv_panel_theme_title").androidAttr("textSize"))
        assertEquals("14sp", layout.elementById("tv_theme_brightness_label").androidAttr("textSize"))
        assertEquals("40dp", layout.elementById("seek_theme_brightness").androidAttr("layout_height"))
        assertEquals("40dp", layout.elementById("seek_theme_contrast").androidAttr("layout_height"))
    }

    @Test
    fun legacyReadStyleSheetIsNotReachableFromReaderMenu() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val readActivity = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt").readText()

        assertFalse(readMenu.contains("showReadStyle"))
        assertFalse(readMenu.contains("openReadStylePanel"))
        assertFalse(readActivity.contains("ReadStyleDialog"))
        assertFalse(repoFile("app/src/main/res/layout/dialog_read_book_style.xml").exists())
    }

    private fun readMenuLayout(): Element {
        val file = findRepoFile("app/src/main/res/layout/view_read_menu.xml")
            ?: findRepoFile("src/main/res/layout/view_read_menu.xml")
            ?: error("view_read_menu.xml not found")

        return parseXml(file)
    }

    private fun drawableRoot(name: String): Element {
        val file = findRepoFile("app/src/main/res/drawable/$name.xml")
            ?: findRepoFile("src/main/res/drawable/$name.xml")
            ?: error("$name.xml not found")

        return parseXml(file)
    }

    private fun parseXml(file: File): Element {
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file).documentElement
    }

    private fun findRepoFile(relativePath: String): File? {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.isFile }
    }

    private fun repoFile(relativePath: String): File {
        return generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull { it.exists() }
            ?: File(relativePath)
    }

    private fun Element.elementById(id: String): Element {
        if (androidAttr("id") == "@+id/$id" || androidAttr("id") == "@id/$id") {
            return this
        }
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element) {
                runCatching { return child.elementById(id) }
            }
        }
        error("Element with id $id not found")
    }

    private fun Element.elementByName(name: String): Element? {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.tagName == name) {
                return child
            }
        }
        return null
    }

    private fun Element.hasAncestor(ancestor: Element): Boolean {
        return generateSequence(parentNode) { it.parentNode }
            .filterIsInstance<Element>()
            .any { it === ancestor }
    }

    private fun Element.isBefore(other: Element): Boolean {
        val parent = parentNode
        require(parent === other.parentNode) { "Elements do not share a parent" }
        val children = parent.childNodes
        for (index in 0 until children.length) {
            when (children.item(index)) {
                this -> return true
                other -> return false
            }
        }
        error("Elements not found under parent")
    }

    private fun Element.androidAttr(name: String): String =
        getAttributeNS(ANDROID_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
