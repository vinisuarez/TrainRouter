package train.router.utils

import kotlinx.coroutines.coroutineScope
import train.router.models.db.CalculatedSvg
import train.router.models.db.RoadGraph
import train.router.models.db.Timetable
import train.router.plugins.RouteService

const val STATUS_PROCESSING = "Processing"
const val STATUS_DONE = "Done"
const val TYPE_SVG = "Svg"
const val TYPE_ROUTE = "Route"

suspend fun startRoadGraphCalculation(
    jobId: String,
    graphId: String,
    roadGraph: List<RoadGraph>,
    startStation: String,
    endStation: String,
    routeService: RouteService
) = coroutineScope {
    val fetchCalculatedSvg = routeService.fetchCalculatedSvg(graphId, startStation, endStation)
    if (fetchCalculatedSvg != null) {
        // calculation for this svg already done
        routeService.updateJobToDone(jobId)
    } else {
         if (roadGraph.isNotEmpty()) {
            val roadGraphRoute = findShortestPath(roadGraph, startStation, endStation)
            if (roadGraphRoute.isNotEmpty()) {
                val svgContent = buildSVG(roadGraphRoute.map { it.roadGraph })
                routeService.create(
                    CalculatedSvg(
                        roadGraphRoute.first().roadGraph.graphId,
                        startStation,
                        endStation,
                        svgContent
                    )
                )
                routeService.updateJobToDone(jobId)
            } else {
                // data error
            }
         } else {
            // data error
         }
    }
}

suspend fun startTimetableCalculation(
    jobId: String,
    graphId: String,
    timetable: List<Timetable>,
    startStation: String,
    endStation: String,
    routeService: RouteService
) = coroutineScope {
    val calculatedRoute = routeService.fetchCalculatedRoute(graphId, startStation, endStation)
    if (calculatedRoute != null) {
        // calculation for this route already done
        routeService.updateJobToDone(jobId)
    } else {
        if (timetable.isNotEmpty()) {
            val calculatedRoute = findFastestPath(timetable, startStation, endStation)
            routeService.create(calculatedRoute)
            routeService.updateJobToDone(jobId)
        } else {
            // data error
        }
    }
}