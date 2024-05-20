package train.router.utils

import train.router.models.db.parseRoadGraph
import train.router.models.db.parseTimetable
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteCalculationTest {
    @Test
    fun shouldNotFindPath() {
        val roadGraphFile = File("src/test/resources/uk_station_coordinates.csv")
        val roadGraphs = parseRoadGraph("test", roadGraphFile.inputStream())

        val path = findShortestPath(roadGraphs, "London King's Cross", "Norwich")
        assertTrue(path.isEmpty())
    }

    @Test
    fun shouldFindShortestRoute() {
        val roadGraphFile = File("src/test/resources/uk_station_coordinates.csv")
        val roadGraphs = parseRoadGraph("test", roadGraphFile.inputStream())

        val path = findShortestPath(roadGraphs, "London King's Cross", "Swansea")

        assertEquals("London King's Cross", path[0].roadGraph.stationName)
        assertEquals("Bristol Temple Meads", path[1].roadGraph.stationName)
        assertEquals("Cardiff Central", path[2].roadGraph.stationName)
        assertEquals("Swansea", path[3].roadGraph.stationName)

        assertEquals(0.0, path[0].distance)
        assertEquals(2.46, path[1].distance, 0.01)
        assertEquals(3.05, path[2].distance, 0.01)
        assertEquals(3.83, path[3].distance, 0.01)

    }

    @Test
    fun shouldFindFastestRoute() {
        val timetableFile = File("src/test/resources/uk_train_schedule.csv")
        val timetable = parseTimetable("test", timetableFile.inputStream())

        val calculate = findFastestPath(timetable, "London King's Cross", "Swansea")

        val firstInRoute = calculate.route.first()
        assertEquals("321", firstInRoute.trainNumber)
        assertEquals("London King's Cross", firstInRoute.fromStation)
        assertEquals("Swansea", firstInRoute.toStation)
        assertEquals("11:09", firstInRoute.departureTime)
        assertEquals("12:56", firstInRoute.arrivalTime)
    }

    @Test
    fun shouldFindFastestRouteWithMultipleTrains() {
        val timetableFile = File("src/test/resources/uk_train_schedule.csv")
        val timetable = parseTimetable("test", timetableFile.inputStream())

        val calculate = findFastestPath(timetable, "London King's Cross", "Portsmouth Harbour")

        assertEquals("313, 309, 309, 309", calculate.route.joinToString { it.trainNumber })
        assertEquals(
            "London King's Cross, Norwich, Manchester Piccadilly, Nottingham",
            calculate.route.joinToString { it.fromStation })
        assertEquals(
            "Norwich, Manchester Piccadilly, Nottingham, Portsmouth Harbour",
            calculate.route.joinToString { it.toStation })
        assertEquals("06:00, 07:51, 09:29, 11:00", calculate.route.joinToString { it.departureTime })
        assertEquals("07:48, 09:06, 10:48, 12:25", calculate.route.joinToString { it.arrivalTime })
    }
}