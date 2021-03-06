package co.there4.hexagon

import co.there4.hexagon.serialization.parse
import co.there4.hexagon.web.Client
import co.there4.hexagon.web.server
import org.asynchttpclient.Response
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

internal const val THREADS = 4
internal const val TIMES = 4

internal val FORTUNE_MESSAGES = setOf(
"fortune: No such file or directory",
"A computer scientist is someone who fixes things that aren't broken.",
"After enough decimal places, nobody gives a damn.",
"A bad random number generator: 1, 1, 1, 1, 1, 4.33e+67, 1, 1, 1",
"A computer program does what you tell it to do, not what you want it to do.",
"Emacs is a nice operating system, but I prefer UNIX. — Tom Christaensen",
"Any program that runs right is obsolete.",
"A list is only as strong as its weakest link. — Donald Knuth",
"Feature: A bug with seniority.",
"Computers make very fast, very accurate mistakes.",
"<script>alert(\"This should not be displayed in a browser alert box.\");</script>",
"フレームワークのベンチマーク"
)

@Test (threadPoolSize = THREADS, invocationCount = TIMES)
class BenchmarkTest {
private val client by lazy { Client("http://localhost:${server.runtimePort}") }

@BeforeClass fun warmup() {
    if (fortuneRepository.isEmpty()) {
        val fortunes = FORTUNE_MESSAGES.mapIndexed { ii, fortune -> Fortune(ii + 1, fortune) }
        fortuneRepository.insertManyObjects(fortunes)
    }

    if (worldRepository.isEmpty()) {
        val world = (1..DB_ROWS).map { World(it, it) }
        worldRepository.insertManyObjects(world)
    }

    main(arrayOf())

    val warmupRounds = if (THREADS > 1) 2 else 0
    (1 ..warmupRounds).forEach {
        json ()
        plaintext ()
        no_query_parameter ()
        empty_query_parameter ()
        text_query_parameter ()
        zero_queries ()
        one_thousand_queries ()
        one_query ()
        ten_queries ()
        one_hundred_queries ()
        five_hundred_queries ()
        fortunes ()
        fortune_page()
        no_updates_parameter ()
        empty_updates_parameter ()
        text_updates_parameter ()
        zero_updates ()
        one_thousand_updates ()
        one_update ()
        ten_updates ()
        one_hundred_updates ()
        five_hundred_updates ()
    }
}

fun json () {
    val response = client.get ("/json")
    val content = response.responseBody

    checkResponse (response, "application/json")
    assert ("Hello, World!" == content.parse(Message::class).message)
}

fun plaintext () {
    val response = client.get ("/plaintext")
    val content = response.responseBody

    checkResponse (response, "text/plain")
    assert ("Hello, World!" == content)
}

fun no_query_parameter () {
    val response = client.get ("/db")
    val body = response.responseBody

    checkResponse (response, "application/json")
    val bodyMap = body.parse(Map::class)
    assert(bodyMap.containsKey (World::_id.name))
    assert(bodyMap.containsKey (World::randomNumber.name))
}

fun fortunes () = fortuneCheck("/fortunes")
fun fortune_page() = fortuneCheck("/fortunes_page")

fun no_updates_parameter () {
    val response = client.get ("/update")
    val body = response.responseBody

    checkResponse (response, "application/json")
    val bodyMap = body.parse(Map::class)
    assert(bodyMap.containsKey (World::_id.name))
    assert(bodyMap.containsKey (World::randomNumber.name))
}

fun empty_query_parameter () = checkDbRequest ("/query?queries", 1)
fun text_query_parameter () = checkDbRequest ("/query?queries=text", 1)
fun zero_queries () = checkDbRequest ("/query?queries=0", 1)
fun one_thousand_queries () = checkDbRequest ("/query?queries=1000", 500)
fun one_query () = checkDbRequest ("/query?queries=1", 1)
fun ten_queries () = checkDbRequest ("/query?queries=10", 10)
fun one_hundred_queries () = checkDbRequest ("/query?queries=100", 100)
fun five_hundred_queries () = checkDbRequest ("/query?queries=500", 500)

fun empty_updates_parameter () = checkDbRequest ("/update?queries", 1)
fun text_updates_parameter () = checkDbRequest ("/update?queries=text", 1)
fun zero_updates () = checkDbRequest ("/update?queries=0", 1)
fun one_thousand_updates () = checkDbRequest ("/update?queries=1000", 500)
fun one_update () = checkDbRequest ("/update?queries=1", 1)
fun ten_updates () = checkDbRequest ("/update?queries=10", 10)
fun one_hundred_updates () = checkDbRequest ("/update?queries=100", 100)
fun five_hundred_updates () = checkDbRequest ("/update?queries=500", 500)

private fun checkDbRequest (path: String, itemsCount: Int) {
    val response = client.get (path)
    val content = response.responseBody

    checkResponse (response, "application/json")
    checkResultItems (content, itemsCount)
}

private fun checkResponse (res: Response, contentType: String) {
    assert(res.headers ["Server"] != null)
    assert(res.headers ["Transfer-Encoding"] != null)
    assert(res.headers ["Content-Type"].contains (contentType))
}

private fun checkResultItems (result: String, size: Int) {
    val resultsList = result.parse(List::class)
    assert (size == resultsList.size)

    (1..size).forEach {
        val r = resultsList[it - 1] as Map<*, *>
        assert (r.containsKey (World::_id.name) && r.containsKey (World::randomNumber.name))
    }
}

private fun fortuneCheck (url: String) {
        val response = client.get (url)
        val content = response.responseBody
        val contentType = response.headers ["Content-Type"]

        assert (response.headers ["Server"] != null)
        assert (response.headers ["Date"] != null)
        assert (content.contains ("&lt;script&gt;alert(&quot;This should not be displayed"))
        assert (content.contains ("フレームワークのベンチマーク"))
        assert (contentType.toLowerCase ().contains ("text/html"))
    }
}
