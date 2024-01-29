package io.horizontalsystems.bankwallet.core

import timber.log.Timber

class DebugTree: Timber.DebugTree() {

    companion object {
        const val TAG = "[wallet]"
    }
    override fun createStackElementTag(element: StackTraceElement): String =
        "${super.createStackElementTag(element)}|${element.methodName}()|${element.fileName}:${element.lineNumber}"

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        var mainTag = TAG
        var methodName = ""
        var msg = ""
        var line = ""


        tag!!.split("|").forEachIndexed() { index, s ->
            when (index) {
                0 -> mainTag = s
                1 -> methodName = s
                2 -> line = s
            }
        }
        super.log(
            priority,
            "[${Thread.currentThread().name}]" + TAG ,
            "$mainTag | $methodName | $line | $message",
            t,
        )
    }
}