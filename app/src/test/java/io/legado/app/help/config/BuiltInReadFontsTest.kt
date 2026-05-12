package io.legado.app.help.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInReadFontsTest {

    @Test
    fun sourceHanSansResolvesToVariableAsset() {
        val sansPlan = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri("font/source_han_sans_cn_regular.otf"),
            875
        )

        assertEquals("font/source_han_sans_sc_vf.otf", sansPlan?.assetPath)
        assertTrue(sansPlan?.variable == true)
        assertEquals(0f, sansPlan?.syntheticStrokeEm ?: -1f, 0f)
    }

    @Test
    fun sourceHanSerifIsNotABuiltInReadFont() {
        assertNull(
            BuiltInReadFonts.weightPlan(
                BuiltInReadFonts.uri("font/source_han_serif_cn_regular.otf"),
                400
            )
        )
        assertNull(
            BuiltInReadFonts.weightPlan(
                BuiltInReadFonts.uri("font/source_han_serif_sc_vf.otf"),
                400
            )
        )
    }

    @Test
    fun wenYuanAndMiSansResolveToVariableAssets() {
        val wenYuanPlan = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.WEN_YUAN_SANS_SC_VF),
            650
        )
        val miSansPlan = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.MI_SANS_VF),
            350
        )

        assertEquals("font/wenyuan_sans_sc_vf.otf", wenYuanPlan?.assetPath)
        assertTrue(wenYuanPlan?.variable == true)
        assertEquals(0f, wenYuanPlan?.syntheticStrokeEm ?: -1f, 0f)
        assertEquals("font/mi_sans_vf.ttf", miSansPlan?.assetPath)
        assertTrue(miSansPlan?.variable == true)
        assertEquals(0f, miSansPlan?.syntheticStrokeEm ?: -1f, 0f)
    }

    @Test
    fun alimamaFangYuanResolvesToVariableAsset() {
        val plan = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.ALIMAMA_FANG_YUAN_TI_VF),
            720
        )

        assertEquals("font/alimama_fang_yuan_ti_vf.ttf", plan?.assetPath)
        assertTrue(plan?.variable == true)
        assertEquals(0f, plan?.syntheticStrokeEm ?: -1f, 0f)
    }

    @Test
    fun harmonyFontUsesNearestLowerStaticWeightWithStrokeCompensation() {
        val plan = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.HARMONYOS_SANS_SC),
            650
        )

        assertEquals("font/harmonyos_sans_sc_medium.ttf", plan?.assetPath)
        assertEquals(500, plan?.baseWeight)
        assertTrue((plan?.syntheticStrokeEm ?: 0f) > 0f)
    }

    @Test
    fun singleWeightFontsReceiveProgressiveSyntheticStrokeWhenHeavier() {
        val medium = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.LXGW_WENKAI_SCREEN),
            500
        )
        val heavy = BuiltInReadFonts.weightPlan(
            BuiltInReadFonts.uri(BuiltInReadFonts.LXGW_WENKAI_SCREEN),
            900
        )

        assertEquals(BuiltInReadFonts.LXGW_WENKAI_SCREEN, medium?.assetPath)
        assertTrue((medium?.syntheticStrokeEm ?: 0f) > 0f)
        assertTrue((heavy?.syntheticStrokeEm ?: 0f) > (medium?.syntheticStrokeEm ?: 0f))
    }
}
