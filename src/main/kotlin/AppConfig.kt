import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import kotlinx.html.*
import repo.Person
import repo.PersonRepo
import java.text.DateFormat
import java.time.Duration

const val REST_ENDPOINT = "/person"

fun Application.main() {

    // middleware
    install(DefaultHeaders)
    install(CORS) {
        maxAge = Duration.ofDays(1)
    }
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    routing {
        get("$REST_ENDPOINT/{id}") {
            errorAware {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Parameter id not found")
                call.respond(PersonRepo.get(id))
            }
        }

        get(REST_ENDPOINT) {
            errorAware {
                call.respond(PersonRepo.getAll())
            }
        }

        delete("$REST_ENDPOINT/{id}") {
            errorAware {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Parameter id not found")
                call.respond(PersonRepo.remove(id))
            }
        }

        delete(REST_ENDPOINT) {
            errorAware {
                PersonRepo.clear()
                call.respondSuccessJson()
            }
        }

        post(REST_ENDPOINT) {
            errorAware {
                val person = call.receive<Person>()
                println("Received Post Request: $person")
                call.respond(PersonRepo.add(person))
            }
        }

        get("/") {
            call.respondHtml {
                head {
                    title(content = "Kotlin API example")
                }
                body {
                    div {
                        h1 {
                         + "Welcome to the Persons API"
                        }
                        p {
                            + "Go to /person' to use the API"
                        }
                    }
                }
            }
        }
    }
}

private suspend fun <R> PipelineContext<*, ApplicationCall>.errorAware(block: suspend () -> R): R? {
    return try {
        block()
    } catch (e: Exception) {
        call.respondText("""{ "error":"$e" }""", ContentType.parse("application/json"), HttpStatusCode.InternalServerError)
        null
    }
}

private suspend fun ApplicationCall.respondSuccessJson(value: Boolean = true) =
        respond("""{ "success": "$value" }""")