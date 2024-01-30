package mc.get.basicnavdemo

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.tomtom.sdk.tts.BuildConfig
import mc.get.basicnavdemo.ui.theme.BasicNavDemoTheme

import com.tomtom.quantity.Distance
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStore
import com.tomtom.sdk.datamanagement.navigationtile.NavigationTileStoreConfiguration
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.location.LocationProvider
import com.tomtom.sdk.location.OnLocationUpdateListener
import com.tomtom.sdk.location.android.AndroidLocationProvider
import com.tomtom.sdk.location.mapmatched.MapMatchedLocationProvider
import com.tomtom.sdk.location.simulation.SimulationLocationProvider
import com.tomtom.sdk.location.simulation.strategy.InterpolationStrategy
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraChangeListener
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.camera.CameraTrackingMode
import com.tomtom.sdk.map.display.common.WidthByZoom
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.gesture.MapLongClickListener
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.location.LocationMarkerOptions
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.route.Instruction
import com.tomtom.sdk.map.display.route.RouteClickListener
import com.tomtom.sdk.map.display.route.RouteOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.ui.currentlocation.CurrentLocationButton.VisibilityPolicy
import com.tomtom.sdk.navigation.ActiveRouteChangedListener
import com.tomtom.sdk.navigation.NavigationOptions
import com.tomtom.sdk.navigation.ProgressUpdatedListener
import com.tomtom.sdk.navigation.RouteAddedListener
import com.tomtom.sdk.navigation.RouteAddedReason
import com.tomtom.sdk.navigation.RoutePlan
import com.tomtom.sdk.navigation.RouteRemovedListener
import com.tomtom.sdk.navigation.TomTomNavigation
import com.tomtom.sdk.navigation.online.Configuration
import com.tomtom.sdk.navigation.online.OnlineTomTomNavigationFactory
import com.tomtom.sdk.navigation.ui.NavigationFragment
import com.tomtom.sdk.navigation.ui.NavigationUiOptions
import com.tomtom.sdk.routing.RoutePlanner
import com.tomtom.sdk.routing.RoutePlanningCallback
import com.tomtom.sdk.routing.RoutePlanningResponse
import com.tomtom.sdk.routing.RoutingFailure
import com.tomtom.sdk.routing.online.OnlineRoutePlanner
import com.tomtom.sdk.routing.options.Itinerary
import com.tomtom.sdk.routing.options.RoutePlanningOptions
import com.tomtom.sdk.routing.options.guidance.ExtendedSections
import com.tomtom.sdk.routing.options.guidance.GuidanceOptions
import com.tomtom.sdk.routing.options.guidance.InstructionPhoneticsType
import com.tomtom.sdk.routing.route.Route
import com.tomtom.sdk.vehicle.Vehicle
import com.tomtom.sdk.vehicle.VehicleProviderFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var mapFragment: MapFragment
    private lateinit var tomTomMap: TomTomMap
    private lateinit var navigationTileStore: NavigationTileStore
    private lateinit var locationProvider: LocationProvider
    private lateinit var onLocationUpdateListener: OnLocationUpdateListener
    private lateinit var routePlanner: RoutePlanner
    private var route: Route? = null
    private lateinit var route1: com.tomtom.sdk.map.display.route.Route
    private lateinit var routePlanningOptions: RoutePlanningOptions
    private lateinit var tomTomNavigation: TomTomNavigation
    private lateinit var navigationFragment: NavigationFragment
    private lateinit var myButton: Button
private val apiKey = "HjdeHZBjFd9C7rdqi8j4FVd1QRNDtg";
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(areLocationPermissionsGranted()){
            initMap()
        }
    }


//    initialization of map

private fun initMap() {
    val mapOptions = MapOptions(mapKey = apiKey)
    mapFragment = MapFragment.newInstance(mapOptions)
    supportFragmentManager.beginTransaction()
        .replace(R.id.map_container, mapFragment)
        .commit()
    mapFragment.getMapAsync { map ->
        tomTomMap = map
        showUserLocation()
        setUpMapListeners()
    }
}

//    getting location permissions
    private fun areLocationPermissionsGranted() = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    // intializing location provider for the map
    private fun initLocationProvider() {
        locationProvider = AndroidLocationProvider(context = this)
        locationProvider.enable()
    }

