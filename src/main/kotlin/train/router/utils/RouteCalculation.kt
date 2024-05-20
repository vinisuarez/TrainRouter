package train.router.utils

import train.router.models.db.CalculatedRoute
import train.router.models.db.RoadGraph
import train.router.models.db.Timetable
import java.lang.Math.pow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

data class RoadGraphRoute(val roadGraph: RoadGraph, val distance: Double)
data class TimetableGraph(
    val stationName: String,
    val arrivalTime: LocalTime
) : Comparable<TimetableGraph> {
    override fun compareTo(other: TimetableGraph): Int {
        return arrivalTime.compareTo(other.arrivalTime)
    }
}

fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return sqrt((lat2 - lat1).pow(2.0) + (lon2 - lon1).pow(2.0))
}

fun findShortestPath(roadGraph: List<RoadGraph>, startStation: String, endStation: String): List<RoadGraphRoute> {
    val stationsByName = roadGraph.associateBy { it.stationName.trim() }

    val distanceByStation = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
    val previous = mutableMapOf<String, String?>()

    val queue = PriorityQueue(compareBy<Pair<String, Double>> { it.second })

    distanceByStation[startStation] = 0.0
    queue.add(Pair(startStation, 0.0))

    while (queue.isNotEmpty()) {
        val (station, currentDistance) = queue.poll()
        if (station == endStation) break
        val currentStation = stationsByName[station] ?: continue

        for (linkedStationName in currentStation.directLinks) {
            val linkedStation = stationsByName[linkedStationName] ?: continue
            val distance = currentDistance + distance(
                currentStation.latitude,
                currentStation.longitude,
                linkedStation.latitude,
                linkedStation.longitude
            )

            if (distance < distanceByStation.getValue(linkedStationName)) {
                distanceByStation[linkedStationName] = distance
                previous[linkedStationName] = station
                queue.add(Pair(linkedStationName, distance))
            }
        }
    }
    return if (!previous.containsKey(endStation)) {
        listOf()
    } else {
        generatePath(previous, distanceByStation, endStation, stationsByName)
    }
}

fun generatePath(
    previous: Map<String, String?>,
    distanceByStation: Map<String, Double>,
    endStation: String,
    stationsByName: Map<String, RoadGraph>
): List<RoadGraphRoute> {
    val path = mutableListOf<RoadGraphRoute>()
    var current: String? = endStation

    while (current != null) {
        val station = stationsByName[current] ?: break
        val distance = distanceByStation.getValue(current)
        path.add(RoadGraphRoute(station, distance))
        current = previous[current]
    }

    return path.reversed()
}

fun findFastestPath(timetable: List<Timetable>, startStation: String, endStation: String): CalculatedRoute {
    val graph = mutableMapOf<String, MutableList<Timetable>>()
    for (entry in timetable) {
        graph.computeIfAbsent(entry.fromStation) { mutableListOf() }.add(entry)
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val queue = PriorityQueue<TimetableGraph>()
    queue.add(TimetableGraph(startStation, LocalTime.MIN))

    val shortestTimes = mutableMapOf<String, LocalTime>().withDefault { LocalTime.MAX }
    val previousStations = mutableMapOf<String, Pair<String, String>>()

    shortestTimes[startStation] = LocalTime.MIN

    while (queue.isNotEmpty()) {
        val current = queue.poll()
        val currentTime = shortestTimes.getValue(current.stationName)

        if (current.stationName == endStation) {
            break
        }

        graph[current.stationName]?.forEach { trip ->
            val departureTime = LocalTime.parse(trip.departureTime, timeFormatter)
            val arrivalTime = LocalTime.parse(trip.arrivalTime, timeFormatter)

            if (departureTime >= currentTime) {
                if (arrivalTime < shortestTimes.getValue(trip.toStation)) {
                    shortestTimes[trip.toStation] = arrivalTime
                    previousStations[trip.toStation] = current.stationName to trip.trainNumber
                    queue.add(TimetableGraph(trip.toStation, arrivalTime))
                }
            }
        }
    }

    val route = mutableListOf<Timetable>()
    var currentStation = endStation

    while (currentStation != startStation) {
        val (previousStation, trainNumber) = previousStations[currentStation] ?: break
        val timetableEntry = timetable.find {
            it.trainNumber == trainNumber &&
            it.fromStation == previousStation &&
            it.toStation == currentStation }?: break

        route.add(0, timetableEntry)
        currentStation = previousStation
    }

    return CalculatedRoute(timetable.first().graphId, startStation, endStation, route)
}