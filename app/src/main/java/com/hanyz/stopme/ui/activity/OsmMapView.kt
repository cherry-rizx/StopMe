package com.hanyz.stopme.ui.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hanyz.stopme.R
import com.hanyz.stopme.model.TransitStop
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

// Peta OpenStreetMap (osmdroid) yang dibungkus untuk Compose
@Composable
fun OsmMapView(
    routePoints: List<MapPoint>,
    stops: List<TransitStop>,
    destination: TransitStop?,
    userPoint: MapPoint,
    isDarkMap: Boolean,
    recenterRequest: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            controller.setZoom(15.0)
        }
    }

    // Ikon marker dibuat sekali
    val icons = remember { MarkerIcons(context) }

    // Ikuti lifecycle layar agar peta tidak bocor memori
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Posisikan kamera ke awal rute saat rute berubah
    LaunchedEffect(routePoints) {
        routePoints.firstOrNull()?.let {
            mapView.controller.setCenter(GeoPoint(it.lat, it.lng))
        }
    }

    // Tombol "lokasi saya" menaikkan recenterRequest
    LaunchedEffect(recenterRequest) {
        if (recenterRequest > 0) {
            mapView.controller.animateTo(GeoPoint(userPoint.lat, userPoint.lng), 16.0, 600L)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            update = { mv ->
                // Tema gelap dengan membalik warna tile
                mv.overlayManager.tilesOverlay.setColorFilter(
                    if (isDarkMap) TilesOverlay.INVERT_COLORS else null
                )

                mv.overlays.clear()

                // Garis rute
                if (routePoints.size >= 2) {
                    val line = Polyline(mv).apply {
                        setPoints(routePoints.map { GeoPoint(it.lat, it.lng) })
                        outlinePaint.color = android.graphics.Color.parseColor("#3DD5F3")
                        outlinePaint.strokeWidth = 8f * context.resources.displayMetrics.density / 2f
                        infoWindow = null
                    }
                    mv.overlays.add(line)
                }

                // Marker kecil tiap halte (kecuali tujuan)
                stops.dropLast(1).forEach { stop ->
                    mv.overlays.add(
                        Marker(mv).apply {
                            position = GeoPoint(stop.lat, stop.lng)
                            title = stop.name
                            icon = icons.stopDot
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                    )
                }

                // Marker merah tujuan
                destination?.let { dest ->
                    mv.overlays.add(
                        Marker(mv).apply {
                            position = GeoPoint(dest.lat, dest.lng)
                            title = "Tujuan: ${dest.name}"
                            icon = icons.destinationPin
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                    )
                }

                // Posisi user
                mv.overlays.add(
                    Marker(mv).apply {
                        position = GeoPoint(userPoint.lat, userPoint.lng)
                        title = "Posisi Anda"
                        icon = icons.userDot
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                )

                mv.invalidate()
            }
        )

        // Atribusi wajib lisensi OpenStreetMap
        Text(
            text = "© OpenStreetMap contributors",
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// Kumpulan ikon marker
private class MarkerIcons(context: Context) {
    private val density = context.resources.displayMetrics.density

    val stopDot: Drawable = dot(10, android.graphics.Color.WHITE, android.graphics.Color.parseColor("#052659"), 2)
    val userDot: Drawable = dot(18, android.graphics.Color.parseColor("#4280D8"), android.graphics.Color.WHITE, 3)
    val destinationPin: Drawable = run {
        val size = (36 * density).toInt()
        val base = ContextCompat.getDrawable(context, R.drawable.ic_lokasi)
        if (base != null) {
            BitmapDrawable(context.resources, base.toBitmap(size, size, Bitmap.Config.ARGB_8888))
        } else {
            dot(16, android.graphics.Color.RED, android.graphics.Color.WHITE, 3)
        }
    }

    private fun dot(sizeDp: Int, fill: Int, stroke: Int, strokeDp: Int): Drawable {
        val px = (sizeDp * density).toInt()
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke((strokeDp * density).toInt(), stroke)
            setSize(px, px)
        }
    }
}
