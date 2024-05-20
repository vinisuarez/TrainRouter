package train.router.models.db

import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String,
    val type: String,
    val status: String,
    val graphId: String,
    val startStation: String,
    val endStation: String
)