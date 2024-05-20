package train.router.plugins

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import train.router.models.db.*
import train.router.utils.STATUS_DONE
import java.sql.Connection
import java.sql.ResultSet

class RouteService(private val connection: Connection) {
    companion object {
        private const val CREATE_TABLE_ROAD_GRAPH = "CREATE TABLE IF NOT EXISTS RoadGraph (" +
                "    graphId VARCHAR(255) NOT NULL," +
                "    stationName VARCHAR(255) NOT NULL," +
                "    latitude DOUBLE PRECISION NOT NULL," +
                "    longitude DOUBLE PRECISION NOT NULL," +
                "    directLinks TEXT NOT NULL," +
                "    PRIMARY KEY (graphId, stationName)" +
                ");"
        private const val CREATE_TABLE_TIMETABLE = "CREATE TABLE IF NOT EXISTS Timetable (" +
                "    graphId VARCHAR(255) NOT NULL," +
                "    trainNumber VARCHAR(255) NOT NULL," +
                "    fromStation VARCHAR(255) NOT NULL," +
                "    toStation VARCHAR(255) NOT NULL," +
                "    departureTime VARCHAR(255) NOT NULL," +
                "    arrivalTime VARCHAR(255) NOT NULL," +
                "    PRIMARY KEY (graphId, trainNumber, fromStation, toStation)" +
                ");"

        private const val CREATE_TABLE_JOB = "CREATE TABLE IF NOT EXISTS Job (" +
                "    id VARCHAR(255) NOT NULL," +
                "    type VARCHAR(255) NOT NULL," +
                "    status VARCHAR(255) NOT NULL," +
                "    graphId VARCHAR(255) NOT NULL," +
                "    startStation VARCHAR(255) NOT NULL," +
                "    endStation VARCHAR(255) NOT NULL," +
                "    PRIMARY KEY (id)" +
                ");"

        private const val CREATE_TABLE_CALCULATED_ROUTE = "CREATE TABLE IF NOT EXISTS CalculatedRoute (" +
                "    graphId VARCHAR(255) NOT NULL," +
                "    startStation VARCHAR(255) NOT NULL," +
                "    endStation VARCHAR(255) NOT NULL," +
                "    route TEXT NOT NULL," +
                "    PRIMARY KEY (graphId, startStation, endStation)" +
                ");"

        private const val CREATE_TABLE_CALCULATED_SVG = "CREATE TABLE IF NOT EXISTS CalculatedSvg (" +
                "    graphId VARCHAR(255) NOT NULL," +
                "    startStation VARCHAR(255) NOT NULL," +
                "    endStation VARCHAR(255) NOT NULL," +
                "    svg TEXT NOT NULL," +
                "    PRIMARY KEY (graphId, startStation, endStation)" +
                ");"

        private const val INSERT_ROAD_GRAPH =
            "INSERT INTO RoadGraph (graphId, stationName, latitude, longitude, directLinks) VALUES (?, ?, ?, ?, ?);"
        private const val FIND_ROAD_GRAPH = "SELECT * FROM RoadGraph WHERE graphId = ?"

        private const val INSERT_TIMETABLE =
            "INSERT INTO Timetable (graphId, trainNumber, fromStation, toStation, departureTime, arrivalTime) VALUES (?, ?, ?, ?, ?, ?);"
        private const val FIND_TIMETABLE = "SELECT * FROM Timetable WHERE graphId = ?"

        private const val INSERT_JOB =
            "INSERT INTO Job (id, type, status, graphId, startStation, endStation) VALUES (?, ?, ?, ?, ?, ?);"
        private const val FIND_JOB = "SELECT * FROM Job WHERE id = ?"
        private const val UPDATE_JOB = "UPDATE Job SET status = ? WHERE id = ?"

        private const val INSERT_CALCULATED_ROUTE =
            "INSERT INTO CalculatedRoute (graphId, startStation, endStation, route) VALUES (?, ?, ?, ?);"
        private const val FIND_CALCULATED_ROUTE =
            "SELECT * FROM CalculatedRoute WHERE graphId = ? AND startStation = ? AND endStation = ?"

        private const val INSERT_CALCULATED_SVG =
            "INSERT INTO CalculatedSvg (graphId, startStation, endStation, svg) VALUES (?, ?, ?, ?);"
        private const val FIND_CALCULATED_SVG =
            "SELECT * FROM CalculatedSvg WHERE graphId = ? AND startStation = ? AND endStation = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_ROAD_GRAPH)
        statement.executeUpdate(CREATE_TABLE_TIMETABLE)
        statement.executeUpdate(CREATE_TABLE_JOB)
        statement.executeUpdate(CREATE_TABLE_CALCULATED_ROUTE)
        statement.executeUpdate(CREATE_TABLE_CALCULATED_SVG)
    }

    suspend fun create(roadGraph: RoadGraph) = withContext(Dispatchers.IO) {
        connection.prepareStatement(INSERT_ROAD_GRAPH).use { preparedStatement ->
            preparedStatement.setString(1, roadGraph.graphId)
            preparedStatement.setString(2, roadGraph.stationName)
            preparedStatement.setDouble(3, roadGraph.latitude)
            preparedStatement.setDouble(4, roadGraph.longitude)
            preparedStatement.setString(5, roadGraph.directLinks.joinToString(","))
            preparedStatement.executeUpdate()
        }
    }

