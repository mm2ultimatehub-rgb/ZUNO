/*
 * ZUNO (2026)
 * Based on ArchiveTune — GPL-3.0 License.
 * Preserve upstream notices and project license.
 */

package moe.rukamori.archivetune.branding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage

/**
 * The shared ZUNO brand mark. Preset and user-imported logos use the same animated surface
 * wherever the app presents its identity.
 */
@Composable
fun ZunoLogoImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ZunoLogoStore.initialize(context)
    }
    val logoState by ZunoLogoStore.state.collectAsState()

    AnimatedContent(
        targetState = logoState.selectedId,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.88f)) togetherWith
                (fadeOut() + scaleIn(initialScale = 1.04f))
        },
        label = "zunoLogoChange",
        modifier = modifier,
    ) { selectedId ->
        if (selectedId.startsWith("custom:")) {
            val fileName = selectedId.removePrefix("custom:")
            val file = ZunoLogoStore.customFile(context, fileName)
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier,
                )
            } else {
                Image(
                    painter = painterResource(ZunoLogoCatalog.drawableFor(ZunoLogoCatalog.DefaultId)),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier,
                )
            }
        } else {
            Image(
                painter = painterResource(ZunoLogoCatalog.drawableFor(selectedId)),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier,
            )
        }
    }
}
