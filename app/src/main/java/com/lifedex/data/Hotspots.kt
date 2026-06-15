package com.lifedex.data

data class Hotspot(val name: String, val lat: Double, val lng: Double)

object Hotspots {
    val geographicHotspots = listOf(
        Hotspot("Area 51, Nevada", 37.2431, -115.7930),
        Hotspot("Bermuda Triangle", 25.0000, -71.0000),
        Hotspot("Mt. Everest Summit", 27.9881, 86.9250),
        Hotspot("Tokyo Skytree", 35.7101, 139.8107),
        Hotspot("Seoul Plaza", 37.5665, 126.9780),
        Hotspot("Amazon Rainforest", -3.4653, -62.2159)
    )
}
