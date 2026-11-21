/*
 * ZUNO (2026)
 * Based on ArchiveTune — GPL-3.0 License.
 * Preserve upstream notices and project license.
 */

package moe.rukamori.archivetune.branding

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import moe.rukamori.archivetune.R

/**
 * The fixed ZUNO brand mark. User-selected launcher icons never alter in-app branding.
 */
@Composable
fun ZunoLogoImage(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(R.drawable.zuno_icon),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