//function to show user location. automatically will zoom the map when map is loaded
    private fun showUserLocation() {
        locationProvider.enable()
        // zoom to current location at city level
        onLocationUpdateListener = OnLocationUpdateListener { location ->
            tomTomMap.moveCamera(CameraOptions(location.position, zoom = 8.0))
            locationProvider.removeOnLocationUpdateListener(onLocationUpdateListener)
        }
        locationProvider.addOnLocationUpdateListener(onLocationUpdateListener)
        tomTomMap.setLocationProvider(locationProvider)
        val locationMarker = LocationMarkerOptions(type = LocationMarkerOptions.Type.Pointer)
        tomTomMap.enableLocationMarker(locationMarker)
    }


//    planning and simulating route
//intializing route
    private fun initRouting() {
    routePlanner = OnlineRoutePlanner.create(context = this, apiKey = apiKey)
}

    //displaying map on route
    private val routePlanningCallback = object : RoutePlanningCallback {
        override fun onSuccess(result: RoutePlanningResponse) {
            route = result.routes.first()
            route?.let { drawRoute(it) }
        }

        override fun onFailure(failure: RoutingFailure) {
            Toast.makeText(this@MainActivity, failure.message, Toast.LENGTH_SHORT).show()
        }

        override fun onRoutePlanned(route: Route) = Unit
    }

//    function to draw route
private fun drawRoute(
    route: Route,
    color: Int = RouteOptions.DEFAULT_COLOR,
    withDepartureMarker: Boolean = true,
    withZoom: Boolean = true
) {
    val instructions = route.legs
        .flatMap { routeLeg -> routeLeg.instructions }
        .map {
            Instruction(
                routeOffset = it.routeOffset
            )
        }
    val routeOptions = RouteOptions(
        geometry = route.geometry,
        destinationMarkerVisible = true,
        departureMarkerVisible = withDepartureMarker,
        instructions = instructions,
        routeOffset = route.routePoints.map { it.routeOffset },
        color = color,
        tag = route.id.toString()
    )
    tomTomMap.addRoute(routeOptions)
    if (withZoom) {
        tomTomMap.zoomToRoutes(ZOOM_TO_ROUTE_PADDING)
    }
}

//    static object
    companion object {
        private const val ZOOM_TO_ROUTE_PADDING = 100
    }

    private fun clearMap() {
        tomTomMap.clear()
    }
    private fun calculateRouteTo(destination: GeoPoint) {
        val userLocation =
            tomTomMap.currentLocation?.position ?: return
        val itinerary = Itinerary(origin = userLocation, destination = destination)
        routePlanningOptions = RoutePlanningOptions(
            itinerary = itinerary,
            guidanceOptions = GuidanceOptions(),
            vehicle = Vehicle.Car()
        )
        routePlanner.planRoute(routePlanningOptions, routePlanningCallback)
    }
    private val mapLongClickListener = MapLongClickListener { geoPoint ->
        clearMap()
        calculateRouteTo(geoPoint)
        true
    }

    private fun setUpMapListeners() {
        tomTomMap.addMapLongClickListener(mapLongClickListener)
        tomTomMap.addRouteClickListener(routeClickListener)
    }

