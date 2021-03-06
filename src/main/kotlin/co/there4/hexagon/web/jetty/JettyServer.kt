package co.there4.hexagon.web.jetty

import co.there4.hexagon.web.Server
import co.there4.hexagon.web.servlet.ServletFilter
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.Server as JettyServletServer
import org.eclipse.jetty.server.session.HashSessionIdManager
import org.eclipse.jetty.server.session.HashSessionManager
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.component.LifeCycle

import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import javax.servlet.DispatcherType
import java.net.InetAddress.getByName as address

/**
 * @author jam
 */
class JettyServer (bindAddress: InetAddress = address ("localhost"), bindPort: Int = 2010):
    Server (bindAddress, bindPort) {

    val jettyServer = JettyServletServer(InetSocketAddress(bindAddress, bindPort))

    override val runtimePort: Int
        get() = (jettyServer.connectors[0] as ServerConnector).localPort.let {
            if (it == -1) error("Jetty port uninitialized. Use lazy evaluation for HTTP client ;)")
            else it
        }

    override fun started() = jettyServer.isStarted

    override fun startup() {
        val context = ServletContextHandler()
        context.sessionHandler = SessionHandler(HashSessionManager())

        jettyServer.sessionIdManager = HashSessionIdManager()
        jettyServer.handler = context

        context.addLifeCycleListener(object : LifeCycle.Listener {
            override fun lifeCycleStopped(event: LifeCycle?) { /* Do nothing */ }
            override fun lifeCycleStopping(event: LifeCycle?) { /* Do nothing */ }
            override fun lifeCycleStarted(event: LifeCycle?) { /* Do nothing */ }
            override fun lifeCycleFailure(event: LifeCycle?, cause: Throwable?) { /* Do nothing */ }

            override fun lifeCycleStarting(event: LifeCycle?) {
                val filter = ServletFilter (this@JettyServer)
                context.servletContext.addFilter("filters", filter)
                    .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")
            }
        })

        jettyServer.start()
    }

    override fun shutdown() {
        jettyServer.stopAtShutdown = true
        jettyServer.stop()
    }
}
