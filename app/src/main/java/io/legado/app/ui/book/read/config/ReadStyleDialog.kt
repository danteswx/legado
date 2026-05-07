package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogReadBookStyleBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.font.FontSelectDialog
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import io.legado.app.utils.postEvent
import io.legado.app.utils.progressAdd
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File
import java.net.URLDecoder

class ReadStyleDialog : BaseDialogFragment(R.layout.dialog_read_book_style),
    FontSelectDialog.CallBack {

    private val binding by viewBinding(DialogReadBookStyleBinding::bind)
    private val callBack get() = activity as? ReadBookActivity
    private val fontOptions = arrayListOf<FontOption>()

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        (activity as ReadBookActivity).bottomDialog++
        initView()
        initData()
        initViewEvent()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
        (activity as? ReadBookActivity)?.let {
            it.bottomDialog--
        }
    }

    private fun initView() = binding.run {
        val bg = requireContext().bottomBackground
        val textColor = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bg))
        val mutedTextColor = ColorUtils.adjustAlpha(textColor, 0.72f)
        val accent = requireContext().accentColor
        val dividerColor = ColorUtils.adjustAlpha(textColor, 0.13f)

        rootView.background = roundedSheet(bg)
        vwDragHandle.background = roundedRect(ColorUtils.adjustAlpha(textColor, 0.22f), 2.5f.dpToPx())
        vwDividerWeight.setBackgroundColor(dividerColor)
        vwDividerSize.setBackgroundColor(dividerColor)
        listOf(vwTabDivider1, vwTabDivider2, vwTabDivider3, vwTabDivider4).forEach {
            it.setBackgroundColor(dividerColor)
        }
        bottomTabBar.background = roundedRect(
            ColorUtils.blendColors(bg, Color.WHITE, if (ColorUtils.isColorLight(bg)) 0.55f else 0.08f),
            20f.dpToPx()
        )
        bottomTabBar.elevation = 8f.dpToPx()

        listOf(
            tvFontTitle,
            tvFontWeightTitle,
            tvTextSizeTitle,
            tvWeightASmall,
            tvWeightABig,
            tvTabLayout,
            tvTabTheme,
            tvTabPage,
            tvTabMore
        ).forEach { it.setTextColor(textColor) }
        listOf(ivTextSizeReduce, ivTextSizePlus, ivTabLayout, ivTabTheme, ivTabPage, ivTabMore)
            .forEach { it.setColorFilter(textColor) }
        listOf(tvFontWeightValue, tvTextSizeValue, tvFontSourceSub).forEach {
            it.setTextColor(accent)
        }

        tabFont.background = roundedRect(accent, 10f.dpToPx())
        tvTabFontIcon.setTextColor(Color.WHITE)
        tvTabFont.setTextColor(Color.WHITE)
        listOf(tabLayout, tabTheme, tabPage, tabMore).forEach {
            it.background = roundedRect(Color.TRANSPARENT, 10f.dpToPx())
        }

        fontOptions.clear()
        fontOptions.add(
            FontOption(
                root = fontOptionSource,
                label = tvFontSource,
                subtitle = tvFontSourceSub,
                check = ivFontSourceCheck,
                systemTypeface = 0,
                typeface = Typeface.SERIF
            )
        )
        fontOptions.add(
            FontOption(
                root = fontOptionSans,
                label = tvFontSans,
                check = ivFontSansCheck,
                systemTypeface = 1,
                typeface = Typeface.SANS_SERIF
            )
        )
        fontOptions.add(
            FontOption(
                root = fontOptionArt,
                label = tvFontArt,
                check = ivFontArtCheck,
                systemTypeface = 2,
                typeface = Typeface.MONOSPACE
            )
        )
        fontOptions.add(
            FontOption(
                root = fontOptionKai,
                label = tvFontKai,
                check = ivFontKaiCheck,
                typeface = Typeface.SERIF,
                opensFontPicker = true
            )
        )
        fontOptionCustom.isVisible = false
        fontOptions.forEach { option ->
            option.label.typeface = option.typeface
            option.check.background = oval(accent)
        }
        updateFontCards()
        updateSeekValues()
        updateTextColors(textColor, mutedTextColor)
    }

    private fun initData() = binding.run {
        seekFontWeight.progress = when (ReadBookConfig.textBold) {
            1 -> 80
            2 -> 20
            else -> 50
        }
        seekTextSize.progress = (ReadBookConfig.textSize - 5).coerceIn(0, seekTextSize.max)
        updateSeekValues()
    }

    private fun initViewEvent() = binding.run {
        fontOptions.forEach { option ->
            option.root.setOnClickListener {
                if (option.opensFontPicker) {
                    showDialogFragment<FontSelectDialog>()
                } else {
                    option.systemTypeface?.let { setSystemFont(it) }
                }
            }
        }

        seekFontWeight.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateFontWeightValue(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setFontWeight(seekBar.progress)
            }
        })
        seekTextSize.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateTextSizeValue(progress)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                setTextSize(seekBar.progress)
            }
        })
        ivTextSizeReduce.setOnClickListener {
            seekTextSize.progressAdd(-1)
            setTextSize(seekTextSize.progress)
        }
        ivTextSizePlus.setOnClickListener {
            seekTextSize.progressAdd(1)
            setTextSize(seekTextSize.progress)
        }

        tabLayout.setOnClickListener {
            dismissAllowingStateLoss()
            callBack?.showPaddingConfig()
        }
        tabTheme.setOnClickListener {
            dismissAllowingStateLoss()
            callBack?.showBgTextConfig()
        }
        tabPage.setOnClickListener {
            showPageAnimSelector()
        }
        tabMore.setOnClickListener {
            dismissAllowingStateLoss()
            callBack?.showMoreSetting()
        }
    }

    private fun updateTextColors(textColor: Int, mutedTextColor: Int) = binding.run {
        listOf(tvFontSans, tvFontArt, tvFontKai, tvFontCustom).forEach {
            it.setTextColor(textColor)
        }
        tvFontSource.setTextColor(textColor)
        listOf<AppCompatImageView>(ivTextSizeReduce, ivTextSizePlus).forEach {
            it.setColorFilter(mutedTextColor)
        }
    }

    private fun setSystemFont(systemTypeface: Int) {
        ReadBookConfig.textFont = ""
        AppConfig.systemTypefaces = systemTypeface
        updateFontCards()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 5))
    }

    private fun setFontWeight(progress: Int) {
        ReadBookConfig.textBold = when {
            progress >= 67 -> 1
            progress <= 33 -> 2
            else -> 0
        }
        binding.seekFontWeight.progress = when (ReadBookConfig.textBold) {
            1 -> 80
            2 -> 20
            else -> 50
        }
        updateFontWeightValue(binding.seekFontWeight.progress)
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 9, 6))
    }

    private fun setTextSize(progress: Int) {
        ReadBookConfig.textSize = progress + 5
        updateTextSizeValue(progress)
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    private fun updateSeekValues() = binding.run {
        updateFontWeightValue(seekFontWeight.progress)
        updateTextSizeValue(seekTextSize.progress)
    }

    private fun updateFontWeightValue(progress: Int) {
        val resId = when {
            progress >= 67 -> R.string.read_style_weight_bold
            progress <= 33 -> R.string.read_style_weight_light
            else -> R.string.read_style_weight_standard
        }
        binding.tvFontWeightValue.text = getString(resId, progress)
    }

    private fun updateTextSizeValue(progress: Int) {
        binding.tvTextSizeValue.text = (progress + 5).toString()
    }

    private fun updateFontCards() {
        val bg = requireContext().bottomBackground
        val textColor = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bg))
        val accent = requireContext().accentColor
        val selectedBg = ColorUtils.blendColors(bg, accent, 0.08f)
        val normalBorder = ColorUtils.adjustAlpha(textColor, 0.16f)
        val hasCustomFont = ReadBookConfig.textFont.isNotBlank()
        fontOptions.forEach { option ->
            val selected = if (option.opensFontPicker) {
                hasCustomFont
            } else {
                !hasCustomFont && AppConfig.systemTypefaces == option.systemTypeface
            }
            option.root.background = roundedRect(
                if (selected) selectedBg else Color.TRANSPARENT,
                9f.dpToPx(),
                1.dpToPx(),
                if (selected) accent else normalBorder
            )
            option.check.isVisible = selected
            option.label.setTextColor(textColor)
        }
        binding.tvFontSourceSub.isVisible = !hasCustomFont && AppConfig.systemTypefaces == 0
        binding.tvFontKai.text = if (hasCustomFont) {
            customFontName()
        } else {
            getString(R.string.read_style_font_kai)
        }
    }

    private fun showPageAnimSelector() {
        val items = listOf(
            getString(R.string.page_anim_cover),
            getString(R.string.page_anim_slide),
            getString(R.string.page_anim_simulation),
            getString(R.string.page_anim_scroll),
            getString(R.string.page_anim_none)
        )
        context?.selector(title = getString(R.string.page_anim), items = items) { _, index ->
            ReadBook.book?.setPageAnim(-1)
            ReadBookConfig.pageAnim = index
            callBack?.upPageAnim()
            ReadBook.loadContent(false)
        }
    }

    override val curFontPath: String
        get() = ReadBookConfig.textFont

    override fun selectFont(path: String) {
        if (path != ReadBookConfig.textFont || path.isEmpty()) {
            ReadBookConfig.textFont = path
            postEvent(EventBus.UP_CONFIG, arrayListOf(2, 5))
        }
        updateFontCards()
    }

    private fun customFontName(): String {
        return kotlin.runCatching {
            val decoded = URLDecoder.decode(ReadBookConfig.textFont, "utf-8")
            decoded.substringAfterLast(File.separator)
                .substringAfterLast('/')
                .substringBeforeLast('.')
                .ifBlank { getString(R.string.read_style_font_custom) }
        }.getOrDefault(getString(R.string.read_style_font_custom))
    }

    private fun roundedSheet(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadii = floatArrayOf(
                24f.dpToPx(), 24f.dpToPx(),
                24f.dpToPx(), 24f.dpToPx(),
                0f, 0f,
                0f, 0f
            )
        }
    }

    private fun roundedRect(
        fillColor: Int,
        radius: Float,
        strokeWidth: Int = 0,
        strokeColor: Int = Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = radius
            if (strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    private fun oval(fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
        }
    }

    private data class FontOption(
        val root: View,
        val label: TextView,
        val check: ImageView,
        val subtitle: TextView? = null,
        val systemTypeface: Int? = null,
        val typeface: Typeface,
        val opensFontPicker: Boolean = false
    )
}
