package train.router.utils

import org.junit.Test
import train.router.models.db.RoadGraph
import kotlin.test.assertContains

class SVGServiceTest {
    @Test
    fun shouldCreateCorrectSvg() {
        val stations = listOf(
            RoadGraph("test", "London King's Cross", 51.5308, -0.1224, listOf()),
            RoadGraph("test", "Nottingham", 52.9476, -1.1469, listOf()),
            RoadGraph("test", "Leeds", 53.7944, -1.5479, listOf()),
            RoadGraph("test", "Liverpool Lime Street", 53.4072, -2.9778, listOf())
        )

        val response = buildSVG(stations)
        assertContains(
            response,
            "<path d=\"M -0.1224 -51.5308 L -0.1224 -51.5308 L -1.1469 -52.9476 L -1.5479 -53.7944 L -2.9778 -53.4072 \""
        )
    }

    @Test
    fun shouldCreateCorrectSvg2() {
        val stations = listOf(
            RoadGraph("test", "London King's Cross", 51.5308, -0.1224, listOf()),
            RoadGraph("test", "York", 53.9586, -1.0906, listOf()),
            RoadGraph("test", "Edinburgh Waverley", 55.9533, -3.1883, listOf())
        )

        val response = buildSVG(stations)
        assertContains(
            response,
            "<path d=\"M -0.1224 -51.5308 L -0.1224 -51.5308 L -1.0906 -53.9586 L -3.1883 -55.9533 \""
        )
    }

    @Test
    fun shouldCreateCorrectSvg3() {
        val stations = listOf(
            RoadGraph("test", "London King's Cross", 51.5308, -0.1224, listOf()),
            RoadGraph("test", "Bristol Temple Meads", 51.4492, -2.5814, listOf()),
            RoadGraph("test", "Cardiff Central", 51.4757, -3.1791, listOf()),
            RoadGraph("test", "Swansea",  51.6251, -3.9409, listOf())
        )

        val response = buildSVG(stations)
        assertContains(
            response,
            "path d=\"M -0.1224 -51.5308 L -0.1224 -51.5308 L -2.5814 -51.4492 L -3.1791 -51.4757 L -3.9409 -51.6251 \""
        )
    }
}