package io.legado.app.ui.book.read.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import io.legado.app.ui.book.read.page.entities.TextPage

internal data class EpubComposePageState(
    val contentView: ContentTextView,
    val textPage: TextPage,
    val frame: Int
)

@Composable
internal fun EpubComposePage(state: EpubComposePageState) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Keep the existing EPUB pagination and draw commands, but host them on Compose.
        drawIntoCanvas { canvas ->
            state.contentView.drawForEpubCompose(canvas.nativeCanvas)
        }
    }
}
