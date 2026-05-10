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
    fun debugBuildUsesWanjuanBranding() {
        val zhDebugStrings = repoFile("app/src/debug/res/values-zh/strings.xml").readText()
        val defaultDebugStrings = repoFile("app/src/debug/res/values/strings.xml").readText()

        assertTrue(zhDebugStrings.contains("<string name=\"app_name\">万卷·D</string>"))
        assertTrue(zhDebugStrings.contains("<string name=\"receiving_shared_label\">万卷·D·搜索</string>"))
        assertTrue(defaultDebugStrings.contains("<string name=\"app_name\">Wanjuan·D</string>"))
        assertTrue(defaultDebugStrings.contains("<string name=\"receiving_shared_label\">Wanjuan·D·search</string>"))
    }

    @Test
    fun primaryReadBottomMenuUsesOptionAOrder() {
        val menu = parseXml(repoFile("app/src/main/res/menu/read_bottom_primary.xml"))
        val items = menu.childElements("item")

        assertEquals(
            listOf(
                "@+id/menu_read_search",
                "@+id/menu_read_toc",
                "@+id/menu_read_aloud",
                "@+id/menu_read_interface",
                "@+id/menu_read_settings"
            ),
            items.map { it.androidAttr("id") }
        )
        assertEquals("@drawable/ic_lucide_search", items.first().androidAttr("icon"))
        assertEquals("@string/search", items.first().androidAttr("title"))
    }

    @Test
    fun interfaceReadBottomMenuUsesOptionAOrder() {
        val menu = parseXml(repoFile("app/src/main/res/menu/read_bottom_interface.xml"))
        val items = menu.childElements("item")

        assertEquals(
            listOf(
                "@+id/menu_read_interface_back",
                "@+id/menu_read_layout",
                "@+id/menu_read_page_turn",
                "@+id/menu_read_background",
                "@+id/menu_read_theme"
            ),
            items.map { it.androidAttr("id") }
        )
        assertEquals("@drawable/ic_lucide_chevron_left", items.first().androidAttr("icon"))
        assertEquals("@string/text_return", items.first().androidAttr("title"))
        assertEquals("@drawable/ic_lucide_list", items[1].androidAttr("icon"))
        assertEquals("@string/compose_type", items[1].androidAttr("title"))
        assertEquals("@string/read_style_page", items[2].androidAttr("title"))
        assertEquals("@drawable/ic_lucide_image", items[3].androidAttr("icon"))
        assertEquals("@string/background", items[3].androidAttr("title"))
    }

    @Test
    fun primaryAndInterfaceBottomBarsUseMatchingFiveItemSpacing() {
        val layout = readMenuLayout()
        val primaryNav = layout.elementById("read_bottom_primary_nav")
        val interfaceNav = layout.elementById("read_bottom_interface_nav")
        val interfaceMenu = parseXml(repoFile("app/src/main/res/menu/read_bottom_interface.xml"))
        val dimens = repoFile("app/src/main/res/values/dimens.xml").readText()

        assertEquals("@dimen/read_bottom_nav_horizontal_padding", primaryNav.androidAttr("paddingStart"))
        assertEquals("@dimen/read_bottom_nav_horizontal_padding", primaryNav.androidAttr("paddingEnd"))
        assertEquals("@dimen/read_bottom_nav_horizontal_padding", interfaceNav.androidAttr("paddingStart"))
        assertEquals("@dimen/read_bottom_nav_horizontal_padding", interfaceNav.androidAttr("paddingEnd"))
        assertEquals("", interfaceNav.androidAttr("layout_marginStart"))
        assertFalse(repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
            .contains("android:id=\"@+id/read_bottom_interface_back\""))
        assertEquals(5, interfaceMenu.childElements("item").size)
        assertTrue(dimens.contains("<dimen name=\"read_bottom_nav_horizontal_padding\">18dp</dimen>"))
    }

    @Test
    fun backgroundTabRemovesTextureStrengthControls() {
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertFalse(layoutXml.contains("background_texture_panel"))
        assertFalse(layoutXml.contains("texture_none"))
        assertFalse(readMenu.contains("setTextureStrength"))
        assertFalse(readMenu.contains("updateTextureButtons"))
    }

    @Test
    fun themePanelRemovesBrightnessAndContrastControls() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(layout.elementById("panel_theme").tagName == "ScrollView")
        assertFalse(layoutXml.contains("seek_theme_brightness"))
        assertFalse(layoutXml.contains("seek_theme_contrast"))
        assertFalse(layoutXml.contains("theme_tone_panel"))
        assertFalse(readMenu.contains("applyTextContrast"))
    }

    @Test
    fun readMenuSearchPrimaryItemOpensInlinePanel() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val searchBranch = readMenu.substringAfter("R.id.menu_read_search ->")
            .substringBefore("R.id.menu_read_toc ->")

        assertTrue(readMenu.contains("BottomTab.Search"))
        assertTrue(searchBranch.contains("toggleBottomTab(BottomTab.Search)"))
        assertFalse(searchBranch.contains("openSearchActivity"))
        assertEquals("panel_search", layout.elementById("panel_search").androidAttr("id").substringAfter("@+id/"))
        assertEquals("rv_panel_search_results", layout.elementById("rv_panel_search_results").androidAttr("id").substringAfter("@+id/"))
    }

    @Test
    fun layoutPanelIncludesFontSamplesWithoutDuplicateFontTitle() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val fontPanel = layout.elementById("panel_layout_font")
        val fontCards = listOf(
            "font_card_source",
            "font_card_sans",
            "font_card_art",
            "font_card_custom",
            "font_card_add_custom"
        ).map { id -> layout.elementById(id) }

        assertFalse(layoutXml.contains("tv_theme_font_label"))
        fontCards.forEach { card ->
            assertTrue(card.getAttribute("layout").contains("view_read_theme_card"))
            assertTrue(card.hasAncestor(fontPanel))
        }
    }

    @Test
    fun interfacePageTurnTabUsesDedicatedPanel() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val pageTurnPanel = layout.elementById("panel_page_turn")

        assertTrue(readMenu.contains("BottomTab.PageTurn"))
        assertTrue(readMenu.contains("R.id.menu_read_page_turn -> BottomTab.PageTurn"))
        assertTrue(readMenu.contains("panelPageTurn.gone(tab != BottomTab.PageTurn)"))
        listOf(
            "panel_page_anim",
            "panel_page_auto_page",
            "panel_page_volume_key",
            "panel_page_mouse_wheel",
            "panel_page_touch_slop"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(pageTurnPanel))
        }
    }

    @Test
    fun backgroundPanelShowsImageSamplesAndContinuousControls() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val backgroundPanel = layout.elementById("panel_background")
        val backgroundCards = listOf(
            "background_card_beach",
            "background_card_night",
            "background_card_green",
            "background_card_parchment",
            "background_card_fresh",
            "background_card_palace",
            "background_card_canvas",
            "background_card_landscape",
            "background_card_bright"
        ).map { id -> layout.elementById(id) }

        backgroundCards.forEach { card ->
            assertTrue(card.getAttribute("layout").contains("view_read_background_card"))
            assertTrue(card.hasAncestor(backgroundPanel))
        }
        assertTrue(layout.elementById("seek_background_brightness").hasAncestor(backgroundPanel))
        assertTrue(layout.elementById("seek_background_saturation").hasAncestor(backgroundPanel))
        assertTrue(layout.elementById("seek_background_alpha").hasAncestor(backgroundPanel))
        assertTrue(readMenu.contains("午后沙滩.jpg"))
        assertTrue(readMenu.contains("羊皮纸4.jpg"))
        assertTrue(readMenu.contains("ReadBookConfig.bgAlpha"))
    }

    @Test
    fun layoutPanelRemovesDuplicatedRegionSpacingAndAddsTipSettings() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val layoutPanel = layout.elementById("panel_layout")

        assertFalse(layoutXml.contains("tv_layout_region_spacing_title"))
        assertFalse(layoutXml.contains("seek_layout_header_spacing"))
        assertFalse(layoutXml.contains("seek_layout_footer_spacing"))
        assertFalse(layoutXml.contains("ll_layout_tip_header_show"))
        assertFalse(layoutXml.contains("ll_layout_tip_footer_show"))
        assertEquals("@string/read_menu_layout_title", layout.elementById("tv_layout_page_margin_title").androidAttr("text"))
        listOf(
            "iv_layout_header_title",
            "iv_layout_footer_title",
            "seek_layout_title_top_spacing",
            "seek_layout_title_bottom_spacing"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(layoutPanel))
        }
        assertTrue(layout.elementById("layout_text_style_entry").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("seek_theme_text_size").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("seek_theme_font_weight").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("seek_layout_letter_spacing").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("seek_layout_line_spacing").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("seek_layout_paragraph_spacing").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_tip_color_value").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_tip_divider_color_value").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_header_show_value").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_footer_show_value").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_header_line_toggle").hasAncestor(layoutPanel))
        assertFalse(layout.elementById("tv_layout_footer_line_toggle").hasAncestor(layoutPanel))
        assertTrue(layout.elementById("tv_layout_header_show_value")
            .hasAncestor(layout.elementById("layout_margin_adjust_preview_host")))
        assertEquals("44dp", layout.elementById("tv_layout_header_title").androidAttr("minWidth"))
        assertEquals("wrap_content", layout.elementById("tv_layout_header_show_value").androidAttr("layout_width"))
        assertTrue(layout.elementById("tv_layout_header_line_toggle")
            .hasAncestor(layout.elementById("layout_margin_adjust_preview_host")))
        assertEquals("86dp", layout.elementById("tv_layout_header_line_toggle").androidAttr("layout_width"))
        assertTrue(layout.elementById("tv_layout_footer_show_value")
            .hasAncestor(layout.elementById("layout_margin_adjust_preview_host")))
        assertEquals("44dp", layout.elementById("tv_layout_footer_title").androidAttr("minWidth"))
        assertEquals("wrap_content", layout.elementById("tv_layout_footer_show_value").androidAttr("layout_width"))
        assertTrue(layout.elementById("tv_layout_footer_line_toggle")
            .hasAncestor(layout.elementById("layout_margin_adjust_preview_host")))
        assertEquals("86dp", layout.elementById("tv_layout_footer_line_toggle").androidAttr("layout_width"))
        assertTrue(readMenu.contains("read_menu_display_auto"))
        assertTrue(readMenu.contains("ReadTipConfig.headerMode"))
        assertTrue(readMenu.contains("ReadBookConfig.titleSize"))
    }

    @Test
    fun readMenuSeekBarsUseRealtimeConfigAndRingThumb() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val seekBar = repoFile("app/src/main/java/io/legado/app/lib/theme/view/ThemeSeekBar.kt").readText()

        assertTrue(seekBar.contains("buildRingThumb"))
        assertTrue(seekBar.contains("Color.WHITE"))
        assertTrue(readMenu.contains("if (fromUser) onStop(progress)"))
        assertFalse(readMenu.contains("override fun onStopTrackingTouch(seekBar: SeekBar) {\n                onStop(seekBar.progress)"))
    }

    @Test
    fun tocPanelHasMetadataAndProgressControls() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val tocPanel = layout.elementById("panel_toc")

        listOf(
            "toc_progress_mode_toggle",
            "tv_toc_prev_chapter",
            "seek_toc_progress",
            "tv_toc_next_chapter"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(tocPanel))
        }
        assertFalse(readMenu.contains("\"${'$'}{chapter.index + 1}. ${'$'}{chapter.title}\""))
        assertTrue(readMenu.contains("chapter.tag"))
        assertTrue(readMenu.contains("chapter.wordCount"))
        assertTrue(readMenu.contains("BookHelp.hasContent"))
        assertTrue(readMenu.contains("ic_lucide_download"))
    }

    @Test
    fun oldPageAndMoreMenuIdsAreNotSecondaryTabs() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertFalse(readMenu.contains("BottomTab.Page,"))
        assertFalse(readMenu.contains("BottomTab.More"))
        assertFalse(readMenu.contains("R.id.menu_read_page ->"))
        assertFalse(readMenu.contains("R.id.menu_read_more"))
    }

    @Test
    fun darkBottomTabGlassUsesNearBlackSurfaceAndTint() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("private fun bottomTabDarkGlassSurfaceColor(): Int"))
        assertTrue(readMenu.contains("Color.rgb(8, 10, 14)"))
        assertTrue(readMenu.contains("private fun bottomTabGlassTintColor(): FloatArray"))
        assertTrue(readMenu.contains("floatArrayOf(0.08f, 0.10f, 0.14f)"))
        assertTrue(readMenu.contains("0.52f + glassLevel * 0.18f"))
    }

    @Test
    fun bottomTabShellDoesNotDrawOuterStroke() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val glassShell = readMenu.substringAfter("private fun bottomTabGlassShell")
            .substringBefore("private fun bottomTabGlassFallbackShell")
        val eInkShell = readMenu.substringAfter("if (AppConfig.isEInkMode)")
            .substringBefore("return@run")

        assertFalse(glassShell.contains("setStroke("))
        assertFalse(eInkShell.contains("1.dpToPx()"))
    }

    @Test
    fun bottomTabSelectedLabelStaysWhiteOnAccentIndicator() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val colorBlock = readMenu.substringAfter("private fun applyBottomNavigationColors()")
            .substringBefore("private fun syncBottomNavigationSelection()")

        assertTrue(readMenu.contains("private fun bottomTabSelectedLabelColor(): Int = Color.WHITE"))
        assertTrue(colorBlock.contains("val iconColors = Selector.colorBuild()"))
        assertTrue(colorBlock.contains("val textColors = Selector.colorBuild()"))
        assertTrue(colorBlock.contains(".setPressedColor(Color.WHITE)"))
        assertTrue(colorBlock.contains(".setSelectedColor(bottomTabSelectedLabelColor())"))
        assertTrue(colorBlock.contains(".setCheckedColor(bottomTabSelectedLabelColor())"))
        assertTrue(colorBlock.contains("nav.itemIconTintList = iconColors"))
        assertTrue(colorBlock.contains("nav.itemTextColor = textColors"))
        assertFalse(colorBlock.contains("nav.itemTextColor = colors"))
    }

    @Test
    fun bottomTabSelectedIconStaysWhiteOnAccentIndicator() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("private fun bottomTabSelectedContentColor(): Int = Color.WHITE"))
        assertTrue(readMenu.substringAfter("val iconColors = Selector.colorBuild()")
            .substringBefore("val textColors = Selector.colorBuild()")
            .contains(".setCheckedColor(bottomTabSelectedContentColor())"))
    }

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
        val previewHost = layout.elementById("layout_margin_adjust_preview_host")
        val fontControls = listOf(
            "font_card_source",
            "font_card_sans",
            "font_card_art",
            "font_card_custom"
        ).map { id -> layout.elementById(id) }
        val textStyleControls = listOf(
            "seek_theme_font_weight",
            "seek_theme_text_size"
        ).map { id -> layout.elementById(id) }

        fontControls.forEach { control ->
            assertTrue(control.hasAncestor(layoutPanel))
            assertFalse(control.hasAncestor(themePanel))
        }
        textStyleControls.forEach { control ->
            assertTrue(control.hasAncestor(previewHost))
            assertFalse(control.hasAncestor(themePanel))
        }
    }

    @Test
    fun layoutPanelKeepsSpacingAndStyleControlsInSingleLayoutView() {
        val layout = readMenuLayout()
        val layoutPanel = layout.elementById("panel_layout")
        val layoutScroll = layout.elementById("panel_layout_scroll")
        val spacingPanel = layout.elementById("panel_layout_spacing")
        val fontPanel = layout.elementById("panel_layout_font")
        val stylePanel = layout.elementById("panel_layout_style")
        val marginEntries = listOf(
            "layout_text_style_entry",
            "layout_margin_entry_body",
            "layout_margin_entry_title",
            "layout_margin_entry_header",
            "layout_margin_entry_footer"
        ).map { id -> layout.elementById(id) }

        assertTrue(spacingPanel.hasAncestor(layoutPanel))
        assertTrue(spacingPanel.hasAncestor(layoutScroll))
        assertEquals("", spacingPanel.androidAttr("visibility"))
        assertEquals("", fontPanel.androidAttr("visibility"))
        assertTrue(stylePanel.hasAncestor(layoutPanel))
        assertTrue(stylePanel.hasAncestor(layoutScroll))
        assertEquals("", stylePanel.androidAttr("visibility"))
        marginEntries.forEach { entry ->
            assertTrue(entry.hasAncestor(spacingPanel))
            assertEquals("44dp", entry.androidAttr("layout_height"))
        }
        assertEquals("14sp", layout.elementById("tv_layout_text_style_entry").androidAttr("textSize"))
        assertEquals("13sp", layout.elementById("tv_layout_text_style_entry_value").androidAttr("textSize"))
    }

    @Test
    fun layoutPanelUsesCompactMarginEntriesAndImmersiveMarginOverlay() {
        val layout = readMenuLayout()
        val spacingPanel = layout.elementById("panel_layout_spacing")
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val overlay = layout.elementById("layout_margin_adjust_overlay")
        val overlayPanel = layout.elementById("layout_margin_adjust_panel")
        val previewHost = layout.elementById("layout_margin_adjust_preview_host")
        val previewFrame = layout.elementById("layout_margin_adjust_preview")

        assertFalse(previewHost.hasAncestor(spacingPanel))
        assertEquals("gone", overlay.androidAttr("visibility"))
        assertEquals("match_parent", overlay.androidAttr("layout_width"))
        assertEquals("match_parent", overlay.androidAttr("layout_height"))
        assertEquals("androidx.constraintlayout.widget.ConstraintLayout", previewHost.tagName)
        assertTrue(overlayPanel.hasAncestor(overlay))
        assertTrue(previewHost.hasAncestor(overlayPanel))
        assertEquals("io.legado.app.ui.book.read.ReadMarginPreviewView", previewFrame.tagName)
        assertEquals("142dp", previewFrame.androidAttr("layout_width"))
        assertEquals("142dp", previewFrame.androidAttr("layout_height"))

        listOf(
            "layout_margin_entry_body",
            "layout_margin_entry_title",
            "layout_margin_entry_header",
            "layout_margin_entry_footer"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(spacingPanel))
        }

        listOf(
            "seek_layout_padding_top",
            "seek_layout_padding_bottom",
            "seek_layout_padding_left",
            "seek_layout_padding_right",
            "seek_layout_title_top_spacing",
            "seek_layout_title_bottom_spacing"
        ).forEach { id ->
            assertEquals("gone", layout.elementById(id).androidAttr("visibility"))
        }

        listOf(
            "seek_theme_text_size",
            "seek_theme_font_weight",
            "seek_layout_letter_spacing",
            "seek_layout_line_spacing",
            "seek_layout_paragraph_spacing",
            "tv_layout_tip_color_value",
            "tv_layout_tip_divider_color_value",
            "tv_layout_header_show_value",
            "tv_layout_footer_show_value",
            "tv_layout_header_line_toggle",
            "tv_layout_footer_line_toggle"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(previewHost))
        }

        mapOf(
            "layout_margin_spinbox_top" to "End_toStartOf",
            "layout_margin_spinbox_left" to "End_toStartOf",
            "layout_margin_spinbox_bottom" to "Start_toEndOf",
            "layout_margin_spinbox_right" to "Start_toEndOf"
        ).forEach { (id, constraint) ->
            val spinbox = layout.elementById(id)
            assertTrue(spinbox.hasAncestor(previewHost))
            assertEquals("@id/layout_margin_adjust_preview", spinbox.appAttr("layout_constraint$constraint"))
            assertEquals("@id/layout_margin_adjust_preview", spinbox.appAttr("layout_constraintTop_toTopOf"))
            assertEquals("@id/layout_margin_adjust_preview", spinbox.appAttr("layout_constraintBottom_toBottomOf"))
        }

        assertTrue(layout.elementById("ll_layout_margin_title_mode").hasAncestor(previewHost))
        assertTrue(layout.elementById("layout_margin_title_size").hasAncestor(previewHost))
        assertTrue(layout.elementById("seek_layout_title_size").hasAncestor(previewHost))
        assertTrue(layout.elementById("layout_text_style_controls").hasAncestor(previewHost))
        assertEquals("@id/layout_margin_adjust_preview_host", layout.elementById("layout_text_style_controls")
            .appAttr("layout_constraintTop_toTopOf"))
        assertTrue(layout.elementById("layout_text_typography_section").hasAncestor(layout.elementById("layout_text_style_controls")))
        assertTrue(layout.elementById("layout_text_spacing_section").hasAncestor(layout.elementById("layout_text_style_controls")))
        assertTrue(layout.elementById("layout_text_color_section").hasAncestor(layout.elementById("layout_text_style_controls")))
        assertEquals("58dp", layout.elementById("layout_text_size_row").androidAttr("layout_height"))
        assertEquals("58dp", layout.elementById("layout_text_weight_row").androidAttr("layout_height"))
        assertTrue(layout.elementById("vw_layout_tip_color_swatch").hasAncestor(layout.elementById("ll_layout_tip_color")))
        assertTrue(layout.elementById("layout_tip_header_controls").hasAncestor(previewHost))
        assertTrue(layout.elementById("layout_tip_footer_controls").hasAncestor(previewHost))
        assertTrue(layout.elementById("ll_layout_tip_divider_color").hasAncestor(previewHost))
        assertEquals("LinearLayout", layout.elementById("layout_tip_controls").tagName)
        assertEquals("", layout.elementById("layout_tip_controls").androidAttr("scrollbars"))
        assertEquals("", layout.elementById("layout_tip_controls")
            .appAttr("layout_constraintTop_toTopOf"))
        assertEquals("@id/layout_margin_adjust_preview", layout.elementById("layout_tip_controls")
            .appAttr("layout_constraintTop_toBottomOf"))
        assertTrue(layout.elementById("layout_header_display_auto_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementById("layout_header_display_show_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementByTag("layout_header_display_hide_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementByTag("layout_footer_display_auto_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        assertTrue(layout.elementById("layout_footer_display_show_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        assertTrue(layout.elementById("layout_footer_display_hide_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        assertEquals("52dp", layout.elementById("layout_header_display_auto_card").androidAttr("layout_height"))
        assertEquals("52dp", layout.elementByTag("layout_header_display_hide_card").androidAttr("layout_height"))
        assertEquals("52dp", layout.elementByTag("layout_footer_display_auto_card").androidAttr("layout_height"))
        assertEquals("52dp", layout.elementById("layout_footer_display_show_card").androidAttr("layout_height"))
        assertTrue(layout.elementById("vw_header_display_auto_radio").hasAncestor(layout.elementById("layout_header_display_auto_card")))
        assertTrue(layout.elementByTag("vw_header_display_hide_radio").hasAncestor(layout.elementByTag("layout_header_display_hide_card")))
        assertTrue(layout.elementByTag("vw_footer_display_auto_radio").hasAncestor(layout.elementByTag("layout_footer_display_auto_card")))
        assertTrue(layout.elementById("vw_footer_display_hide_radio").hasAncestor(layout.elementById("layout_footer_display_hide_card")))
        assertEquals("gone", layout.elementById("vw_header_display_auto_radio").androidAttr("visibility"))
        assertEquals("gone", layout.elementByTag("vw_footer_display_auto_radio").androidAttr("visibility"))
        assertTrue(repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
            .contains("android:tag=\"read_menu_tip_preview\""))
        assertFalse(repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
            .contains("android:text=\"@string/read_menu_footer_show_summary\"\n                                    android:textSize=\"12sp\"\n                                    android:visibility=\"gone\""))
        assertTrue(layout.elementById("vw_layout_tip_divider_color_swatch").hasAncestor(layout.elementById("ll_layout_tip_divider_color")))
        assertEquals("@drawable/ic_arrow_right", layout.elementById("iv_layout_tip_divider_color_arrow").androidAttr("src"))
        assertEquals("gone", layout.elementByTag("layout_tip_padding_section").androidAttr("visibility"))
        assertTrue(layout.elementById("layout_tip_header_padding_controls").hasAncestor(previewHost))
        assertTrue(layout.elementById("layout_tip_footer_padding_controls").hasAncestor(previewHost))
        assertTrue(layout.elementById("seek_layout_header_padding_top").hasAncestor(layout.elementById("layout_tip_header_padding_controls")))
        assertTrue(layout.elementById("seek_layout_footer_padding_bottom").hasAncestor(layout.elementById("layout_tip_footer_padding_controls")))
        assertEquals(
            "@id/layout_margin_spinbox_bottom",
            layout.elementById("layout_margin_title_size")
                .appAttr("layout_constraintTop_toBottomOf")
        )
        assertTrue(readMenu.contains("layoutMarginAdjustPreview.setMode"))
        assertTrue(readMenu.contains("layoutMarginAdjustPreview.setTitleSize"))
        assertTrue(readMenu.contains("layoutMarginAdjustPreview.setTitleMode"))
        assertTrue(readMenu.contains("LayoutMarginAdjustMode.Text"))
        assertTrue(readMenu.contains("ReadMarginPreviewView.Mode.Text"))
        assertTrue(readMenu.contains("layoutMarginAdjustPreview.visible(!showTextStyleMode)"))
        assertTrue(readMenu.contains("val showVerticalSpinboxes = !showTextStyleMode"))
        assertTrue(readMenu.contains("layoutMarginSpinboxTop.visible(showVerticalSpinboxes)"))
        assertTrue(readMenu.contains("ReadMarginPreviewView.Mode.Title"))
        assertTrue(readMenu.contains("ReadMarginPreviewView.Mode.Header"))
        assertTrue(readMenu.contains("ReadMarginPreviewView.Mode.Footer"))
        assertTrue(readMenu.contains("updateLayoutMarginPreview"))
        assertTrue(readMenu.contains("showLayoutMarginAdjustOverlay"))
        assertTrue(readMenu.contains("hideLayoutMarginAdjustOverlay"))
        assertTrue(readMenu.contains("setHeaderDisplayMode(2)"))
        assertTrue(readMenu.contains("setFooterDisplayMode(2)"))
        assertTrue(readMenu.contains("tintDescendantText(card, if (selected) Color.WHITE else textColor)"))
        assertTrue(readMenu.contains("context.accentColor"))
    }

    @Test
    fun layoutPanelUsesCompactReferenceSizing() {
        val layout = readMenuLayout()

        assertEquals("18sp", layout.elementById("tv_panel_layout_title").androidAttr("textSize"))
        assertEquals("14sp", layout.elementById("tv_layout_letter_spacing_label").androidAttr("textSize"))
        assertEquals("14sp", layout.elementById("tv_layout_padding_top_label").androidAttr("textSize"))
    }

    @Test
    fun fontAndLayoutPanelsDoNotExposeTertiaryLayoutTabs() {
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertFalse(layoutXml.contains("ll_layout_tabs"))
        assertFalse(layoutXml.contains("layout_tab_font"))
        assertFalse(layoutXml.contains("layout_tab_spacing"))
        assertFalse(layoutXml.contains("layout_tab_style"))
        assertFalse(readMenu.contains("layoutTabFont.setOnClickListener"))
        assertFalse(readMenu.contains("layoutTabSpacing.setOnClickListener"))
        assertFalse(readMenu.contains("layoutTabStyle.setOnClickListener"))
        assertFalse(readMenu.contains("LayoutTab"))
        assertFalse(readMenu.contains("BottomTab.Font"))
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
        assertEquals("14sp", layout.elementById("tv_theme_font_weight_label").androidAttr("textSize"))
        assertEquals("40dp", layout.elementById("seek_theme_font_weight").androidAttr("layout_height"))
        assertEquals("40dp", layout.elementById("seek_theme_text_size").androidAttr("layout_height"))
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

    private fun Element.elementByTag(tag: String): Element {
        if (androidAttr("tag") == tag) {
            return this
        }
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element) {
                runCatching { return child.elementByTag(tag) }
            }
        }
        error("Element with tag $tag not found")
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

    private fun Element.childElements(name: String): List<Element> {
        val children = childNodes
        return buildList {
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element && child.tagName == name) {
                    add(child)
                }
            }
        }
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

    private fun Element.appAttr(name: String): String =
        getAttributeNS(APP_NAMESPACE, name)

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"
    }
}
