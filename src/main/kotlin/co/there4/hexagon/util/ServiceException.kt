package co.there4.hexagon.util

/**
 * Exception with a code and a list of causes.
 *
 * To pass a list of causes
 * CodedException (500, "Error", *list)
 *
 * @author jam
 */
class ServiceException(val code: Int, message: String = "", vararg causes: Throwable) :
    RuntimeException (message, causes.firstOrNull()) {

    val causes = causes.toList()
}
