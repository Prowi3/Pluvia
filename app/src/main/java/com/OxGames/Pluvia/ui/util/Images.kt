package com.OxGames.Pluvia.ui.util

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@Composable
internal fun ListItemImage(
    contentDescription: String? = null,
    size: Dp = 40.dp,
    image: () -> Any?,
) {
    CoilImage(
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        imageModel = image,
        imageOptions = ImageOptions(
            contentScale = ContentScale.Crop,
            contentDescription = contentDescription,
        ),
        loading = {
            CircularProgressIndicator()
        },
        failure = {
            Icon(Icons.Filled.QuestionMark, null)
        },
        previewPlaceholder = painterResource(R.drawable.icon_mono_foreground),
    )
}

@Composable
fun EmoticonImage(
    size: Dp = 54.dp,
    image: () -> Any?,
) {
    CoilImage(
        modifier = Modifier.size(size), // TODO may not be pixel perfect
        imageModel = image,
        loading = {
            CircularProgressIndicator()
        },
        failure = {
            Icon(Icons.Filled.QuestionMark, null)
        },
        previewPlaceholder = painterResource(R.drawable.icon_mono_foreground),
    )
}

@Composable
fun StickerImage(
    size: Dp = 150.dp,
    image: () -> Any?,
) {
    EmoticonImage(size, image)
}

@Preview
@Composable
private fun Preview_ListItemImage() {
    PluviaTheme {
        ListItemImage { }
    }
}

@Preview
@Composable
private fun Preview_EmoticonImage() {
    PluviaTheme {
        EmoticonImage { "https://steamcommunity-a.akamaihd.net/economy/emoticonlarge/roar" }
    }
}

@Preview
@Composable
private fun Preview_StickerImage() {
    PluviaTheme {
        StickerImage { "https://steamcommunity-a.akamaihd.net/economy/sticker/Delivery%20Cat%20in%20a%20Blanket" }
    }
}
