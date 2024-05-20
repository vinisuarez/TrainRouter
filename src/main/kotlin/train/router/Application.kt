package train.router


import io.ktor.server.application.*
import train.router.plugins.configureRouting
import train.router.plugins.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRouting(embeddedDb = false)
}