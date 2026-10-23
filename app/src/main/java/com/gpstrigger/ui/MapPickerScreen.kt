package com.gpstrigger.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.location.Address
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gpstrigger.Permissions
import com.gpstrigger.R
import com.gpstrigger.data.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.roundToInt

private val DEFAULT_CENTER = GeoPoint(41.9, 12.5)
private const val MIN_RADIUS = 100f
private const val MAX_RADIUS = 2000f
private const val RADIUS_STEP = 50f

/**
 * Turns the light OpenStreetMap tiles dark: inverts the colours, then rotates the hue by 180°
 * so water stays blue and parks stay green.
 */
private val DarkTilesFilter = ColorMatrixColorFilter(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    ).apply {
        // W3C hue-rotate(180deg) matrix.
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    -0.574f, 1.430f, 0.144f, 0f, 0f,
                    0.426f, 0.430f, 0.144f, 0f, 0f,
                    0.426f, 1.430f, -0.856f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }
)

/** A one-shot request to move the camera; identity-compared so the same point can be requested twice. */
private class CenterRequest(val point: GeoPoint)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initial: Zone,
    onConfirm: (latitude: Double, longitude: Double, radius: Float, address: String?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var point by remember {
        mutableStateOf(if (initial.hasLocation) GeoPoint(initial.latitude, initial.longitude) else null)
    }
    var radius by remember { mutableFloatStateOf(initial.radiusMeters) }
    var address by remember { mutableStateOf(initial.address) }
    var centerRequest by remember { mutableStateOf<CenterRequest?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Address>?>(null) }
    var searching by remember { mutableStateOf(false) }

    fun moveTo(target: GeoPoint, label: String? = null) {
        point = target
        address = label
        centerRequest = CenterRequest(target)
    }

    fun search() {
        if (query.isBlank()) return
        keyboard?.hide()
        searching = true
        scope.launch {
            results = LocationLookup.search(context, query)
            searching = false
        }
    }

    // New zone: start from the current position.
    LaunchedEffect(Unit) {
        if (point == null) LocationLookup.currentLocation(context)?.let { moveTo(it) }
    }
    // Look up the address of a pin placed by hand.
    LaunchedEffect(point) {
        val p = point ?: return@LaunchedEffect
        if (address != null) return@LaunchedEffect
        delay(400)
        address = LocationLookup.reverse(context, p)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pick_location_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OsmMap(
                point = point,
                radius = radius,
                centerRequest = centerRequest,
                onPointChange = {
                    point = it
                    address = null
                    results = null
                },
                modifier = Modifier.fillMaxSize(),
            )

            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), shadowElevation = 4.dp) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            when {
                                searching -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                query.isNotEmpty() -> IconButton(onClick = { query = ""; results = null }) {
                                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                results?.let { list ->
                    Card(Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                            when {
                                !LocationLookup.geocoderAvailable -> MessageItem(R.string.geocoder_unavailable)
                                list.isEmpty() -> MessageItem(R.string.search_no_results)
                                else -> list.forEach { result ->
                                    val label = LocationLookup.format(result)
                                    ListItem(
                                        headlineContent = { Text(label) },
                                        modifier = Modifier.clickable {
                                            moveTo(GeoPoint(result.latitude, result.longitude), label)
                                            results = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().imePadding().navigationBarsPadding()) {
                if (Permissions.hasFineLocation(context)) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { LocationLookup.currentLocation(context)?.let { moveTo(it) } } },
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.my_location))
                    }
                }
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        val p = point
                        Text(
                            text = when {
                                p == null -> stringResource(R.string.map_hint)
                                address != null -> address!!
                                else -> stringResource(R.string.coordinates, p.latitude, p.longitude)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.radius_value, radius.roundToInt()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Slider(
                            value = radius,
                            onValueChange = { radius = (it / RADIUS_STEP).roundToInt() * RADIUS_STEP },
                            valueRange = MIN_RADIUS..MAX_RADIUS,
                            steps = ((MAX_RADIUS - MIN_RADIUS) / RADIUS_STEP).toInt() - 1,
                        )
                        Button(
                            onClick = { p?.let { onConfirm(it.latitude, it.longitude, radius, address) } },
                            enabled = p != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.confirm_location))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(text: Int) {
    Text(stringResource(text), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
}

/** OpenStreetMap view with a draggable pin and the zone circle. Tapping the map moves the pin. */
@Composable
private fun OsmMap(
    point: GeoPoint?,
    radius: Float,
    centerRequest: CenterRequest?,
    onPointChange: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnPointChange by rememberUpdatedState(onPointChange)
    val currentRadius by rememberUpdatedState(radius)
    val circleColor = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(if (point != null) 16.0 else 6.0)
            controller.setCenter(point ?: DEFAULT_CENTER)
        }
    }
    val circle = remember {
        Polygon(mapView).apply {
            fillPaint.color = circleColor.copy(alpha = 0.2f).toArgb()
            outlinePaint.color = circleColor.toArgb()
            outlinePaint.strokeWidth = 4f
            infoWindow = null
            // Let taps inside the circle reach the map so the pin can be moved there too.
            setOnClickListener { _, _, _ -> false }
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            // The pin tip sits at y=22 of the 24-unit viewport.
            setAnchor(Marker.ANCHOR_CENTER, 22f / 24f)
            isDraggable = true
            setOnMarkerClickListener { _, _ -> true }
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {
                    circle.points = Polygon.pointsAsCircle(marker.position, currentRadius.toDouble())
                }

                override fun onMarkerDragEnd(marker: Marker) = currentOnPointChange(marker.position)
                override fun onMarkerDragStart(marker: Marker) = Unit
            })
        }
    }
    val copyright = remember { CopyrightOverlay(context) }
    val myLocation = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    }

    DisposableEffect(Unit) {
        val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                currentOnPointChange(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                currentOnPointChange(p)
                return true
            }
        })
        // Overlays get touch events top-down, so the tap catcher goes at the bottom.
        mapView.overlays.addAll(listOf(tapOverlay, circle, myLocation, marker, copyright))
        onDispose { mapView.onDetach() }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    if (Permissions.hasFineLocation(context)) myLocation.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    myLocation.disableMyLocation()
                    mapView.onPause()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            myLocation.disableMyLocation()
        }
    }

    LaunchedEffect(centerRequest) {
        centerRequest?.let { mapView.controller.animateTo(it.point, 17.0, 600L) }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = {
            mapView.overlayManager.tilesOverlay.setColorFilter(if (dark) DarkTilesFilter else null)
            copyright.setTextColor(if (dark) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY)
            val p = point
            marker.isEnabled = p != null
            circle.isEnabled = p != null
            if (p != null) {
                marker.position = p
                circle.points = Polygon.pointsAsCircle(p, radius.toDouble())
            }
            mapView.invalidate()
        },
    )
}
