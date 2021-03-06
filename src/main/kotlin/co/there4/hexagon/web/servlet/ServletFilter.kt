package co.there4.hexagon.web.servlet

import java.nio.charset.Charset.defaultCharset
import co.there4.hexagon.util.CompanionLogger
import co.there4.hexagon.web.*
import co.there4.hexagon.web.FilterOrder.*
import co.there4.hexagon.web.Filter as HexagonFilter
import javax.servlet.*
import javax.servlet.Filter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author jam
 */
class ServletFilter (private val router: Router) : Filter {
    companion object : CompanionLogger(ServletFilter::class)

    private val routesByMethod: Map<HttpMethod, List<Pair<Route, Exchange.() -> Unit>>> =
        router.routes.entries.map { it.key to it.value }.groupBy { it.first.method }

    private fun filter(
        request: BServletRequest,
        exchange: Exchange,
        filterOrder: FilterOrder,
        filters: Map<HexagonFilter, Exchange.() -> Unit>): Boolean {

        var handled = false

        filters
            .filter { it.key.order == filterOrder }.entries.map {
            request.actionPath = it.key.path
            exchange.(it.value)()
            handled = true
        }

        return handled
    }

    override fun init(filterConfig: FilterConfig) { /* Not implemented */ }
    override fun destroy() { /* Not implemented */ }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request !is HttpServletRequest || response !is HttpServletResponse)
            error("Invalid request/response parmeters")

        val filters = router.filters.filter { it.key.path.matches(request.servletPath) }
        val methodRoutes = routesByMethod[HttpMethod.valueOf (request.method)]?.filter {
            it.first.path.matches(request.servletPath)
        }

        val bRequest = BServletRequest (request)
        val bResponse = BServletResponse (request, response)
        val bSession = BServletSession (request)
        val exchange = Exchange(bRequest, bResponse, bSession)
        var handled = false

        try {
            handled = filter(bRequest, exchange, BEFORE, filters)

            val stream = javaClass.getResourceAsStream("/public" + request.servletPath)
            if (bRequest.method == HttpMethod.GET &&
                !request.servletPath.endsWith("/") && // Reading a folder as resource gets all files
                stream != null) {

                val contentType = bResponse.getMimeType(request.servletPath)

                // Should be done BEFORE flushing the stream (if not content type is ignored)
                if (response.contentType == null && contentType != null)
                    response.contentType = "$contentType; charset=${defaultCharset().name()}"

                response.outputStream.write(stream.readBytes())
                response.outputStream.flush()

                handled = true
            }
            else {
                if (methodRoutes == null) {
                    bResponse.status = 405
                    bResponse.body = "Invalid method '${request.method}'"
                    handled = true
                }
                else {
                    for (r in methodRoutes) {
                        try {
                            bRequest.actionPath = r.first.path
                            exchange.(r.second)()
                            handled = true
                            break
                        }
                        catch (e: PassException) {
                            continue
                        }
                    }
                }
            }

            handled = filter(bRequest, exchange, AFTER, filters) || handled // Order matters!!!
        }
        catch (e: EndException) {
            handled = true
        }
        catch (e: Exception) {
            err ("Error processing request", e)
            router.handleException(e, exchange)
            handled = true
        }
        finally {
            if (!handled)
                exchange.(router.notFoundHandler)()

            response.status = exchange.response.status
            response.outputStream.write(exchange.response.body.toString().toByteArray())
            response.outputStream.flush()
        }
    }
}
