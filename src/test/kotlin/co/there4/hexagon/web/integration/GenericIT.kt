package co.there4.hexagon.web.integration

import co.there4.hexagon.web.Client
import co.there4.hexagon.web.Exchange
import co.there4.hexagon.web.HttpMethod
import co.there4.hexagon.web.Server
import java.time.LocalDateTime
import java.util.Locale.getDefault as defaultLocale

import kotlin.test.assertTrue

@Suppress("unused") // Test methods are flagged as unused
class GenericIT : ItTest () {
    private val part = "param"

    override fun initialize(server: Server) {
        server.before("/protected/*") { halt(401, "Go Away!") }

        server.get("/request/data") {
            response.body = request.url

            request.cookies["method"]?.value = request.method.toString()
            request.cookies["host"]?.value = request.ip
            request.cookies["uri"]?.value = request.url
            request.cookies["params"]?.value = request.parameters.size.toString()

            response.addHeader("agent", request.userAgent)
            response.addHeader("scheme", request.scheme)
            response.addHeader("host", request.host)
            response.addHeader("query", request.queryString)
            response.addHeader("port", request.port.toString())

            ok ("${response.body}!!!")
        }

        server.error(UnsupportedOperationException::class) {
            response.addHeader("error", it.message ?: it.javaClass.name)
        }

        server.get("/*") { pass() }
        server.get("/exception") { throw UnsupportedOperationException("error message") }
        server.get("/hi") { ok ("Hello World!") }
        server.get("/param/{param}") { ok ("echo: ${request ["param"]}") }
        server.get("/paramwithmaj/{paramWithMaj}") { ok ("echo: ${request ["paramWithMaj"]}") }
        server.get("/") { ok("Hello Root!") }
        server.post("/poster") { created("Body was: ${request.body}") }
        server.patch("/patcher") { ok ("Body was: ${request.body}") }
        server.delete ("/method") { okRequestMethod () }
        server.options ("/method") { okRequestMethod () }
        server.get ("/method") { okRequestMethod () }
        server.patch ("/method") { okRequestMethod () }
        server.post ("/method") { okRequestMethod () }
        server.put ("/method") { okRequestMethod () }
        server.trace ("/method") { okRequestMethod () }
        server.head ("/method") { response.addHeader ("header", request.method.toString()) }
        server.get("/halt") { halt("halted") }
        server.get("/tworoutes/$part/{param}") { ok ("$part route: ${request ["param"]}") }
        server.get("/template") {
            template("pebble_template.html", defaultLocale(), mapOf("date" to LocalDateTime.now()))
        }

        server.get("/tworoutes/${part.toUpperCase()}/{param}") {
            ok ("${part.toUpperCase()} route: ${request ["param"]}")
        }

        server.get("/reqres") { ok (request.method) }

        server.get("/redirect") { redirect("http://example.com") }

        server.after("/hi") {
            response.addHeader ("after", "foobar")
        }
    }

    private fun Exchange.okRequestMethod() = ok (request.method)

    fun reqres() {
        withClients {
            val response = get("/reqres")
            assertResponseEquals(response, 200, "GET")
        }
    }

    fun getHi() {
        withClients {
            val response = get("/hi")
            assertResponseEquals(response, 200, "Hello World!")
        }
    }

    fun hiHead() {
        withClients {
            val response = head("/hi")
            assertResponseEquals(response, 200, "")
        }
    }

    fun template() {
        withClients {
            val response = get("/template")
            assert(response.statusCode == 200)
        }
    }

    fun getHiAfterFilter() {
        withClients {
            val response = get ("/hi")
            assertResponseEquals(response, 200, "Hello World!")
            assert(response.headers["after"]?.contains("foobar") ?: false)
        }
    }

    fun getRoot() {
        withClients {
            val response = get ("/")
            assertResponseEquals(response, 200, "Hello Root!")
        }
    }

    fun echoParam1() {
        withClients {
            val response = get ("/param/shizzy")
            assertResponseEquals(response, 200, "echo: shizzy")
        }
    }

