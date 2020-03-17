import kotlin.test.*

fun suppressedError(n: Int): Exception {
    try {
        throw RuntimeException("Closing error $n")
    } catch (e: Exception) {
        return e
    }
}

fun functionRoot() {
    throw Error("Root cause")
}


fun functionTopLevel() {
    try {
        functionRoot()
    } catch (e: Throwable) {
        e.addSuppressed(suppressedError(1))
        throw IllegalStateException("Top Level\nDetails", e)
    }
}


fun main(args : Array<String>) {
    try {
        functionTopLevel()
        println("Should have failed")
    } catch (e: Throwable) {
        e.addSuppressed(suppressedError(2))

        val detailTrace = e.toStringWithTrace()
        val indexTop = detailTrace.indexOf(e.toString())
        val indexCause = detailTrace.indexOf(e.cause!!.toString())

        assertEquals(0, indexTop)
        assertTrue(indexCause > 0, "cause should be present in\n$detailTrace")

        for (s in e.suppressedExceptions) {
            val indexSuppressed = detailTrace.indexOf(s.toString())
            assertTrue(indexSuppressed > 0 && indexSuppressed < indexCause, "Suppressed $s should be before cause in\n$detailTrace")
        }

//        println(detailTrace)
        println("OK")
    }
}
