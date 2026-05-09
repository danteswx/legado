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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
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
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
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
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.ConstraintModify
import io.legado.app.utils.activity
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.gone
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
import java.util.Locale
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
    private var confirmSkipToChapter: Boolean = false
    private var isMenuOutAnimating = false
    private enum class BottomTab {
        Toc,
        Font,
        Layout,
        Theme,
        Page,
        More
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

    private enum class LayoutTab {
        Font,
        Spacing,
        Style
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
    private var activeThemeTab: ThemeTab = ThemeTab.Preset
    private var activeLayoutTab: LayoutTab = LayoutTab.Spacing
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
        tvPre.setTextColor(textColor)
        tvNext.setTextColor(textColor)
        tvPanelLayoutTitle.setTextColor(textColor)
        tvPanelThemeTitle.setTextColor(textColor)
        tvPanelPageTitle.setTextColor(textColor)
        tvPanelMoreTitle.setTextColor(textColor)
        panelPageAnim.setTextColor(textColor)
        panelMoreSearch.setTextColor(textColor)
        panelMoreAutoPage.setTextColor(textColor)
        panelMoreReplace.setTextColor(textColor)
        panelMoreSettings.setTextColor(textColor)
        tintLayoutPanel(textColor)
        tintThemePanel(textColor)
        renderLayoutTabs()
        renderBottomTabState()
        renderThemeTabs()
        updateLayoutControlsFromConfig()
        updateThemeControlsFromConfig()
        updateThemePresetCards()
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
            binding.seekThemeBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.setColorFilter(context.buttonDisabledColor)
            binding.seekBrightness.isEnabled = true
            binding.seekThemeBrightness.isEnabled = true
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
        if (tab == BottomTab.Toc) {
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
            BottomTab.Toc -> {
                if (previousTab != BottomTab.Toc) {
                    tocPanelFullscreen = false
                }
                loadTocPanel()
            }

            BottomTab.Font -> {
                activeLayoutTab = LayoutTab.Font
                renderLayoutTabs()
            }

            BottomTab.Layout -> {
                activeLayoutTab = LayoutTab.Spacing
                renderLayoutTabs()
            }

            BottomTab.Theme,
            BottomTab.Page,
            BottomTab.More -> Unit
        }
        panelToc.gone(tab != BottomTab.Toc)
        panelLayout.gone(tab != BottomTab.Font && tab != BottomTab.Layout)
        panelTheme.gone(tab != BottomTab.Theme)
        panelPage.gone(tab != BottomTab.Page)
        panelMore.gone(tab != BottomTab.More)
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
        if (tab == BottomTab.Font || tab == BottomTab.Layout || tab == BottomTab.Theme) {
            applyAdaptivePanelHeight(tab, expandedPanelMaxHeight())
        }
    }

    private fun applyAdaptivePanelHeight(tab: BottomTab, maxHeight: Int) = binding.run {
        when (tab) {
            BottomTab.Font,
            BottomTab.Layout -> {
                val measuredHeight = measureBottomPanelHeight(panelLayout, maxHeight)
                if (measuredHeight >= maxHeight) {
                    panelLayout.updateLayoutParams<FrameLayout.LayoutParams> {
                        height = maxHeight
                    }
                    val fixedChromeHeight = measuredOrLayoutHeight(llLayoutTabs) +
                            measuredOrLayoutHeight(vwLayoutTabsDivider) +
                            panelLayout.paddingTop +
                            panelLayout.paddingBottom
                    panelLayoutScroll.updateLayoutParams<LinearLayout.LayoutParams> {
                        height = (maxHeight - fixedChromeHeight).coerceAtLeast(96.dpToPx())
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

            BottomTab.Toc,
            BottomTab.Page,
            BottomTab.More -> Unit
        }
    }

    private fun measuredOrLayoutHeight(view: View): Int {
        return view.measuredHeight.takeIf { it > 0 }
            ?: view.height.takeIf { it > 0 }
            ?: view.layoutParams?.height?.takeIf { it > 0 }
            ?: 0
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
        val currentLayoutTab = activeLayoutTab
        val currentBottomTab = activeBottomTab

        activeLayoutTab = LayoutTab.Font
        renderLayoutTabs()
        panelToc.gone()
        panelLayout.visible()
        panelTheme.gone()
        val fontHeight = measureBottomPanelHeight(panelLayout, maxHeight)

        activeLayoutTab = LayoutTab.Spacing
        renderLayoutTabs()
        val spacingHeight = measureBottomPanelHeight(panelLayout, maxHeight)

        activeLayoutTab = LayoutTab.Style
        renderLayoutTabs()
        val styleHeight = measureBottomPanelHeight(panelLayout, maxHeight)

        panelLayout.gone()
        panelTheme.visible()
        val themeHeight = measureBottomPanelHeight(panelTheme, maxHeight)

        panelTheme.gone()
        panelPage.visible()
        val pageHeight = measureBottomPanelHeight(panelPage, maxHeight)

        panelPage.gone()
        panelMore.visible()
        val moreHeight = measureBottomPanelHeight(panelMore, maxHeight)
        val tocHeight = tocFullPanelHeight()

        activeLayoutTab = currentLayoutTab
        renderLayoutTabs()
        panelToc.gone(currentBottomTab != BottomTab.Toc)
        panelLayout.gone(currentBottomTab != BottomTab.Font && currentBottomTab != BottomTab.Layout)
        panelTheme.gone(currentBottomTab != BottomTab.Theme)
        panelPage.gone(currentBottomTab != BottomTab.Page)
        panelMore.gone(currentBottomTab != BottomTab.More)

        maxOf(fontHeight, spacingHeight, styleHeight, themeHeight, pageHeight, moreHeight, tocHeight)
            .coerceIn(120.dpToPx(), tocFullPanelHeight())
    }

    private fun selectedBottomPanelView(tab: BottomTab): View {
        return when (tab) {
            BottomTab.Toc -> binding.panelToc
            BottomTab.Font -> binding.panelLayout
            BottomTab.Layout -> binding.panelLayout
            BottomTab.Theme -> binding.panelTheme
            BottomTab.Page -> binding.panelPage
            BottomTab.More -> binding.panelMore
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
        readBottomInterfaceBack.setColorFilter(bottomTabContentColor(), PorterDuff.Mode.SRC_IN)
    }

    private fun syncBottomNavigationSelection() = binding.run {
        setBottomNavigationSelection(
            readBottomPrimaryNav,
            when {
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
        readBottomInterfaceBack.animate().cancel()
        if (!animate || wasMode == mode || width <= 0) {
            readBottomPrimaryNav.translationX = if (mode == BottomTabMode.Primary) 0f else -width.toFloat()
            readBottomInterfaceNav.translationX =
                if (mode == BottomTabMode.Interface) 0f else width.toFloat()
            readBottomInterfaceBack.translationX =
                if (mode == BottomTabMode.Interface) 0f else width.toFloat()
            readBottomPrimaryNav.visible(mode == BottomTabMode.Primary)
            readBottomInterfaceNav.visible(mode == BottomTabMode.Interface)
            readBottomInterfaceBack.visible(mode == BottomTabMode.Interface)
            return@run
        }
        when (mode) {
            BottomTabMode.Primary -> {
                readBottomPrimaryNav.visible()
                readBottomPrimaryNav.translationX = -width.toFloat()
                readBottomInterfaceNav.visible()
                readBottomInterfaceNav.translationX = 0f
                readBottomInterfaceBack.visible()
                readBottomInterfaceBack.translationX = 0f
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
                readBottomInterfaceBack.animate()
                    .translationX(width.toFloat())
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .withEndAction {
                        if (bottomTabMode == BottomTabMode.Primary) {
                            readBottomInterfaceBack.invisible()
                        }
                    }
                    .start()
            }

            BottomTabMode.Interface -> {
                readBottomPrimaryNav.visible()
                readBottomPrimaryNav.translationX = 0f
                readBottomInterfaceNav.visible()
                readBottomInterfaceNav.translationX = width.toFloat()
                readBottomInterfaceBack.visible()
                readBottomInterfaceBack.translationX = width.toFloat()
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
                readBottomInterfaceBack.animate()
                    .translationX(0f)
                    .setDuration(220L)
                    .setInterpolator(bottomTabSlideInterpolator)
                    .start()
            }
        }
    }

    private fun BottomTab.menuItemId(): Int {
        return when (this) {
            BottomTab.Toc -> R.id.menu_read_toc
            BottomTab.Font -> R.id.menu_read_font
            BottomTab.Layout -> R.id.menu_read_layout
            BottomTab.Theme -> R.id.menu_read_theme
            BottomTab.Page -> R.id.menu_read_page
            BottomTab.More -> R.id.menu_read_more
        }
    }

    private fun Int.toBottomTab(): BottomTab? {
        return when (this) {
            R.id.menu_read_font -> BottomTab.Font
            R.id.menu_read_layout -> BottomTab.Layout
            R.id.menu_read_theme -> BottomTab.Theme
            R.id.menu_read_page -> BottomTab.Page
            R.id.menu_read_more -> BottomTab.More
            else -> null
        }
    }

    private fun tintLayoutPanel(color: Int) = binding.run {
        listOf(
            tvLayoutTextSpacingTitle,
            tvLayoutPageMarginTitle,
            tvLayoutRegionSpacingTitle,
            tvLayoutLetterSpacingLabel,
            tvLayoutLineSpacingLabel,
            tvLayoutParagraphSpacingLabel,
            tvLayoutPaddingTopLabel,
            tvLayoutPaddingBottomLabel,
            tvLayoutPaddingLeftLabel,
            tvLayoutPaddingRightLabel,
            tvLayoutHeaderSpacingLabel,
            tvLayoutFooterSpacingLabel,
            tvLayoutLetterSpacingValue,
            tvLayoutLineSpacingValue,
            tvLayoutParagraphSpacingValue,
            tvLayoutPaddingTopValue,
            tvLayoutPaddingBottomValue,
            tvLayoutPaddingLeftValue,
            tvLayoutPaddingRightValue,
            tvLayoutHeaderSpacingValue,
            tvLayoutFooterSpacingValue
        ).forEach { it.setTextColor(color) }
        listOf(
            ivLayoutPaddingTop,
            ivLayoutPaddingBottom,
            ivLayoutPaddingLeft,
            ivLayoutPaddingRight,
            ivLayoutHeaderSpacing,
            ivLayoutFooterSpacing
        ).forEach { it.setColorFilter(color, PorterDuff.Mode.SRC_IN) }
    }

    private fun tintThemePanel(color: Int) = binding.run {
        listOf(
            tvThemeBrightnessLabel,
            tvThemeContrastLabel,
            tvThemeTextureLabel,
            tvThemeFontLabel,
            tvThemeFontWeightLabel,
            tvThemeTextSizeLabel,
            tvThemeFontWeightBig,
            tvThemeTextSizeSmall,
            tvThemeTextSizeBig,
            tvThemeBrightnessValue,
            tvThemeContrastValue,
            tvThemeFontWeightValue,
            tvThemeTextSizeValue
        ).forEach { it.setTextColor(color) }
        listOf(
            ivThemeBrightnessLow,
            ivThemeBrightnessHigh,
            ivThemeContrastLow,
            ivThemeContrastHigh,
            ivThemeFontWeightLow
        ).forEach { it.setColorFilter(color, PorterDuff.Mode.SRC_IN) }
        themeTonePanel.background = roundedRect(
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

    private fun renderLayoutTabs() = binding.run {
        panelLayoutFont.gone(activeLayoutTab != LayoutTab.Font)
        panelLayoutSpacing.gone(activeLayoutTab != LayoutTab.Spacing)
        panelLayoutStyle.gone(activeLayoutTab != LayoutTab.Style)
        configureLayoutTopTab(
            tvLayoutTabFont,
            vwLayoutTabFontIndicator,
            activeLayoutTab == LayoutTab.Font
        )
        configureLayoutTopTab(
            tvLayoutTabSpacing,
            vwLayoutTabSpacingIndicator,
            activeLayoutTab == LayoutTab.Spacing
        )
        configureLayoutTopTab(
            tvLayoutTabStyle,
            vwLayoutTabStyleIndicator,
            activeLayoutTab == LayoutTab.Style
        )
    }

    private fun configureLayoutTopTab(tab: TextView, indicator: android.view.View, selected: Boolean) {
        tab.setTextColor(if (selected) context.accentColor else textColor)
        indicator.background = roundedRect(context.accentColor, 1.5f.dpToPx())
        indicator.isVisible = selected
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

    private fun showLayoutTab(tab: LayoutTab) = binding.run {
        activeLayoutTab = tab
        renderLayoutTabs()
        panelLayoutScroll.post {
            panelLayoutScroll.smoothScrollTo(0, 0)
            if (activeBottomTab == BottomTab.Font || activeBottomTab == BottomTab.Layout) {
                updateExpandedPanelHeight(BottomTab.Layout)
            }
        }
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

        val headerProgress = maxOf(ReadBookConfig.headerPaddingTop, ReadBookConfig.headerPaddingBottom)
            .coerceIn(0, seekLayoutHeaderSpacing.max)
        seekLayoutHeaderSpacing.progress = headerProgress
        updateLayoutIntValue(tvLayoutHeaderSpacingValue, headerProgress)

        val footerProgress = maxOf(ReadBookConfig.footerPaddingTop, ReadBookConfig.footerPaddingBottom)
            .coerceIn(0, seekLayoutFooterSpacing.max)
        seekLayoutFooterSpacing.progress = footerProgress
        updateLayoutIntValue(tvLayoutFooterSpacingValue, footerProgress)
    }

    private fun updateLayoutDecimalValue(view: TextView, progress: Int) {
        view.text = String.format(Locale.US, "%.1f", progress / 10f)
    }

    private fun updateLayoutIntValue(view: TextView, progress: Int) {
        view.text = progress.toString()
    }

    private fun updateThemeControlsFromConfig() = binding.run {
        val brightnessProgress = (AppConfig.readBrightness / 255f * 100f)
            .roundToInt()
            .coerceIn(0, 100)
        seekThemeBrightness.progress = brightnessProgress
        updateThemeBrightnessValue(brightnessProgress)
        seekThemeBrightness.isEnabled = !brightnessAuto()

        seekThemeContrast.progress = 50
        updateThemeContrastValue(50)

        seekThemeFontWeight.progress = when (ReadBookConfig.textBold) {
            1 -> 80
            2 -> 20
            else -> 50
        }
        updateThemeFontWeightValue(seekThemeFontWeight.progress)

        seekThemeTextSize.progress = (ReadBookConfig.textSize - 5)
            .coerceIn(0, seekThemeTextSize.max)
        updateThemeTextSizeValue(seekThemeTextSize.progress)
        updateTextureButtons()
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

    private fun applyTextContrast(progress: Int) {
        val textColor = ReadMenuThemePreset.contrastTextColor(
            baseTextColor = ReadBookConfig.durConfig.curTextColor(),
            backgroundColor = currentPageBackgroundColor(),
            progress = progress
        )
        ReadBookConfig.durConfig.setCurTextColor(textColor)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        if (AppConfig.readBarStyleFollowPage) {
            postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
        }
        upColorConfig()
        initView(true)
        binding.seekThemeContrast.progress = progress
        updateThemeContrastValue(progress)
    }

    private fun currentPageBackgroundColor(): Int {
        return if (ReadBookConfig.durConfig.curBgType() == 0) {
            kotlin.runCatching {
                Color.parseColor(ReadBookConfig.durConfig.curBgStr())
            }.getOrDefault(bgColor)
        } else {
            bgColor
        }
    }

    private fun updateThemeBrightnessValue(progress: Int) {
        binding.tvThemeBrightnessValue.text = "${progress.coerceIn(0, 100)}%"
    }

    private fun updateThemeContrastValue(progress: Int) {
        binding.tvThemeContrastValue.text = "${progress.coerceIn(0, 100)}%"
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

    private fun themeBrightnessToConfig(progress: Int): Int {
        return (progress.coerceIn(0, 100) / 100f * 255f).roundToInt().coerceIn(1, 255)
    }

    private fun setTextureStrength(alpha: Int) {
        ReadBookConfig.bgAlpha = alpha.coerceIn(0, 100)
        ReadBookConfig.save()
        updateTextureButtons()
        postEvent(EventBus.UP_CONFIG, arrayListOf(3))
    }

    private fun updateTextureButtons() = binding.run {
        val alpha = ReadBookConfig.bgAlpha
        configureOptionButton(textureNone, alpha <= 17)
        configureOptionButton(textureWeak, alpha in 18..50)
        configureOptionButton(textureMedium, alpha in 51..82)
        configureOptionButton(textureStrong, alpha >= 83)
    }

    private fun setSystemFont(systemTypeface: Int) {
        ReadBookConfig.textFont = ""
        AppConfig.systemTypefaces = systemTypeface
        ReadBookConfig.save()
        updateFontButtons()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 5))
    }

    private fun updateFontButtons() = binding.run {
        val hasCustomFont = ReadBookConfig.textFont.isNotBlank()
        configureOptionButton(fontOptionSource, !hasCustomFont && AppConfig.systemTypefaces == 0)
        configureOptionButton(fontOptionSans, !hasCustomFont && AppConfig.systemTypefaces == 1)
        configureOptionButton(fontOptionArt, !hasCustomFont && AppConfig.systemTypefaces == 2)
        configureOptionButton(fontOptionCustom, hasCustomFont)
    }

    private fun setFontWeight(progress: Int) = binding.run {
        ReadBookConfig.textBold = when {
            progress >= 67 -> 1
            progress <= 33 -> 2
            else -> 0
        }
        val snapped = when (ReadBookConfig.textBold) {
            1 -> 80
            2 -> 20
            else -> 50
        }
        seekThemeFontWeight.progress = snapped
        updateThemeFontWeightValue(snapped)
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

    private fun setLayoutHeaderSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutHeaderSpacing.max)
        ReadBookConfig.headerPaddingTop = value
        ReadBookConfig.headerPaddingBottom = value
        updateLayoutIntValue(tvLayoutHeaderSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
    }

    private fun setLayoutFooterSpacing(progress: Int) = binding.run {
        val value = progress.coerceIn(0, seekLayoutFooterSpacing.max)
        ReadBookConfig.footerPaddingTop = value
        ReadBookConfig.footerPaddingBottom = value
        updateLayoutIntValue(tvLayoutFooterSpacingValue, value)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
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
                    28f.dpToPx(),
                    1.dpToPx(),
                    ColorUtils.adjustAlpha(textColor, 0.18f)
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

        setupBottomTabFrostedGlassView(
            liquidGlassView = bottomTabGlassView,
            target = target,
            cornerRadius = 28f.dpToPx(),
            refractionHeight = (12f + glassLevel * 8f).dpToPx(),
            refractionOffset = (34f + glassLevel * 18f).dpToPx(),
            blurRadius = (22f + glassLevel * 30f).dpToPx(),
            dispersion = (0.18f + glassLevel * 0.16f).coerceAtMost(0.42f),
            tintAlpha = (0.12f + glassLevel * 0.18f).coerceAtMost(0.30f)
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
        tintAlpha: Float
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
        liquidGlassView.setTintColorRed(0.70f)
        liquidGlassView.setTintColorGreen(0.79f)
        liquidGlassView.setTintColorBlue(0.86f)
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
            ColorUtils.blendColors(Color.BLACK, context.primaryColor, 0.12f)
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val topAlpha = if (isDark) {
            0.20f + glassLevel * 0.16f
        } else {
            0.28f + glassLevel * 0.16f
        }
        val centerAlpha = if (isDark) {
            0.16f + glassLevel * 0.12f
        } else {
            0.20f + glassLevel * 0.12f
        }
        val bottomAlpha = if (isDark) {
            0.12f + glassLevel * 0.10f
        } else {
            0.14f + glassLevel * 0.10f
        }
        val strokeColor = ColorUtils.withAlpha(
            if (isDark) Color.WHITE else Color.BLACK,
            if (isDark) 0.18f else 0.08f
        )
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                ColorUtils.withAlpha(surfaceColor, topAlpha.coerceAtMost(0.44f)),
                ColorUtils.withAlpha(surfaceColor, centerAlpha.coerceAtMost(0.34f)),
                ColorUtils.withAlpha(surfaceColor, bottomAlpha.coerceAtMost(0.26f))
            )
        ).apply {
            cornerRadius = 28f.dpToPx()
            setStroke(1.dpToPx(), strokeColor)
        }
    }

    private fun bottomTabGlassFallbackShell(glassLevel: Float): GradientDrawable {
        val isDark = bottomTabUseDarkGlass()
        val surfaceColor = if (isDark) {
            ColorUtils.blendColors(Color.BLACK, context.primaryColor, 0.12f)
        } else {
            ColorUtils.blendColors(Color.WHITE, context.accentColor, 0.05f)
        }
        val alpha = if (isDark) {
            0.20f + glassLevel * 0.16f
        } else {
            0.26f + glassLevel * 0.18f
        }
        return GradientDrawable().apply {
            cornerRadius = 28f.dpToPx()
            setColor(ColorUtils.withAlpha(surfaceColor, alpha.coerceAtMost(0.46f)))
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
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onStop(seekBar.progress)
            }
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
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onStop(seekBar.progress)
            }
        })
    }

    private fun setupBottomNavigationEvents() = binding.run {
        readBottomPrimaryNav.setOnItemSelectedListener { item ->
            if (suppressBottomNavSelection) {
                return@setOnItemSelectedListener true
            }
            flashBottomTabIndicator(readBottomPrimaryNav, item.itemId)
            when (item.itemId) {
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
            val tab = item.itemId.toBottomTab() ?: return@setOnItemSelectedListener false
            toggleBottomTab(tab)
        }
        readBottomInterfaceNav.setOnItemReselectedListener { item ->
            if (suppressBottomNavSelection) {
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
        readBottomInterfaceBack.setOnClickListener {
            hideExpandedPanel(returnToPrimary = true)
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
        //阅读进度
        seekReadPage.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.vwMenuBg.setOnClickListener { handleBackgroundDismiss() }
                when (AppConfig.progressBarBehavior) {
                    "page" -> ReadBook.skipToPage(seekBar.progress)
                    "chapter" -> {
                        if (confirmSkipToChapter) {
                            callBack.skipToChapter(seekBar.progress)
                        } else {
                            context.alert("章节跳转确认", "确定要跳转章节吗？") {
                                yesButton {
                                    confirmSkipToChapter = true
                                    callBack.skipToChapter(seekBar.progress)
                                }
                                noButton {
                                    upSeekBar()
                                }
                                onCancelled {
                                    upSeekBar()
                                }
                            }
                        }
                    }
                }
            }

        })

        //搜索

        //自动翻页

        //替换

        //夜间模式

        //上一章
        tvPre.setOnClickListener { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.setOnClickListener { ReadBook.moveToNextChapter(true) }

        //目录
        setupBottomNavigationEvents()

        //朗读
        layoutTabFont.setOnClickListener { showLayoutTab(LayoutTab.Font) }
        layoutTabSpacing.setOnClickListener { showLayoutTab(LayoutTab.Spacing) }
        layoutTabStyle.setOnClickListener { showLayoutTab(LayoutTab.Style) }
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
        panelMoreSearch.setOnClickListener {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
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
            panelTheme.post { panelTheme.smoothScrollTo(0, themeTonePanel.top) }
        }
        llThemeTabEye.setOnClickListener {
            activeThemeTab = ThemeTab.Eye
            renderThemeTabs()
            themePresets.firstOrNull { it.key == "eye_green" }?.let(::applyThemePreset)
        }
        seekThemeBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeBrightnessValue(progress)
                if (fromUser) {
                    setScreenBrightness(themeBrightnessToConfig(progress).toFloat())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val brightness = themeBrightnessToConfig(seekBar.progress)
                AppConfig.readBrightness = brightness
                seekBrightness.progress = brightness
                setScreenBrightness(brightness.toFloat())
            }
        })
        seekThemeContrast.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeContrastValue(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                applyTextContrast(seekBar.progress)
            }
        })
        textureNone.setOnClickListener { setTextureStrength(0) }
        textureWeak.setOnClickListener { setTextureStrength(35) }
        textureMedium.setOnClickListener { setTextureStrength(65) }
        textureStrong.setOnClickListener { setTextureStrength(100) }
        fontOptionSource.setOnClickListener { setSystemFont(0) }
        fontOptionSans.setOnClickListener { setSystemFont(1) }
        fontOptionArt.setOnClickListener { setSystemFont(2) }
        fontOptionCustom.setOnClickListener { openFontSelectDialog() }
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
        bindLayoutIntSeek(
            seekLayoutHeaderSpacing,
            tvLayoutHeaderSpacingValue,
            ::setLayoutHeaderSpacing
        )
        bindLayoutIntSeek(
            seekLayoutFooterSpacing,
            tvLayoutFooterSpacingValue,
            ::setLayoutFooterSpacing
        )
        seekThemeFontWeight.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeFontWeightValue(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setFontWeight(seekBar.progress)
            }
        })
        seekThemeTextSize.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateThemeTextSizeValue(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setTextSize(seekBar.progress)
            }
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
            binding.tvPre.isEnabled = ReadBook.durChapterIndex != 0
            binding.tvNext.isEnabled = ReadBook.durChapterIndex != ReadBook.simulatedChapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
            binding.tvChapterUrl.gone()
        }
    }

    fun upSeekBar() {
        binding.seekReadPage.apply {
            when (AppConfig.progressBarBehavior) {
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
        binding.seekReadPage.progress = seek
    }

    fun setAutoPage(autoPage: Boolean) = binding.run {
        if (autoPage) {
            panelMoreAutoPage.text = context.getString(R.string.auto_next_page_stop)
            panelMoreAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            panelMoreAutoPage.text = context.getString(R.string.auto_next_page)
            panelMoreAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        panelMoreAutoPage.setTextColor(textColor)
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
            val view = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    46.dpToPx()
                )
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
                setPadding(4.dpToPx(), 0, 4.dpToPx(), 0)
                textSize = 15f
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val chapter = chapters[position]
            val context = holder.title.context
            val selected = chapter.index == ReadBook.durChapterIndex
            holder.title.text = "${chapter.index + 1}. ${chapter.title}"
            holder.title.setTypeface(null, if (chapter.isVolume) Typeface.BOLD else Typeface.NORMAL)
            holder.title.setTextColor(
                when {
                    selected -> context.accentColor
                    AppConfig.isNightTheme -> ColorUtils.adjustAlpha(Color.WHITE, 0.86f)
                    else -> ColorUtils.adjustAlpha(Color.BLACK, 0.82f)
                }
            )
            holder.title.setOnClickListener {
                onChapterClick(chapter)
            }
        }

        override fun getItemCount(): Int = chapters.size

        class Holder(val title: TextView) : RecyclerView.ViewHolder(title)
    }

}