    fun echoParam2() {
        withClients {
            val response = get ("/param/gunit")
            assertResponseEquals(response, 200, "echo: gunit")
        }
    }

    fun echoParamWithUpperCaseInValue() {
        withClients {
            val camelCased = "ThisIsAValueAndBlacksheepShouldRetainItsUpperCasedCharacters"
            val response = get ("/param/" + camelCased)
            assertResponseEquals(response, 200, "echo: $camelCased")
        }
    }

    fun twoRoutesWithDifferentCase() {
        withClients {
            var expected = "expected"
            val response1 = get ("/tworoutes/$part/$expected")
            assertResponseEquals(response1, 200, "$part route: $expected")

            expected = expected.toUpperCase()
            val response = get ("/tworoutes/${part.toUpperCase()}/$expected")
            assertResponseEquals(response, 200, "${part.toUpperCase()} route: $expected")
        }
    }

    fun echoParamWithMaj() {
        withClients {
            val response = get ("/paramwithmaj/plop")
            assertResponseEquals(response, 200, "echo: plop")
        }
    }

    fun unauthorized() {
        withClients {
            val response = get ("/protected/resource")
            assertTrue(response.statusCode == 401)
        }
    }

    fun notFound() {
        withClients {
            val response = get ("/no/resource")
            assertResponseContains(response, 404, "http://localhost:", "/no/resource not found")
        }
    }

    fun postOk() {
        withClients {
            val response = post ("/poster", "Fo shizzy")
            assertResponseContains(response, 201, "Fo shizzy")
        }
    }

    fun patchOk() {
        withClients {
            val response = patch ("/patcher", "Fo shizzy")
            assertResponseContains(response, 200, "Fo shizzy")
        }
    }

    fun staticFile() {
        withClients {
            val response = get ("/file.txt")
            assertResponseEquals(response, 200, "file content\n")
        }
    }

    fun fileContentType() {
        withClients {
            val response = get ("/file.css")
            assert(response.contentType.contains("css"))
            assertResponseEquals(response, 200, "/* css */\n")
        }
    }

    fun halt() {
        withClients {
            val response = get ("/halt")
            assertResponseEquals(response, 500, "halted")
        }
    }

    fun redirect() {
        withClients {
            val response = get ("/redirect")
            assert(response.statusCode == 302)
            assert(response.headers["Location"] == "http://example.com")
        }
    }

    // TODO Check with asserts
    fun requestData() {
        withClients {
            val response = get ("/request/data?query")
            val port = endpointUrl.port.toString ()
            val protocol = "http"
//            val protocol = if (testScenario.secure) "https" else "http"

//            assert ("error message" == response.cookies["method"].value)
//            assert ("error message" == response.cookies["host"].value)
//            assert ("error message" == response.cookies["uri"].value)
//            assert ("error message" == response.cookies["params"].value)

            assert("AHC/2.0" == response.headers["agent"])
            assert(protocol == response.headers["scheme"])
            assert("127.0.0.1" == response.headers["host"])
            assert("query" == response.headers["query"])
            assert(port == response.headers["port"])

            assert(response.responseBody == "$protocol://localhost:$port/request/data!!!")
            assert(200 == response.statusCode)
        }
    }

    fun handleException() {
        withClients {
            val response = get ("/exception")
            assert("error message" == response.headers["error"]?.toString())
        }
    }

    fun methods () {
        withClients {
            checkMethod (this, "HEAD", "header") // Head does not support body message
            checkMethod (this, "DELETE")
            checkMethod (this, "OPTIONS")
            checkMethod (this, "GET")
            checkMethod (this, "PATCH")
            checkMethod (this, "POST")
            checkMethod (this, "PUT")
            checkMethod (this, "TRACE")
        }
    }

    private fun checkMethod (client: Client, methodName: String, headerName: String? = null) {
        val res = client.send(HttpMethod.valueOf (methodName), "/method")
        assert (
            if (headerName == null) res.responseBody != null
            else res.headers.get(headerName) == methodName
        )
        assert (200 == res.statusCode)
    }
}

