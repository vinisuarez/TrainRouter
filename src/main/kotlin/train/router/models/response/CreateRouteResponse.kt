package train.router.models.response

import kotlinx.serialization.Serializable

@Serializable
data class CreateRouteResponse(val jobId: String)