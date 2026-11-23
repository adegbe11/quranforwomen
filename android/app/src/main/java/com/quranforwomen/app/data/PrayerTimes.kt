package com.quranforwomen.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

// Calculation

/** Standard astronomical prayer time calculation (the widely used PrayTimes method).
 *  Runs fully on the phone. Location is never sent to us; Android looks up the city name once. */
enum class Prayer(val title: String) {
    fajr("Fajr"), sunrise("Sunrise"), dhuhr("Dhuhr"), asr("Asr"), maghrib("Maghrib"), isha("Isha");

    /** Sunrise is shown for reference but is not a prayer. */
    val isPrayer: Boolean get() = this != sunrise

    companion object {
        val prayers: List<Prayer> get() = entries.filter { it.isPrayer }
    }
}

enum class CalculationMethod(val title: String, val fajrAngle: Double, val ishaAngle: Double?, val ishaMinutes: Double?) {
    muslimWorldLeague("Muslim World League", 18.0, 17.0, null),
    isna("North America (ISNA)", 15.0, 15.0, null),
    egypt("Egyptian General Authority", 19.5, 17.5, null),
    ummAlQura("Umm al-Qura, Makkah", 18.5, null, 90.0),
    karachi("University of Islamic Sciences, Karachi", 18.0, 18.0, null),
    dubai("Dubai", 18.2, 18.2, null),
    singapore("Singapore, Malaysia, Indonesia", 20.0, 18.0, null),
    turkey("Diyanet, Turkey", 18.0, 17.0, null);

    companion object {
        /** A sensible default for the region of a country code. */
        fun suggested(countryCode: String?): CalculationMethod = when (countryCode?.uppercase(Locale.US)) {
            "US", "CA" -> isna
            "SA", "YE", "OM", "BH", "KW", "QA" -> ummAlQura
            "AE" -> dubai
            "EG", "SD", "LY", "DZ", "MA", "TN", "SY", "LB", "JO", "IQ", "PS" -> egypt
            "PK", "IN", "BD", "AF" -> karachi
            "MY", "SG", "ID", "BN" -> singapore
            "TR" -> turkey
            else -> muslimWorldLeague
        }
    }
}

enum class AsrMethod(val title: String, val shadowFactor: Double) {
    standard("Standard (Shafi'i, Maliki, Hanbali)", 1.0),
    hanafi("Hanafi", 2.0),
}

class PrayerSchedule(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    fun time(prayer: Prayer): ZonedDateTime? = times[prayer]

    /** The next prayer after a moment, if any remain today. */
    fun next(after: ZonedDateTime = ZonedDateTime.now()): Pair<Prayer, ZonedDateTime>? =
        Prayer.prayers.mapNotNull { p -> times[p]?.let { p to it } }.firstOrNull { it.second.isAfter(after) }
}

object PrayerCalculator {
    fun schedule(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        asr: AsrMethod,
        zone: ZoneId = ZoneId.systemDefault(),
    ): PrayerSchedule {
        val day = date.atStartOfDay(zone)
        val tzHours = zone.rules.getOffset(day.plusHours(12).toInstant()).totalSeconds / 3600.0
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - longitude / (15 * 24)

        fun sunPosition(jd: Double): Pair<Double, Double> {
            val d = jd - 2451545.0
            val g = fixAngle(357.529 + 0.98560028 * d)
            val q = fixAngle(280.459 + 0.98564736 * d)
            val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
            val e = 23.439 - 0.00000036 * d
            val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15
            val eqt = q / 15 - fixHour(ra)
            val decl = darcsin(dsin(e) * dsin(l))
            return decl to eqt
        }

        fun midDay(t: Double) = fixHour(12 - sunPosition(jDate + t).second)

        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean = false): Double {
            val decl = sunPosition(jDate + t).first
            val noon = midDay(t)
            val ratio = (-dsin(angle) - dsin(decl) * dsin(latitude)) / (dcos(decl) * dcos(latitude))
            val span = darccos(ratio.coerceIn(-1.0, 1.0)) / 15
            return noon + if (ccw) -span else span
        }

