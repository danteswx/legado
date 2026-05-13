package io.legado.app.ui.book.read

import android.content.Context
import android.graphics.Color
import io.legado.app.R
import io.legado.app.constant.PageAnim
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import kotlin.math.abs

data class ReadMenuThemeSuite(
    val name: String,
    val backgroundColor: Int,
    val textColor: Int,
    val bgType: Int,
    val bgValue: String,
    val textSize: Int,
    val textWeight: Int,
    val letterSpacing: Float,
    val lineSpacingExtra: Int,
    val paragraphSpacing: Int,
    val pageAnim: Int,
    val pageAnimationSpeed: Int,
    val bgBrightness: Int,
    val bgSaturation: Int,
    val bgAlpha: Int,
    val paperInkStrength: Int,
    val createdAt: Long = System.currentTimeMillis()
) {

    fun matchesCurrentTheme(): Boolean {
        return matches(captureCurrent(name))
    }

    fun matches(other: ReadMenuThemeSuite): Boolean {
        return textColor == other.textColor &&
                bgType == other.bgType &&
                bgValue.equals(other.bgValue, ignoreCase = true) &&
                textSize == other.textSize &&
                textWeight == other.textWeight &&
                abs(letterSpacing - other.letterSpacing) < 0.01f &&
                lineSpacingExtra == other.lineSpacingExtra &&
                paragraphSpacing == other.paragraphSpacing &&
                pageAnim == other.pageAnim &&
                pageAnimationSpeed == other.pageAnimationSpeed &&
                bgBrightness == other.bgBrightness &&
                bgSaturation == other.bgSaturation &&
                bgAlpha == other.bgAlpha &&
                paperInkStrength == other.paperInkStrength
    }

    fun summaryText(context: Context): String {
        return context.getString(
            R.string.read_menu_theme_suite_summary,
            context.getString(R.string.read_menu_theme_layout_custom, textSize),
            context.getString(pageTurnLabelRes()),
            backgroundLabel(context)
        )
    }

    fun applyToReader() {
        ReadBookConfig.durConfig.setCurBg(bgType, bgValue)
        ReadBookConfig.durConfig.setCurTextColor(textColor)
        ReadBookConfig.textSize = textSize
        ReadBookConfig.textWeight = textWeight
        ReadBookConfig.letterSpacing = letterSpacing
        ReadBookConfig.lineSpacingExtra = lineSpacingExtra
        ReadBookConfig.paragraphSpacing = paragraphSpacing
        ReadBookConfig.pageAnim = pageAnim
        ReadBook.book?.setPageAnim(null)
        ReadBook.saveRead()
        AppConfig.pageAnimationSpeed = pageAnimationSpeed
        ReadBookConfig.bgBrightness = bgBrightness
        ReadBookConfig.bgSaturation = bgSaturation
        ReadBookConfig.bgAlpha = bgAlpha
        ReadBookConfig.paperInkStrength = paperInkStrength
    }

    private fun pageTurnLabelRes(): Int {
        return when (pageAnim) {
            PageAnim.linkedCoverPageAnim -> R.string.page_anim_linked_cover
            PageAnim.slidePageAnim -> R.string.page_anim_slide
            PageAnim.simulationPageAnim -> R.string.page_anim_simulation
            PageAnim.scrollPageAnim -> R.string.page_anim_scroll
            PageAnim.noAnim -> R.string.page_anim_none
            else -> R.string.page_anim_cover
        }
    }

    private fun backgroundLabel(context: Context): String {
        return when (bgType) {
            0 -> context.getString(R.string.read_menu_theme_bg_color)
            1 -> bgValue.substringBeforeLast(".")
            else -> context.getString(R.string.read_menu_theme_bg_custom)
        }
    }

    companion object {
        fun captureCurrent(name: String): ReadMenuThemeSuite {
            val bgType = ReadBookConfig.durConfig.curBgType()
            val bgValue = ReadBookConfig.durConfig.curBgStr()
            val backgroundColor = if (bgType == 0) {
                runCatching { Color.parseColor(bgValue) }
                    .getOrDefault(ReadBookConfig.bgMeanColor)
            } else {
                ReadBookConfig.bgMeanColor.takeIf { it != 0 } ?: Color.TRANSPARENT
            }
            return ReadMenuThemeSuite(
                name = name,
                backgroundColor = backgroundColor,
                textColor = ReadBookConfig.durConfig.curTextColor(),
                bgType = bgType,
                bgValue = bgValue,
                textSize = ReadBookConfig.textSize,
                textWeight = ReadBookConfig.textWeight,
                letterSpacing = ReadBookConfig.letterSpacing,
                lineSpacingExtra = ReadBookConfig.lineSpacingExtra,
                paragraphSpacing = ReadBookConfig.paragraphSpacing,
                pageAnim = ReadBookConfig.pageAnim,
                pageAnimationSpeed = AppConfig.pageAnimationSpeed,
                bgBrightness = ReadBookConfig.bgBrightness,
                bgSaturation = ReadBookConfig.bgSaturation,
                bgAlpha = ReadBookConfig.bgAlpha,
                paperInkStrength = ReadBookConfig.paperInkStrength
            )
        }
    }
}

