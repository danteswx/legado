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
    fun discoverTopActionButtonsUseReaderToolbarScale() {
        val layout = parseXml(repoFile("app/src/main/res/layout/fragment_explore.xml"))
        val dimens = repoFile("app/src/main/res/values/dimens.xml").readText()

        listOf(
            "btn_discover_source_search",
            "btn_discover_layout_toggle",
            "btn_discover_tag_filter",
            "btn_discover_more"
        ).forEach { id ->
            val button = layout.elementById(id)
            assertEquals("@dimen/discover_top_action_button_size", button.androidAttr("layout_width"))
            assertEquals("@dimen/discover_top_action_button_size", button.androidAttr("layout_height"))
            assertEquals("@dimen/discover_top_action_button_padding", button.androidAttr("padding"))
            assertEquals("centerInside", button.androidAttr("scaleType"))
        }
        assertTrue(dimens.contains("<dimen name=\"discover_top_action_button_size\">48dp</dimen>"))
        assertTrue(dimens.contains("<dimen name=\"discover_top_action_button_padding\">12dp</dimen>"))
    }

    @Test
    fun discoverTopActionLayoutToggleUsesLucideIconsBetweenSearchAndMenu() {
        val layout = parseXml(repoFile("app/src/main/res/layout/fragment_explore.xml"))
        val toggle = layout.elementById("btn_discover_layout_toggle")
        val settings = layout.elementById("btn_discover_tag_filter")

        assertTrue(layout.elementById("btn_discover_source_search").isBefore(toggle))
        assertTrue(toggle.isBefore(settings))
        assertTrue(toggle.isBefore(layout.elementById("btn_discover_more")))
        assertEquals("@string/switchLayout", toggle.androidAttr("contentDescription"))
        assertEquals("@drawable/ic_lucide_layout_grid", toggle.androidAttr("src"))
        assertEquals("@drawable/ic_lucide_settings", settings.androidAttr("src"))
        assertTrue(repoFile("app/src/main/res/drawable/ic_lucide_layout_grid.xml").exists())
        assertTrue(repoFile("app/src/main/res/drawable/ic_lucide_layout_list.xml").exists())
    }

    @Test
    fun discoverTopActionLayoutToggleSwitchesBetweenListAndGridOnly() {
        val exploreFragment = repoFile("app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt").readText()

        assertTrue(exploreFragment.contains("binding.btnDiscoverLayoutToggle.setOnClickListener"))
        assertTrue(exploreFragment.contains("private fun switchDiscoverBookLayout()"))
        assertTrue(exploreFragment.contains("const val DISCOVER_LAYOUT_LIST = 0"))
        assertTrue(exploreFragment.contains("const val DISCOVER_LAYOUT_GRID = 1"))
        assertTrue(exploreFragment.contains("R.drawable.ic_lucide_layout_grid"))
        assertTrue(exploreFragment.contains("R.drawable.ic_lucide_layout_list"))
        assertTrue(exploreFragment.contains("DISCOVER_LAYOUT_GRID -> GridLayoutManager(requireContext(), AppConfig.modernDiscoveryGridColumns)"))
        assertFalse(exploreFragment.contains("DISCOVER_LAYOUT_COUNT = 3"))
    }

    @Test
    fun discoverGridLayoutColumnsAreConfigurableFromSettings() {
        val exploreFragment = repoFile("app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt").readText()
        val exploreShowAdapter = repoFile("app/src/main/java/io/legado/app/ui/book/explore/ExploreShowAdapter.kt").readText()
        val appConfig = repoFile("app/src/main/java/io/legado/app/help/config/AppConfig.kt").readText()
        val preferKey = repoFile("app/src/main/java/io/legado/app/constant/PreferKey.kt").readText()
        val rowUiViewFactory = repoFile("app/src/main/java/io/legado/app/ui/widget/RowUiViewFactory.kt").readText()
        val defaultStrings = repoFile("app/src/main/res/values/strings.xml").readText()
        val zhStrings = repoFile("app/src/main/res/values-zh/strings.xml").readText()

        assertTrue(preferKey.contains("const val modernDiscoveryGridColumns = \"modernDiscoveryGridColumns\""))
        assertTrue(appConfig.contains("var modernDiscoveryGridColumns: Int"))
        assertTrue(appConfig.contains("get() = appCtx.getPrefInt(PreferKey.modernDiscoveryGridColumns, 2).coerceIn(2, 4)"))
        assertTrue(exploreFragment.contains("private const val DISCOVER_GRID_COLUMNS_SETTING_NAME = \"discover_grid_columns\""))
        assertTrue(exploreFragment.contains("private val DISCOVER_GRID_COLUMN_VALUES = arrayOf<String?>(\"2\", \"3\", \"4\")"))
        assertTrue(exploreFragment.contains("name = DISCOVER_GRID_COLUMNS_SETTING_NAME"))
        assertTrue(exploreFragment.contains("viewName = getString(R.string.discover_grid_columns)"))
        assertTrue(exploreFragment.contains("AppConfig.modernDiscoveryGridColumns = value.toIntOrNull() ?: return"))
        assertTrue(exploreFragment.contains("put(DISCOVER_GRID_COLUMNS_SETTING_NAME, AppConfig.modernDiscoveryGridColumns.toString())"))
        assertTrue(exploreFragment.contains("discoverBookAdapter.gridColumns = AppConfig.modernDiscoveryGridColumns"))
        assertTrue(exploreShowAdapter.contains("var gridColumns: Int = 2"))
        assertTrue(exploreShowAdapter.contains("val isCompact = gridColumns >= 3"))
        assertTrue(rowUiViewFactory.contains("rowUi.viewName ?: rowUi.name"))
        assertTrue(defaultStrings.contains("<string name=\"discover_grid_columns\">Items per row</string>"))
        assertTrue(zhStrings.contains("<string name=\"discover_grid_columns\">每行数量</string>"))
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
        val primaryNavEvents = readMenu.substringAfter("readBottomPrimaryNav.setOnItemSelectedListener")
        val searchBranch = primaryNavEvents.substringAfter("R.id.menu_read_search ->")
            .substringBefore("R.id.menu_read_toc ->")

        assertTrue(readMenu.contains("BottomTab.Search"))
        assertTrue(searchBranch.contains("toggleBottomTab(BottomTab.Search)"))
        assertFalse(searchBranch.contains("openSearchActivity"))
        assertEquals("panel_search", layout.elementById("panel_search").androidAttr("id").substringAfter("@+id/"))
        assertEquals("rv_panel_search_results", layout.elementById("rv_panel_search_results").androidAttr("id").substringAfter("@+id/"))
    }

    @Test
    fun readMenuAloudPrimaryItemOpensInlinePanel() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val primaryNavEvents = readMenu.substringAfter("readBottomPrimaryNav.setOnItemSelectedListener")
        val aloudBranch = primaryNavEvents.substringAfter("R.id.menu_read_aloud ->")
            .substringBefore("R.id.menu_read_interface ->")
        val aloudPanel = layout.elementByTag("panel_aloud")

        assertTrue(readMenu.contains("BottomTab.Aloud"))
        assertTrue(aloudBranch.contains("toggleBottomTab(BottomTab.Aloud)"))
        assertFalse(aloudBranch.contains("showReadAloudDialog"))
        assertEquals("panel_aloud", aloudPanel.androidAttr("tag"))
        assertTrue(layout.elementByTag("iv_aloud_play_pause").hasAncestor(aloudPanel))
        assertTrue(layout.elementByTag("seek_aloud_timer").hasAncestor(aloudPanel))
        assertTrue(readMenu.contains("taggedView(\"panel_aloud\").gone(tab != BottomTab.Aloud)"))
    }

    @Test
    fun readMenuSettingsPrimaryItemOpensInlinePanel() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val primaryNavEvents = readMenu.substringAfter("readBottomPrimaryNav.setOnItemSelectedListener")
        val settingsBranch = primaryNavEvents.substringAfter("R.id.menu_read_settings ->")
            .substringBefore("else -> false")
        val settingsPanel = layout.elementByTag("panel_settings")

        assertTrue(readMenu.contains("BottomTab.Settings"))
        assertTrue(settingsBranch.contains("toggleBottomTab(BottomTab.Settings)"))
        assertFalse(settingsBranch.contains("showMoreSetting"))
        assertEquals("panel_settings", settingsPanel.androidAttr("tag"))
        assertTrue(layout.elementByTag("panel_settings_list").hasAncestor(settingsPanel))
        assertTrue(readMenu.contains("taggedView(\"panel_settings\").gone(tab != BottomTab.Settings)"))
        assertTrue(readMenu.contains("renderSettingsPanel()"))
        assertFalse(readMenu.contains("MoreConfigDialog.ReadPreferenceFragment"))
    }

    @Test
    fun tocAndAloudPanelButtonsReserveReadableTextAndBorders() {
        val layout = readMenuLayout()

        assertEquals("52dp", layout.elementById("toc_progress_panel").androidAttr("layout_height"))
        assertEquals("false", layout.elementById("toc_progress_panel").androidAttr("clipChildren"))
        assertEquals("false", layout.elementById("toc_progress_panel").androidAttr("clipToPadding"))
        assertEquals("64dp", layout.elementById("toc_progress_mode_toggle").androidAttr("layout_width"))
        assertEquals("60dp", layout.elementById("tv_toc_prev_chapter").androidAttr("layout_width"))
        assertEquals("60dp", layout.elementById("tv_toc_next_chapter").androidAttr("layout_width"))
        assertEquals("38dp", layout.elementById("toc_progress_mode_toggle").androidAttr("layout_height"))
        assertEquals("36dp", layout.elementById("tv_toc_prev_chapter").androidAttr("layout_height"))
        assertEquals("36dp", layout.elementById("tv_toc_next_chapter").androidAttr("layout_height"))
        assertEquals("12sp", layout.elementById("toc_progress_mode_toggle").androidAttr("textSize"))

        assertEquals("56dp", layout.elementByTag("aloud_transport_panel").androidAttr("layout_height"))
        assertEquals("68dp", layout.elementByTag("tv_aloud_prev_chapter").androidAttr("layout_width"))
        assertEquals("68dp", layout.elementByTag("tv_aloud_next_chapter").androidAttr("layout_width"))
        assertEquals("42dp", layout.elementByTag("tv_aloud_prev_chapter").androidAttr("layout_height"))
        assertEquals("13sp", layout.elementByTag("tv_aloud_prev_chapter").androidAttr("textSize"))
        assertEquals("36dp", layout.elementByTag("iv_aloud_play_prev").androidAttr("layout_width"))
        assertEquals("42dp", layout.elementByTag("iv_aloud_play_pause").androidAttr("layout_width"))
        assertEquals("36dp", layout.elementByTag("iv_aloud_stop").androidAttr("layout_width"))
        assertEquals("36dp", layout.elementByTag("iv_aloud_play_next").androidAttr("layout_width"))
    }

    @Test
    fun tocPanelAddsBookmarkPageInsideSameFrostedSheet() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val tocPanel = layout.elementById("panel_toc")
        val tocHeader = layout.elementById("toc_header")
        val tabPanel = layout.elementByTag("toc_page_tabs")
        val contentHost = layout.elementByTag("toc_content_host")

        assertTrue(tabPanel.hasAncestor(tocPanel))
        assertTrue(tabPanel.hasAncestor(tocHeader))
        assertTrue(tabPanel.isBefore(layout.elementById("tv_panel_toc_count")))
        assertEquals("", tabPanel.androidAttr("background"))
        assertTrue(layout.elementByTag("tv_toc_tab_chapters").hasAncestor(tabPanel))
        assertTrue(layout.elementByTag("tv_toc_tab_bookmarks").hasAncestor(tabPanel))
        assertEquals("@string/chapter_list", layout.elementByTag("tv_toc_tab_chapters").androidAttr("text"))
        assertEquals("@string/bookmark", layout.elementByTag("tv_toc_tab_bookmarks").androidAttr("text"))
        assertTrue(layout.elementById("rv_panel_toc").hasAncestor(contentHost))
        assertTrue(layout.elementByTag("rv_panel_bookmarks").hasAncestor(contentHost))
        assertEquals("gone", layout.elementByTag("rv_panel_bookmarks").androidAttr("visibility"))
        assertEquals("@string/read_menu_bookmark_empty", layout.elementByTag("tv_panel_bookmarks_empty").androidAttr("text"))
        assertTrue(readMenu.contains("private enum class TocPanelPage"))
        assertTrue(readMenu.contains("ReadMenuBookmarkAdapter(::openBookmark)"))
        assertTrue(readMenu.contains("loadBookmarkPanel()"))
        assertTrue(readMenu.contains("appDb.bookmarkDao.getByBook(book.name, book.author)"))
    }

    @Test
    fun tocPanelListsUseFastDragScrollbars() {
        val layout = readMenuLayout()
        val tocRecycler = layout.elementById("rv_panel_toc")
        val bookmarkRecycler = layout.elementByTag("rv_panel_bookmarks")
        val fastScrollRecyclerView =
            "io.legado.app.ui.widget.recycler.scroller.FastScrollRecyclerView"

        listOf(tocRecycler, bookmarkRecycler).forEach { recycler ->
            assertEquals(fastScrollRecyclerView, recycler.tagName)
            assertEquals("none", recycler.androidAttr("scrollbars"))
            assertEquals("false", recycler.appAttr("fadeScrollbar"))
            assertEquals("true", recycler.appAttr("showTrack"))
            assertEquals("false", recycler.appAttr("showBubble"))
            assertEquals("18dp", recycler.androidAttr("paddingEnd"))
        }
    }

    @Test
    fun settingsPanelUsesNativeGlassRowsInsteadOfEmbeddedPreferenceSurface() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val settingsPanel = layout.elementByTag("panel_settings")
        val settingsScroll = layout.elementByTag("panel_settings_scroll")
        val settingsList = layout.elementByTag("panel_settings_list")

        assertTrue(settingsScroll.hasAncestor(settingsPanel))
        assertTrue(settingsList.hasAncestor(settingsScroll))
        assertEquals("", settingsScroll.androidAttr("background"))
        assertEquals("false", settingsScroll.androidAttr("clipToPadding"))
        assertTrue(readMenu.contains("private fun renderSettingsPanel()"))
        assertTrue(readMenu.contains("addReadSettingSwitch("))
        assertTrue(readMenu.contains("addReadSettingChoice("))
        assertTrue(readMenu.contains("addReadSettingAction("))
        assertTrue(readMenu.contains("row.background = readSettingRowBackground()"))
        assertFalse(readMenu.contains("PreferenceFragment"))
        assertFalse(readMenu.contains("ReadPreferenceFragment"))
    }

    @Test
    fun brightnessControlLivesInThemePanelInsteadOfSideRail() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val brightness = layout.elementById("ll_brightness")
        val layoutPanel = layout.elementById("panel_layout")
        val themePanel = layout.elementById("panel_theme")

        assertTrue(brightness.hasAncestor(themePanel))
        assertFalse(brightness.hasAncestor(layoutPanel))
        assertTrue(layout.elementById("seek_brightness").hasAncestor(brightness))
        assertEquals("io.legado.app.lib.theme.view.ThemeSeekBar", layout.elementById("seek_brightness").tagName)
        assertFalse(layoutXml.contains("vw_brightness_pos_adjust"))
        assertFalse(readMenu.contains("upBrightnessVwPos"))
        assertFalse(readMenu.contains("R.string.show_brightness_view"))
        assertFalse(readMenu.contains("binding.llBrightness.visible(showBrightnessView)"))
    }

    @Test
    fun bottomTabSelectedIndicatorDoesNotAutoHideActivePrimaryTab() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("private fun flashBottomTabIndicator(nav: BottomNavigationView, itemId: Int)"))
        assertTrue(readMenu.contains("showBottomTabIndicator(nav, itemId, animate = true, autoHide = false)"))
        assertFalse(readMenu.contains("showBottomTabIndicator(nav, itemId, animate = true, autoHide = true)"))
    }

    @Test
    fun readMenuTopBarAlwaysGetsOpaqueMenuSurface() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val initBlock = readMenu.substringAfter("private fun initView")
            .substringBefore("if (AppConfig.isEInkMode)")

        assertTrue(initBlock.contains("titleBar.setBackgroundColor(ColorUtils.withAlpha(bgColor, 1f))"))
        assertFalse(initBlock.contains("} else if (reset) {"))
    }

    @Test
    fun readerNavigationSpacerUsesBookBackground() {
        val activityLayout = parseXml(repoFile("app/src/main/res/layout/activity_book_read.xml"))
        val baseReadActivity = repoFile("app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt").readText()

        assertEquals("", activityLayout.elementById("navigation_bar").androidAttr("background"))
        assertTrue(baseReadActivity.contains("binding.navigationBar.setBackgroundColor("))
        assertTrue(baseReadActivity.contains("private fun updateNavigationSpacerBackground(color: Int = ReadBookConfig.bgMeanColor)"))
        assertTrue(baseReadActivity.contains("ColorUtils.withAlpha(color, 1f)"))
    }

    @Test
    fun readerNavigationBarAlwaysUsesBookBackgroundWithoutSystemContrastScrim() {
        val baseReadActivity = repoFile("app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val navBlock = baseReadActivity.substringAfter("override fun upNavigationBarColor()")
            .substringBefore("@SuppressLint(\"RtlHardcoded\")")

        assertTrue(navBlock.contains("updateReaderNavigationBarColor()"))
        assertFalse(navBlock.contains("super.upNavigationBarColor()"))
        assertTrue(baseReadActivity.contains("window.isNavigationBarContrastEnforced = false"))
        assertTrue(baseReadActivity.contains("private fun updateReaderNavigationBarColor()"))
        assertTrue(baseReadActivity.contains("val navigationColor = ReadBookConfig.bgMeanColor"))
        assertTrue(baseReadActivity.contains("setNavigationBarColorAuto(navigationColor)"))
        assertFalse(readMenu.contains("fun navigationBarSpacerColor(): Int"))
        assertFalse(readMenu.contains("bottomTabNavigationSurfaceColor"))
    }

    @Test
    fun tocDownloadButtonShowsPendingStateAndRefreshesWhenDownloadFinishes() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val readBook = repoFile("app/src/main/java/io/legado/app/model/ReadBook.kt").readText()
        val cacheBook = repoFile("app/src/main/java/io/legado/app/model/CacheBook.kt").readText()

        assertTrue(readMenu.contains("private val downloadingChapters = mutableSetOf<Int>()"))
        assertTrue(readMenu.contains("val downloading = chapter.index in downloadingChapters"))
        assertTrue(readMenu.contains("context.getString(R.string.downloading)"))
        assertTrue(readMenu.contains("downloadingChapters.add(chapter.index)"))
        assertTrue(readMenu.contains("notifyTocChapterChanged(chapter.index)"))
        assertTrue(readMenu.contains("downloadingChapters.remove(chapter.index)"))
        assertTrue(readBook.contains("download(\n                    downloadScope,\n                    chapter,\n                    resetPageOffset,\n                    success = success"))
        assertTrue(cacheBook.contains("finish: (() -> Unit)? = null"))
        assertTrue(cacheBook.contains("finish?.invoke()"))
    }

    @Test
    fun layoutPanelIncludesFontSamplesWithoutDuplicateFontTitle() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val fontPanel = layout.elementById("panel_layout_font")
        val fontRow = layout.elementById("ll_font_sample_row")

        assertFalse(layoutXml.contains("tv_theme_font_label"))
        assertTrue(fontRow.hasAncestor(fontPanel))
        assertTrue(readMenu.contains("private fun newFontSampleCard(withEndMargin: Boolean = true): ViewReadThemeCardBinding"))
        assertTrue(readMenu.contains("ViewReadThemeCardBinding.inflate("))
        assertTrue(readMenu.contains("binding.llFontSampleRow.addView(card.root, params)"))
        assertFalse(layoutXml.contains("font_card_source"))
        assertFalse(layoutXml.contains("font_card_add_custom"))
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
            "hsv_page_anim_cards",
            "ll_page_anim_card_row",
            "panel_page_auto_page",
            "panel_page_volume_key",
            "panel_page_mouse_wheel",
            "panel_page_touch_slop"
        ).forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(pageTurnPanel))
        }
    }

    @Test
    fun pageTurnPanelUsesAnimationCardsInsteadOfSecondaryMenu() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val pageTurnPanel = layout.elementById("panel_page_turn")

        listOf("hsv_page_anim_cards", "ll_page_anim_card_row").forEach { id ->
            assertTrue(layout.elementById(id).hasAncestor(pageTurnPanel))
        }
        listOf(
            "R.string.btn_default_s, null",
            "R.string.page_anim_cover, PageAnim.coverPageAnim",
            "R.string.page_anim_linked_cover, PageAnim.linkedCoverPageAnim",
            "R.string.page_anim_slide, PageAnim.slidePageAnim",
            "R.string.page_anim_simulation, PageAnim.simulationPageAnim",
            "R.string.page_anim_scroll, PageAnim.scrollPageAnim",
            "R.string.page_anim_none, PageAnim.noAnim"
        ).forEach { sampleConfig ->
            assertTrue(readMenu.contains(sampleConfig))
        }
        assertFalse(layoutXml.contains("android:id=\"@+id/panel_page_anim\""))
        assertFalse(layoutXml.contains("android:id=\"@+id/page_anim_card_"))
        assertTrue(readMenu.contains("private data class PageAnimSample"))
        assertTrue(readMenu.contains("private val pageAnimSampleBindings by lazy"))
        assertTrue(readMenu.contains("private fun newPageAnimSampleCard(withEndMargin: Boolean = true): ViewReadThemeCardBinding"))
        assertTrue(readMenu.contains("ViewReadThemeCardBinding.inflate("))
        assertTrue(readMenu.contains("binding.llPageAnimCardRow.addView(card.root, params)"))
        assertTrue(readMenu.contains("PageAnimPreviewDrawable"))
        assertTrue(readMenu.contains("private fun applyPageAnimSample(anim: Int?)"))
        assertFalse(readMenu.contains("panelPageAnim.setOnClickListener"))
        assertFalse(readMenu.contains("showPageAnimConfig"))
    }

    @Test
    fun backgroundPanelShowsImageSamplesAndContinuousControls() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val readActivity = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt").readText()
        val backgroundPanel = layout.elementById("panel_background")
        val colorCard = layout.elementByTag("background_card_color")
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
            .plus(colorCard)

        backgroundCards.forEach { card ->
            assertTrue(card.getAttribute("layout").contains("view_read_background_card"))
            assertTrue(card.hasAncestor(backgroundPanel))
        }
        assertTrue(colorCard.isBefore(layout.elementById("background_card_beach")))
        assertTrue(layout.elementById("seek_background_brightness").hasAncestor(backgroundPanel))
        assertTrue(layout.elementById("seek_background_saturation").hasAncestor(backgroundPanel))
        assertTrue(layout.elementById("seek_background_alpha").hasAncestor(backgroundPanel))
        assertTrue(readMenu.contains("午后沙滩.jpg"))
        assertTrue(readMenu.contains("羊皮纸4.jpg"))
        assertTrue(readMenu.contains("ReadBookConfig.bgAlpha"))
        assertTrue(readMenu.contains("bindBackgroundColorCard("))
        assertTrue(readMenu.contains("binding.llBackgroundImageRow.getChildAt(0)"))
        assertTrue(readMenu.contains("ReadBookConfig.durConfig.setCurBg(0"))
        assertTrue(readMenu.contains("setDialogId(BG_COLOR)"))
        assertTrue(readMenu.contains("context.accentColor"))
        assertTrue(readActivity.contains("BG_COLOR ->"))
        assertTrue(readActivity.contains("binding.readMenu.reset()"))
    }

    @Test
    fun backgroundCardsUseThemeBorderForSelectedState() {
        val cardLayout = parseXml(
            findRepoFile("app/src/main/res/layout/view_read_background_card.xml")
                ?: findRepoFile("src/main/res/layout/view_read_background_card.xml")
                ?: error("view_read_background_card.xml not found")
        )
        val preview = cardLayout.elementById("background_card_preview")
        val border = cardLayout.elementById("background_card_selection_border")
        val check = cardLayout.elementById("iv_background_card_check")
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(border.hasAncestor(preview))
        assertEquals("gone", border.androidAttr("visibility"))
        assertTrue(border.isBefore(check))
        assertTrue(readMenu.contains("backgroundCardSelectionBorder.background = roundedRect"))
        assertTrue(readMenu.contains("backgroundCardSelectionBorder.isVisible = selected"))
        assertTrue(readMenu.contains("if (selected) context.accentColor"))
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
    fun tocDragHandleKeepsDraggedHeightBelowFullscreenThreshold() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val tocSetup = readMenu.substringAfter("private fun setupTocPanel()")
            .substringBefore("private fun setTocPanelPage")

        assertTrue(tocSetup.contains("tocDragHandle.setOnTouchListener"))
        assertFalse(tocSetup.contains("rvPanelToc.setOnTouchListener"))
        assertFalse(tocSetup.contains("taggedRecycler(\"rv_panel_bookmarks\").setOnTouchListener"))
        assertTrue(readMenu.contains("private fun settleTocPanelDrag()"))
        assertTrue(readMenu.contains("val currentHeight = binding.flExpandedPanel.height"))
        assertTrue(readMenu.contains("val thresholdHeight = tocFullscreenThresholdHeight()"))
        assertTrue(readMenu.contains("animateTocPanelTo(currentHeight)"))
        assertFalse(readMenu.contains("val middleHeight = (tocDefaultPanelHeight() + tocFullPanelHeight()) / 2"))
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
    fun themePanelUsesCardRailForThemeSelectionAndAddAction() {
        val layout = readMenuLayout()
        val layoutXml = repoFile("app/src/main/res/layout/view_read_menu.xml").readText()
        val row = layout.elementById("ll_theme_preset_row")
        val nightCard = layout.elementById("theme_card_night")
        val addCard = layout.elementById("theme_card_add")

        assertFalse(layoutXml.contains("ll_theme_tabs"))
        assertFalse(layoutXml.contains("ll_theme_tab_preset"))
        assertFalse(layoutXml.contains("ll_theme_tab_custom"))
        assertFalse(layoutXml.contains("ll_theme_tab_eye"))
        assertFalse(layoutXml.contains("@string/read_menu_theme_mine"))
        assertFalse(layoutXml.contains("@string/read_menu_theme_save_current"))
        assertFalse(layoutXml.contains("@string/read_menu_theme_eye_mode"))
        assertTrue(nightCard.isBefore(addCard))
        assertTrue(addCard.hasAncestor(row))
        assertEquals("96dp", addCard.androidAttr("layout_width"))
        assertEquals("8dp", nightCard.androidAttr("layout_marginEnd"))
        assertEquals("", addCard.androidAttr("layout_marginEnd"))
    }

    @Test
    fun themePresetAppliesLayoutPageTurnAndBackgroundSuite() {
        val presetModel = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemePreset.kt").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(presetModel.contains("layoutLabelRes"))
        assertTrue(presetModel.contains("pageTurnLabelRes"))
        assertTrue(presetModel.contains("backgroundLabelRes"))
        assertTrue(presetModel.contains("fun summaryText"))
        assertTrue(presetModel.contains("pageAnimationSpeed"))
        assertTrue(readMenu.contains("bindThemeCardPreviewText(card, preset.textColor)"))
        assertTrue(readMenu.contains("ReadBookConfig.textSize = preset.textSize"))
        assertTrue(readMenu.contains("ReadBookConfig.lineSpacingExtra = preset.lineSpacingExtra"))
        assertTrue(readMenu.contains("ReadBookConfig.paragraphSpacing = preset.paragraphSpacing"))
        assertTrue(readMenu.contains("ReadBookConfig.pageAnim = preset.pageAnim"))
        assertTrue(readMenu.contains("AppConfig.pageAnimationSpeed = preset.pageAnimationSpeed"))
        assertTrue(readMenu.contains("ReadBookConfig.bgBrightness = preset.bgBrightness"))
    }

    @Test
    fun themeSuiteSummaryStaysCompactInsideNarrowPresetCards() {
        val defaultStrings = repoFile("app/src/main/res/values/strings.xml").readText()

        assertTrue(defaultStrings.contains("""<string name="read_menu_theme_suite_summary">%1${'$'}s / %2${'$'}s\n%3${'$'}s</string>"""))
        assertFalse(defaultStrings.contains("Layout: %1${'$'}s / Turn: %2${'$'}s"))
    }

    @Test
    fun themeSaveCurrentAndMyThemesHaveReaderSuiteEntryPoints() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val suiteStore = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemeSuite.kt")

        assertTrue(suiteStore.exists())
        val suiteStoreText = suiteStore.readText()
        assertTrue(suiteStoreText.contains("object ReadMenuThemeSuiteStore"))
        assertTrue(suiteStoreText.contains("fun captureCurrent"))
        assertTrue(suiteStoreText.contains("fun applyToReader"))
        assertTrue(readMenu.contains("saveCurrentThemeSuite()"))
        assertTrue(readMenu.contains("bindThemeAddCard("))
        assertTrue(readMenu.contains("binding.themeCardAdd.root.setOnClickListener"))
        assertTrue(readMenu.contains("DialogEditTextBinding.inflate"))
        assertFalse(readMenu.contains("ThemeTab"))
        assertFalse(readMenu.contains("showSavedThemeSuites"))
        assertFalse(readMenu.contains("llThemeTab"))
    }

    @Test
    fun currentThemeAlwaysHasASelectedNonAddCard() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val suiteStore = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemeSuite.kt").readText()

        assertTrue(suiteStore.contains("fun matchesCurrentTheme"))
        assertTrue(readMenu.contains("val currentSuite = ReadMenuThemeSuiteStore.captureCurrent"))
        assertTrue(readMenu.contains("val needsCurrentCard = selectedPresetIndex == -1 && selectedSavedIndex == -1"))
        assertTrue(readMenu.contains("currentSuite.takeIf { needsCurrentCard }"))
        assertTrue(readMenu.contains("ThemeSuiteCard(it, true, false)"))
        assertTrue(readMenu.contains("bindThemeAddCard(binding.themeCardAdd)"))
    }

    @Test
    fun themeRailChoosesOnlyOneSelectedThemeCard() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("val selectedPresetIndex = if (explicitSavedIndex == -1)"))
        assertTrue(readMenu.contains("val selectedSavedIndex = when"))
        assertTrue(readMenu.contains("presetIndex == selectedPresetIndex"))
        assertTrue(readMenu.contains("savedIndex == selectedSavedIndex"))
        assertFalse(readMenu.contains("savedSuites.any { it.matchesCurrentTheme() }"))
    }

    @Test
    fun themeRailRemembersExplicitSavedCardSelectionForDuplicateSuites() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val suiteStore = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemeSuite.kt").readText()

        assertTrue(suiteStore.contains("ACTIVE_PREF_KEY"))
        assertTrue(suiteStore.contains("fun explicitSavedIndex"))
        assertTrue(suiteStore.contains("fun selectedSavedIndex"))
        assertTrue(suiteStore.contains("it.createdAt == activeCreatedAt"))
        assertTrue(readMenu.contains("ReadMenuThemeSuiteStore.selectedSavedIndex(context, savedSuites)"))
        assertTrue(readMenu.contains("ReadMenuThemeSuiteStore.select(context, suite)"))
    }

    @Test
    fun themeRailLetsExplicitSavedThemeWinOverMatchingPreset() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("val explicitSavedIndex = ReadMenuThemeSuiteStore.explicitSavedIndex(context, savedSuites)"))
        assertTrue(readMenu.contains("val selectedPresetIndex = if (explicitSavedIndex == -1)"))
        assertTrue(readMenu.contains("explicitSavedIndex != -1 -> explicitSavedIndex"))
    }

    @Test
    fun savingDuplicateThemeSelectsExistingThemeInsteadOfCreatingAnotherCard() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val suiteStore = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemeSuite.kt").readText()

        assertTrue(suiteStore.contains("fun matchingSuite"))
        assertTrue(suiteStore.contains("fun saveOrSelectExisting"))
        assertTrue(suiteStore.contains("load(context).firstOrNull { it.matches(suite) }"))
        assertTrue(readMenu.contains("ReadMenuThemeSuiteStore.saveOrSelectExisting(context, suite)"))
        assertFalse(readMenu.contains("ReadMenuThemeSuiteStore.save(context, suite)\n                ReadMenuThemeSuiteStore.select(context, suite)"))
    }

    @Test
    fun savingThemeThatMatchesPresetStillCreatesCustomTheme() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertFalse(readMenu.contains("themePresets.any { preset ->"))
        assertFalse(readMenu.contains("ReadMenuThemeSuiteStore.clearSelection(context)\n                    updateThemePresetCards()\n                    return@okButton"))
        assertTrue(readMenu.contains("val selectedSuite = ReadMenuThemeSuiteStore.saveOrSelectExisting(context, suite)"))
        assertTrue(readMenu.contains("context.toastOnUi(context.getString(R.string.read_menu_theme_saved, selectedSuite.name))"))
    }

    @Test
    fun themePreviewCardsUseConsistentSampleTextForPresetAndSavedThemes() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val presetModel = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemePreset.kt").readText()

        assertTrue(presetModel.contains("DEFAULT_PREVIEW_TITLE"))
        assertTrue(presetModel.contains("DEFAULT_PREVIEW_BODY"))
        assertTrue(readMenu.contains("bindThemeCardPreviewText(card, preset.textColor)"))
        assertTrue(readMenu.contains("bindThemeCardPreviewText(card, suite.textColor)"))
        assertTrue(readMenu.contains("card.tvThemeCardTitle.text = ReadMenuThemePreset.DEFAULT_PREVIEW_TITLE"))
        assertTrue(readMenu.contains("card.tvThemeCardBody.text = ReadMenuThemePreset.DEFAULT_PREVIEW_BODY"))
        assertFalse(readMenu.contains("card.tvThemeCardTitle.text = suite.name"))
        assertFalse(readMenu.contains("card.tvThemeCardBody.text = suite.summaryText(context)"))
    }

    @Test
    fun selectedSavedThemeShowsDeleteIconAfterCard() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val suiteStore = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenuThemeSuite.kt").readText()

        assertTrue(readMenu.contains("selectedThemeDeleteButton"))
        assertTrue(readMenu.contains("showSelectedThemeDeleteButton("))
        assertTrue(readMenu.contains("R.drawable.ic_outline_delete"))
        assertTrue(readMenu.contains("context.getCompatColor(R.color.error)"))
        assertFalse(readMenu.contains("imageTintList = ColorStateList.valueOf(context.accentColor)"))
        assertTrue(readMenu.contains("deleteThemeSuite(suite)"))
        assertTrue(readMenu.contains("llThemePresetRow.indexOfChild(selectedCard.root) + 1"))
        assertTrue(readMenu.contains("hsvThemePresets.post"))
        assertTrue(readMenu.contains("button.right > visibleRight"))
        assertTrue(suiteStore.contains("fun delete"))
        assertTrue(suiteStore.contains("filterNot { it.createdAt == suite.createdAt }"))
    }

    @Test
    fun selectedSavedThemeDeleteIconRevealsAfterAnimatedSpaceOpens() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("selectedThemeDeleteButtonAnimator"))
        assertTrue(readMenu.contains("button.alpha = 0f"))
        assertTrue(readMenu.contains("button.isEnabled = false"))
        assertTrue(readMenu.contains("LinearLayout.LayoutParams(0, 70.dpToPx())"))
        assertTrue(readMenu.contains("ValueAnimator.ofInt(0, 42.dpToPx())"))
        assertTrue(readMenu.contains("button.animate()"))
        assertTrue(readMenu.contains(".alpha(1f)"))
        assertFalse(readMenu.contains("LinearLayout.LayoutParams(42.dpToPx(), 70.dpToPx()).apply"))
    }

    @Test
    fun savedThemeCardsShowCustomBadgeButPresetAndCurrentCardsDoNot() {
        val cardLayout = repoFile("app/src/main/res/layout/view_read_theme_card.xml").readText()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val zhStrings = repoFile("app/src/main/res/values-zh/strings.xml").readText()

        assertTrue(cardLayout.contains("@+id/tv_theme_card_badge"))
        assertTrue(cardLayout.contains("@string/read_menu_theme_custom_badge"))
        assertTrue(zhStrings.contains("<string name=\"read_menu_theme_custom_badge\">自定义</string>"))
        assertTrue(readMenu.contains("private data class ThemeSuiteCard"))
        assertTrue(readMenu.contains("val custom: Boolean"))
        assertTrue(readMenu.contains("savedSuites.mapIndexed { savedIndex, suite ->"))
        assertTrue(readMenu.contains("ThemeSuiteCard(suite, savedIndex == selectedSavedIndex, true)"))
        assertTrue(readMenu.contains("ThemeSuiteCard(it, true, false)"))
        assertTrue(readMenu.contains("card.tvThemeCardBadge.isVisible = false"))
        assertTrue(readMenu.contains("card.tvThemeCardBadge.isVisible = custom"))
    }

    @Test
    fun themeAddCardUsesCenteredLargePlusWithoutPreviewSummary() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("card.tvThemeCardBody.isGone = true"))
        assertTrue(readMenu.contains("card.tvThemeCardTitle.gravity = Gravity.CENTER"))
        assertTrue(readMenu.contains("card.tvThemeCardTitle.textSize = 28f"))
        assertFalse(readMenu.contains("card.tvThemeCardBody.setText(R.string.read_menu_theme_add_summary)"))
    }

    @Test
    fun customFontAddCardUsesSameCenteredPlusStyleAsThemeAddCard() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("bindFontAddCard(card)"))
        assertFalse(readMenu.contains("card.tvThemeCardBody.setText(R.string.read_style_font_custom)"))
    }

    @Test
    fun chapterLayoutKeyIncludesTypefaceInputs() {
        val readBook = repoFile("app/src/main/java/io/legado/app/model/ReadBook.kt").readText()

        assertTrue(readBook.contains("append(ReadBookConfig.textFont)"))
        assertTrue(readBook.contains("append(AppConfig.systemTypefaces)"))
        assertTrue(readBook.contains("append(ReadBookConfig.textWeight)"))
        assertTrue(readBook.contains("append(paint.letterSpacing)"))
        assertTrue(readBook.contains("append(titlePaint.letterSpacing)"))
    }

    @Test
    fun fontCardsUseTallerPreviewSizingThanThemeCards() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("height = 82.dpToPx()"))
    }

    @Test
    fun fontControlsBelongToLayoutPanelOnly() {
        val layout = readMenuLayout()
        val layoutPanel = layout.elementById("panel_layout")
        val themePanel = layout.elementById("panel_theme")
        val previewHost = layout.elementById("layout_margin_adjust_preview_host")
        val fontRow = layout.elementById("ll_font_sample_row")
        val textStyleControls = listOf(
            "seek_theme_font_weight",
            "seek_theme_text_size"
        ).map { id -> layout.elementById(id) }

        assertTrue(fontRow.hasAncestor(layoutPanel))
        assertFalse(fontRow.hasAncestor(themePanel))
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
        assertEquals("220dp", previewHost.androidAttr("layout_height"))
        assertTrue(overlayPanel.hasAncestor(overlay))
        assertTrue(previewHost.hasAncestor(overlayPanel))
        assertEquals("io.legado.app.ui.book.read.ReadMarginPreviewView", previewFrame.tagName)
        assertEquals("142dp", previewFrame.androidAttr("layout_width"))
        assertEquals("142dp", previewFrame.androidAttr("layout_height"))
        assertEquals("0dp", layout.elementById("layout_margin_adjust_glass_view").androidAttr("layout_height"))
        assertEquals("0dp", layout.elementById("layout_margin_adjust_shell_overlay").androidAttr("layout_height"))

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
        assertEquals(
            layout.elementById("tv_layout_padding_top_label").androidAttr("layout_marginTop"),
            layout.elementById("tv_layout_padding_bottom_label").androidAttr("layout_marginTop")
        )
        assertEquals(
            layout.elementById("layout_margin_spinbox_top_field").androidAttr("layout_marginTop"),
            layout.elementById("layout_margin_spinbox_bottom_field").androidAttr("layout_marginTop")
        )
        assertEquals(
            layout.elementById("tv_layout_padding_left_label").androidAttr("layout_marginTop"),
            layout.elementById("tv_layout_padding_right_label").androidAttr("layout_marginTop")
        )
        assertEquals(
            layout.elementById("layout_margin_spinbox_left_field").androidAttr("layout_marginTop"),
            layout.elementById("layout_margin_spinbox_right_field").androidAttr("layout_marginTop")
        )

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
        assertEquals("wrap_content", layout.elementById("layout_tip_controls").androidAttr("layout_height"))
        assertEquals("", layout.elementById("layout_tip_controls").androidAttr("scrollbars"))
        assertEquals("", layout.elementById("layout_tip_controls")
            .appAttr("layout_constraintTop_toTopOf"))
        assertEquals("@id/layout_margin_adjust_preview", layout.elementById("layout_tip_controls")
            .appAttr("layout_constraintTop_toBottomOf"))
        assertEquals("", layout.elementById("layout_tip_controls")
            .appAttr("layout_constraintBottom_toBottomOf"))
        assertTrue(layout.elementById("layout_header_display_auto_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementById("layout_header_display_show_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementByTag("layout_header_display_hide_card").hasAncestor(layout.elementById("layout_tip_header_controls")))
        assertTrue(layout.elementByTag("layout_footer_display_auto_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        assertTrue(layout.elementById("layout_footer_display_show_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        assertTrue(layout.elementById("layout_footer_display_hide_card").hasAncestor(layout.elementById("layout_tip_footer_controls")))
        listOf(
            layout.elementById("layout_header_display_auto_card"),
            layout.elementById("layout_header_display_show_card"),
            layout.elementByTag("layout_header_display_hide_card"),
            layout.elementByTag("layout_footer_display_auto_card"),
            layout.elementById("layout_footer_display_show_card"),
            layout.elementById("layout_footer_display_hide_card")
        ).forEach { card ->
            assertEquals("wrap_content", card.androidAttr("layout_height"))
            assertEquals("64dp", card.androidAttr("minHeight"))
            assertEquals("10dp", card.androidAttr("paddingTop"))
            assertEquals("10dp", card.androidAttr("paddingBottom"))
        }
        listOf(
            "@string/read_menu_header_auto_summary",
            "@string/read_menu_header_show_summary",
            "@string/read_menu_header_hide_summary",
            "@string/read_menu_footer_auto_summary",
            "@string/read_menu_footer_show_summary",
            "@string/read_menu_footer_hide_summary"
        ).forEach { text ->
            val summary = layout.elementsByAndroidText(text).single()
            assertEquals("2", summary.androidAttr("maxLines"))
            assertEquals("", summary.androidAttr("singleLine"))
            assertEquals("end", summary.androidAttr("ellipsize"))
        }
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
        assertTrue(readMenu.contains("syncLayoutAdjustPopupMetrics"))
        assertTrue(readMenu.contains("setLayoutAdjustGlassLayerHeight"))
        assertTrue(readMenu.contains("layoutTipHeaderControls.gone(!showHeaderMode)"))
        assertTrue(readMenu.contains("layoutTipFooterControls.gone(!showFooterMode)"))
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
    fun bodyMarginAdjustPopupReflowsFourSpinboxesAroundPreview() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("syncLayoutMarginSpinboxLayout(showHorizontal)"))
        assertTrue(readMenu.contains("private fun syncLayoutMarginSpinboxLayout(useBodyCrossLayout: Boolean)"))
        assertTrue(readMenu.contains("LayoutMarginAdjustMode.Body -> 300.dpToPx()"))
        assertTrue(readMenu.contains("layoutMarginSpinboxTop.updateLayoutParams<ConstraintLayout.LayoutParams>"))
        assertTrue(readMenu.contains("bottomToTop = R.id.layout_margin_adjust_preview"))
        assertTrue(readMenu.contains("layoutMarginSpinboxBottom.updateLayoutParams<ConstraintLayout.LayoutParams>"))
        assertTrue(readMenu.contains("topToBottom = R.id.layout_margin_adjust_preview"))
        assertTrue(readMenu.contains("layoutMarginSpinboxLeft.updateLayoutParams<ConstraintLayout.LayoutParams>"))
        assertTrue(readMenu.contains("endToStart = R.id.layout_margin_adjust_preview"))
        assertTrue(readMenu.contains("layoutMarginSpinboxRight.updateLayoutParams<ConstraintLayout.LayoutParams>"))
        assertTrue(readMenu.contains("startToEnd = R.id.layout_margin_adjust_preview"))
    }

    @Test
    fun layoutAdjustOverlayUsesBottomTabFrostedGlassRecipe() {
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()

        assertTrue(readMenu.contains("layoutAdjustGlassShell(glassLevel, cornerRadius)"))
        assertTrue(readMenu.contains("layoutAdjustGlassFallbackShell(glassLevel, cornerRadius)"))
        assertTrue(readMenu.contains("return readBottomTabGlassShell(glassLevel, cornerRadius)"))
        assertTrue(readMenu.contains("return readBottomTabGlassFallbackShell(glassLevel, cornerRadius)"))
        assertTrue(readMenu.contains("refractionHeight = (12f + glassLevel * 8f).dpToPx()"))
        assertTrue(readMenu.contains("refractionOffset = (34f + glassLevel * 18f).dpToPx()"))
        assertTrue(readMenu.contains("blurRadius = (22f + glassLevel * 30f).dpToPx()"))
        assertTrue(readMenu.contains("tintAlpha = bottomTabGlassTintAlpha(glassLevel)"))
        assertFalse(
            Regex("""private fun bottomTabGlassShell\(glassLevel: Float\): GradientDrawable \{\s*return layoutAdjustGlassShell""")
                .containsMatchIn(readMenu)
        )
    }

    @Test
    fun layoutAdjustOverlayCapturesBlurredBackdropBeforeShowingPanel() {
        val layout = readMenuLayout()
        val readMenu = repoFile("app/src/main/java/io/legado/app/ui/book/read/ReadMenu.kt").readText()
        val panel = layout.elementById("layout_margin_adjust_panel")
        val blurBackdrop = layout.elementById("layout_margin_adjust_blur_backdrop")
        val glassView = layout.elementById("layout_margin_adjust_glass_view")

        assertTrue(blurBackdrop.hasAncestor(panel))
        assertEquals("ImageView", blurBackdrop.tagName)
        assertEquals("0dp", blurBackdrop.androidAttr("layout_height"))
        assertTrue(blurBackdrop.isBefore(glassView))
        assertTrue(readMenu.contains("captureLayoutAdjustBackdrop()"))
        assertTrue(readMenu.contains("updateLayoutAdjustBlurBackdrop()"))
        assertTrue(readMenu.contains("clearLayoutAdjustBackdrop()"))
        assertTrue(readMenu.contains("layoutMarginAdjustBlurBackdrop.setImageBitmap"))
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

    private fun Element.elementsByAndroidText(text: String): List<Element> {
        val children = childNodes
        return buildList {
            if (androidAttr("text") == text) {
                add(this@elementsByAndroidText)
            }
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child is Element) {
                    addAll(child.elementsByAndroidText(text))
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
