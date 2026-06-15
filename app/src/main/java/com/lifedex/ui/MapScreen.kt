package com.lifedex.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lifedex.data.GotchaCard
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: GotchaViewModel
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsState()

    // Initialize osmdroid configuration
    DisposableEffect(Unit) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            
            // Set initial view slightly zoomed out over a general area or the first item
            val startPoint = if (cards.isNotEmpty()) {
                GeoPoint(cards.first().latitude, cards.first().longitude)
            } else {
                GeoPoint(37.5665, 126.9780) // Seoul
            }
            
            controller.setZoom(10.0)
            controller.setCenter(startPoint)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView
        },
        update = { view ->
            // Clear existing overlays
            view.overlays.clear()
            
            // Add markers for each GotchaCard
            for (card in cards) {
                val marker = Marker(view)
                marker.position = GeoPoint(card.latitude, card.longitude)
                marker.title = card.title
                
                // Use captured image as marker icon if available
                card.imagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 120, 120, true)
                            marker.icon = android.graphics.drawable.BitmapDrawable(context.resources, scaled)
                        }
                    }
                }
                
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                view.overlays.add(marker)
            }
            view.invalidate()
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }
}
