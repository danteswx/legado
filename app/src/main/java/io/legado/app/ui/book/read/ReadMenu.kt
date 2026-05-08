package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import io.legado.app.R
import io.legado.app.constant.EventBus
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
        Layout,
        Theme,
        Page,
        More
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
            activeBottomTab = null
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
        } else {
            flExpandedPanel.backgroundTintList = bottomBackgroundList
        }
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

    private fun toggleBottomTab(tab: BottomTab) {
        if (activeBottomTab == tab && binding.flExpandedPanel.isVisible) {
            hideExpandedPanel()
            return
        }
        showBottomPanel(tab)
    }

    private fun showBottomPanel(tab: BottomTab) = binding.run {
        val wasVisible = flExpandedPanel.isVisible
        activeBottomTab = tab
        panelLayout.gone(tab != BottomTab.Layout)
        panelTheme.gone(tab != BottomTab.Theme)
        panelPage.gone(tab != BottomTab.Page)
        panelMore.gone(tab != BottomTab.More)
        updateExpandedPanelHeight(tab)
        renderBottomTabState()
        binding.flExpandedPanel.visible()
        binding.flExpandedPanel.post {
            if (activeBottomTab == tab) {
                updateExpandedPanelHeight(tab)
            }
        }
        if (!wasVisible && !AppConfig.isEInkMode) {
            binding.flExpandedPanel.startAnimation(panelIn)
        }
    }

    private fun hideExpandedPanel(anim: Boolean = !AppConfig.isEInkMode) {
        if (!binding.flExpandedPanel.isVisible) {
            activeBottomTab = null
            renderBottomTabState()
            return
        }
        activeBottomTab = null
        renderBottomTabState()
        if (anim) {
            panelOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) = Unit
                override fun onAnimationRepeat(animation: Animation) = Unit
                override fun onAnimationEnd(animation: Animation) {
                    binding.flExpandedPanel.gone()
                    panelOut.setAnimationListener(null)
                }
            })
            binding.flExpandedPanel.startAnimation(panelOut)
        } else {
            binding.flExpandedPanel.gone()
        }
    }

    private fun handleBackgroundDismiss() {
        when {
            binding.flExpandedPanel.isVisible -> hideExpandedPanel()
            else -> runMenuOut()
        }
    }

    private fun renderBottomTabState() = binding.run {
        bottomTabBar.setBackgroundResource(
            if (activeBottomTab == null) {
                R.drawable.bg_read_menu_island
            } else {
                R.drawable.bg_read_menu_island_connected
            }
        )
        if (!AppConfig.isEInkMode) {
            bottomTabBar.backgroundTintList = bottomBackgroundList
            flExpandedPanel.backgroundTintList = bottomBackgroundList
        }
        bottomTabBar.elevation = 8f.dpToPx()
        flExpandedPanel.elevation = 10f.dpToPx()
        ivTabBack.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)
        llTabBack.background = null
        configureBottomTab(llTabLayout, ivTabLayout, tvTabLayout, BottomTab.Layout)
        configureThemeBottomTab()
        configureBottomTab(llTabPage, ivTabPage, tvTabPage, BottomTab.Page)
        configureBottomTab(llTabMore, ivTabMore, tvTabMore, BottomTab.More)
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
                val measuredHeight = panelLayout.height
                if (measuredHeight <= 0) {
                    return@run
                }
                if (measuredHeight > maxHeight) {
                    panelLayout.updateLayoutParams<FrameLayout.LayoutParams> {
                        height = maxHeight
                    }
                    val fixedChromeHeight = llLayoutTabs.height +
                            vwLayoutTabsDivider.height +
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
                val measuredHeight = panelTheme.height
                if (measuredHeight <= 0) {
                    return@run
                }
                panelTheme.updateLayoutParams<FrameLayout.LayoutParams> {
                    height = if (measuredHeight > maxHeight) {
                        maxHeight
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            }

            BottomTab.Page,
            BottomTab.More -> Unit
        }
    }

    private fun expandedPanelMaxHeight(): Int {
        val rootHeight = binding.vwMenuRoot.height
            .takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels
        val bottomBarHeight = binding.bottomTabBar.height
            .takeIf { it > 0 }
            ?: 78.dpToPx()
        val topGap = 16.dpToPx()
        val bottomPadding = binding.bottomMenu.paddingBottom
        val screenCap = (rootHeight - bottomBarHeight - bottomPadding - topGap)
            .coerceAtLeast(96.dpToPx())
        val minHeight = 180.dpToPx().coerceAtMost(screenCap)
        val belowTitle = rootHeight - binding.titleBar.bottom - topGap - bottomBarHeight - bottomPadding
        return belowTitle.coerceIn(minHeight, screenCap)
    }

    private fun configureBottomTab(
        root: LinearLayout,
        icon: AppCompatImageView,
        label: TextView,
        tab: BottomTab
    ) {
        val selected = activeBottomTab == tab
        root.setBackgroundResource(if (selected) R.drawable.bg_read_menu_tab_selected else 0)
        val color = if (selected) Color.WHITE else textColor
        icon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        label.setTextColor(color)
    }

    private fun configureThemeBottomTab() = binding.run {
        val selected = activeBottomTab == BottomTab.Theme
        themeTabPill.setBackgroundResource(if (selected) R.drawable.bg_read_menu_tab_selected else 0)
        val color = if (selected) Color.WHITE else textColor
        ivTabTheme.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        tvTabTheme.setTextColor(color)
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
            if (activeBottomTab == BottomTab.Layout) {
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
        llTabBack.setOnClickListener { runMenuOut() }

        //朗读
        llTabLayout.setOnClickListener { toggleBottomTab(BottomTab.Layout) }
        llTabTheme.setOnClickListener { toggleBottomTab(BottomTab.Theme) }
        llTabPage.setOnClickListener { toggleBottomTab(BottomTab.Page) }
        llTabMore.setOnClickListener { toggleBottomTab(BottomTab.More) }
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

}
