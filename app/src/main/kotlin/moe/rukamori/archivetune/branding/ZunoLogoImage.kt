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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import moe.rukamori.archivetune.R

/**
 * Shared ZUNO brand mark. The default brand selection uses the official
 * silver-waveform vector; user-selected presets and imported logos remain supported.
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
        targetState = logoState.selectedId to logoState.isInitialized,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.88f)) togetherWith
                (fadeOut() + scaleOut(targetScale = 1.04f))
        },
        label = "zunoLogoChange",
        modifier = modifier,
    ) { (selectedId, _) ->
        if (selectedId == ZunoLogoCatalog.DefaultId) {
            Image(
                painter = painterResource(R.drawable.zuno_icon),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val file = ZunoLogoStore.logoFile(context, selectedId)
                ?: ZunoLogoStore.logoFile(context, ZunoLogoCatalog.DefaultId)
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.zuno_icon),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
