package coroutine

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

// From Codelab:
// https://developer.android.com/codelabs/basic-android-kotlin-compose-coroutines-kotlin-playground?authuser=3&continue=https%3A%2F%2Fdeveloper.android.com%2Fcourses%2Fpathways%2Fandroid-basics-compose-unit-5-pathway-1%3Fauthuser%3D3%23codelab-https%3A%2F%2Fdeveloper.android.com%2Fcodelabs%2Fbasic-android-kotlin-compose-coroutines-kotlin-playground

fun main(args: Array<String>) {
    // The scenarios are meant to be mutually exclusive
    // Next scenario is a follow-up to the previous one

    initialScenario()
    initialScenarioRefactoring()
    initialScenarioWithLaunch()
    asyncAndAwait()
    parallelDecomposition()
    exceptionsWithCoroutines()
    cancellationsWithCoroutines()
    understandWithContextAndDispatchers()
}

fun initialScenario() {
    runBlocking {
        // runBlocking will not return until all work within its lambda block is completed
        // The thread runs runBlocking gets blocked until all the coroutines inside runBlocking { ... } complete their execution
        println("Weather forecast initial")
        // delay() will suspend this coroutine
        // which means "println("Sunny")" will not execute after delayed time
        delay(1000)
        println("Sunny")
    }
}

fun initialScenarioRefactoring() {
    val time = measureTimeMillis {
        runBlocking {
            println("Weather forecast initial refactoring")
            printForecast()
            printTemperature()
        }
    }

    println("Execution time: ${time / 1000.0} seconds")
}

// This will print "Have a good day!" first
// because of the delay() suspend function.
// It suspends there, then the main thread
// is freed to execute other code after the launch {}.
// Then after the delay it resumes execution
// after the delay() suspend function.
fun initialScenarioWithLaunch() {
    val time = measureTimeMillis {
        runBlocking {
            println("Weather forecast initial with launch")
            // Below 2 print functions will be run concurrently
            // because they are in separate coroutines
            launch {
                // launch() functions returns immediately after called
                printForecast()
            }
            launch {
                // launch() functions returns immediately after called
                printTemperature()
            }
            // Note here that this will be called before the above 2 print functions.
            // Because after the above 2 coroutines are launched, you can proceed
            // with the next instruction. (The 2 coroutines will be immediately scheduled to execute)
            // This demonstrates the "fire and forget" nature of launch().
            // You fire off a new coroutine with launch(), and don't have to worry about when its work is finished.
            println("Have a good day!")
        }
    }
    println("Execution time: ${time / 1000.0} seconds")
}

// A suspending function is like a regular function,
// but it can be suspended and resumed again later.
// To do this, suspend functions can only be called from other suspend functions that make this capability available.
suspend fun printForecast() {
    delay(1000)
    println("Sunny")
}

suspend fun printTemperature() {
    delay(1000)
    println("30\u00b0C")
}

fun asyncAndAwait() {
    runBlocking {
        println("Weather forecast async and await")
        val forecast: Deferred<String> = async {
            getForecast()
        }
        val temperature: Deferred<String> = async {
            getTemperature()
        }
        println("${forecast.await()} ${temperature.await()}")
        println("Have a good day!")
    }
}

fun parallelDecomposition() {
    runBlocking {
        println("Weather forecast parallel decomposition")
        // getWeatherReport() won't return until it's completed
        println(getWeatherReport())
        println("Have a good day!")
    }
}

// coroutineScope{} creates a local scope for this weather report task
// The coroutines launched within this scope are grouped together within this scope, which has implications for cancellation and exceptions.
// coroutineScope() will only return once all its work, including any coroutines it launched, have completed.
// With coroutineScope(), even though the function is internally doing work concurrently,
// it appears to the caller as a synchronous operation because coroutineScope won't return until all work is done.
// The key insight here for structured concurrency is that you can take multiple concurrent operations
// and put it into a single synchronous operation, where concurrency is an implementation detail.
// The only requirement on the calling code is to be in a suspend function or coroutine.
// Other than that, the structure of the calling code doesn't need to take into account the concurrency details.

// runBlocking and coroutineScope builders may look similar because they both wait for their body and all its children to complete.
// The main difference is that the runBlocking method blocks the current thread for waiting, while coroutineScope just suspends,
// **releasing the underlying thread for other usages**. Because of that difference, runBlocking is a regular function and coroutineScope is a suspending function.
suspend fun getWeatherReport() = coroutineScope {
    val forecast = async { getForecast() }
    val temperature = async { getTemperature() }
    // This ensures that each coroutine completes its work
    // and returns its result, before we return from this function
    "${forecast.await()} ${temperature.await()}"
}

suspend fun getForecast(): String {
    delay(1000)
    return "Sunny"
}

suspend fun getTemperature(): String {
    delay(1000)
    return "30\u00b0C"
}

fun exceptionsWithCoroutines() {
    runBlocking {
        println("Weather forecast exceptions")
        try {
            // getWeatherReport() won't return until it's completed
            println(getWeatherReportWithException())
        } catch (e: AssertionError) {
            println("Caught exception in runBlocking(): $e")
            println("Report unavailable at this time")
        }
        println("Have a good day!")
    }
}

suspend fun getWeatherReportWithException() = coroutineScope {
    val forecast = async { getForecast() }
    val temperature = async {
        try {
            getTemperatureWithException()
        } catch (e: AssertionError) {
            println("Caught exception $e")
            "{ No temperature found }"
        }
    }
    // This ensures that each coroutine completes its work
    // and returns its result, before we return from this function
    "${forecast.await()} ${temperature.await()}"
}

suspend fun getTemperatureWithException(): String {
    delay(500)
    throw AssertionError("Temperature is invalid")
}

fun cancellationsWithCoroutines() {
    runBlocking {
        println("Weather forecast cancellations")
        println(getWeatherReportWithCancellation())
        println("Have a good day!")
    }
}

suspend fun getWeatherReportWithCancellation() = coroutineScope {
    val forecast = async { getForecast() }
    val temperature = async { getTemperature() }

    delay(200)
    temperature.cancel()

    forecast.await()
}

fun understandWithContextAndDispatchers() {
    runBlocking {
        // Coroutine 1
        println("Dispatchers demo")
        launch {
            // Coroutine 2

            // This changes the CoroutineContext that the coroutine is executed within.
            // My understanding is that the block of code inside withContext
            // is still the same coroutine but the thread it is running in is switched.
            println("Thread for the launched coroutine: ${Thread.currentThread().name}")
            withContext(Dispatchers.Default) {
                // seems this is still Coroutine 2

                println("Thread for the code under withContext(Dispatchers.Default): ${Thread.currentThread().name}")
                delay(1000)
                println("10 results found.")
            }
            // Once withContext is done, it will switch back to the original dispatcher
            println("Results loaded")
        }

        // these 2 statements are executed before the coroutine
        println("Thread for the runBlocking: ${Thread.currentThread().name}")
        println("Loading...")
    }
}