package com.example.beaconble

import android.app.*
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import okhttp3.ResponseBody
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.MonitorNotifier
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BeaconReferenceApplication : Application() {
    // API service
    private lateinit var ApiService: APIService

    // Bluetooth scanning
    var region = Region("all-beacons", null, null, null)  // representa el criterio que se usa para busacar las balizas, como no se quiere buscar una UUID especifica los 3 útlimos campos son null

    // LiveData observers for monitoring and ranging
    lateinit var regionState: MutableLiveData<Int>
    lateinit var rangedBeacons: MutableLiveData<Collection<Beacon>>

    override fun onCreate() {
        super.onCreate()

        val beaconManager = BeaconManager.getInstanceForApplication(this)
        // Beacon Manager configura la interaccion con las beacons y el start/stop de ranging/monitoring

        // Por defecto la biblioteca solo detecta AltBeacon si se quiere otro tipo de protocolo hay que añadir el layout
        // m:0-1=0505 stands for InPlay's Company Identifier Code (0x0505), see https://www.bluetooth.com/specifications/assigned-numbers/
        // i:2-7 stands for the identifier, UUID (MAC) [little endian]
        // d:8-9 stands for the data, CH1 analog value [little endian]
        val customParser = BeaconParser().setBeaconLayout("m:0-1=0505,i:2-7,d:8-9")
        beaconManager.beaconParsers.add(customParser)

        // Activate debug mode only if build variant is debug
        BeaconManager.setDebug(BuildConfig.DEBUG)

        //configurar escaneo
        setupBeaconScanning()

        // Set API service
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val apiEndpoint = sharedPreferences.getString("api_uri", BuildConfig.SERVER_URL)!!
        setService(apiEndpoint)

        // Save instance for singleton access
        instance = this
    }


    fun setupBeaconScanning() {
        val beaconManager = BeaconManager.getInstanceForApplication(this)

        // Empieza a escanear en la region definida
        beaconManager.startMonitoring(region)
        beaconManager.startRangingBeacons(region)

        // Establecen dos observadores Live Data para cambios en el estado de la región y la lista de beacons detectado
        val regionViewModel =
            BeaconManager.getInstanceForApplication(this).getRegionViewModel(region)
        // Se llamara al observador cuando la region cambie de estado dentro/fuera
        regionViewModel.regionState.observeForever(centralMonitoringObserver)
        // Se llamara al observador cuando se actualice la lista de de beacons (normalmente se actualiza cada 1 seg)
        regionViewModel.rangedBeacons.observeForever(centralRangingObserver)

        // Save observers for public use in ViewModel(s)
        this.regionState = regionViewModel.regionState
        this.rangedBeacons = regionViewModel.rangedBeacons
    }

    //registra los cambios de si estas dentro o fuera de la region con la interfaz MonitorNotifier de la biblioteca
    // si estas dentro te envia una notificacion
    val centralMonitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.OUTSIDE) {
            Log.d(TAG, "Outside beacon region: $region")
        } else {
            Log.d(TAG, "Inside beacon region: $region")
            sendNotification()
        }
    }

    //recibe actualizaciones de la lista de beacon detectados y su info
    // hace un calculo del tiempo en millis que ha pasado desde la ultima actualizacion
    val centralRangingObserver = Observer<Collection<Beacon>> { beacons ->
        val rangeAgeMillis =
            System.currentTimeMillis() - (beacons.firstOrNull()?.lastCycleDetectionTimestamp ?: 0)
        if (rangeAgeMillis < 10000) {
            Log.d(MainActivity.TAG, "Ranged: ${beacons.count()} beacons")
            for (beacon: Beacon in beacons) {
                Log.d(TAG, "$beacon about ${beacon.distance} meters away")
                val uuid = beacon.serviceUuid
                Log.i(TAG, "UUID: $uuid")
                val id = beacon.id1
                Log.i(TAG, "ID: $id")
                val data = beacon.dataFields
                // analogReading is the CH1 analog value, as two bytes in little endian
                val analogReading = data[0]
                val hexString = analogReading.toString(16)
                Log.i(TAG, "Data: $analogReading (0x$hexString)")
            }
        } else {
            Log.d(MainActivity.TAG, "Ignoring stale ranged beacons from $rangeAgeMillis millis ago")
        }
    }

    //envia notificacion cuando se detecta un beacon en la region
    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, "beacon-nearby-notifications-id")
            .setContentTitle("Beacon Reference Application")
            .setContentText("A beacon is nearby.")
            .setSmallIcon(R.mipmap.logo_ies_foreground)
        // TODO: UPDATE i18n strings, double check icon (make one that is a silhouette of a beacon)

        // Explicit intent to open the app when notification is clicked
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntent(Intent(this, MainActivity::class.java))
        val resultPendingIntent = stackBuilder.getPendingIntent(
            0,
            PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(resultPendingIntent)
        val channel = NotificationChannel(
            "beacon-nearby-notifications-id",
            "Beacons Nearby",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Notifies when a beacon is nearby"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId(channel.id)
        notificationManager.notify(1, builder.build())
    }

    /**
     * Set Retrofit instance for API calls
     * @param baseURL Base URL for API service
     * @return void
     */
    private fun setService(baseURL: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        this.ApiService = retrofit.create(APIService::class.java)
    }

    /**
     * Update the API service endpoint (callback for configuration changes)
     * @param endpoint New API endpoint
     * @return void
     */
    fun updateService(endpoint: String) {
        setService(endpoint)
    }

    /**
     * Iterate over SensorData objects and send them to the server asynchronously
     * @param data List of SensorData objects to send
     * @return Boolean indicating success or failure
     */
    fun sendSensorData(data: List<SensorData>): Boolean {
        for (sensorData in data) {
            ApiService.sendSensorData(
                PreferenceManager.getDefaultSharedPreferences(this).getString("user_token", "")!!,
                sensorData,
            ).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseBody>,
                    response: retrofit2.Response<ResponseBody>
                ) {
                    Log.d(TAG, "Data sent to server")
                }

                override fun onFailure(call: retrofit2.Call<ResponseBody>, t: Throwable) {
                    Log.e(TAG, "Failed to send data to server")
                }
            })
        }
        return true
    }

    companion object {
        lateinit var instance: BeaconReferenceApplication
            private set  // This is a singleton, setter is private but access is public
        const val TAG = "BeaconReferenceApplication"
    }  // companion object
}
