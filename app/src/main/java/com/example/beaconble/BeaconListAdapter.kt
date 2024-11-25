package com.example.beaconble

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.ArrayAdapter

/**
 * Adapter for the list of beacons.
 * @param context The context of the activity.
 * @param layout The layout of the list item.
 * @param beaconsList The list of beacons to be displayed.
 */
class BeaconListAdapter(val activityContext: Context, val beaconsList: List<BeaconSimplified>) : ArrayAdapter<BeaconSimplified>(activityContext, R.layout.row_item_beacon, beaconsList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val beacon = getItem(position)
        if (beacon == null) {
            Log.e("BeaconListAdapter", "Beacon is null")
        }
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.row_item_beacon, parent, false)

        val beaconIdTextView = view.findViewById<TextView>(R.id.tvBeaconIdentifier)
        val beaconLastReadingTextView = view.findViewById<TextView>(R.id.tvBeaconLastReading)
        val beaconLastSeenTextView = view.findViewById<TextView>(R.id.tvBeaconLastSeen)

        beaconIdTextView.text = beacon?.id.toString()
        beaconLastReadingTextView.text = beacon?.sensorData?.lastOrNull()?.data.toString()
        beaconLastSeenTextView.text = beacon?.sensorData?.lastOrNull()?.timestamp.toString()

        return view
    }
}
