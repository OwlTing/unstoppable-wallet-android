package io.horizontalsystems.bankwallet.core

import timber.log.Timber

class DebugTree: Timber.DebugTree() {

    companion object {
        const val TAG = "[wallet]"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, TAG + tag, message, t)
    }
}