        fun asrTime(factor: Double, t: Double): Double {
            val decl = sunPosition(jDate + t).first
            val angle = -darccot(factor + dtan(abs(latitude - decl)))
            return sunAngleTime(angle, t)
        }

        // Two passes: the first gives approximate times, the second refines them.
        var fajr = 5.0; var sunrise = 6.0; var dhuhr = 12.0; var asrT = 13.0; var sunset = 18.0; var isha = 18.0
        repeat(2) {
            val f = sunAngleTime(method.fajrAngle, fajr / 24, ccw = true)
            val r = sunAngleTime(0.833, sunrise / 24, ccw = true)
            val d = midDay(dhuhr / 24)
            val a = asrTime(asr.shadowFactor, asrT / 24)
            val s = sunAngleTime(0.833, sunset / 24)
            val i = method.ishaAngle?.let { sunAngleTime(it, isha / 24) } ?: s
            fajr = f; sunrise = r; dhuhr = d; asrT = a; sunset = s; isha = i
        }
        method.ishaMinutes?.let { isha = sunset + it / 60 }

        // High latitudes: keep Fajr and Isha within a share of the night (angle based rule).
        val night = fixHour(sunrise - sunset)
        val fajrLimit = method.fajrAngle / 60 * night
        if (fajr.isNaN() || fixHour(sunrise - fajr) > fajrLimit) fajr = sunrise - fajrLimit
        method.ishaAngle?.let { angle ->
            val ishaLimit = angle / 60 * night
            if (isha.isNaN() || fixHour(isha - sunset) > ishaLimit) isha = sunset + ishaLimit
        }

        val shift = tzHours - longitude / 15
        fun at(hours: Double): ZonedDateTime? =
            if (hours.isFinite()) day.plusSeconds(((hours + shift) * 3600).toLong()) else null

        val times = buildMap {
            at(fajr)?.let { put(Prayer.fajr, it) }
            at(sunrise)?.let { put(Prayer.sunrise, it) }
            // A minute after the sun passes its height, as most timetables do.
            at(dhuhr + 1.0 / 60)?.let { put(Prayer.dhuhr, it) }
            at(asrT)?.let { put(Prayer.asr, it) }
            at(sunset)?.let { put(Prayer.maghrib, it) }
            at(isha)?.let { put(Prayer.isha, it) }
        }
        return PrayerSchedule(date, times)
    }

    /** Direction of the Kaaba from a location, in degrees clockwise from true north. */
    fun qiblaBearing(latitude: Double, longitude: Double): Double {
        val kaabaLat = 21.4225
        val kaabaLon = 39.8262
        val dLon = kaabaLon - longitude
        val y = dsin(dLon)
        val x = dcos(latitude) * dtan(kaabaLat) - dsin(latitude) * dcos(dLon)
        return fixAngle(darctan2(y, x))
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year.toDouble()
        var m = month.toDouble()
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fixAngle(a: Double): Double { val r = a % 360; return if (r < 0) r + 360 else r }
    private fun fixHour(h: Double): Double { val r = h % 24; return if (r < 0) r + 24 else r }
}

/** The sound played with each prayer notification. Android ties a sound to a channel. */
enum class PrayerSound(val title: String, val channelId: String) {
    /** Original bell made for this app. Loud and clear. */
    bell("Prayer bell", "prayer_bell"),
    /** The phone's own notification sound. */
    system("Phone default", "prayer_default"),
}

// Store