//initializing navigationTile
    private fun initNavigationTileStore() {
        navigationTileStore = NavigationTileStore.create(
            context = this,
            navigationTileStoreConfig = NavigationTileStoreConfiguration(apiKey)
        )
    }
    private fun initNavigation() {
        tomTomNavigation = OnlineTomTomNavigationFactory.create(
            Configuration(
                context = this,
                navigationTileStore = navigationTileStore,
                locationProvider = locationProvider,
                routePlanner = routePlanner
            )
        )
        tomTomNavigation.preferredLanguage = Locale.US
    }

    private fun initNavigationFragment() {
        val navigationUiOptions = NavigationUiOptions(
            keepInBackground = true
        )
        navigationFragment = NavigationFragment.newInstance(navigationUiOptions)
        supportFragmentManager.beginTransaction()
            .add(R.id.navigation_fragment_container, navigationFragment)
            .commitNow()
    }

    private fun startNavigation(route: Route) {
        initNavigationFragment()
        navigationFragment.setTomTomNavigation(tomTomNavigation)
        val routePlan = RoutePlan(route, routePlanningOptions)
        navigationFragment.startNavigation(routePlan)
        navigationFragment.addNavigationListener(navigationListener)
        tomTomNavigation.addProgressUpdatedListener(progressUpdatedListener)
        tomTomNavigation.addRouteAddedListener(routeAddedListener)
        tomTomNavigation.addRouteRemovedListener(routeRemovedListener)
        tomTomNavigation.addActiveRouteChangedListener(activeRouteChangedListener)
    }

    private val navigationListener = object : NavigationFragment.NavigationListener {
        override fun onStarted() {
            tomTomMap.addCameraChangeListener(cameraChangeListener)
            tomTomMap.cameraTrackingMode = CameraTrackingMode.FollowRouteDirection
            tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Chevron))
            setMapMatchedLocationProvider()
            route?.let { setSimulationLocationProviderToNavigation(it) }
            setMapNavigationPadding()
        }

        override fun onStopped() {
            stopNavigation()
        }
    }

    private val progressUpdatedListener = ProgressUpdatedListener {
        tomTomMap.routes.first().progress = it.distanceAlongRoute
    }

    private fun isNavigationRunning(): Boolean = tomTomNavigation.navigationSnapshot != null

    private val routeClickListener = RouteClickListener {
        if (!isNavigationRunning()) {
            route?.let { route ->
                mapFragment.currentLocationButton.visibilityPolicy = VisibilityPolicy.Invisible
                startNavigation(route)
            }
        }
    }

    private val routeAddedListener by lazy {
        RouteAddedListener { route, _, routeAddedReason ->
            if (routeAddedReason !is RouteAddedReason.NavigationStarted) {
                drawRoute(
                    route = route,
                    color = Color.GRAY,
                    withDepartureMarker = false,
                    withZoom = false
                )
            }
        }
    }

    private val routeRemovedListener by lazy {
        RouteRemovedListener { route, _ ->
            tomTomMap.routes.find { it.tag == route.id.toString() }?.remove()
        }
    }

    private val activeRouteChangedListener by lazy {
        ActiveRouteChangedListener { route ->
            tomTomMap.routes.forEach {
                if (it.tag == route.id.toString()) {
                    it.color = RouteOptions.DEFAULT_COLOR
                } else {
                    it.color = Color.GRAY
                }
            }
        }
    }
    private val cameraChangeListener by lazy {
        CameraChangeListener {
            val cameraTrackingMode = tomTomMap.cameraTrackingMode
            if (cameraTrackingMode == CameraTrackingMode.FollowRouteDirection) {
                navigationFragment.navigationView.showSpeedView()
            } else {
                navigationFragment.navigationView.hideSpeedView()
            }
        }
    }
    private fun setSimulationLocationProviderToNavigation(route: Route) {
        val routeGeoLocations = route.geometry.map { GeoLocation(it) }
        val simulationStrategy = InterpolationStrategy(routeGeoLocations)
        val oldLocationProvider = tomTomNavigation.locationProvider
        locationProvider = SimulationLocationProvider.create(strategy = simulationStrategy)
        tomTomNavigation.locationProvider = locationProvider
        oldLocationProvider.close()
        locationProvider.enable()
    }

    private fun setMapMatchedLocationProvider() {
        val mapMatchedLocationProvider = MapMatchedLocationProvider(tomTomNavigation)
        tomTomMap.setLocationProvider(mapMatchedLocationProvider)
        mapMatchedLocationProvider.enable()
    }


    private fun setMapNavigationPadding() {
        val paddingBottom = resources.getDimensionPixelOffset(R.dimen.map_padding_bottom)
        val padding = Padding(0, 0, 0, paddingBottom)
        tomTomMap.setPadding(padding)
    }
    private fun stopNavigation() {
        navigationFragment.stopNavigation()
        mapFragment.currentLocationButton.visibilityPolicy =
            VisibilityPolicy.InvisibleWhenRecentered
        tomTomMap.removeCameraChangeListener(cameraChangeListener)
        tomTomMap.cameraTrackingMode = CameraTrackingMode.None
        tomTomMap.enableLocationMarker(LocationMarkerOptions(LocationMarkerOptions.Type.Pointer))
        tomTomMap.setPadding(Padding(0, 0, 0, 0))
        navigationFragment.removeNavigationListener(navigationListener)
        tomTomNavigation.removeProgressUpdatedListener(progressUpdatedListener)
        tomTomNavigation.removeRouteAddedListener(routeAddedListener)
        tomTomNavigation.removeRouteRemovedListener(routeRemovedListener)
        tomTomNavigation.removeActiveRouteChangedListener(activeRouteChangedListener)
        clearMap()
        initLocationProvider()
        showUserLocation()
    }

    override fun onDestroy() {
        tomTomMap.setLocationProvider(null)
        tomTomNavigation.close()
        locationProvider.close()
        navigationTileStore.close()
        super.onDestroy()
    }
}
