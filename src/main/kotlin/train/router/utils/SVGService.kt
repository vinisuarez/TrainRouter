package train.router.utils

import train.router.models.db.RoadGraph

fun buildSVG(stations: List<RoadGraph>): String {
    val minX = stations.minOf { it.longitude } - 1
    val minY = stations.maxOf { it.latitude } + 1

    val width = 7
    val height = 10
    val response = StringBuilder()
    val first = stations[0]
    response.appendLine("<svg viewBox=\"$minX -$minY $width $height\">")
    response.append("<path d=\"M ${first.longitude} -${first.latitude} ")
    stations.forEach { s ->
        response.append("L ${s.longitude} -${s.latitude} ")
    }
    response.appendLine("\" stroke=\"#FF0000\" stroke-width=\"0.03\" fill=\"none\"/>")
    var value = 0.05
    stations.forEach { s ->
        response.appendLine("<text x=\"${s.longitude + 0.1}\" y=\"-${s.latitude + value}\" font-size=\"0.1\">${s.stationName}: (${s.longitude}, ${s.latitude})</text>")
        response.appendLine("<circle cx=\"${s.longitude}\" cy=\"-${s.latitude}\" r=\"0.02\"/>")
        value += 0.05
    }
    response.appendLine("</svg>")
    return response.toString()
}