package io.legado.app.ui.book;

import org.junit.Assert;
import org.junit.Test;

public class ProgressMinimapDragCalculatorTest {

    @Test
    public void outsideThumbStartCentersThumbOnTouch() {
        float touchY = 170f;
        float thumbTop = 24f;
        float thumbHeight = 60f;
        float trackTop = 0f;
        float trackBottom = 300f;

        float offset = ProgressMinimapDragCalculator.dragTouchOffset(
                false,
                touchY,
                thumbTop,
                thumbHeight
        );
        float ratio = ProgressMinimapDragCalculator.ratioForY(
                touchY,
                offset,
                trackTop,
                trackBottom,
                thumbHeight
        );

        Assert.assertEquals(thumbHeight / 2f, offset, 0f);
        Assert.assertEquals((touchY - thumbHeight / 2f) / (trackBottom - trackTop - thumbHeight), ratio, 0.0001f);
    }

    @Test
    public void insideThumbStartPreservesTouchOffset() {
        float touchY = 82f;
        float thumbTop = 48f;
        float thumbHeight = 60f;

        float offset = ProgressMinimapDragCalculator.dragTouchOffset(
                true,
                touchY,
                thumbTop,
                thumbHeight
        );

        Assert.assertEquals(touchY - thumbTop, offset, 0f);
    }
}
