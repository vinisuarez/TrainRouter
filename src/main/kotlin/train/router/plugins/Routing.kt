package train.router.plugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import train.router.models.db.Job
import train.router.models.db.parseRoadGraph
import train.router.models.db.parseTimetable
import train.router.models.request.CreateRouteRequest
import train.router.models.response.CreateRouteResponse
import train.router.utils.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

fun Application.configureRouting(embeddedDb: Boolean = true) {
    val dbConnection: Connection = connectToPostgres(embeddedDb)
    val routeService = RouteService(dbConnection)
    val mapper = jacksonObjectMapper()

    routing {
        get("/") {
            call.respondText("heartbeat", status = HttpStatusCode.OK)
        }

        post("/api/v1/graph") {
            val multipart = call.receiveMultipart()
            val response = StringBuilder()
            response.appendLine("{")

            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val fileName = part.originalFileName ?: ""
                    when {
                        fileName.contains("coordinates", ignoreCase = true) -> {
                            val roadGraphId = UUID.randomUUID().toString()
                            val roadGraphs = parseRoadGraph(roadGraphId, part.streamProvider())
                            for (roadGraph in roadGraphs) {
                                routeService.create(roadGraph)
                            }
                            response.appendLine("\"roadGraphId\" : \"$roadGraphId\"")
                        }

                        fileName.contains("schedule", ignoreCase = true) -> {
                            val timetableId = UUID.randomUUID().toString()
                            val timetables = parseTimetable(timetableId, part.streamProvider())
                            for (timetable in timetables) {
                                routeService.create(timetable)
                            }
                            response.appendLine("\"timetableId\" : \"$timetableId\"")
                        }
                    }
                }
                part.dispose()
            }
            response.append("}")
            call.respondText(response.toString())
        }

        // DEBUG API TO CHECK THE PERSISTED DATA
        get("/api/v1/graph/road/{id}") {
            val graphId =
                call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or malformed ID")
            val roadGraphData = routeService.fetchRoadGraph(graphId)
            call.respond(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(roadGraphData))
        }

        get("/api/v1/graph/timetable/{id}") {
            val graphId =
                call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or malformed ID")
            val timetableData = routeService.fetchTimetable(graphId)
            call.respond(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(timetableData))
        }

        val customScope = CoroutineScope(Dispatchers.Default)

        post("/api/v1/route") {
            val createRouteRequest = call.receive<CreateRouteRequest>()
            if (createRouteRequest.startStation.isEmpty() || createRouteRequest.endStation.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, "start and end stations can't be empty")
            } else {
                when (call.request.acceptItems().firstOrNull()?.value) {
                    "image/svg+xml" -> {
                        if (createRouteRequest.roadGraphId.isEmpty()) {
                            call.respond("Can't generate SVG without road graph Id")
                        } else {
                            val jobId = UUID.randomUUID().toString()
                            routeService.create(
                                Job(
                                    jobId,
                                    TYPE_SVG,
                                    STATUS_PROCESSING,
                                    createRouteRequest.roadGraphId,
                                    createRouteRequest.startStation,
                                    createRouteRequest.endStation
                                )
                            )
                            val routeGraph = routeService.fetchRoadGraph(createRouteRequest.roadGraphId)

                            customScope.launch {
                                startRoadGraphCalculation(
                                    jobId,
                                    createRouteRequest.roadGraphId,
                                    routeGraph,
                                    createRouteRequest.startStation,
                                    createRouteRequest.endStation,
                                    routeService
                                )
                            }
                            call.respond(
                                (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(CreateRouteResponse(jobId)))
                            )
                        }
                    }

                    else -> {
                        if (createRouteRequest.timetableId.isEmpty()) {
                            call.respond("Can't generate route json with timetable Id")
                        } else {
                            val jobId = UUID.randomUUID().toString()
                            routeService.create(
                                Job(
                                    jobId,
                                    TYPE_ROUTE,
                                    STATUS_PROCESSING,
                                    createRouteRequest.timetableId,
                                    createRouteRequest.startStation,
                                    createRouteRequest.endStation
                                )
                            )
                            val timetable = routeService.fetchTimetable(createRouteRequest.timetableId)
                            customScope.launch {
                                startTimetableCalculation(
                                    jobId,
                                    createRouteRequest.timetableId,
                                    timetable,
                                    createRouteRequest.startStation,
                                    createRouteRequest.endStation,
                                    routeService
                                )
                            }
                            call.respond(
                                (mapper.writerWithDefaultPrettyPrinter().writeValueAsString(CreateRouteResponse(jobId)))
                            )
                        }
                    }
                }
            }
        }

        get("/api/v1/route/{id}") {
            val jobId =
                call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or malformed ID")
            val job = routeService.fetchJob(jobId)

            if (job == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                if (job.status == STATUS_PROCESSING) {
                    call.respond("Job is still being processed")
                } else {
                    if (job.type == TYPE_SVG) {
                        val calculatedSvg =
                            routeService.fetchCalculatedSvg(job.graphId, job.startStation, job.endStation)
                        if (calculatedSvg == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            val file = withContext(Dispatchers.IO) {
                                File.createTempFile("svg", ".xml")
                            }
                            file.writeText(calculatedSvg.svg)
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    "svg.xml"
                                ).toString()
                            )
                            call.respondFile(file)
                        }
                    } else {
                        val calculatedRoute =
                            routeService.fetchCalculatedRoute(job.graphId, job.startStation, job.endStation)
                        if (calculatedRoute == null) {
                            call.respond(HttpStatusCode.InternalServerError)
                        } else {
                            call.respond(
                                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(calculatedRoute.route)
                            )
                        }
                    }
                }
            }
        }
    }
}


fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    } else {
        val url = environment.config.property("postgres.url").getString()
        val user = environment.config.property("postgres.user").getString()
        val password = environment.config.property("postgres.password").getString()

        DriverManager.getConnection(url, user, password)
    }
}