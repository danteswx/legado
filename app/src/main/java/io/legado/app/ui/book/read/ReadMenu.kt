package io.legado.app.ui.book.read

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qmdeve.liquidglass.widget.LiquidGlassView
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ViewReadThemeCardBinding
import io.legado.app.databinding.ViewReadMenuBinding
import io.legado.app.databinding.ViewReadBackgroundCardBinding
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AdvancedTitleConfig
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ReadTipConfig
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.source.getSourceType
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.buttonDisabledColor
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.font.FontSelectDialog
import io.legado.app.ui.book.searchContent.SearchContentAdapter
import io.legado.app.ui.book.searchContent.SearchContentViewModel
import io.legado.app.ui.book.searchContent.SearchResult
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.ConstraintModify
import io.legado.app.utils.activity
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.gone
import io.legado.app.utils.hexString
import io.legado.app.utils.invisible
import io.legado.app.utils.loadAnimation
import io.legado.app.utils.modifyBegin
import io.legado.app.utils.openUrl
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import splitties.views.onClick
import java.io.File
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var canShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private var isMenuOutAnimating = false
    private enum class BottomTab {
        Search,
        Toc,
        Layout,
        PageTurn,
        Background,
        Theme
    }

    private enum class BottomTabMode {
        Primary,
        Interface
    }

    private enum class ThemeTab {
        Preset,
        Custom,
        Eye
    }

    private var activeBottomTab: BottomTab? = null
    private var bottomTabMode: BottomTabMode = BottomTabMode.Primary
    private var suppressBottomNavSelection: Boolean = false
    private var bottomTabPanelAttached: Boolean = false
    private var bottomTabHeightAnimator: ValueAnimator? = null
    private var bottomTabGlassStyleKey: String? = null
    private var bottomTabGlassLayerHeight: Int = 0
    private var tocLoadJob: Coroutine<List<BookChapter>>? = null
    private var tocPanelFullscreen: Boolean = false
    private var tocDragStartY: Float = 0f
    private var tocDragStartPanelHeight: Int = 0
    private val boundBottomTabGlassViewIds = hashSetOf<Int>()
    private val tocAdapter by lazy { ReadMenuTocAdapter(::openTocChapter) }
    private val searchPanelAdapter by lazy {
        SearchContentAdapter(context, object : SearchContentAdapter.Callback {
            override fun openSearchResult(searchResult: SearchResult, index: Int) {
                if (searchResult.query.isBlank()) {
                    return
                }
                callBack.openInlineSearchResult(
                    searchResult,
                    searchPanelResults.toList(),
                    index
                )
            }

            override fun durChapterIndex(): Int = ReadBook.durChapterIndex
        })
    }
    private val searchPanelResults = arrayListOf<SearchResult>()
    private var searchPanelJob: Coroutine<List<SearchResult>>? = null
    private var tocProgressWholeBook: Boolean = false
    private var activeThemeTab: ThemeTab = ThemeTab.Preset
    private data class FontSample(
        val binding: ViewReadThemeCardBinding,
        val titleRes: Int,
        val typeface: Typeface,
        val isSelected: () -> Boolean,
        val onClick: () -> Unit
    )

    private data class BackgroundSample(
        val binding: ViewReadBackgroundCardBinding,
        val assetName: String
    )

    private val fontSampleBindings by lazy {
        listOf(
            FontSample(
                binding.fontCardSource,
                R.string.read_style_font_source,
                Typeface.SERIF,
                { ReadBookConfig.textFont.isBlank() && AppConfig.systemTypefaces == 0 },
                { setSystemFont(0) }
            ),
            FontSample(
                binding.fontCardSans,
                R.string.read_style_font_sans,
                Typeface.SANS_SERIF,
                { ReadBookConfig.textFont.isBlank() && AppConfig.systemTypefaces == 1 },
                { setSystemFont(1) }
            ),
            FontSample(
                binding.fontCardArt,
                R.string.read_style_font_art,
                Typeface.DEFAULT_BOLD,
                { ReadBookConfig.textFont.isBlank() && AppConfig.systemTypefaces == 2 },
                { setSystemFont(2) }
            ),
            FontSample(
                binding.fontCardCustom,
                R.string.read_style_font_custom,
                Typeface.DEFAULT,
                { ReadBookConfig.textFont.isNotBlank() },
                { openFontSelectDialog() }
            ),
            FontSample(
                binding.fontCardAddCustom,
                R.string.read_menu_add_custom_font,
                Typeface.DEFAULT,
                { false },
                { openFontSelectDialog() }
            )
        )
    }
    private val backgroundSampleBindings by lazy {
        listOf(
            BackgroundSample(binding.backgroundCardBeach, "午后沙滩.jpg"),
            BackgroundSample(binding.backgroundCardNight, "宁静夜色.jpg"),
            BackgroundSample(binding.backgroundCardGreen, "护眼漫绿.jpg"),
            BackgroundSample(binding.backgroundCardInk, "山水墨影.jpg"),
            BackgroundSample(binding.backgroundCardParchment, "羊皮纸4.jpg"),
            BackgroundSample(binding.backgroundCardNewParchment, "新羊皮纸.jpg"),
            BackgroundSample(binding.backgroundCardParchment1, "羊皮纸1.jpg"),
            BackgroundSample(binding.backgroundCardParchment2, "羊皮纸2.jpg"),
            BackgroundSample(binding.backgroundCardParchment3, "羊皮纸3.jpg"),
            BackgroundSample(binding.backgroundCardFresh, "清新时光.jpg"),
            BackgroundSample(binding.backgroundCardPalace, "深宫魅影.jpg"),
            BackgroundSample(binding.backgroundCardCanvas, "边彩画布.jpg"),
            BackgroundSample(binding.backgroundCardLandscape, "山水画.jpg"),
            BackgroundSample(binding.backgroundCardBright, "明媚倾城.jpg")
        )
    }
    private val themePresets by lazy { ReadMenuThemePreset.defaultPresets() }
    private val themeCardBindings by lazy {
        listOf(
            binding.themeCardFollowSystem,
            binding.themeCardDark,
            binding.themeCardPaper,
            binding.themeCardEyeGreen,
            binding.themeCardQuietBlue,
            binding.themeCardNight
        )
    }
    private val menuTopIn: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_top_in)
    }
    private val menuTopOut: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_top_out)
    }
    private val menuBottomIn: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_bottom_in)
    }
    private val menuBottomOut: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_bottom_out)
    }
    private val panelIn: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_panel_in_up)
    }
    private val panelOut: Animation by lazy {
        loadAnimation(context, R.anim.anim_readbook_panel_out_down)
    }
    private val bottomTabIndicatorInterpolator = DecelerateInterpolator(1.8f)
    private val bottomTabSlideInterpolator = DecelerateInterpolator(1.4f)
    private val bottomTabPulseInterpolator = AccelerateDecelerateInterpolator()
    private val hideBottomTabIndicatorRunnable = Runnable {
        fadeBottomTabIndicator()
    }
    private val immersiveMenu: Boolean
        get() = AppConfig.readBarStyleFollowPage && ReadBookConfig.durConfig.curBgType() == 0
    private var bgColor: Int = if (immersiveMenu) {
        kotlin.runCatching {
            Color.parseColor(ReadBookConfig.durConfig.curBgStr())
        }.getOrDefault(context.bottomBackground)
    } else {
        context.bottomBackground
    }
    private var textColor: Int = if (immersiveMenu) {
        ReadBookConfig.durConfig.curTextColor()
    } else {
        context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    }

    private var bottomBackgroundList: ColorStateList = Selector.colorBuild()
        .setDefaultColor(bgColor)
        .setPressedColor(ColorUtils.darkenColor(bgColor))
        .create()
    private var onMenuOutEnd: (() -> Unit)? = null
    private val showBrightnessView
        get() = context.getPrefBoolean(
            PreferKey.showBrightnessView,
            true
        )
    private val sourceMenu by lazy {
        PopupMenu(context, binding.tvSourceAction).apply {
            inflate(R.menu.book_read_source)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_login -> callBack.showLogin()
                    R.id.menu_chapter_pay -> callBack.payAction()
                    R.id.menu_edit_source -> callBack.openSourceEditActivity()
                    R.id.menu_disable_source -> callBack.disableSource()
                }
                true
            }
        }
    }
    private val menuInListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            binding.tvSourceAction.text =
                ReadBook.bookSource?.bookSourceName ?: context.getString(R.string.book_source)
            binding.tvSourceAction.isGone = ReadBook.isLocalBook
            callBack.upSystemUiVisibility()
            binding.llBrightness.visible(showBrightnessView)
        }

        @SuppressLint("RtlHardcoded")
        override fun onAnimationEnd(animation: Animation) {
            binding.vwMenuBg.setOnClickListener { handleBackgroundDismiss() }
            callBack.upSystemUiVisibility()
            if (!LocalConfig.readMenuHelpVersionIsLast) {
                callBack.showHelp()
            }
        }

        override fun onAnimationRepeat(animation: Animation) = Unit
    }
    private val menuOutListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {
            isMenuOutAnimating = true
            binding.vwMenuBg.setOnClickListener(null)
        }

        override fun onAnimationEnd(animation: Animation) {
            this@ReadMenu.invisible()
            binding.titleBar.invisible()
            binding.bottomMenu.invisible()
            binding.flExpandedPanel.gone()
            binding.flExpandedPanel.alpha = 0f
            setBottomTabBarHeight(bottomTabCollapsedHeight())
            activeBottomTab = null
            switchBottomTabMode(BottomTabMode.Primary, animate = false)
            renderBottomTabState()
            canShowMenu = false
            isMenuOutAnimating = false
            onMenuOutEnd?.invoke()
            callBack.upSystemUiVisibility()
        }

        override fun onAnimationRepeat(animation: Animation) = Unit
    }

    init {
        initView()
        upBrightnessState()
        bindEvent()
    }

    private fun initView(reset: Boolean = false) = binding.run {
        initAnimation()
        setupExpandableBottomTabContainer()
        setupSearchPanel()
        setupTocPanel()
        if (immersiveMenu) {
            val lightTextColor = ColorUtils.withAlpha(ColorUtils.lightenColor(textColor), 0.75f)
            titleBar.setTextColor(textColor)
            titleBar.setBackgroundColor(bgColor)
            titleBar.setColorFilter(textColor)
            tvChapterName.setTextColor(lightTextColor)
            tvChapterUrl.setTextColor(lightTextColor)
        } else if (reset) {
            val bgColor = context.primaryColor
            val textColor = context.primaryTextColor
            titleBar.setTextColor(textColor)
            titleBar.setBackgroundColor(bgColor)
            titleBar.setColorFilter(textColor)
            tvChapterName.setTextColor(textColor)
            tvChapterUrl.setTextColor(textColor)
        }
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dpToPx()
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        llBrightness.background = brightnessBackground
        if (AppConfig.isEInkMode) {
            titleBar.setBackgroundResource(R.drawable.bg_eink_border_bottom)
        }
        flExpandedPanel.background = null
        flExpandedPanel.elevation = 0f
        tvPanelTocTitle.setTextColor(textColor)
        tvPanelTocCount.setTextColor(textColor)
        tocProgressModeToggle.setTextColor(textColor)
        tvTocPrevChapter.setTextColor(textColor)
        tvTocNextChapter.setTextColor(textColor)
        tvPanelSearchTitle.setTextColor(textColor)
        tvPanelLayoutTitle.setTextColor(textColor)
        tvPanelBackgroundTitle.setTextColor(textColor)
        tvPanelThemeTitle.setTextColor(textColor)
        tvPanelPageTurnTitle.setTextColor(textColor)
        tvPanelMoreTitle.setTextColor(textColor)
        panelPageAnim.setTextColor(textColor)
        panelPageAutoPage.setTextColor(textColor)
        panelPageTouchSlop.setTextColor(textColor)
        panelPageVolumeKey.setTextColor(textColor)
        panelPageMouseWheel.setTextColor(textColor)
        panelMoreSearch.setTextColor(textColor)
        panelMoreAutoPage.setTextColor(textColor)
        panelMoreReplace.setTextColor(textColor)
        panelMoreSettings.setTextColor(textColor)
        tintLayoutPanel(textColor)
        tintThemePanel(textColor)
        tintSearchPanel(textColor)
        tintBackgroundPanel(textColor)
        tintPageTurnPanel(textColor)
        renderLayoutPanel()
        renderBottomTabState()
        renderThemeTabs()
        updateLayoutControlsFromConfig()
        updatePageTurnControls()
        updateThemeControlsFromConfig()
        updateThemePresetCards()
        updateFontSampleCards()
        updateBackgroundSampleCards()
        updateBackgroundControlsFromConfig()
        vwBrightnessPosAdjust.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        llBrightness.setOnClickListener(null)
        seekBrightness.post {
            seekBrightness.progress = AppConfig.readBrightness
        }
        if (AppConfig.showReadTitleBarAddition) {
            titleBarAddition.visible()
        } else {
            titleBarAddition.gone()
        }
        upBrightnessVwPos()
        /**
         * 确保视图不被导航栏遮挡
         */
        applyNavigationBarPadding()
    }

    fun reset() {
        upColorConfig()
        initView(true)
    }

    fun refreshMenuColorFilter() {
        if (immersiveMenu) {
            binding.titleBar.setColorFilter(textColor)
        }
    }

    private fun upColorConfig() {
        bgColor = if (immersiveMenu) {
            kotlin.runCatching {
                Color.parseColor(ReadBookConfig.durConfig.curBgStr())
            }.getOrDefault(context.bottomBackground)
        } else {
            context.bottomBackground
        }
        textColor = if (immersiveMenu) {
            ReadBookConfig.durConfig.curTextColor()
        } else {
            context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
        }
        bottomBackgroundList = Selector.colorBuild()
            .setDefaultColor(bgColor)
            .setPressedColor(ColorUtils.darkenColor(bgColor))
            .create()
    }

    fun upBrightnessState() {
        if (brightnessAuto()) {
            binding.ivBrightnessAuto.setColorFilter(context.accentColor)
            binding.seekBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.setColorFilter(context.buttonDisabledColor)
            binding.seekBrightness.isEnabled = true
        }
        setScreenBrightness(AppConfig.readBrightness.toFloat())
    }

    /**
     * 设置屏幕亮度
     */
    fun setScreenBrightness(value: Float) {
        activity?.run {
            var brightness = BRIGHTNESS_OVERRIDE_NONE
            if (!brightnessAuto() && value != BRIGHTNESS_OVERRIDE_NONE) {
                brightness = value
                if (brightness < 1f) brightness = 1f
                brightness /= 255f
            }
            val params = window.attributes
            params.screenBrightness = brightness
            window.attributes = params
        }
    }

    fun runMenuIn(anim: Boolean = !AppConfig.isEInkMode) {
        callBack.onMenuShow()
        this.visible()
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        switchBottomTabMode(BottomTabMode.Primary, animate = false)
        hideExpandedPanel(anim = false)
        if (anim) {
            binding.titleBar.startAnimation(menuTopIn)
            binding.bottomMenu.startAnimation(menuBottomIn)
        } else {
            menuInListener.onAnimationStart(menuBottomIn)
            menuInListener.onAnimationEnd(menuBottomIn)
        }
    }

    fun runMenuOut(anim: Boolean = !AppConfig.isEInkMode, onMenuOutEnd: (() -> Unit)? = null) {
        if (isMenuOutAnimating) {
            return
        }
        callBack.onMenuHide()
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            if (anim) {
                binding.titleBar.startAnimation(menuTopOut)
                binding.bottomMenu.startAnimation(menuBottomOut)
            } else {
                menuOutListener.onAnimationStart(menuBottomOut)
                menuOutListener.onAnimationEnd(menuBottomOut)
            }
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun setupExpandableBottomTabContainer() = binding.run {
        if (bottomTabPanelAttached) {
            return@run
        }
        val oldParent = flExpandedPanel.parent as? ViewGroup
        oldParent?.removeView(flExpandedPanel)
        flExpandedPanel.background = null
        flExpandedPanel.elevation = 0f
        flExpandedPanel.alpha = 0f
        flExpandedPanel.gone()
        val panelParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        )
        bottomTabBar.addView(flExpandedPanel, 2.coerceAtMost(bottomTabBar.childCount), panelParams)
        setBottomTabBarHeight(bottomTabCollapsedHeight())
        bottomTabPanelAttached = true
    }

    private fun setupSearchPanel() = binding.run {
        if (rvPanelSearchResults.adapter == null) {
            rvPanelSearchResults.layoutManager = LinearLayoutManager(context)
            rvPanelSearchResults.adapter = searchPanelAdapter
        }
        etPanelSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startInlineSearch(etPanelSearchQuery.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        btnPanelSearchSubmit.setOnClickListener {
            startInlineSearch(etPanelSearchQuery.text?.toString().orEmpty())
        }
    }

    private fun startInlineSearch(rawQuery: String) = binding.run {
        val query = rawQuery.trim()
        if (query.isBlank()) {
            searchPanelJob?.cancel()
            searchPanelResults.clear()
            searchPanelAdapter.setItems(emptyList())
            tvPanelSearchCount.text = null
            return@run
        }
        val book = ReadBook.book ?: return@run
        searchPanelJob?.cancel()
        tvPanelSearchCount.text = context.getString(R.string.loading)
        searchPanelAdapter.setItems(emptyList())
        searchPanelResults.clear()
        searchPanelJob = Coroutine.async {
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            val chapters = appDb.bookChapterDao.getChapterList(book.bookUrl)
            val results = arrayListOf<SearchResult>()
            chapters.forEach { chapter ->
                currentCoroutineContext().ensureActive()
                if (!chapter.isVolume && BookHelp.hasContent(book, chapter)) {
                    val content = BookHelp.getContent(book, chapter) ?: return@forEach
                    currentCoroutineContext().ensureActive()
                    val displayTitle = when (AppConfig.chineseConverterType) {
                        1 -> ChineseUtils.t2s(chapter.title)
                        2 -> ChineseUtils.s2t(chapter.title)
                        else -> chapter.title
                    }
                    val searchableContent = contentProcessor.getContent(
                        book = book,
                        chapter = chapter,
                        content = content,
                        useReplace = SearchContentViewModel.replaceEnabled
                    ).toString()
                    inlineSearchPositions(searchableContent, query).forEachIndexed { index, position ->
                        currentCoroutineContext().ensureActive()
                        val (queryIndex, resultText) = inlineSearchResultText(
                            searchableContent,
                            position,
                            query
                        )
                        results.add(
                            SearchResult(
                                resultCount = results.size,
                                resultCountWithinChapter = index,
                                resultText = resultText,
                                chapterTitle = displayTitle,
                                query = query,
                                chapterIndex = chapter.index,
                                queryIndexInResult = queryIndex,
                                queryIndexInChapter = position,
                                isRegex = SearchContentViewModel.regexReplace
                            )
                        )
                    }
                }
            }
            results.toList()
        }.onSuccess { results ->
            searchPanelResults.clear()
            searchPanelResults.addAll(results)
            searchPanelAdapter.setItems(results)
            tvPanelSearchCount.text = context.getString(R.string.search_content_size) + ": ${results.size}"
        }.onError {
            tvPanelSearchCount.text = context.getString(R.string.search_content_size) + ": 0"
        }
    }

    private suspend fun inlineSearchPositions(content: String, query: String): List<Int> {
        val positions = arrayListOf<Int>()
        if (SearchContentViewModel.regexReplace) {
            runCatching {
                Regex(query).findAll(content).forEach { match ->
                    currentCoroutineContext().ensureActive()
                    positions.add(match.range.first)
                }
            }
        } else {
            var index = content.indexOf(query)
            while (index >= 0) {
                currentCoroutineContext().ensureActive()
                positions.add(index)
                index = content.indexOf(query, index + query.length)
            }
        }
        return positions
    }

    private fun inlineSearchResultText(
        content: String,
        queryIndexInContent: Int,
        query: String
    ): Pair<Int, String> {
        val length = 20
        val start = (queryIndexInContent - length).coerceAtLeast(0)
        val end = (queryIndexInContent + query.length + length).coerceAtMost(content.length)
        return queryIndexInContent - start to content.substring(start, end)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTocPanel() = binding.run {
        if (rvPanelToc.adapter == null) {
            rvPanelToc.layoutManager = LinearLayoutManager(context)
            rvPanelToc.adapter = tocAdapter
        }
        tocDragHandleIndicator.background = roundedRect(
            ColorUtils.adjustAlpha(bottomTabContentColor(), 0.26f),
            2f.dpToPx()
        )
        tocDragHandle.setOnClickListener {
            if (activeBottomTab == BottomTab.Toc && flExpandedPanel.isVisible) {
                animateTocPanelTo(if (tocPanelFullscreen) tocDefaultPanelHeight() else tocFullPanelHeight())
            }
        }
        tocProgressModeToggle.setOnClickListener {
            tocProgressWholeBook = !tocProgressWholeBook
            upSeekBar()
        }
        tvTocPrevChapter.setOnClickListener { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }
        tvTocNextChapter.setOnClickListener { ReadBook.moveToNextChapter(true) }
        seekTocProgress.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (tocProgressWholeBook) {
                    if (seekBar.progress == ReadBook.durChapterIndex) {
                        return
                    }
                    callBack.skipToChapter(seekBar.progress)
                } else {
                    ReadBook.skipToPage(seekBar.progress)
                    setSeekPage(seekBar.progress)
                    upSeekBar()
                }
            }
        })
        tocDragHandle.setOnTouchListener { _, event ->
            if (activeBottomTab != BottomTab.Toc || !flExpandedPanel.isVisible) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    bottomTabHeightAnimator?.cancel()
                    tocDragStartY = event.rawY
                    tocDragStartPanelHeight = flExpandedPanel.height
                    tocDragHandle.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dragOffset = (tocDragStartY - event.rawY).roundToInt()
                    val targetHeight = (tocDragStartPanelHeight + dragOffset)
                        .coerceIn(tocDefaultPanelHeight(), tocFullPanelHeight())
                    setBottomTabPanelHeight(targetHeight)
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    val middleHeight = (tocDefaultPanelHeight() + tocFullPanelHeight()) / 2
                    val targetHeight = if (flExpandedPanel.height >= middleHeight) {
                        tocFullPanelHeight()
                    } else {
                        tocDefaultPanelHeight()
                    }
                    animateTocPanelTo(targetHeight)
                    tocDragHandle.parent.requestDisallowInterceptTouchEvent(false)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadTocPanel() {
        val book = ReadBook.book
        if (book == null) {
            tocAdapter.submit(emptyList())
            binding.tvPanelTocCount.text = null
            return
        }
        tocLoadJob?.cancel()
        tocLoadJob = Coroutine.async {
            appDb.bookChapterDao.getChapterList(book.bookUrl)
        }.onSuccess {
            tocAdapter.submit(it)
            binding.tvPanelTocCount.text = if (it.isEmpty()) {
                context.getString(R.string.chapter_list_empty)
            } else {
                "${ReadBook.durChapterIndex + 1}/${it.size}"
            }
            val currentIndex = it.indexOfFirst { chapter -> chapter.index == ReadBook.durChapterIndex }
            if (currentIndex >= 0) {
                binding.rvPanelToc.post {
                    (binding.rvPanelToc.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(currentIndex, 80.dpToPx())
                }
            }
        }
    }

    private fun openTocChapter(chapter: BookChapter) {
        if (chapter.isVolume) {
            return
        }
        runMenuOut {
            callBack.skipToChapter(chapter.index)
        }
    }

    private fun animateTocPanelTo(targetHeight: Int) {
        tocPanelFullscreen = targetHeight >= tocFullPanelHeight()
        updateBottomTabGlassLayerHeight(tocFullPanelHeight())
        animateBottomTabPanelHeight(
            targetPanelHeight = targetHeight,
            animate = !AppConfig.isEInkMode
        ) {
            renderBottomTabState()
        }
    }

    private fun toggleBottomTab(tab: BottomTab): Boolean {
        if (activeBottomTab == tab && binding.flExpandedPanel.isVisible) {
            hideExpandedPanel(returnToPrimary = true)
            return false
        }
        showBottomPanel(tab)
        return true
    }

    private fun showBottomPanel(tab: BottomTab) = binding.run {
        if (tab == BottomTab.Search || tab == BottomTab.Toc) {
            if (bottomTabMode != BottomTabMode.Primary) {
                switchBottomTabMode(BottomTabMode.Primary, animate = false)
            }
        } else if (bottomTabMode != BottomTabMode.Interface) {
            switchBottomTabMode(BottomTabMode.Interface, animate = false)
        }
        val previousTab = activeBottomTab
        val wasExpanded = flExpandedPanel.isVisible &&
                bottomTabBar.height > bottomTabCollapsedHeight()
        activeBottomTab = tab
        when (tab) {
            BottomTab.Search -> Unit

            BottomTab.Toc -> {
                if (previousTab != BottomTab.Toc) {
                    tocPanelFullscreen = false
                }
                loadTocPanel()
            }

            BottomTab.Layout -> Unit

            BottomTab.PageTurn -> updatePageTurnControls()

            BottomTab.Background,
            BottomTab.Theme -> Unit
        }
        panelSearch.gone(tab != BottomTab.Search)
        panelToc.gone(tab != BottomTab.Toc)
        panelLayout.gone(tab != BottomTab.Layout)
        panelPageTurn.gone(tab != BottomTab.PageTurn)
        panelBackground.gone(tab != BottomTab.Background)
        panelTheme.gone(tab != BottomTab.Theme)
        panelMore.gone()
        updateExpandedPanelHeight(tab)
        val glassPanelHeight = expandedPanelStableHeight()
        updateBottomTabGlassLayerHeight(glassPanelHeight)
        val panelHeight = expandedPanelTargetHeight(tab)
        flExpandedPanel.alpha = if (wasExpanded) 1f else 0f
        flExpandedPanel.visible()
        if (!wasExpanded) {
            setExpandedPanelHeight(0)
        }
        renderBottomTabState()
        animateBottomTabPanelHeight(
            targetPanelHeight = panelHeight,
            animate = !AppConfig.isEInkMode
        ) {
            if (activeBottomTab == tab && flExpandedPanel.isVisible) {
                flExpandedPanel.animate().cancel()
                if (wasExpanded) {
                    flExpandedPanel.alpha = 1f
                } else {
                    flExpandedPanel.animate()
                        .alpha(1f)
                        .setDuration(if (AppConfig.isEInkMode) 0L else 90L)
                        .start()
                }
            }
        }
    }

    private fun hideExpandedPanel(
        anim: Boolean = !AppConfig.isEInkMode,
        returnToPrimary: Boolean = false
    ) {
        if (!binding.flExpandedPanel.isVisible) {
            activeBottomTab = null
            if (returnToPrimary) {
                switchBottomTabMode(BottomTabMode.Primary)
            } else {
                renderBottomTabState()
            }
            return
        }
        activeBottomTab = null
        binding.flExpandedPanel.animate().cancel()
        binding.flExpandedPanel.alpha = 0f
        if (returnToPrimary) {
            switchBottomTabMode(BottomTabMode.Primary)
        } else {
            renderBottomTabState()
        }
        animateBottomTabPanelHeight(
            targetPanelHeight = 0,
            animate = anim
        ) {
            binding.flExpandedPanel.gone()
            binding.flExpandedPanel.updateLayoutParams<FrameLayout.LayoutParams> {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    private fun handleBackgroundDismiss() {
        when {
            binding.flExpandedPanel.isVisible -> hideExpandedPanel(returnToPrimary = true)
            else -> runMenuOut()
        }
    }

    private fun renderBottomTabState() = binding.run {
        configureBottomTabFrostedGlass()
        bottomTabBar.elevation = 8f.dpToPx()
        flExpandedPanel.elevation = 0f
        applyBottomNavigationColors()
        syncBottomNavigationSelection()
        updateBottomTabIndicator()
    }

    private fun updateExpandedPanelHeight(tab: BottomTab) = binding.run {
        panelLayout.updateLayoutParams<FrameLayout.LayoutParams> {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        panelLayoutScroll.updateLayoutParams<LinearLayout.LayoutParams> {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        panelTheme.updateLayoutParams<FrameLayout.LayoutParams> {
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (tab == BottomTab.Layout || tab == BottomTab.Theme) {
            applyAdaptivePanelHeight(tab, expandedPanelMaxHeight())
        }
    }

    private fun applyAdaptivePanelHeight(tab: BottomTab, maxHeight: Int) = binding.run {
        when (tab) {
            BottomTab.Layout -> {
                val measuredHeight = measureBottomPanelHeight(panelLayout, maxHeight)
                if (measuredHeight >= maxHeight) {
                    panelLayout.updateLayoutParams<FrameLayout.LayoutParams> {
                        height = maxHeight
                    }
                    panelLayoutScroll.updateLayoutParams<LinearLayout.LayoutParams> {
                        height = maxHeight
                    }
                } else {
                    panelLayout.updateLayoutParams<FrameLayout.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    panelLayoutScroll.updateLayoutParams<LinearLayout.LayoutParams> {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }

            BottomTab.Theme -> {
                val measuredHeight = measureBottomPanelHeight(panelTheme, maxHeight)
                panelTheme.updateLayoutParams<FrameLayout.LayoutParams> {
                    height = if (measuredHeight >= maxHeight) {
                        maxHeight
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }

            BottomTab.Search,
            BottomTab.Toc,
            BottomTab.PageTurn,
            BottomTab.Background -> Unit
        }
    }

    private fun expandedPanelMaxHeight(): Int {
        val rootHeight = binding.vwMenuRoot.height
            .takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val bottomBarHeight = bottomTabCollapsedHeight()
        val topGap = 16.dpToPx()
        val bottomPadding = binding.bottomMenu.paddingBottom
        val screenCap = (rootHeight - bottomBarHeight - bottomPadding - topGap)
            .coerceAtLeast(96.dpToPx())
        val minHeight = 180.dpToPx().coerceAtMost(screenCap)
        val belowTitle = rootHeight - binding.titleBar.bottom - topGap -
                bottomBarHeight - bottomPadding
        return belowTitle.coerceIn(minHeight, screenCap)
    }

    private fun tocDefaultPanelHeight(): Int {
        return 360.dpToPx()
            .coerceAtMost(tocFullPanelHeight())
            .coerceAtLeast(220.dpToPx().coerceAtMost(tocFullPanelHeight()))
    }

    private fun tocFullPanelHeight(): Int {
        val rootHeight = binding.vwMenuRoot.height
            .takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val bottomBarHeight = bottomTabCollapsedHeight()
        val bottomPadding = binding.bottomMenu.paddingBottom
        return (rootHeight - bottomBarHeight - bottomPadding - 8.dpToPx())
            .coerceAtLeast(tocDefaultMinHeight())
    }

    private fun tocDefaultMinHeight(): Int = 220.dpToPx()

    private fun bottomTabCollapsedHeight(): Int = 64.dpToPx()

    private fun expandedPanelTargetHeight(tab: BottomTab): Int {
        if (tab == BottomTab.Search) {
            return tocDefaultPanelHeight()
        }
        if (tab == BottomTab.Toc) {
            return if (tocPanelFullscreen) {
                tocFullPanelHeight()
            } else {
                tocDefaultPanelHeight()
            }
        }
        val maxHeight = expandedPanelMaxHeight()
        val panelView = selectedBottomPanelView(tab)
        val measuredHeight = measureBottomPanelHeight(panelView, maxHeight)
        return measuredHeight.coerceIn(120.dpToPx(), maxHeight)
    }

    private fun expandedPanelStableHeight(): Int = binding.run {
        val maxHeight = expandedPanelMaxHeight()
        val currentBottomTab = activeBottomTab

        panelToc.gone()
        panelSearch.gone()
        panelLayout.visible()
        panelTheme.gone()
        val layoutHeight = measureBottomPanelHeight(panelLayout, maxHeight)

        panelLayout.gone()
        panelPageTurn.visible()
        val pageTurnHeight = measureBottomPanelHeight(panelPageTurn, maxHeight)
        panelPageTurn.gone()
        panelBackground.visible()
        val backgroundHeight = measureBottomPanelHeight(panelBackground, maxHeight)
        panelBackground.gone()
        panelTheme.visible()
        val themeHeight = measureBottomPanelHeight(panelTheme, maxHeight)

        panelTheme.gone()
        panelMore.gone()
        val tocHeight = tocFullPanelHeight()
        val searchHeight = tocDefaultPanelHeight()

        panelSearch.gone(currentBottomTab != BottomTab.Search)
        panelToc.gone(currentBottomTab != BottomTab.Toc)
        panelLayout.gone(currentBottomTab != BottomTab.Layout)
        panelPageTurn.gone(currentBottomTab != BottomTab.PageTurn)
        panelBackground.gone(currentBottomTab != BottomTab.Background)
        panelTheme.gone(currentBottomTab != BottomTab.Theme)
        panelMore.gone()

        maxOf(
            layoutHeight,
            pageTurnHeight,
            backgroundHeight,
            themeHeight,
            searchHeight,
            tocHeight
        )
            .coerceIn(120.dpToPx(), tocFullPanelHeight())
    }

    private fun selectedBottomPanelView(tab: BottomTab): View {
        return when (tab) {
            BottomTab.Search -> binding.panelSearch
            BottomTab.Toc -> binding.panelToc
            BottomTab.Layout -> binding.panelLayout
            BottomTab.PageTurn -> binding.panelPageTurn
            BottomTab.Background -> binding.panelBackground
            BottomTab.Theme -> binding.panelTheme
        }
    }

    private fun setBottomTabPanelHeight(height: Int) {
        setExpandedPanelHeight(height)
        setBottomTabBarHeight(bottomTabCollapsedHeight() + height)
    }

    private fun measureBottomPanelHeight(view: View, maxHeight: Int): Int {
        val width = binding.bottomTabBar.width
            .takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        view.measure(widthSpec, heightSpec)
        return view.measuredHeight
    }

    private fun setExpandedPanelHeight(height: Int) = binding.flExpandedPanel.run {
        val targetHeight = height.coerceAtLeast(0)
        val params = layoutParams as? FrameLayout.LayoutParams
        if (params?.height == targetHeight) {
            return@run
        }
        updateLayoutParams<FrameLayout.LayoutParams> {
            this.height = targetHeight
        }
    }

    private fun setBottomTabBarHeight(height: Int) = binding.bottomTabBar.run {
        val targetHeight = height.coerceAtLeast(bottomTabCollapsedHeight())
        val params = layoutParams as? FrameLayout.LayoutParams
        if (params?.height == targetHeight) {
            return@run
        }
        updateLayoutParams<FrameLayout.LayoutParams> {
            this.height = targetHeight
        }
    }

    private fun updateBottomTabGlassLayerHeight(panelHeight: Int) = binding.run {
        val targetHeight = bottomTabCollapsedHeight() + panelHeight.coerceAtLeast(0)
        if (bottomTabGlassLayerHeight == targetHeight) {
            return@run
        }
        bottomTabGlassLayerHeight = targetHeight
        setBottomTabGlassLayerChildHeight(bottomTabGlassView, targetHeight)
        setBottomTabGlassLayerChildHeight(bottomTabShellOverlay, targetHeight)
    }

    private fun setBottomTabGlassLayerChildHeight(view: View, height: Int) {
        view.updateLayoutParams<FrameLayout.LayoutParams> {
            this.height = height
            gravity = Gravity.BOTTOM
        }
    }

    private fun animateBottomTabBarHeight(
        targetHeight: Int,
        animate: Boolean,
        onEnd: () -> Unit
    ) = binding.run {
        bottomTabHeightAnimator?.cancel()
        val startHeight = bottomTabBar.height
            .takeIf { it > 0 }
            ?: bottomTabCollapsedHeight()
        if (!animate || startHeight == targetHeight) {
            if (startHeight != targetHeight) {
                setBottomTabBarHeight(targetHeight)
            }
            onEnd()
            return@run
        }
        bottomTabHeightAnimator = ValueAnimator.ofInt(startHeight, targetHeight).apply {
            duration = 220L
            interpolator = bottomTabSlideInterpolator
            addUpdateListener { animator ->
                setBottomTabBarHeight(animator.animatedValue as Int)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) {
                        setBottomTabBarHeight(targetHeight)
                        onEnd()
                    }
                    if (bottomTabHeightAnimator == this@apply) {
                        bottomTabHeightAnimator = null
                    }
                }
            })
            start()
        }
    }

    private fun animateBottomTabPanelHeight(
        targetPanelHeight: Int,
        animate: Boolean,
        onEnd: () -> Unit
    ) = binding.run {
        bottomTabHeightAnimator?.cancel()
        val startPanelHeight = flExpandedPanel.height.takeIf { it >= 0 } ?: 0
        if (!animate || startPanelHeight == targetPanelHeight) {
            setBottomTabPanelHeight(targetPanelHeight)
            onEnd()
            return@run
        }
        bottomTabHeightAnimator = ValueAnimator.ofInt(startPanelHeight, targetPanelHeight).apply {
            duration = 220L
            interpolator = bottomTabSlideInterpolator
            addUpdateListener { animator ->
                setBottomTabPanelHeight(animator.animatedValue as Int)
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelled) {
                        setBottomTabPanelHeight(targetPanelHeight)
                        onEnd()
                    }
                    if (bottomTabHeightAnimator == this@apply) {
                        bottomTabHeightAnimator = null
                    }
                }
            })
            start()
        }
    }

    private fun updateBottomTabIndicator() = binding.run {
        val tab = activeBottomTab
        if (tab == null) {
            hideBottomTabIndicator()
            return@run
        }
        if (tab == BottomTab.Toc) {
            showBottomTabIndicator(
                nav = readBottomPrimaryNav,
                itemId = tab.menuItemId(),
                animate = true,
                autoHide = false
            )
            return@run
        }
        showBottomTabIndicator(
            nav = readBottomInterfaceNav,
            itemId = tab.menuItemId(),
            animate = true,
            autoHide = false
        )
    }

    private fun applyBottomNavigationColors() = binding.run {
        val colors = Selector.colorBuild()
            .setDefaultColor(bottomTabContentColor())
            .setSelectedColor(bottomTabSelectedContentColor())
            .create()
        listOf(readBottomPrimaryNav, readBottomInterfaceNav).forEach { nav ->
            nav.itemIconTintList = colors
            nav.itemTextColor = colors
            nav.itemRippleColor = null
        }
    }

    private fun syncBottomNavigationSelection() = binding.run {
        setBottomNavigationSelection(
            readBottomPrimaryNav,
            when {
                activeBottomTab == BottomTab.Search -> R.id.menu_read_search
                activeBottomTab == BottomTab.Toc -> R.id.menu_read_toc
                bottomTabMode == BottomTabMode.Interface -> R.id.menu_read_interface
                else -> null
            }
        )
        setBottomNavigationSelection(
            readBottomInterfaceNav,
            if (activeBottomTab == BottomTab.Toc) {
                null
            } else {
                activeBottomTab?.menuItemId()
            }
        )
    }

    private fun setBottomNavigationSelection(nav: BottomNavigationView, itemId: Int?) {
        suppressBottomNavSelection = true
        try {
            if (itemId == null) {
                clearBottomNavigationSelection(nav)
            } else {
                nav.selectedItemId = itemId
            }
        } finally {
            suppressBottomNavSelection = false
        }
    }

    private fun clearBottomNavigationSelection(nav: BottomNavigationView) {
        nav.menu.setGroupCheckable(0, true, false)
        for (index in 0 until nav.menu.size()) {
            nav.menu.getItem(index).isChecked = false
        }
        nav.menu.setGroupCheckable(0, true, true)
    }

    private fun flashBottomTabIndicator(nav: BottomNavigationView, itemId: Int) {
        showBottomTabIndicator(nav, itemId, animate = true, autoHide = true)
    }

    private fun showBottomTabIndicator(
        nav: BottomNavigationView,
        itemId: Int,
        animate: Boolean,
        autoHide: Boolean
    ) = binding.run {
        bottomTabIndicatorContainer.removeCallbacks(hideBottomTabIndicatorRunnable)
        bottomTabIndicatorContainer.animate().cancel()
        bottomTabIndicatorContainer.post {
            val itemView = findBottomNavigationItemView(nav, itemId) ?: return@post
            val menuView = nav.getChildAt(0) as? ViewGroup ?: return@post
            val maxWidth = 68.dpToPx()
            val minWidth = 52.dpToPx()
            val targetWidth = minOf(
                maxWidth,
                (itemView.width - 12.dpToPx()).coerceAtLeast(minWidth)
            )
            bottomTabIndicatorContainer.updateLayoutParams<FrameLayout.LayoutParams> {
                width = targetWidth
            }
            val targetX = bottomTabNavViewport.x +
                    nav.x +
                    nav.translationX +
                    menuView.x +
                    itemView.x +
                    (itemView.width - targetWidth) / 2f
            val firstPlacement = bottomTabIndicatorContainer.alpha <= 0f ||
                    !bottomTabIndicatorContainer.isVisible
            bottomTabIndicatorContainer.visible()
            if (!animate || firstPlacement || AppConfig.isEInkMode) {
                bottomTabIndicatorContainer.x = targetX
                bottomTabIndicatorContainer.alpha = 1f
                bottomTabIndicatorContainer.scaleX = 1f
                bottomTabIndicatorContainer.scaleY = 1f
            } else if (abs(bottomTabIndicatorContainer.x - targetX) >= 0.5f) {
                bottomTabIndicatorContainer.animate()
                    .x(targetX)
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220L)
                    .setInterpolator(bottomTabIndicatorInterpolator)
                    .start()
            }
            if (autoHide) {
                bottomTabIndicatorContainer.postDelayed(hideBottomTabIndicatorRunnable, 520L)
            }
        }
    }

    private fun hideBottomTabIndicator() = binding.run {
        bottomTabIndicatorContainer.removeCallbacks(hideBottomTabIndicatorRunnable)
        bottomTabIndicatorContainer.animate().cancel()
        if (!bottomTabIndicatorContainer.isVisible) {
            bottomTabIndicatorContainer.alpha = 0f
            return@run
        }
        if (AppConfig.isEInkMode) {
            bottomTabIndicatorContainer.alpha = 0f
            bottomTabIndicatorContainer.invisible()
            return@run
        }
        fadeBottomTabIndicator()
    }

    private fun fadeBottomTabIndicator() = binding.run {
        bottomTabIndicatorContainer.animate().cancel()
        bottomTabIndicatorContainer.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(140L)
            .setInterpolator(bottomTabPulseInterpolator)
            .withEndAction {
                if (activeBottomTab == null) {
                    bottomTabIndicatorContainer.invisible()
                }
            }
            .start()
    }

    private fun findBottomNavigationItemView(nav: BottomNavigationView, itemId: Int): View? {
        val menuView = nav.getChildAt(0) as? ViewGroup ?: return null
        for (index in 0 until menuView.childCount) {
            val child = menuView.getChildAt(index)
            if (child.id == itemId && child.visibility == View.VISIBLE) {
                return child
            }
        }
        return null
    }

    private fun switchBottomTabMode(
        mode: BottomTabMode,
        animate: Boolean = !AppConfig.isEInkMode
    ) = binding.run {
        val wasMode = bottomTabMode
        bottomTabMode = mode
        renderBottomTabState()
        val width = bottomTabNavViewport.width.takeIf { it > 0 }
            ?: bottomTabBar.width.takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        readBottomPrimaryNav.animate().cancel()
        readBottomInterfaceNav.animate().cancel()
        if (!animate || wasMode == mode || width <= 0) {
            readBottomPrimaryNav.translationX = if (mode == BottomTabMode.Primary) 0f else -width.toFloat()
            readBottomInterfaceNav.translationX =
                if (mode == BottomTabMode.Interface) 0f else width.toFloat()
            readBottomPrimaryNav.visible(mode == BottomTabMode.Primary)
            readBottomInterfaceNav.visible(mode == BottomTabMode.Interface)
            return@run
        }
        when (mode) {
            BottomTabMode.Primary -> {
                readBottomPrimaryNav.visible()
                readBottomPrimaryNav.translationX = -width.toFloat()
                readBottomInterfaceNav.visible()
                readBottomInterfaceNav.translationX = 0f
                readBottomPrimaryNav.animate()
                    .translationX(0f)
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .start()
                readBottomInterfaceNav.animate()
                    .translationX(width.toFloat())
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .withEndAction {
                        if (bottomTabMode == BottomTabMode.Primary) {
                            readBottomInterfaceNav.invisible()
                        }
                    }
                    .start()
            }

            BottomTabMode.Interface -> {
                readBottomPrimaryNav.visible()
                readBottomPrimaryNav.translationX = 0f
                readBottomInterfaceNav.visible()
                readBottomInterfaceNav.translationX = width.toFloat()
                readBottomPrimaryNav.animate()
                    .translationX(-width.toFloat())
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .withEndAction {
                        if (bottomTabMode == BottomTabMode.Interface) {
                            readBottomPrimaryNav.invisible()
                        }
                    }
                    .start()
                readBottomInterfaceNav.animate()
                    .translationX(0f)
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .start()
            }
        }
    }

    private fun BottomTab.menuItemId(): Int {
        return when (this) {
            BottomTab.Search -> R.id.menu_read_search
            BottomTab.Toc -> R.id.menu_read_toc
            BottomTab.Layout -> R.id.menu_read_layout
            BottomTab.PageTurn -> R.id.menu_read_page_turn
            BottomTab.Background -> R.id.menu_read_background
            BottomTab.Theme -> R.id.menu_read_theme
        }
    }

    private fun Int.toBottomTab(): BottomTab? {
        return when (this) {
            R.id.menu_read_search -> BottomTab.Search
            R.id.menu_read_layout -> BottomTab.Layout
            R.id.menu_read_page_turn -> BottomTab.PageTurn
            R.id.menu_read_background -> BottomTab.Background
            R.id.menu_read_theme -> BottomTab.Theme
            else -> null
        }
    }

    private fun tintLayoutPanel(color: Int) = binding.run {
        listOf(
            tvLayoutFontTitle,
            tvLayoutTextSpacingTitle,
            tvLayoutPageMarginTitle,
            tvLayoutTitleSettingsTitle,
            tvLayoutTitleSizeLabel,
            tvLayoutTitleTopSpacingLabel,
            tvLayoutTitleBottomSpacingLabel,
            tvLayoutHeaderShowLabel,
            tvLayoutHeaderShowValue,
            tvLayoutFooterShowLabel,
            tvLayoutFooterShowValue,
            tvLayoutTipColorLabel,
            tvLayoutTipColorValue,
            tvLayoutTipDividerColorLabel,
            tvLayoutTipDividerColorValue,
            tvLayoutLetterSpacingLabel,
            tvLayoutLineSpacingLabel,
            tvLayoutParagraphSpacingLabel,
            tvLayoutPaddingTopLabel,
            tvLayoutPaddingBottomLabel,
            tvLayoutPaddingLeftLabel,
            tvLayoutPaddingRightLabel,
            tvLayoutHeaderTitle,
            tvLayoutHeaderLineToggle,
            tvLayoutTitleSizeValue,
            tvLayoutTitleTopSpacingValue,
            tvLayoutTitleBottomSpacingValue,
            tvLayoutHeaderPaddingTopValue,
            tvLayoutHeaderPaddingBottomValue,
            tvLayoutFooterTitle,
            tvLayoutFooterLineToggle,
            tvLayoutFooterPaddingTopValue,
            tvLayoutFooterPaddingBottomValue,
            tvLayoutLetterSpacingValue,
            tvLayoutLineSpacingValue,
            tvLayoutParagraphSpacingValue,
            tvLayoutPaddingTopValue,
            tvLayoutPaddingBottomValue,
            tvLayoutPaddingLeftValue,
            tvLayoutPaddingRightValue
        ).forEach { it.setTextColor(color) }
        listOf(
            ivLayoutPaddingTop,
            ivLayoutPaddingBottom,
            ivLayoutPaddingLeft,
            ivLayoutPaddingRight,
            ivLayoutHeaderTitle,
            ivLayoutFooterTitle
        ).forEach { it.setColorFilter(color, PorterDuff.Mode.SRC_IN) }
        listOf(
            tvLayoutTitleModeLeft,
            tvLayoutTitleModeCenter,
            tvLayoutTitleModeAdvanced,
            tvLayoutTitleModeHide
        ).forEach { it.setTextColor(color) }
    }

    private fun tintPageTurnPanel(color: Int) = binding.run {
        listOf(
            tvPanelPageTurnTitle,
            panelPageAnim,
            panelPageAutoPage,
            panelPageTouchSlop,
            panelPageVolumeKey,
            panelPageMouseWheel
        ).forEach { it.setTextColor(color) }
    }

    private fun tintThemePanel(color: Int) = binding.run {
        listOf(
            tvThemeFontWeightLabel,
            tvThemeTextSizeLabel,
            tvThemeFontWeightBig,
            tvThemeTextSizeSmall,
            tvThemeTextSizeBig,
            tvThemeFontWeightValue,
            tvThemeTextSizeValue
        ).forEach { it.setTextColor(color) }
        listOf(
            ivThemeFontWeightLow
        ).forEach { it.setColorFilter(color, PorterDuff.Mode.SRC_IN) }
    }

    private fun tintSearchPanel(color: Int) = binding.run {
        listOf(
            tvPanelSearchTitle,
            tvPanelSearchCount,
            etPanelSearchQuery
        ).forEach { it.setTextColor(color) }
        btnPanelSearchSubmit.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        searchInputPanel.background = roundedRect(
            ColorUtils.adjustAlpha(color, 0.06f),
            16f.dpToPx(),
            1.dpToPx(),
            ColorUtils.adjustAlpha(color, 0.14f)
        )
    }

    private fun tintBackgroundPanel(color: Int) = binding.run {
        listOf(
            tvPanelBackgroundTitle,
            tvBackgroundBrightnessLabel,
            tvBackgroundSaturationLabel,
            tvBackgroundAlphaLabel,
            tvBackgroundBrightnessValue,
            tvBackgroundSaturationValue,
            tvBackgroundAlphaValue
        ).forEach { it.setTextColor(color) }
        backgroundTonePanel.background = roundedRect(
            ColorUtils.adjustAlpha(color, 0.06f),
            14f.dpToPx(),
            1.dpToPx(),
            ColorUtils.adjustAlpha(color, 0.14f)
        )
    }

    private fun renderThemeTabs() = binding.run {
        configureThemeTopTab(llThemeTabPreset, activeThemeTab == ThemeTab.Preset)
        configureThemeTopTab(llThemeTabCustom, activeThemeTab == ThemeTab.Custom)
        configureThemeTopTab(llThemeTabEye, activeThemeTab == ThemeTab.Eye)
    }

    private fun renderLayoutPanel() = binding.run {
        tvPanelLayoutTitle.setText(
            R.string.compose_type
        )
        panelLayoutFont.visible()
        panelLayoutSpacing.visible()
        panelLayoutStyle.visible()
    }

    private fun configureThemeTopTab(tab: TextView, selected: Boolean) {
        tab.background = roundedRect(
            if (selected) context.accentColor else Color.TRANSPARENT,
            12f.dpToPx(),
            1.dpToPx(),
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.12f)
        )
        tab.setTextColor(if (selected) Color.WHITE else textColor)
    }

    private fun updateLayoutControlsFromConfig() = binding.run {
        val letterSpacingProgress = (ReadBookConfig.letterSpacing * 10f)
            .roundToInt()
            .coerceIn(0, seekLayoutLetterSpacing.max)
        seekLayoutLetterSpacing.progress = letterSpacingProgress
        updateLayoutDecimalValue(tvLayoutLetterSpacingValue, letterSpacingProgress)

        val lineSpacingProgress = ReadBookConfig.lineSpacingExtra
            .coerceIn(0, seekLayoutLineSpacing.max)
        seekLayoutLineSpacing.progress = lineSpacingProgress
        updateLayoutDecimalValue(tvLayoutLineSpacingValue, lineSpacingProgress)

        val paragraphSpacingProgress = ReadBookConfig.paragraphSpacing
            .coerceIn(0, seekLayoutParagraphSpacing.max)
        seekLayoutParagraphSpacing.progress = paragraphSpacingProgress
        updateLayoutDecimalValue(tvLayoutParagraphSpacingValue, paragraphSpacingProgress)

        val paddingTopProgress = ReadBookConfig.paddingTop.coerceIn(0, seekLayoutPaddingTop.max)
        seekLayoutPaddingTop.progress = paddingTopProgress
        updateLayoutIntValue(tvLayoutPaddingTopValue, paddingTopProgress)

        val paddingBottomProgress = ReadBookConfig.paddingBottom.coerceIn(0, seekLayoutPaddingBottom.max)
        seekLayoutPaddingBottom.progress = paddingBottomProgress
        updateLayoutIntValue(tvLayoutPaddingBottomValue, paddingBottomProgress)

        val paddingLeftProgress = ReadBookConfig.paddingLeft.coerceIn(0, seekLayoutPaddingLeft.max)
        seekLayoutPaddingLeft.progress = paddingLeftProgress
        updateLayoutIntValue(tvLayoutPaddingLeftValue, paddingLeftProgress)

        val paddingRightProgress = ReadBookConfig.paddingRight.coerceIn(0, seekLayoutPaddingRight.max)
        seekLayoutPaddingRight.progress = paddingRightProgress
        updateLayoutIntValue(tvLayoutPaddingRightValue, paddingRightProgress)

        seekLayoutTitleSize.progress = ReadBookConfig.titleSize.coerceIn(0, seekLayoutTitleSize.max)
        updateLayoutIntValue(tvLayoutTitleSizeValue, seekLayoutTitleSize.progress)
        seekLayoutTitleTopSpacing.progress = ReadBookConfig.titleTopSpacing
            .coerceIn(0, seekLayoutTitleTopSpacing.max)
        updateLayoutIntValue(tvLayoutTitleTopSpacingValue, seekLayoutTitleTopSpacing.progress)
        seekLayoutTitleBottomSpacing.progress = ReadBookConfig.titleBottomSpacing
            .coerceIn(0, seekLayoutTitleBottomSpacing.max)
        updateLayoutIntValue(tvLayoutTitleBottomSpacingValue, seekLayoutTitleBottomSpacing.progress)
        updateTitleModeButtons()

        seekLayoutHeaderPaddingTop.progress = ReadBookConfig.headerPaddingTop
            .coerceIn(0, seekLayoutHeaderPaddingTop.max)
        updateLayoutIntValue(tvLayoutHeaderPaddingTopValue, seekLayoutHeaderPaddingTop.progress)
        seekLayoutHeaderPaddingBottom.progress = ReadBookConfig.headerPaddingBottom
            .coerceIn(0, seekLayoutHeaderPaddingBottom.max)
        updateLayoutIntValue(tvLayoutHeaderPaddingBottomValue, seekLayoutHeaderPaddingBottom.progress)
        configureOptionButton(tvLayoutHeaderLineToggle, ReadBookConfig.showHeaderLine)

        seekLayoutFooterPaddingTop.progress = ReadBookConfig.footerPaddingTop
            .coerceIn(0, seekLayoutFooterPaddingTop.max)
        updateLayoutIntValue(tvLayoutFooterPaddingTopValue, seekLayoutFooterPaddingTop.progress)
        seekLayoutFooterPaddingBottom.progress = ReadBookConfig.footerPaddingBottom
            .coerceIn(0, seekLayoutFooterPaddingBottom.max)
        updateLayoutIntValue(tvLayoutFooterPaddingBottomValue, seekLayoutFooterPaddingBottom.progress)
        configureOptionButton(tvLayoutFooterLineToggle, ReadBookConfig.showFooterLine)
        updateTipSettingValues()
    }

    private fun updateLayoutDecimalValue(view: TextView, progress: Int) {
        view.text = String.format(Locale.US, "%.1f", progress / 10f)
    }

    private fun updateLayoutIntValue(view: TextView, progress: Int) {
        view.text = progress.toString()
    }

    private fun updateTitleModeButtons() = binding.run {
        listOf(
            tvLayoutTitleModeLeft to (ReadBookConfig.titleMode == 0),
            tvLayoutTitleModeCenter to (ReadBookConfig.titleMode == 1),
            tvLayoutTitleModeAdvanced to (ReadBookConfig.titleMode == AdvancedTitleConfig.TITLE_MODE_ADVANCED),
            tvLayoutTitleModeHide to (ReadBookConfig.titleMode == 2)
        ).forEach { (view, selected) ->
            configureOptionButton(view, selected)
        }
    }

    private fun updateTipSettingValues() = binding.run {
        tvLayoutHeaderShowValue.text =
            ReadTipConfig.getHeaderModes(context)[ReadTipConfig.headerMode].orEmpty()
        tvLayoutFooterShowValue.text =
            ReadTipConfig.getFooterModes(context)[ReadTipConfig.footerMode].orEmpty()
        tvLayoutTipColorValue.text = if (ReadTipConfig.tipColor == 0) {
            ReadTipConfig.tipColorNames.firstOrNull().orEmpty()
        } else {
            "#${ReadTipConfig.tipColor.hexString}"
        }
        tvLayoutTipDividerColorValue.text = when (ReadTipConfig.tipDividerColor) {
            -1, 0 -> ReadTipConfig.tipDividerColorNames
                .getOrElse(ReadTipConfig.tipDividerColor + 1) { "" }
            else -> "#${ReadTipConfig.tipDividerColor.hexString}"
        }
    }

    private fun updatePageTurnControls() = binding.run {
        configureOptionButton(panelPageAnim, false)
        configureOptionButton(panelPageAutoPage, false)
        configureOptionButton(panelPageTouchSlop, AppConfig.pageTouchSlop > 0)
        configureOptionButton(panelPageVolumeKey, AppConfig.volumeKeyPage)
        configureOptionButton(panelPageMouseWheel, AppConfig.mouseWheelPage)
    }

    private fun updateThemeControlsFromConfig() = binding.run {
        seekThemeFontWeight.progress = ReadBookConfig.textWeight.coerceIn(0, seekThemeFontWeight.max)
        updateThemeFontWeightValue(seekThemeFontWeight.progress)

        seekThemeTextSize.progress = (ReadBookConfig.textSize - 5)
            .coerceIn(0, seekThemeTextSize.max)
        updateThemeTextSizeValue(seekThemeTextSize.progress)
        updateFontButtons()
    }

    private fun updateThemePresetCards() {
        val currentBgType = ReadBookConfig.durConfig.curBgType()
        val currentBg = ReadBookConfig.durConfig.curBgStr()
        val currentText = ReadBookConfig.durConfig.curTextColor()
        themeCardBindings.zip(themePresets).forEach { (card, preset) ->
            val selected = currentBgType == preset.bgType &&
                    currentBg.equals(preset.bgValue, ignoreCase = true) &&
                    currentText == preset.textColor
            bindThemePresetCard(card, preset, selected)
        }
    }

    private fun bindThemePresetCard(
        card: ViewReadThemeCardBinding,
        preset: ReadMenuThemePreset,
        selected: Boolean
    ) {
        card.themeCardPreview.background = roundedRect(
            preset.backgroundColor,
            12f.dpToPx(),
            if (selected) 2.dpToPx() else 1.dpToPx(),
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.12f)
        )
        card.tvThemeCardTitle.text = preset.previewTitle
        card.tvThemeCardBody.text = preset.previewBody
        card.tvThemeCardLabel.setText(preset.labelRes)
        card.tvThemeCardTitle.setTextColor(preset.textColor)
        card.tvThemeCardBody.setTextColor(ColorUtils.adjustAlpha(preset.textColor, 0.72f))
        card.tvThemeCardLabel.setTextColor(if (selected) context.accentColor else textColor)
        card.ivThemeCardCheck.background = roundedRect(context.accentColor, 13f.dpToPx())
        card.ivThemeCardCheck.isVisible = selected
    }

    private fun applyThemePreset(preset: ReadMenuThemePreset) {
        ReadBookConfig.durConfig.setCurBg(preset.bgType, preset.bgValue)
        ReadBookConfig.durConfig.setCurTextColor(preset.textColor)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5, 6, 9, 11))
        if (AppConfig.readBarStyleFollowPage) {
            postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
        }
        reset()
        if (activeBottomTab != BottomTab.Theme) {
            showBottomPanel(BottomTab.Theme)
        }
    }

    private fun updateThemeFontWeightValue(progress: Int) {
        val resId = when {
            progress >= 67 -> R.string.read_style_weight_bold
            progress <= 33 -> R.string.read_style_weight_light
            else -> R.string.read_style_weight_standard
        }
        binding.tvThemeFontWeightValue.text = context.getString(resId, progress)
    }

    private fun updateThemeTextSizeValue(progress: Int) {
        binding.tvThemeTextSizeValue.text = (progress + 5).toString()
    }

    private fun setSystemFont(systemTypeface: Int) {
        ReadBookConfig.textFont = ""
        AppConfig.systemTypefaces = systemTypeface
        ReadBookConfig.save()
        updateFontButtons()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 5))
    }

    private fun updateFontButtons() = binding.run {
        updateFontSampleCards()
    }

    private fun updateFontSampleCards() {
        fontSampleBindings.forEach { sample ->
            bindFontSampleCard(sample, sample.isSelected())
        }
    }

    private fun bindFontSampleCard(sample: FontSample, selected: Boolean) {
        val card = sample.binding
        card.themeCardPreview.background = roundedRect(
            ColorUtils.adjustAlpha(textColor, if (selected) 0.18f else 0.06f),
            12f.dpToPx(),
            if (selected) 2.dpToPx() else 1.dpToPx(),
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.14f)
        )
        if (sample.titleRes == R.string.read_menu_add_custom_font) {
            card.tvThemeCardTitle.text = "+"
            card.tvThemeCardBody.setText(R.string.read_style_font_custom)
        } else {
            card.tvThemeCardTitle.text = context.getString(R.string.chapter_list)
            card.tvThemeCardBody.text = "夜色微凉，星光洒落..."
        }
        card.tvThemeCardLabel.setText(sample.titleRes)
        card.tvThemeCardTitle.typeface = sample.typeface
        card.tvThemeCardBody.typeface = sample.typeface
        card.tvThemeCardTitle.setTextColor(textColor)
        card.tvThemeCardBody.setTextColor(ColorUtils.adjustAlpha(textColor, 0.72f))
        card.tvThemeCardLabel.setTextColor(if (selected) context.accentColor else textColor)
        card.ivThemeCardCheck.background = roundedRect(context.accentColor, 13f.dpToPx())
        card.ivThemeCardCheck.isVisible = selected
    }

    private fun updateBackgroundSampleCards() {
        val currentBgType = ReadBookConfig.durConfig.curBgType()
        val currentBg = ReadBookConfig.durConfig.curBgStr()
        backgroundSampleBindings.forEach { sample ->
            bindBackgroundSampleCard(
                sample,
                currentBgType == 1 && currentBg.equals(sample.assetName, ignoreCase = true)
            )
        }
    }

    private fun bindBackgroundSampleCard(sample: BackgroundSample, selected: Boolean) {
        val card = sample.binding
        kotlin.runCatching {
            ImageLoader.load(
                context,
                context.assets.open("bg${File.separator}${sample.assetName}").readBytes()
            ).centerCrop().into(card.ivBackgroundCardImage)
        }
        card.backgroundCardPreview.background = roundedRect(
            ColorUtils.adjustAlpha(textColor, if (selected) 0.16f else 0.06f),
            12f.dpToPx(),
            if (selected) 2.dpToPx() else 1.dpToPx(),
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.14f)
        )
        card.backgroundCardScrim.setBackgroundColor(
            ColorUtils.adjustAlpha(Color.BLACK, if (selected) 0.10f else 0.20f)
        )
        card.tvBackgroundCardLabel.text = sample.assetName.substringBeforeLast(".")
        card.tvBackgroundCardLabel.setTextColor(if (selected) context.accentColor else textColor)
        card.ivBackgroundCardCheck.background = roundedRect(context.accentColor, 13f.dpToPx())
        card.ivBackgroundCardCheck.isVisible = selected
        card.root.setOnClickListener {
            ReadBookConfig.durConfig.setCurBg(1, sample.assetName)
            ReadBookConfig.save()
            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 3, 5, 6, 9, 11))
            if (AppConfig.readBarStyleFollowPage) {
                postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
            }
            updateBackgroundSampleCards()
        }
    }

    private fun updateBackgroundControlsFromConfig() = binding.run {
        seekBackgroundBrightness.progress = ReadBookConfig.bgBrightness
            .coerceIn(0, seekBackgroundBrightness.max)
        updatePercentValue(tvBackgroundBrightnessValue, seekBackgroundBrightness.progress)
        seekBackgroundSaturation.progress = ReadBookConfig.bgSaturation
            .coerceIn(0, seekBackgroundSaturation.max)
        updatePercentValue(tvBackgroundSaturationValue, seekBackgroundSaturation.progress)
        seekBackgroundAlpha.progress = ReadBookConfig.bgAlpha.coerceIn(0, seekBackgroundAlpha.max)
        updatePercentValue(tvBackgroundAlphaValue, seekBackgroundAlpha.progress)
    }

    private fun updatePercentValue(view: TextView, progress: Int) {
        view.text = "${progress.coerceIn(0, 100)}%"
    }

    private fun setFontWeight(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekThemeFontWeight.max)
        ReadBookConfig.textWeight = value
        ReadBookConfig.textBold = when {
            value >= 67 -> 1
            value <= 33 -> 2
            else -> 0
        }
        seekThemeFontWeight.progress = value
        updateThemeFontWeightValue(value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 9, 6))
    }

    private fun setTextSize(progress: Int) {
        ReadBookConfig.textSize = progress + 5
        updateThemeTextSizeValue(progress)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutLetterSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutLetterSpacing.max)
        ReadBookConfig.letterSpacing = value / 10f
        updateLayoutDecimalValue(tvLayoutLetterSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutLineSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutLineSpacing.max)
        ReadBookConfig.lineSpacingExtra = value
        updateLayoutDecimalValue(tvLayoutLineSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutParagraphSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutParagraphSpacing.max)
        ReadBookConfig.paragraphSpacing = value
        updateLayoutDecimalValue(tvLayoutParagraphSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutBodyPadding(progress: Int, seekBar: SeekBar, valueView: TextView, setter: (Int) -> Unit) {
        val value = progress.coerceIn(0, seekBar.max)
        setter(value)
        updateLayoutIntValue(valueView, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
    }

    private fun setLayoutTitleSize(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutTitleSize.max)
        ReadBookConfig.titleSize = value
        updateLayoutIntValue(tvLayoutTitleSizeValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutTitleTopSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutTitleTopSpacing.max)
        ReadBookConfig.titleTopSpacing = value
        updateLayoutIntValue(tvLayoutTitleTopSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutTitleBottomSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutTitleBottomSpacing.max)
        ReadBookConfig.titleBottomSpacing = value
        updateLayoutIntValue(tvLayoutTitleBottomSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun setLayoutTitleMode(mode: Int) {
        ReadBookConfig.titleMode = mode
        ReadBookConfig.save()
        updateTitleModeButtons()
        postEvent(EventBus.UP_CONFIG, arrayListOf(5))
    }

    private fun setLayoutTipPadding(progress: Int, seekBar: SeekBar, valueView: TextView, setter: (Int) -> Unit) {
        val value = progress.coerceIn(0, seekBar.max)
        setter(value)
        updateLayoutIntValue(valueView, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
    }

    private fun cycleHeaderDisplay() {
        val modes = ReadTipConfig.getHeaderModes(context).keys.toList()
        val next = modes.nextAfter(ReadTipConfig.headerMode)
        ReadTipConfig.headerMode = next
        ReadBookConfig.save()
        updateTipSettingValues()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
    }

    private fun cycleFooterDisplay() {
        val modes = ReadTipConfig.getFooterModes(context).keys.toList()
        val next = modes.nextAfter(ReadTipConfig.footerMode)
        ReadTipConfig.footerMode = next
        ReadBookConfig.save()
        updateTipSettingValues()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
    }

    private fun List<Int>.nextAfter(current: Int): Int {
        if (isEmpty()) return current
        val index = indexOf(current).takeIf { it >= 0 } ?: 0
        return get((index + 1) % size)
    }

    private fun bindBackgroundSeek(
        seekBar: SeekBar,
        valueView: TextView,
        onStop: (Int) -> Unit
    ) {
        seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updatePercentValue(valueView, progress)
                if (fromUser) onStop(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun configureOptionButton(view: TextView, selected: Boolean) {
        view.background = roundedRect(
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.06f),
            12f.dpToPx(),
            1.dpToPx(),
            if (selected) context.accentColor else ColorUtils.adjustAlpha(textColor, 0.14f)
        )
        view.setTextColor(if (selected) Color.WHITE else textColor)
    }

    private fun roundedRect(
        color: Int,
        radius: Float,
        strokeWidth: Int = 0,
        strokeColor: Int = Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun configureBottomTabFrostedGlass() = binding.run {
        bottomTabBar.clipToOutline = true
        if (AppConfig.isEInkMode) {
            val glassStyleKey = "eink:$bgColor:$textColor:${context.accentColor}"
            if (bottomTabGlassStyleKey != glassStyleKey) {
                bottomTabBar.backgroundTintList = null
                bottomTabBar.foreground = null
                bottomTabGlassView.gone()
                bottomTabShellOverlay.gone()
                bottomTabIndicatorContainer.background = bottomTabIndicatorBackground()
                bottomTabBar.background = roundedRect(
                    bgColor,
                    28f.dpToPx()
                )
                bottomTabGlassStyleKey = glassStyleKey
            }
            return@run
        }

        val glassLevel = (AppConfig.frostedGlassLevel / 100f)
            .coerceIn(0.45f, 1f)
        val glassStyleKey = "glass:${bottomTabUseDarkGlass()}:${context.accentColor}:${AppConfig.frostedGlassLevel}"
        if (bottomTabGlassStyleKey != glassStyleKey) {
            bottomTabBar.backgroundTintList = null
            bottomTabBar.foreground = null
            bottomTabBar.background = bottomTabGlassFallbackShell(glassLevel)
            bottomTabGlassView.visible()
            bottomTabShellOverlay.visible()
            bottomTabShellOverlay.background = bottomTabGlassShell(glassLevel)
            bottomTabIndicatorContainer.background = bottomTabIndicatorBackground()
            bottomTabGlassStyleKey = glassStyleKey
            bottomTabBar.post {
                setupBottomTabFrostedGlassViews(glassLevel)
            }
        } else {
            if (!bottomTabGlassView.isVisible) {
                bottomTabGlassView.visible()
            }
            if (!bottomTabShellOverlay.isVisible) {
                bottomTabShellOverlay.visible()
            }
            if (!boundBottomTabGlassViewIds.contains(bottomTabGlassView.id)) {
                bottomTabBar.post {
                    setupBottomTabFrostedGlassViews(glassLevel)
                }
            }
        }
    }

    private fun setupBottomTabFrostedGlassViews(glassLevel: Float) = binding.run {
        val target = bottomTabGlassTarget()
        if (!target.isLaidOut || !bottomTabBar.isLaidOut) {
            return@run
        }
        val tintColor = bottomTabGlassTintColor()

        setupBottomTabFrostedGlassView(
            liquidGlassView = bottomTabGlassView,
            target = target,
            cornerRadius = 28f.dpToPx(),
            refractionHeight = (12f + glassLevel * 8f).dpToPx(),
            refractionOffset = (34f + glassLevel * 18f).dpToPx(),
            blurRadius = (22f + glassLevel * 30f).dpToPx(),
            dispersion = (0.18f + glassLevel * 0.16f).coerceAtMost(0.42f),
            tintAlpha = bottomTabGlassTintAlpha(glassLevel),
            tintColor = tintColor
        )
    }

    private fun bottomTabGlassTarget(): ViewGroup {
        val readView = activity?.findViewById<View>(R.id.read_view)
        return activity?.findViewById<ViewGroup>(R.id.read_content_container)
            ?: readView?.parent as? ViewGroup
            ?: parent as? ViewGroup
            ?: binding.vwMenuRoot
    }

    private fun setupBottomTabFrostedGlassView(
        liquidGlassView: LiquidGlassView,
        target: ViewGroup,
        cornerRadius: Float,
        refractionHeight: Float,
        refractionOffset: Float,
        blurRadius: Float,
        dispersion: Float,
        tintAlpha: Float,
        tintColor: FloatArray
    ) {
        if (boundBottomTabGlassViewIds.add(liquidGlassView.id)) {
            liquidGlassView.bind(target)
        }
        liquidGlassView.setCornerRadius(cornerRadius)
        liquidGlassView.setRefractionHeight(refractionHeight)
        liquidGlassView.setRefractionOffset(refractionOffset)
        liquidGlassView.setDispersion(dispersion)
        liquidGlassView.setBlurRadius(blurRadius)
        liquidGlassView.setTintAlpha(tintAlpha)
        liquidGlassView.setTintColorRed(tintColor[0])
        liquidGlassView.setTintColorGreen(tintColor[1])
        liquidGlassView.setTintColorBlue(tintColor[2])
        liquidGlassView.setDraggableEnabled(false)
        liquidGlassView.setElasticEnabled(false)
        liquidGlassView.setTouchEffectEnabled(false)
        liquidGlassView.isClickable = false
        liquidGlassView.isFocusable = false
        liquidGlassView.invalidate()
    }

    private fun bottomTabGlassShell(glassLevel: Float): GradientDrawable {
        val isDark = bottomTabUseDarkGlass()
        val surfaceColor = if (isDark) {
            bottomTabDarkGlassSurfaceColor()
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val topAlpha = if (isDark) {
            0.52f + glassLevel * 0.18f
        } else {
            0.28f + glassLevel * 0.16f
        }
        val centerAlpha = if (isDark) {
            0.44f + glassLevel * 0.16f
        } else {
            0.20f + glassLevel * 0.12f
        }
        val bottomAlpha = if (isDark) {
            0.34f + glassLevel * 0.14f
        } else {
            0.14f + glassLevel * 0.10f
        }
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.withAlpha(surfaceColor, topAlpha.coerceAtMost(if (isDark) 0.70f else 0.44f)),
                ColorUtils.withAlpha(surfaceColor, centerAlpha.coerceAtMost(if (isDark) 0.60f else 0.34f)),
                ColorUtils.withAlpha(surfaceColor, bottomAlpha.coerceAtMost(if (isDark) 0.50f else 0.26f))
            )
        ).apply {
            cornerRadius = 28f.dpToPx()
        }
    }

    private fun bottomTabGlassFallbackShell(glassLevel: Float): GradientDrawable {
        val isDark = bottomTabUseDarkGlass()
        val surfaceColor = if (isDark) {
            bottomTabDarkGlassSurfaceColor()
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val alpha = if (isDark) {
            0.58f + glassLevel * 0.16f
        } else {
            0.26f + glassLevel * 0.18f
        }
        return GradientDrawable().apply {
            cornerRadius = 28f.dpToPx()
            setColor(ColorUtils.withAlpha(surfaceColor, alpha.coerceAtMost(if (isDark) 0.74f else 0.46f)))
        }
    }

    private fun bottomTabDarkGlassSurfaceColor(): Int {
        return Color.rgb(8, 10, 14)
    }

    private fun bottomTabGlassTintAlpha(glassLevel: Float): Float {
        return if (bottomTabUseDarkGlass()) {
            (0.22f + glassLevel * 0.22f).coerceAtMost(0.44f)
        } else {
            (0.12f + glassLevel * 0.18f).coerceAtMost(0.30f)
        }
    }

    private fun bottomTabGlassTintColor(): FloatArray {
        return if (bottomTabUseDarkGlass()) {
            floatArrayOf(0.08f, 0.10f, 0.14f)
        } else {
            floatArrayOf(0.70f, 0.79f, 0.86f)
        }
    }

    private fun bottomTabIndicatorBackground(): GradientDrawable {
        return roundedRect(context.accentColor, 18f.dpToPx())
    }

    private fun bottomTabUseDarkGlass(): Boolean {
        return AppConfig.isNightTheme && !AppConfig.isEInkMode
    }

    private fun bottomTabContentColor(): Int {
        return if (bottomTabUseDarkGlass()) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun bottomTabSelectedContentColor(): Int {
        return if (ColorUtils.isColorLight(context.accentColor)) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    private fun openFontSelectDialog() {
        hideExpandedPanel(anim = false)
        activity?.showDialogFragment<FontSelectDialog>()
    }

    private fun bindLayoutDecimalSeek(
        seekBar: SeekBar,
        valueView: TextView,
        onStop: (Int) -> Unit
    ) {
        seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateLayoutDecimalValue(valueView, progress)
                if (fromUser) onStop(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun bindLayoutIntSeek(
        seekBar: SeekBar,
        valueView: TextView,
        onStop: (Int) -> Unit
    ) {
        seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateLayoutIntValue(valueView, progress)
                if (fromUser) onStop(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun setupBottomNavigationEvents() = binding.run {
        readBottomPrimaryNav.setOnItemSelectedListener { item ->
            if (suppressBottomNavSelection) {
                return@setOnItemSelectedListener true
            }
            flashBottomTabIndicator(readBottomPrimaryNav, item.itemId)
            when (item.itemId) {
                R.id.menu_read_search -> {
                    toggleBottomTab(BottomTab.Search)
                }

                R.id.menu_read_toc -> {
                    toggleBottomTab(BottomTab.Toc)
                }

                R.id.menu_read_aloud -> {
                    runMenuOut {
                        callBack.showReadAloudDialog()
                    }
                    false
                }

                R.id.menu_read_interface -> {
                    if (activeBottomTab == BottomTab.Toc) {
                        hideExpandedPanel(anim = false)
                    }
                    switchBottomTabMode(BottomTabMode.Interface)
                    true
                }

                R.id.menu_read_settings -> {
                    runMenuOut {
                        callBack.showMoreSetting()
                    }
                    false
                }

                else -> false
            }
        }
        readBottomPrimaryNav.setOnItemReselectedListener { item ->
            if (!suppressBottomNavSelection) {
                when (item.itemId) {
                    R.id.menu_read_search -> toggleBottomTab(BottomTab.Search)
                    R.id.menu_read_toc -> toggleBottomTab(BottomTab.Toc)
                    R.id.menu_read_interface -> {
                        flashBottomTabIndicator(readBottomPrimaryNav, item.itemId)
                        switchBottomTabMode(BottomTabMode.Interface)
                    }
                }
            }
        }
        readBottomInterfaceNav.setOnItemSelectedListener { item ->
            if (suppressBottomNavSelection) {
                return@setOnItemSelectedListener true
            }
            if (item.itemId == R.id.menu_read_interface_back) {
                hideExpandedPanel(returnToPrimary = true)
                return@setOnItemSelectedListener false
            }
            val tab = item.itemId.toBottomTab() ?: return@setOnItemSelectedListener false
            toggleBottomTab(tab)
        }
        readBottomInterfaceNav.setOnItemReselectedListener { item ->
            if (suppressBottomNavSelection) {
                return@setOnItemReselectedListener
            }
            if (item.itemId == R.id.menu_read_interface_back) {
                hideExpandedPanel(returnToPrimary = true)
                return@setOnItemReselectedListener
            }
            item.itemId.toBottomTab()?.let { tab ->
                if (activeBottomTab == tab && flExpandedPanel.isVisible) {
                    hideExpandedPanel(returnToPrimary = true)
                } else {
                    showBottomPanel(tab)
                }
            }
        }
    }

    private fun bindEvent() = binding.run {
        vwMenuBg.setOnClickListener { handleBackgroundDismiss() }
        titleBar.toolbar.setOnClickListener {
            callBack.openBookInfoActivity()
        }
        val chapterViewClickListener = OnClickListener {
            if (ReadBook.isLocalBook) {
                return@OnClickListener
            }
            if (AppConfig.readUrlInBrowser) {
                context.openUrl(tvChapterUrl.text.toString().substringBefore(",{"))
            } else {
                Coroutine.async {
                    context.startActivity<WebViewActivity> {
                        val url = tvChapterUrl.text.toString()
                        val bookSource = ReadBook.bookSource
                        putExtra("title", tvChapterName.text)
                        putExtra("url", url)
                        putExtra("sourceOrigin", bookSource?.bookSourceUrl)
                        putExtra("sourceName", bookSource?.bookSourceName)
                        putExtra("sourceType", bookSource?.getSourceType())
                    }
                }
            }
        }
        val chapterViewLongClickListener = OnLongClickListener {
            if (ReadBook.isLocalBook) {
                return@OnLongClickListener true
            }
            context.alert(R.string.open_fun) {
                setMessage(R.string.use_browser_open)
                okButton {
                    AppConfig.readUrlInBrowser = true
                }
                noButton {
                    AppConfig.readUrlInBrowser = false
                }
            }
            true
        }
        tvChapterName.setOnClickListener(chapterViewClickListener)
        tvChapterName.setOnLongClickListener(chapterViewLongClickListener)
        tvChapterUrl.setOnClickListener(chapterViewClickListener)
        tvChapterUrl.setOnLongClickListener(chapterViewLongClickListener)
        //书源操作
        tvSourceAction.onClick {
            sourceMenu.menu.findItem(R.id.menu_login).isVisible =
                !ReadBook.bookSource?.loginUrl.isNullOrEmpty()
            sourceMenu.menu.findItem(R.id.menu_chapter_pay).isVisible =
                !ReadBook.bookSource?.loginUrl.isNullOrEmpty()
                        && ReadBook.curTextChapter?.isVip == true
                        && ReadBook.curTextChapter?.isPay != true
            sourceMenu.show()
        }
        //亮度跟随
        ivBrightnessAuto.setOnClickListener {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seekBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setScreenBrightness(progress.toFloat())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppConfig.readBrightness = seekBar.progress
            }

        })
        vwBrightnessPosAdjust.setOnClickListener {
            AppConfig.brightnessVwPos = !AppConfig.brightnessVwPos
            upBrightnessVwPos()
        }
        //目录
        setupBottomNavigationEvents()

        //朗读
        panelPageAnim.setOnClickListener {
            runMenuOut {
                activity?.let { owner ->
                    if (owner is BaseReadBookActivity) {
                        owner.showPageAnimConfig {
                            ReadBook.loadContent(resetPageOffset = false)
                        }
                    }
                }
            }
        }
        panelPageAutoPage.setOnClickListener {
            runMenuOut {
                callBack.autoPage()
            }
        }
        panelPageTouchSlop.setOnClickListener {
            NumberPickerDialog(context)
                .setTitle(context.getString(R.string.page_touch_slop_dialog_title))
                .setMaxValue(9999)
                .setMinValue(0)
                .setValue(AppConfig.pageTouchSlop)
                .show {
                    AppConfig.pageTouchSlop = it
                    postEvent(EventBus.UP_CONFIG, arrayListOf(4))
                    updatePageTurnControls()
                }
        }
        panelPageVolumeKey.setOnClickListener {
            context.putPrefBoolean(PreferKey.volumeKeyPage, !AppConfig.volumeKeyPage)
            updatePageTurnControls()
        }
        panelPageMouseWheel.setOnClickListener {
            context.putPrefBoolean(PreferKey.mouseWheelPage, !AppConfig.mouseWheelPage)
            updatePageTurnControls()
        }
        panelMoreSearch.setOnClickListener {
            showBottomPanel(BottomTab.Search)
        }
        panelMoreAutoPage.setOnClickListener {
            runMenuOut {
                callBack.autoPage()
            }
        }
        panelMoreReplace.setOnClickListener {
            callBack.openReplaceRule()
        }
        panelMoreSettings.setOnClickListener {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }
        themeCardBindings.zip(themePresets).forEach { (card, preset) ->
            card.root.setOnClickListener {
                activeThemeTab = ThemeTab.Preset
                applyThemePreset(preset)
            }
        }
        llThemeTabPreset.setOnClickListener {
            activeThemeTab = ThemeTab.Preset
            renderThemeTabs()
            panelTheme.smoothScrollTo(0, 0)
        }
        llThemeTabCustom.setOnClickListener {
            activeThemeTab = ThemeTab.Custom
            renderThemeTabs()
            panelTheme.post { panelTheme.smoothScrollTo(0, hsvThemePresets.top) }
        }
        llThemeTabEye.setOnClickListener {
            activeThemeTab = ThemeTab.Eye
            renderThemeTabs()
            themePresets.firstOrNull { it.key == "eye_green" }?.let(::applyThemePreset)
        }
        fontSampleBindings.forEach { sample ->
            sample.binding.root.setOnClickListener { sample.onClick() }
        }
        bindLayoutDecimalSeek(
            seekLayoutLetterSpacing,
            tvLayoutLetterSpacingValue,
            ::setLayoutLetterSpacing
        )
        bindLayoutDecimalSeek(
            seekLayoutLineSpacing,
            tvLayoutLineSpacingValue,
            ::setLayoutLineSpacing
        )
        bindLayoutDecimalSeek(
            seekLayoutParagraphSpacing,
            tvLayoutParagraphSpacingValue,
            ::setLayoutParagraphSpacing
        )
        bindLayoutIntSeek(seekLayoutPaddingTop, tvLayoutPaddingTopValue) {
            setLayoutBodyPadding(it, seekLayoutPaddingTop, tvLayoutPaddingTopValue) { value ->
                ReadBookConfig.paddingTop = value
            }
        }
        bindLayoutIntSeek(seekLayoutPaddingBottom, tvLayoutPaddingBottomValue) {
            setLayoutBodyPadding(it, seekLayoutPaddingBottom, tvLayoutPaddingBottomValue) { value ->
                ReadBookConfig.paddingBottom = value
            }
        }
        bindLayoutIntSeek(seekLayoutPaddingLeft, tvLayoutPaddingLeftValue) {
            setLayoutBodyPadding(it, seekLayoutPaddingLeft, tvLayoutPaddingLeftValue) { value ->
                ReadBookConfig.paddingLeft = value
            }
        }
        bindLayoutIntSeek(seekLayoutPaddingRight, tvLayoutPaddingRightValue) {
            setLayoutBodyPadding(it, seekLayoutPaddingRight, tvLayoutPaddingRightValue) { value ->
                ReadBookConfig.paddingRight = value
            }
        }
        bindLayoutIntSeek(seekLayoutTitleSize, tvLayoutTitleSizeValue, ::setLayoutTitleSize)
        bindLayoutIntSeek(
            seekLayoutTitleTopSpacing,
            tvLayoutTitleTopSpacingValue,
            ::setLayoutTitleTopSpacing
        )
        bindLayoutIntSeek(
            seekLayoutTitleBottomSpacing,
            tvLayoutTitleBottomSpacingValue,
            ::setLayoutTitleBottomSpacing
        )
        tvLayoutTitleModeLeft.setOnClickListener { setLayoutTitleMode(0) }
        tvLayoutTitleModeCenter.setOnClickListener { setLayoutTitleMode(1) }
        tvLayoutTitleModeAdvanced.setOnClickListener {
            setLayoutTitleMode(AdvancedTitleConfig.TITLE_MODE_ADVANCED)
        }
        tvLayoutTitleModeHide.setOnClickListener { setLayoutTitleMode(2) }
        bindLayoutIntSeek(seekLayoutHeaderPaddingTop, tvLayoutHeaderPaddingTopValue) {
            setLayoutTipPadding(it, seekLayoutHeaderPaddingTop, tvLayoutHeaderPaddingTopValue) { value ->
                ReadBookConfig.headerPaddingTop = value
            }
        }
        bindLayoutIntSeek(seekLayoutHeaderPaddingBottom, tvLayoutHeaderPaddingBottomValue) {
            setLayoutTipPadding(it, seekLayoutHeaderPaddingBottom, tvLayoutHeaderPaddingBottomValue) { value ->
                ReadBookConfig.headerPaddingBottom = value
            }
        }
        bindLayoutIntSeek(seekLayoutFooterPaddingTop, tvLayoutFooterPaddingTopValue) {
            setLayoutTipPadding(it, seekLayoutFooterPaddingTop, tvLayoutFooterPaddingTopValue) { value ->
                ReadBookConfig.footerPaddingTop = value
            }
        }
        bindLayoutIntSeek(seekLayoutFooterPaddingBottom, tvLayoutFooterPaddingBottomValue) {
            setLayoutTipPadding(it, seekLayoutFooterPaddingBottom, tvLayoutFooterPaddingBottomValue) { value ->
                ReadBookConfig.footerPaddingBottom = value
            }
        }
        tvLayoutHeaderLineToggle.setOnClickListener {
            ReadBookConfig.showHeaderLine = !ReadBookConfig.showHeaderLine
            ReadBookConfig.save()
            configureOptionButton(tvLayoutHeaderLineToggle, ReadBookConfig.showHeaderLine)
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        tvLayoutFooterLineToggle.setOnClickListener {
            ReadBookConfig.showFooterLine = !ReadBookConfig.showFooterLine
            ReadBookConfig.save()
            configureOptionButton(tvLayoutFooterLineToggle, ReadBookConfig.showFooterLine)
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        llLayoutTipHeaderShow.setOnClickListener { cycleHeaderDisplay() }
        llLayoutTipFooterShow.setOnClickListener { cycleFooterDisplay() }
        bindBackgroundSeek(seekBackgroundBrightness, tvBackgroundBrightnessValue) {
            ReadBookConfig.bgBrightness = it.coerceIn(0, 100)
            ReadBookConfig.save()
            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 3))
        }
        bindBackgroundSeek(seekBackgroundSaturation, tvBackgroundSaturationValue) {
            ReadBookConfig.bgSaturation = it.coerceIn(0, 100)
            ReadBookConfig.save()
            postEvent(EventBus.UP_CONFIG, arrayListOf(1, 3))
        }
        bindBackgroundSeek(seekBackgroundAlpha, tvBackgroundAlphaValue) {
            ReadBookConfig.bgAlpha = it.coerceIn(0, 100)
            ReadBookConfig.save()
            postEvent(EventBus.UP_CONFIG, arrayListOf(3))
        }
        seekThemeFontWeight.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeFontWeightValue(progress)
                if (fromUser) setFontWeight(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
        seekThemeTextSize.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeTextSizeValue(progress)
                if (fromUser) setTextSize(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun initAnimation() {
        menuTopIn.setAnimationListener(menuInListener)
        menuTopOut.setAnimationListener(menuOutListener)
    }

    fun upBookView() {
        binding.titleBar.title = ReadBook.curTextChapter?.title ?: ReadBook.book?.name
        ReadBook.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            binding.tvChapterName.visible()
            if (!ReadBook.isLocalBook) {
                binding.tvChapterUrl.text = it.chapter.getAbsoluteURL()
                binding.tvChapterUrl.visible()
            } else {
                binding.tvChapterUrl.gone()
            }
            upSeekBar()
            binding.tvTocPrevChapter.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvTocNextChapter.isEnabled = ReadBook.durChapterIndex != ReadBook.simulatedChapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
            binding.tvChapterUrl.gone()
        }
    }

    fun upSeekBar() {
        binding.seekTocProgress.apply {
            val behavior = if (tocProgressWholeBook) "chapter" else AppConfig.progressBarBehavior
            binding.tocProgressModeToggle.text = if (behavior == "chapter") {
                context.getString(R.string.read_menu_whole_book)
            } else {
                context.getString(R.string.chapter)
            }
            when (behavior) {
                "page" -> {
                    ReadBook.curTextChapter?.let {
                        max = it.pageSize.minus(1)
                        progress = ReadBook.durPageIndex
                    }
                }

                "chapter" -> {
                    max = ReadBook.simulatedChapterSize - 1
                    progress = ReadBook.durChapterIndex
                }
            }
        }
    }

    fun setSeekPage(seek: Int) {
        binding.seekTocProgress.progress = seek
    }

    fun setAutoPage(autoPage: Boolean) = binding.run {
        if (autoPage) {
            panelMoreAutoPage.text = context.getString(R.string.auto_next_page_stop)
            panelMoreAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
            panelPageAutoPage.text = context.getString(R.string.auto_next_page_stop)
            panelPageAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            panelMoreAutoPage.text = context.getString(R.string.auto_next_page)
            panelMoreAutoPage.contentDescription = context.getString(R.string.auto_next_page)
            panelPageAutoPage.text = context.getString(R.string.auto_next_page)
            panelPageAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        panelMoreAutoPage.setTextColor(textColor)
        configureOptionButton(panelPageAutoPage, autoPage)
    }

    private fun upBrightnessVwPos() {
        if (AppConfig.brightnessVwPos) {
            binding.root.modifyBegin()
                .clear(R.id.ll_brightness, ConstraintModify.Anchor.LEFT)
                .rightToRightOf(R.id.ll_brightness, R.id.vw_menu_root)
                .commit()
        } else {
            binding.root.modifyBegin()
                .clear(R.id.ll_brightness, ConstraintModify.Anchor.RIGHT)
                .leftToLeftOf(R.id.ll_brightness, R.id.vw_menu_root)
                .commit()
        }
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openInlineSearchResult(searchResult: SearchResult, results: List<SearchResult>, index: Int)
        fun openSourceEditActivity()
        fun openBookInfoActivity()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
        fun skipToChapter(index: Int)
        fun onMenuShow()
        fun onMenuHide()
    }

    private class ReadMenuTocAdapter(
        private val onChapterClick: (BookChapter) -> Unit
    ) : RecyclerView.Adapter<ReadMenuTocAdapter.Holder>() {

        private val chapters = arrayListOf<BookChapter>()

        fun submit(items: List<BookChapter>) {
            chapters.clear()
            chapters.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val context = parent.context
            val row = LinearLayout(context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                minimumHeight = 54.dpToPx()
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setPadding(4.dpToPx(), 6.dpToPx(), 4.dpToPx(), 6.dpToPx())
            }
            val textColumn = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                orientation = LinearLayout.VERTICAL
            }
            val title = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                textSize = 15f
            }
            val meta = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dpToPx()
                }
                includeFontPadding = false
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                textSize = 12f
            }
            val download = AppCompatImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(42.dpToPx(), 42.dpToPx()).apply {
                    marginStart = 8.dpToPx()
                }
                background = null
                setImageResource(R.drawable.ic_lucide_download)
                contentDescription = context.getString(R.string.read_menu_download_chapter)
                scaleType = android.widget.ImageView.ScaleType.CENTER
            }
            textColumn.addView(title)
            textColumn.addView(meta)
            row.addView(textColumn)
            row.addView(download)
            return Holder(row, title, meta, download)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val chapter = chapters[position]
            val context = holder.title.context
            val selected = chapter.index == ReadBook.durChapterIndex
            val book = ReadBook.book
            val downloaded = book != null && BookHelp.hasContent(book, chapter)
            holder.title.text = chapter.title
            holder.title.setTypeface(null, if (chapter.isVolume) Typeface.BOLD else Typeface.NORMAL)
            holder.title.setTextColor(
                when {
                    selected -> context.accentColor
                    AppConfig.isNightTheme -> ColorUtils.adjustAlpha(Color.WHITE, 0.86f)
                    else -> ColorUtils.adjustAlpha(Color.BLACK, 0.82f)
                }
            )
            holder.meta.text = buildTocMeta(chapter, downloaded)
            holder.meta.setTextColor(
                when {
                    selected -> ColorUtils.adjustAlpha(context.accentColor, 0.72f)
                    AppConfig.isNightTheme -> ColorUtils.adjustAlpha(Color.WHITE, 0.52f)
                    else -> ColorUtils.adjustAlpha(Color.BLACK, 0.48f)
                }
            )
            holder.download.isVisible = !chapter.isVolume && !downloaded
            holder.download.setColorFilter(
                if (selected) context.accentColor else holder.title.currentTextColor,
                PorterDuff.Mode.SRC_IN
            )
            holder.download.setOnClickListener {
                ReadBook.loadContent(chapter.index, upContent = false) {
                    holder.itemView.post {
                        val adapterPosition = holder.bindingAdapterPosition
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            notifyItemChanged(adapterPosition)
                        }
                    }
                }
            }
            holder.itemView.setOnClickListener {
                onChapterClick(chapter)
            }
        }

        override fun getItemCount(): Int = chapters.size

        private fun buildTocMeta(chapter: BookChapter, downloaded: Boolean): String {
            val parts = arrayListOf<String>()
            chapter.tag?.takeIf { it.isNotBlank() }?.let(parts::add)
            if (downloaded) {
                chapter.wordCount?.takeIf { it.isNotBlank() }?.let(parts::add)
            }
            return parts.joinToString("  ")
        }

        class Holder(
            itemView: View,
            val title: TextView,
            val meta: TextView,
            val download: AppCompatImageButton
        ) : RecyclerView.ViewHolder(itemView)
    }

}
