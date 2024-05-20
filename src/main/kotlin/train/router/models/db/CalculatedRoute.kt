package train.router.models.db

import kotlinx.serialization.Serializable

@Serializable
data class CalculatedRoute(
    val graphId: String,
    val startStation: String,
    val endStation: String,
    val route: List<Timetable>
)