object ReadMenuThemeSuiteStore {
    private const val PREF_KEY = "readMenuThemeSuites"
    private const val ACTIVE_PREF_KEY = "readMenuThemeActiveSuiteCreatedAt"
    private const val MAX_SUITES = 12

    fun load(context: Context): List<ReadMenuThemeSuite> {
        val raw = context.getPrefString(PREF_KEY).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        return GSON.fromJsonArray<ReadMenuThemeSuite>(raw)
            .getOrElse { emptyList() }
    }

    fun save(context: Context, suite: ReadMenuThemeSuite) {
        val suites = buildList {
            add(suite)
            addAll(load(context).filterNot { it.name == suite.name })
        }.take(MAX_SUITES)
        context.putPrefString(PREF_KEY, GSON.toJson(suites))
    }

    fun matchingSuite(context: Context, suite: ReadMenuThemeSuite): ReadMenuThemeSuite? {
        return load(context).firstOrNull { it.matches(suite) }
    }

    fun saveOrSelectExisting(context: Context, suite: ReadMenuThemeSuite): ReadMenuThemeSuite {
        val existing = matchingSuite(context, suite)
        return if (existing != null) {
            select(context, existing)
            existing
        } else {
            save(context, suite)
            select(context, suite)
            suite
        }
    }

    fun select(context: Context, suite: ReadMenuThemeSuite) {
        context.putPrefString(ACTIVE_PREF_KEY, suite.createdAt.toString())
    }

    fun clearSelection(context: Context) {
        context.putPrefString(ACTIVE_PREF_KEY, "")
    }

    fun explicitSavedIndex(context: Context, suites: List<ReadMenuThemeSuite>): Int {
        val activeCreatedAt = context.getPrefString(ACTIVE_PREF_KEY).orEmpty().toLongOrNull()
        return suites.indexOfFirst {
            it.createdAt == activeCreatedAt && it.matchesCurrentTheme()
        }
    }

    fun selectedSavedIndex(context: Context, suites: List<ReadMenuThemeSuite>): Int {
        val activeIndex = explicitSavedIndex(context, suites)
        return activeIndex.takeIf { it != -1 }
            ?: suites.indexOfFirst { it.matchesCurrentTheme() }
    }

    fun delete(context: Context, suite: ReadMenuThemeSuite) {
        val suites = load(context).filterNot { it.createdAt == suite.createdAt }
        context.putPrefString(PREF_KEY, GSON.toJson(suites))
        clearSelection(context)
    }

    fun captureCurrent(name: String): ReadMenuThemeSuite {
        return ReadMenuThemeSuite.captureCurrent(name)
    }
}
