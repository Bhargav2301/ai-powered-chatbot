package com.polymath.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.polymath.model.SourceImage

@Composable fun SourceImages(images: List<SourceImage>, compact: Boolean = false) {
    val context = LocalContext.current
    (if (compact) images.take(1) else images).forEach { image ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(image.url).size(1000, 800)
                    .diskCachePolicy(CachePolicy.DISABLED).crossfade(true).build(),
                contentDescription = image.alt.ifBlank { "Source illustration" },
                modifier = Modifier.fillMaxWidth().height(if (compact) 170.dp else 230.dp),
                contentScale = ContentScale.Fit,
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } },
                error = { Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ImageNotSupported, null, tint = Muted)
                    Text("Image unavailable", color = Muted)
                    Text(image.alt, style = MaterialTheme.typography.bodySmall, color = Muted)
                } }
            )
            if (image.caption.isNotBlank()) Text(image.caption, style = MaterialTheme.typography.bodySmall, color = Muted)
            if (compact && images.size > 1) Text("${images.size} source images · Open to view all", color = Sage, style = MaterialTheme.typography.bodySmall)
        }
    }
}
