package train.router

import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import train.router.models.request.CreateRouteRequest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("heartbeat", bodyAsText())
        }
    }

    @Test
    fun testUploadFiles() = testApplication {
        runBlocking {
            val roadGraphFile = File("src/test/resources/uk_station_coordinates.csv")
            val timetableFile = File("src/test/resources/uk_train_schedule.csv")

            val response: HttpResponse = client.post("/api/v1/graph") {
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", roadGraphFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/csv")
                            append(HttpHeaders.ContentDisposition, "filename=uk_station_coordinates.csv")
                        })
                        append("file", timetableFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/csv")
                            append(HttpHeaders.ContentDisposition, "filename=uk_train_schedule.csv")
                        })
                    }
                ))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertContains(responseBody, "roadGraphId")
            assertContains(responseBody, "timetableId")
            println(responseBody)
        }
    }

    @Test
    fun testFetchRoadGraph() = testApplication {
        runBlocking {
            val roadGraphFile = File("src/test/resources/uk_station_coordinates.csv")
            val response: HttpResponse = client.post("/api/v1/graph") {
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", roadGraphFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/csv")
                            append(HttpHeaders.ContentDisposition, "filename=uk_station_coordinates.csv")
                        })
                    }
                ))
            }
            val roadGraphId = response.bodyAsText()
                .replace("\"", "")
                .replace("\n", "")
                .replace("}", "")
                .split(" : ")[1]
            val responseGet: HttpResponse = client.get("/api/v1/graph/road/${roadGraphId}")
            assertEquals(HttpStatusCode.OK, responseGet.status)
            val responseBodyGet = responseGet.bodyAsText()
            assertContains(responseBodyGet, "\"stationName\" : \"Birmingham New Street\"")
            assertContains(responseBodyGet, "\"stationName\" : \"Bristol Temple Meads\"")
            assertContains(
                responseBodyGet,
                "\"directLinks\" : [ \"Cardiff Central\", \"London King's Cross\", \"Southampton Central\" ]"
            )
        }
    }

    @Test
    fun testFetchTimetable() = testApplication {
        runBlocking {
            val timetableFile = File("src/test/resources/uk_train_schedule.csv")
            val response: HttpResponse = client.post("/api/v1/graph") {
                contentType(ContentType.MultiPart.FormData)
                setBody(MultiPartFormDataContent(
                    formData {
                        append("file", timetableFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/csv")
                            append(HttpHeaders.ContentDisposition, "filename=uk_train_schedule.csv")
                        })
                    }
                ))
            }
            val timetableId = response.bodyAsText()
                .replace("\"", "")
                .replace("\n", "")
                .replace("}", "")
                .split(" : ")[1]
            val responseGet: HttpResponse = client.get("/api/v1/graph/timetable/$timetableId")
            assertEquals(HttpStatusCode.OK, responseGet.status)
            val responseBodyGet = responseGet.bodyAsText()
            assertContains(responseBodyGet, "\"trainNumber\" : \"301\"")
            assertContains(responseBodyGet, "\"fromStation\" : \"Birmingham New Street\"")
            assertContains(responseBodyGet, "\"toStation\" : \"Norwich\"")
            assertContains(responseBodyGet, "\"departureTime\" : \"06:00\"")
            assertContains(responseBodyGet, "\"arrivalTime\" : \"07:51\"")
        }
    }


    @Test
    fun testCreateRouteSvg() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val request = CreateRouteRequest("123", "321", "A", "B")
        val routeResponse: HttpResponse = client.post("/api/v1/route") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Image.SVG)
            setBody(request)
        }
        assertEquals(HttpStatusCode.OK, routeResponse.status)
        val routeResponseBody = routeResponse.bodyAsText()
        println(routeResponseBody)
        assertContains(routeResponseBody, "jobId")
    }

    @Test
    fun testCreateRouteJson() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        runBlocking {
            val request = CreateRouteRequest("123", "321", "A", "B")
            val routeResponse: HttpResponse = client.post("/api/v1/route") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }
            assertEquals(HttpStatusCode.OK, routeResponse.status)
            val routeResponseBody = routeResponse.bodyAsText()
            assertContains(routeResponseBody, "jobId")
        }
    }
}