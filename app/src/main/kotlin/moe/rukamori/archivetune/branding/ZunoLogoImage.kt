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
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
                (fadeOut() + scaleOut(targetScale = 1.04f))
        },
        label = "zunoLogoChange",
        modifier = modifier,
    ) { selectedId ->
        val file = ZunoLogoStore.logoFile(context, selectedId)
            ?: ZunoLogoStore.logoFile(context, ZunoLogoCatalog.DefaultId)
        AsyncImage(
            model = file,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
