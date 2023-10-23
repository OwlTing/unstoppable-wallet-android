package com.owlting.app.stellarkit.models

abstract class StellarAsset {
    val decimals: Int = 7
}

class Native: StellarAsset()

class Alphanum4(
    val code: String,
    val issuer: String,
): StellarAsset()