/** Location, settings and today's schedule. Location is kept only on this phone. */
class PrayerStore(private val context: Context, private val scope: CoroutineScope) {
    private val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var latitude by mutableStateOf(if (sp.contains(K_LAT)) sp.getFloat(K_LAT, 0f).toDouble() else null); private set
    var longitude by mutableStateOf(if (sp.contains(K_LON)) sp.getFloat(K_LON, 0f).toDouble() else null); private set
    var placeName by mutableStateOf(sp.getString(K_PLACE, null)); private set
    var isLocating by mutableStateOf(false); private set
    var locationDenied by mutableStateOf(false); private set
    /** Compass heading in degrees from true north, while the Qibla screen listens. */
    var heading by mutableStateOf<Double?>(null); private set
    /** True when the compass needs a figure eight to calibrate. */
    var compassUnreliable by mutableStateOf(false); private set

    var calculationMethod by mutableStateOf(enumOr(sp.getString(K_METHOD, null), CalculationMethod.muslimWorldLeague))
        private set
    var asr by mutableStateOf(enumOr(sp.getString(K_ASR, null), AsrMethod.standard)); private set
    var notifyAtPrayerTimes by mutableStateOf(sp.getBoolean(K_NOTIFY, false)); private set
    var sound by mutableStateOf(enumOr(sp.getString(K_SOUND, null), PrayerSound.bell)); private set
    /** Which of the five prayers send a notification. All five by default. */
    var enabledPrayers by mutableStateOf(
        sp.getStringSet(K_ENABLED, null)?.mapNotNull { n -> Prayer.entries.firstOrNull { it.name == n } }?.toSet()
            ?: Prayer.prayers.toSet()
    ); private set

    /** Set by the app so notifications pause on rest days. */
    var isResting: Boolean = false
        set(value) { if (field != value) { field = value; refreshNotifications() } }

    fun updateMethod(m: CalculationMethod) { calculationMethod = m; sp.edit().putString(K_METHOD, m.name).apply(); refreshNotifications() }
    fun updateAsr(a: AsrMethod) { asr = a; sp.edit().putString(K_ASR, a.name).apply(); refreshNotifications() }
    fun updateSound(s: PrayerSound) { sound = s; sp.edit().putString(K_SOUND, s.name).apply(); refreshNotifications() }
    fun updateNotify(on: Boolean) { notifyAtPrayerTimes = on; sp.edit().putBoolean(K_NOTIFY, on).apply(); refreshNotifications() }
    fun updateEnabled(set: Set<Prayer>) {
        enabledPrayers = set
        sp.edit().putStringSet(K_ENABLED, set.map { it.name }.toSet()).apply()
        refreshNotifications()
    }

    val hasLocation: Boolean get() = latitude != null && longitude != null

