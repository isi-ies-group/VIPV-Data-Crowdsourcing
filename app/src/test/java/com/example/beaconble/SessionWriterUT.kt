package com.example.beaconble

import com.example.beaconble.io_files.SessionWriter
import org.altbeacon.beacon.Identifier
import org.junit.Test
import java.time.Instant

class SessionWriterUT {
    @Test
    fun createJSONHeader() {
        val beaconsCollectionRef = LoggingSession

        val beacons = beaconsCollectionRef.beacons.value!!

        // add two example beacons
        val beacon0 = BeaconSimplified(Identifier.parse("0x010203040506"))
        beacon0.tilt = 0.0f
        beacon0.direction = 0.0f
        beacon0.description = "Soy la cosita más linda y mona de este mundo."
        beacons.add(beacon0)

        val beacon1 = BeaconSimplified(Identifier.parse("0x010203040507"))
        beacon1.tilt = 10.0f
        beacon1.direction = 180.0f
        beacon1.description = "Soy la cosita más linda y mona de este mundo."
        beacons.add(beacon1)

        // set start and finish instants
        beaconsCollectionRef.startInstant = Instant.parse("2021-10-01T12:00:00Z")
        beaconsCollectionRef.stopInstant = Instant.parse("2021-10-01T12:30:00Z")

        val jsonHeader = SessionWriter.createJSONHeader(beacons)

        val expected = """
        {
          "start_instant": "2021-10-01T12:00:00Z",
          "end_instant": "2021-10-01T12:30:00Z",
          "beacons": [{
            "0": {
              "id": "0x010203040506",
              "tilt": 0.0,
              "orientation": 0.0,
              "description": "U295IGxhIGNvc2l0YSBtw6FzIGxpbmRhIHkgbW9uYSBkZSBlc3RlIG11bmRvLg=="
            },
            "1": {
              "id": "0x010203040507",
              "tilt": 10.0,
              "orientation": 180.0,
              "description": "U295IGxhIGNvc2l0YSBtw6FzIGxpbmRhIHkgbW9uYSBkZSBlc3RlIG11bmRvLg=="
            }
           }]
        }
        """.replace("\n", "").replace(" ", "")

        val jsonHeaderFormatted = jsonHeader.replace(" ", "")

        assert(jsonHeaderFormatted == expected)
    }
}
