package train.router.models.db

import kotlinx.serialization.Serializable
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream
import java.io.InputStreamReader

@Serializable
data class RoadGraph(
    val graphId: String,
    val stationName: String,
    val latitude: Double,
    val longitude: Double,
    val directLinks: List<String>
)

fun parseRoadGraph(graphId: String, inputStream: InputStream): List<RoadGraph> {
    val reader = InputStreamReader(inputStream)
    val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withHeader())
    return csvParser.records.map {
        RoadGraph(
            graphId = graphId,
            stationName = it.get("StationName"),
            latitude = it.get("Latitude").toDouble(),
            longitude = it.get("Longitude").toDouble(),
            directLinks = it.get("Direct Links").split(", ")
        )
    }
}