    fun schedule(date: LocalDate = LocalDate.now()): PrayerSchedule? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        return PrayerCalculator.schedule(date, lat, lon, calculationMethod, asr)
    }

    /** The next prayer, looking into tomorrow after Isha. */
    fun nextPrayer(after: ZonedDateTime = ZonedDateTime.now()): Pair<Prayer, ZonedDateTime>? {
        schedule(after.toLocalDate())?.next(after)?.let { return it }
        val fajr = schedule(after.toLocalDate().plusDays(1))?.time(Prayer.fajr) ?: return null
        return Prayer.fajr to fajr
    }

    val qiblaBearing: Double?
        get() {
            val lat = latitude ?: return null
            val lon = longitude ?: return null
            return PrayerCalculator.qiblaBearing(lat, lon)
        }

    // Location

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Call after the location permission is granted. Asks every source the phone has at once
     *  (network, fused, GPS), keeps the first answer, and falls back to the last known place. */
    @SuppressLint("MissingPermission")
    fun requestLocation() {
        locationDenied = false
        if (!hasPermission()) { locationDenied = true; return }
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        val providers = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        fun lastKnown(): Location? =
            (providers + LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }

        if (providers.isEmpty()) {
            // Location is switched off on the phone; use the last known place if there is one.
            lastKnown()?.let(::save) ?: run { isLocating = false }
            return
        }
        isLocating = true
        val signals = providers.map { CancellationSignal() }
        var answered = false
        var pending = providers.size
        providers.forEachIndexed { index, provider ->
            LocationManagerCompat.getCurrentLocation(lm, provider, signals[index], ContextCompat.getMainExecutor(context)) { location ->
                pending--
                if (answered) return@getCurrentLocation
                if (location != null) {
                    answered = true
                    signals.forEach { runCatching { it.cancel() } }
                    save(location)
                } else if (pending == 0) {
                    lastKnown()?.let(::save) ?: run { isLocating = false }
                }
            }
        }
    }

    fun markDenied() { locationDenied = true; isLocating = false }

    private fun save(location: Location) {
        isLocating = false
        val isFirst = !hasLocation
        latitude = location.latitude
        longitude = location.longitude
        sp.edit().putFloat(K_LAT, location.latitude.toFloat()).putFloat(K_LON, location.longitude.toFloat()).apply()
        scope.launch {
            val (name, country) = withContext(Dispatchers.IO) { cityName(location) }
            placeName = name
            sp.edit().putString(K_PLACE, name).apply()
            if (isFirst) updateMethod(CalculationMethod.suggested(country))
        }
        refreshNotifications()
    }

    @Suppress("DEPRECATION")
    private fun cityName(location: Location): Pair<String?, String?> = runCatching {
        if (!Geocoder.isPresent()) return null to null
        val mark = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        val name = listOfNotNull(mark?.locality ?: mark?.subAdminArea, mark?.countryName).joinToString(", ")
        name.ifEmpty { null } to mark?.countryCode
    }.getOrDefault(null to null)

    // Compass

    private var sensorListener: SensorEventListener? = null

    fun startHeading() {
        val sm = context.getSystemService(SensorManager::class.java) ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return
        val declination = latitude?.let { lat ->
            GeomagneticField(lat.toFloat(), (longitude ?: 0.0).toFloat(), 0f, System.currentTimeMillis()).declination
        } ?: 0f
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothed: Double? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val raw = (Math.toDegrees(orientation[0].toDouble()) + declination + 360) % 360
                // Light smoothing on the shortest way round, so the needle does not jitter.
                val prev = smoothed
                smoothed = if (prev == null) raw else {
                    val delta = ((raw - prev + 540) % 360) - 180
                    (prev + delta * 0.15 + 360) % 360
                }
                heading = smoothed
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                compassUnreliable = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        sensorListener = listener
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopHeading() {
        val sm = context.getSystemService(SensorManager::class.java)
        sensorListener?.let { sm?.unregisterListener(it) }
        sensorListener = null
        heading = null
    }

    val hasCompass: Boolean
        get() = context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    // Notifications

    /** Schedules the next chosen prayer. Each alarm schedules the one after it,
     *  and opening the app or restarting the phone refreshes it. */
    fun refreshNotifications() {
        PrayerAlarms.scheduleNext(context, this)
    }

    /** The next prayer alarm to set, skipping prayers she turned off. */
    fun nextAlarm(after: ZonedDateTime = ZonedDateTime.now()): Pair<Prayer, ZonedDateTime>? {
        if (!notifyAtPrayerTimes || isResting || !hasLocation) return null
        for (offset in 0L..2L) {
            val schedule = schedule(after.toLocalDate().plusDays(offset)) ?: return null
            Prayer.prayers.filter { it in enabledPrayers }.forEach { p ->
                val t = schedule.time(p) ?: return@forEach
                if (t.isAfter(after)) return p to t
            }
        }
        return null
    }

    companion object {
        private const val K_LAT = "prayer.latitude"
        private const val K_LON = "prayer.longitude"
        private const val K_PLACE = "prayer.place"
        private const val K_METHOD = "prayer.method"
        private const val K_ASR = "prayer.asr"
        private const val K_NOTIFY = "prayer.notify"
        private const val K_SOUND = "prayer.sound"
        private const val K_ENABLED = "prayer.enabledPrayers"

        private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
    }
}
