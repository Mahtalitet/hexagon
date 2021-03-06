package co.there4.hexagon.web

import java.util.*
import co.there4.hexagon.web.HttpMethod.*
import co.there4.hexagon.web.FilterOrder.*
import co.there4.hexagon.util.CompanionLogger
import co.there4.hexagon.util.toText
import kotlin.reflect.KClass

/**
 * TODO Compose routers with a map of context to router (children)
 * TODO val children: MutableMap<String, Router> = LinkedHashMap()
 * TODO Add warning in Hexagon when paths DO NOT start with '/' or contains ':' (bad formats)
 */
open class Router(
    val filters: MutableMap<Filter, Exchange.() -> Unit> = LinkedHashMap (),
    val routes: MutableMap<Route, Exchange.() -> Unit> = LinkedHashMap (),
    val assets: MutableList<String> = ArrayList (),
    val errors: MutableMap<Class<out Exception>, Exchange.(e: Exception) -> Unit> = LinkedHashMap ()
    ) {

    companion object : CompanionLogger (Router::class)

    var notFoundHandler: Exchange.() -> Unit = { error(404, request.url + " not found") }
        private set

    private var errorHandler: Exchange.(e: Exception) -> Unit = { e -> error(500, e.toText()) }

    fun after(path: String = "/*", block: Exchange.() -> Unit) = addFilter(path, AFTER, block)
    fun before(path: String = "/*", block: Exchange.() -> Unit) = addFilter (path, BEFORE, block)

    fun get(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, GET, block)
    fun head(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, HEAD, block)
    fun post(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, POST, block)
    fun put(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, PUT, block)
    fun delete(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, DELETE, block)
    fun trace(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, TRACE, block)
    fun options(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, OPTIONS, block)
    fun patch(path: String = "/", block: Exchange.() -> Unit) = addRoute(path, PATCH, block)

    fun notFound(block: Exchange.() -> Unit) { notFoundHandler = block }
    fun internalError(block: Exchange.(e: Exception) -> Unit) { errorHandler = block }

    fun handleException (exception: Exception, exchange: Exchange) {
        val handler = errors[exception.javaClass]

        if (handler != null)
            exchange.(handler)(exception)
        else
            exchange.errorHandler(exception)
    }

    fun assets (path: String) = assets.add (path)

    fun error(exception: Class<out Exception>, callback: Exchange.(e: Exception) -> Unit) =
        errors.put (exception, callback)

    fun error(exception: KClass<out Exception>, callback: Exchange.(e: Exception) -> Unit) =
        error (exception.java, callback)

    private fun addFilter(path: String, order: FilterOrder, block: Exchange.() -> Unit) {
        val filter = Filter (Path (path), order)

        if (filters.containsKey(filter))
            throw IllegalArgumentException ("$order $path Filter is already added")

        filters.put (filter, block)
        info ("$order $path Filter ADDED")
    }

    private fun addRoute(path: String, method: HttpMethod, block: Exchange.() -> Unit): Route {
        val route = Route (Path (path), method)

        if (routes.containsKey(route))
            throw IllegalArgumentException ("$method $path Route is already added")

        routes.put (route, block)
        info ("$method $path Route ADDED")

        return route
    }

    fun reset() {
        filters.clear()
        routes.clear()
        assets.clear()
        errors.clear()
    }
}
