package train.router.models.db

import kotlinx.serialization.Serializable

@Serializable
data class CalculatedSvg(
    val graphId: String,
    val startStation: String,
    val endStation: String,
    val svg: String
)