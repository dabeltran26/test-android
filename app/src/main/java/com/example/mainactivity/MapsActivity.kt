package com.example.mainactivity

import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.mainactivity.Json.PathJSONParser
import com.example.mainactivity.network.HttpConnection

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.util.ArrayList
import java.util.HashMap

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        drawRout()
    }

    private fun drawRout() {

        var oringin: String
        var destination: String

        oringin = "4.667426"
        oringin += "," + "-74.056624"

        destination = "4.672655"
        destination += "," + "-74.054071"

        var latLng = LatLng(4.667426, -74.056624)
        mMap.addMarker(MarkerOptions().position(latLng).title("Inicio"))

        latLng = LatLng(4.672655, -74.054071)
        mMap.addMarker(MarkerOptions().position(latLng).title("Fin"))

        val downloadTask = ReadTask()
        downloadTask.execute(
            "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + oringin + "&destination=" + destination +
                    "&mode=driving&key=" + resources.getString(R.string.google_maps_key)
        )

    }

    private inner class ReadTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg url: String): String {
            var data = ""
            try {

                val http = HttpConnection()
                data = http.readUrl(url[0])
                Log.d("Background Task data", data)

            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            ParserTask().execute(result)
        }
    }

    private inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        override fun doInBackground(
            vararg jsonData: String
        ): List<List<HashMap<String, String>>>? {

            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null

            try {
                jObject = JSONObject(jsonData[0])
                val parser = PathJSONParser()
                routes = parser.parse(jObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return routes
        }

        override fun onPostExecute(routes: List<List<HashMap<String, String>>>) {

            try {
                var points: ArrayList<LatLng>? = null
                var polyLineOptions: PolylineOptions? = null

                // traversing through routes
                for (i in routes.indices) {
                    points = ArrayList()
                    polyLineOptions = PolylineOptions()
                    val path = routes[i]

                    for (j in path.indices) {
                        val point = path[j]

                        val lat = java.lang.Double.parseDouble(point["lat"]!!)
                        val lng = java.lang.Double.parseDouble(point["lng"]!!)
                        val position = LatLng(lat, lng)

                        points.add(position)
                    }

                    polyLineOptions.addAll(points)
                    polyLineOptions.width(8f)
                    polyLineOptions.color(Color.RED)
                }

                mMap.addPolyline(polyLineOptions)

                val builder = LatLngBounds.Builder()
                for (latLng in points!!) {
                    builder.include(latLng)
                }

                val bounds = builder.build()
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                mMap.animateCamera(cu)
            } catch (e: Exception) {

            }
        }
    }
}
