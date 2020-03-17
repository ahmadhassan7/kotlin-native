/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.ExportTypeInfo
import kotlin.native.internal.NativePtrArray

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@ExportTypeInfo("theThrowableTypeInfo")
public open class Throwable(open val message: String?, open val cause: Throwable?) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    @get:ExportForCppRuntime("Kotlin_Throwable_getStackTrace")
    private val stackTrace = getCurrentStackTrace()

    private val stackTraceStrings: Array<String> by lazy {
        getStackTraceStrings(stackTrace).freeze()
    }

    /**
     * Returns an array of stack trace strings representing the stack trace
     * pertaining to this throwable.
     */
    public fun getStackTrace(): Array<String> = stackTraceStrings

    internal fun getStackTraceAddressesInternal(): List<Long> =
            (0 until stackTrace.size).map { index -> stackTrace[index].toLong() }

    /**
     * Prints the stack trace of this throwable to the standard output.
     */
    public fun printStackTrace(): Unit = dumpStackTrace("", "") { println(it) }

    internal fun dumpStackTrace(): String = buildString {
        dumpStackTrace("", "") { appendln(it) }
    }

    private fun Throwable.dumpStackTrace(indent: String, qualifier: String, writeln: (String) -> Unit) {
        this.dumpSelfTrace(indent, qualifier, writeln)

        var cause = this.cause
        while (cause != null) {
            // TODO: should skip common stack frames
            cause.dumpSelfTrace(indent, "Caused by: ", writeln)
            cause = cause.cause
        }
    }

    private fun Throwable.dumpSelfTrace(indent: String, qualifier: String, writeln: (String) -> Unit) {
        writeln(indent + qualifier + this.toString())
        for (element in stackTraceStrings) {
            writeln("$indent\tat $element")
        }
        val suppressed = suppressedExceptionsList
        if (!suppressed.isNullOrEmpty()) {
            val suppressedIndent = indent + '\t'
            for (s in suppressed) {
                s.dumpStackTrace(suppressedIndent, "Suppressed: ", writeln)
            }
        }
    }


    /**
     * Returns a short description of this throwable consisting of
     * the exception class name (fully qualified if possible)
     * followed by the exception message if it is not null.
     */
    public override fun toString(): String {
        val kClass = this::class
        val s = kClass.qualifiedName ?: kClass.simpleName ?: "Throwable"
        return if (message != null) s + ": " + message.toString() else s
    }

    internal var suppressedExceptionsList: MutableList<Throwable>? = null
}

@SymbolName("Kotlin_getCurrentStackTrace")
private external fun getCurrentStackTrace(): NativePtrArray

@SymbolName("Kotlin_getStackTraceStrings")
private external fun getStackTraceStrings(stackTrace: NativePtrArray): Array<String>

/**
 * Returns a short description of this throwable with the complete back trace.
 *
 * The back trace includes the stack frames ...
 */
@SinceKotlin("1.4")
public fun Throwable.toStringWithTrace(): String = dumpStackTrace()

/**
 * Adds the specified exception to the list of exceptions that were
 * suppressed in order to deliver this exception.
 *
 * Does nothing if this [Throwable] is frozen.
 */
@SinceKotlin("1.4")
public actual fun Throwable.addSuppressed(exception: Throwable) {
    if (this !== exception && !this.isFrozen)
        initSuppressed().add(exception)
}

/**
 * Returns a list of all exceptions that were suppressed in order to deliver this exception.
 */
@SinceKotlin("1.4")
public actual val Throwable.suppressedExceptions: List<Throwable> get() {
    return this.suppressedExceptionsList ?: emptyList()
}

private fun Throwable.initSuppressed(): MutableList<Throwable> {
    return this.suppressedExceptionsList ?: mutableListOf<Throwable>().also { this.suppressedExceptionsList = it }
}