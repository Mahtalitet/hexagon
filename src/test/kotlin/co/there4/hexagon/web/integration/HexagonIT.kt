package co.there4.hexagon.web.integration

import co.there4.hexagon.web.*
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import org.asynchttpclient.Response

@Test class HexagonIT {
    val client: Client by lazy { Client ("http://localhost:${server.runtimePort}") }

    @BeforeClass fun startServers () {
        stop()

        get ("/books/{id}") {
            ok ("${request ["id"]}:${request.body}")
        }
        get ("/books/{id}/{title}") { ok ("${request ["id"]}:${request ["title"]} ${request.body}") }
        trace ("/books/{id}/{title}") { ok ("${request ["id"]}:${request ["title"]} ${request.body}") }
        patch ("/books/{id}/{title}") { ok ("${request ["id"]}:${request ["title"]} ${request.body}") }
        head ("/books/{id}/{title}") {
            response.addHeader("id", request.parameter("id"))
            response.addHeader("title", request.parameter("title"))
        }

        run()
    }

    @AfterClass fun stopServers () {
        stop()
    }

    fun foo () {
        assertResponseContains (client.get ("/books/101"), 200, "101")
    }

    fun getBook () {
        assertResponseContains (client.get ("/books/101/Hamlet"), 200, "101", "Hamlet")
        assertResponseContains (client.trace ("/books/101/Hamlet"), 200, "101", "Hamlet")
        assertResponseContains (client.patch ("/books/101/Hamlet"), 200, "101", "Hamlet")
        assertResponseContains (client.head ("/books/101/Hamlet"), 200)

        assertResponseContains (client.get ("/books/101/Hamlet", "body"), 200, "101", "Hamlet", "body")
        assertResponseContains (client.trace ("/books/101/Hamlet", "body"), 200, "101", "Hamlet", "body")
        assertResponseContains (client.patch ("/books/101/Hamlet", "body"), 200, "101", "Hamlet", "body")
        assertResponseContains (client.head ("/books/101/Hamlet", "body"), 200)
    }

    private fun assertResponseContains(response: Response?, status: Int, vararg content: String) {
        assert (response?.statusCode == status)
        content.forEach {
            assert (response?.responseBody?.contains (it) ?: false)
        }
    }
}
