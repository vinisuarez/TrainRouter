package train.router.models.db

import kotlinx.serialization.Serializable
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream
import java.io.InputStreamReader

@Serializable
data class Timetable(
    val graphId: String,
    val trainNumber: String,
    val fromStation: String,
    val toStation: String,
    val departureTime: String,
    val arrivalTime: String
)

fun parseTimetable(graphId: String, inputStream: InputStream): List<Timetable> {
    val reader = InputStreamReader(inputStream)
    val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withHeader())
    return csvParser.records.map {
        Timetable(
            graphId = graphId,
            trainNumber = it.get("TrainNumber"),
            fromStation = it.get("FromStation"),
            toStation = it.get("ToStation"),
            departureTime = it.get("DepartureTime"),
            arrivalTime = it.get("ArrivalTime")
        )
    }
}