    suspend fun create(timetable: Timetable) = withContext(Dispatchers.IO) {
        connection.prepareStatement(INSERT_TIMETABLE).use { preparedStatement ->
            preparedStatement.setString(1, timetable.graphId)
            preparedStatement.setString(2, timetable.trainNumber)
            preparedStatement.setString(3, timetable.fromStation)
            preparedStatement.setString(4, timetable.toStation)
            preparedStatement.setString(5, timetable.departureTime)
            preparedStatement.setString(6, timetable.arrivalTime)
            preparedStatement.executeUpdate()
        }
    }

    suspend fun create(job: Job) = withContext(Dispatchers.IO) {
        connection.prepareStatement(INSERT_JOB).use { preparedStatement ->
            preparedStatement.setString(1, job.id)
            preparedStatement.setString(2, job.type)
            preparedStatement.setString(3, job.status)
            preparedStatement.setString(4, job.graphId)
            preparedStatement.setString(5, job.startStation)
            preparedStatement.setString(6, job.endStation)
            preparedStatement.executeUpdate()
        }
    }

    suspend fun create(calculatedRoute: CalculatedRoute) = withContext(Dispatchers.IO) {
        connection.prepareStatement(INSERT_CALCULATED_ROUTE).use { preparedStatement ->
            preparedStatement.setString(1, calculatedRoute.graphId)
            preparedStatement.setString(2, calculatedRoute.startStation)
            preparedStatement.setString(3, calculatedRoute.endStation)
            preparedStatement.setString(4, jsonMapper().writeValueAsString(calculatedRoute))
            preparedStatement.executeUpdate()
        }
    }

    suspend fun create(calculatedSvg: CalculatedSvg) = withContext(Dispatchers.IO) {
        connection.prepareStatement(INSERT_CALCULATED_SVG).use { preparedStatement ->
            preparedStatement.setString(1, calculatedSvg.graphId)
            preparedStatement.setString(2, calculatedSvg.startStation)
            preparedStatement.setString(3, calculatedSvg.endStation)
            preparedStatement.setString(4, calculatedSvg.svg)
            preparedStatement.executeUpdate()
        }
    }

    suspend fun updateJobToDone(id: String) = withContext(Dispatchers.IO) {
        connection.prepareStatement(UPDATE_JOB).use { preparedStatement ->
            preparedStatement.setString(1, STATUS_DONE)
            preparedStatement.setString(2, id)
            preparedStatement.executeUpdate()
        }
    }

    suspend fun fetchRoadGraph(graphId: String): List<RoadGraph> {
        return withContext(Dispatchers.IO) {
            val roadGraphs = mutableListOf<RoadGraph>()
            connection.prepareStatement(FIND_ROAD_GRAPH).use { preparedStatement ->
                preparedStatement.setString(1, graphId)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val stationName = resultSet.getString("stationName")
                    val latitude = resultSet.getDouble("latitude")
                    val longitude = resultSet.getDouble("longitude")
                    val directLinks = resultSet.getString("directLinks").split(",")
                    roadGraphs.add(RoadGraph(graphId, stationName, latitude, longitude, directLinks))
                }
            }
            roadGraphs
        }
    }

    suspend fun fetchTimetable(graphId: String): List<Timetable> {
        return withContext(Dispatchers.IO) {
            val timetables = mutableListOf<Timetable>()
            connection.prepareStatement(FIND_TIMETABLE).use { preparedStatement ->
                preparedStatement.setString(1, graphId)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val trainNumber = resultSet.getString("trainNumber")
                    val fromStation = resultSet.getString("fromStation")
                    val toStation = resultSet.getString("toStation")
                    val departureTime = resultSet.getString("departureTime")
                    val arrivalTime = resultSet.getString("arrivalTime")
                    timetables.add(Timetable(graphId, trainNumber, fromStation, toStation, departureTime, arrivalTime))
                }
            }
            timetables
        }
    }

    suspend fun fetchJob(id: String): Job? {
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(FIND_JOB).use { preparedStatement ->
                preparedStatement.setString(1, id)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val type = resultSet.getString("type")
                    val status = resultSet.getString("status")
                    val graphId = resultSet.getString("graphId")
                    val startStation = resultSet.getString("startStation")
                    val endStation = resultSet.getString("endStation")
                    Job(id, type, status, graphId, startStation, endStation)
                } else {
                    null
                }
            }
        }
    }

    suspend fun fetchCalculatedRoute(graphId: String, startStation: String, endStation: String): CalculatedRoute? {
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(FIND_CALCULATED_ROUTE).use { preparedStatement ->
                preparedStatement.setString(1, graphId)
                preparedStatement.setString(2, startStation)
                preparedStatement.setString(3, endStation)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val routeJson = resultSet.getString("route")
                    val route = jsonMapper().registerKotlinModule().readValue(routeJson, CalculatedRoute::class.java)
                    CalculatedRoute(
                        graphId = resultSet.getString("graphId"),
                        startStation = resultSet.getString("startStation"),
                        endStation = resultSet.getString("endStation"),
                        route = route.route
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun fetchCalculatedSvg(graphId: String, startStation: String, endStation: String): CalculatedSvg? {
        return withContext(Dispatchers.IO) {
            connection.prepareStatement(FIND_CALCULATED_SVG).use { preparedStatement ->
                preparedStatement.setString(1, graphId)
                preparedStatement.setString(2, startStation)
                preparedStatement.setString(3, endStation)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    CalculatedSvg(
                        graphId = resultSet.getString("graphId"),
                        startStation = resultSet.getString("startStation"),
                        endStation = resultSet.getString("endStation"),
                        svg = resultSet.getString("svg"),
                    )
                } else {
                    null
                }
            }
        }
    }
}