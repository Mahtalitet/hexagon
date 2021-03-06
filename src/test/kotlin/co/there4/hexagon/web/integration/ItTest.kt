package co.there4.hexagon.web.integration

import co.there4.hexagon.web.Client
import org.testng.annotations.Test

import co.there4.hexagon.web.Server
import co.there4.hexagon.web.jetty.JettyServer
import org.asynchttpclient.Response
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import java.net.InetAddress.getByName as address

internal const val THREADS = 1
internal const val TIMES = 1

/*
 * TODO Fix errors with several threads
 */
@Test (threadPoolSize = THREADS, invocationCount = TIMES)
abstract class ItTest {
    val servers = listOf(
        JettyServer()
    )

    protected abstract fun initialize (server: Server)

    @BeforeClass fun startServers () {
        servers.forEach {
            initialize (it)
            it.run ()
        }
    }

    @AfterClass fun stopServers () {
        servers.forEach { it.stop () }
    }

    protected fun withClients(lambda: Client.() -> Unit) {
        servers.forEach {
            val client = Client ("http://localhost:${it.runtimePort}")
            client.cookies.clear()
            client.(lambda) ()
        }
    }

    protected fun assertResponseEquals(response: Response?, status: Int, content: String) {
        assert (response?.statusCode == status)
        assert (response?.responseBody == content)
    }

    protected fun assertResponseContains(response: Response?, status: Int, vararg content: String) {
        assert (response?.statusCode == status)
        content.forEach {
            assert (response?.responseBody?.contains (it) ?: false)
        }
    }
}
