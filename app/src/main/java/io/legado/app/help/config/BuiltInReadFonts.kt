package io.legado.app.help.config

object BuiltInReadFonts {

    private const val ASSET_PREFIX = "asset://"
    private const val MAX_SYNTHETIC_STROKE_EM = 0.045f

    const val HARMONYOS_SANS_SC = "font/harmonyos_sans_sc_regular.ttf"
    const val HARMONYOS_SANS_SC_THIN = "font/harmonyos_sans_sc_thin.ttf"
    const val HARMONYOS_SANS_SC_LIGHT = "font/harmonyos_sans_sc_light.ttf"
    const val HARMONYOS_SANS_SC_MEDIUM = "font/harmonyos_sans_sc_medium.ttf"
    const val HARMONYOS_SANS_SC_BOLD = "font/harmonyos_sans_sc_bold.ttf"
    const val HARMONYOS_SANS_SC_BLACK = "font/harmonyos_sans_sc_black.ttf"
    const val SOURCE_HAN_SANS_CN = "font/source_han_sans_cn_regular.otf"
    const val SOURCE_HAN_SANS_SC_VF = "font/source_han_sans_sc_vf.otf"
    const val WEN_YUAN_SANS_SC_VF = "font/wenyuan_sans_sc_vf.otf"
    const val MI_SANS_VF = "font/mi_sans_vf.ttf"
    const val ALIMAMA_FANG_YUAN_TI_VF = "font/alimama_fang_yuan_ti_vf.ttf"
    const val LXGW_WENKAI_SCREEN = "font/lxgw_wenkai_screen.ttf"
    const val LXGW_NEO_XIHEI = "font/lxgw_neo_xihei.ttf"
    const val LXGW_FASMART_GOTHIC = "font/lxgw_fasmart_gothic.ttf"
    const val LXGW_ZHENKAI_GB = "font/lxgw_zhenkai_gb_regular.ttf"

    data class WeightPlan(
        val assetPath: String,
        val variable: Boolean,
        val baseWeight: Int,
        val syntheticStrokeEm: Float,
    )

    private data class StaticAsset(
        val assetPath: String,
        val weight: Int,
    )

    private val harmonyWeights = listOf(
        StaticAsset(HARMONYOS_SANS_SC_THIN, 100),
        StaticAsset(HARMONYOS_SANS_SC_LIGHT, 300),
        StaticAsset(HARMONYOS_SANS_SC, 400),
        StaticAsset(HARMONYOS_SANS_SC_MEDIUM, 500),
        StaticAsset(HARMONYOS_SANS_SC_BOLD, 700),
        StaticAsset(HARMONYOS_SANS_SC_BLACK, 900),
    )

    private val harmonyAliases = harmonyWeights
        .map { it.assetPath }
        .toSet()

    private val singleWeightAssets = setOf(
        LXGW_WENKAI_SCREEN,
        LXGW_NEO_XIHEI,
        LXGW_FASMART_GOTHIC,
        LXGW_ZHENKAI_GB,
    )

    fun uri(assetPath: String): String {
        return "$ASSET_PREFIX$assetPath"
    }

    fun assetPath(fontPath: String): String? {
        return fontPath
            .takeIf { it.startsWith(ASSET_PREFIX, ignoreCase = true) }
            ?.substring(ASSET_PREFIX.length)
            ?.takeIf { it.isNotBlank() }
    }

    fun weightPlan(fontPath: String, targetWeight: Int): WeightPlan? {
        val assetPath = assetPath(fontPath) ?: fontPath.takeIf { it.startsWith("font/") } ?: return null
        val weight = targetWeight.coerceIn(100, 900)
        return when (assetPath) {
            SOURCE_HAN_SANS_CN,
            SOURCE_HAN_SANS_SC_VF -> WeightPlan(
                assetPath = SOURCE_HAN_SANS_SC_VF,
                variable = true,
                baseWeight = weight,
                syntheticStrokeEm = 0f,
            )

            WEN_YUAN_SANS_SC_VF,
            MI_SANS_VF,
            ALIMAMA_FANG_YUAN_TI_VF -> WeightPlan(
                assetPath = assetPath,
                variable = true,
                baseWeight = weight,
                syntheticStrokeEm = 0f,
            )

            in harmonyAliases -> {
                val staticAsset = harmonyWeights
                    .lastOrNull { it.weight <= weight }
                    ?: harmonyWeights.first()
                WeightPlan(
                    assetPath = staticAsset.assetPath,
                    variable = false,
                    baseWeight = staticAsset.weight,
                    syntheticStrokeEm = syntheticStroke(staticAsset.weight, weight),
                )
            }

            in singleWeightAssets -> WeightPlan(
                assetPath = assetPath,
                variable = false,
                baseWeight = 400,
                syntheticStrokeEm = syntheticStroke(400, weight),
            )

            else -> null
        }
    }

    private fun syntheticStroke(baseWeight: Int, targetWeight: Int): Float {
        if (targetWeight <= baseWeight) {
            return 0f
        }
        val progress = (targetWeight - baseWeight) / 500f
        return (progress * MAX_SYNTHETIC_STROKE_EM).coerceIn(0f, MAX_SYNTHETIC_STROKE_EM)
    }
}
