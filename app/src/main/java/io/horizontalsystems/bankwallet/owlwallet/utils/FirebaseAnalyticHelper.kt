package io.horizontalsystems.bankwallet.owlwallet.utils

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber


///create a firebase analytic helper class
class FirebaseAnalyticHelper {
    private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    fun logLaunchEvent() {
        Timber.d("logLaunchEvent")
        firebaseAnalytics.logEvent("event_launch", null)
    }

}