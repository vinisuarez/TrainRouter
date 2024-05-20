package train.router.models.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateRouteRequest(
    val roadGraphId: String,
    val timetableId: String,
    val startStation: String,
    val endStation: String
)