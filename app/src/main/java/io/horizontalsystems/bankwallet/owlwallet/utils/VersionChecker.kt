package io.horizontalsystems.bankwallet.owlwallet.utils

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTS3DataSource
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.OTS3RemoteDataSource
import io.horizontalsystems.bankwallet.owlwallet.data.succeeded
import timber.log.Timber

enum class UpdateAction { Nothing, Flexible, Immediate }


class VersionChecker(
    private val s3: OTS3DataSource = OTS3RemoteDataSource()
) {
    var isChecked = false
    suspend fun check(): UpdateAction {
        Timber.d("check ${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE}")
        if (isChecked) {
            return UpdateAction.Nothing
        }
        isChecked = true

        try {
            val currentVersion = Version(BuildConfig.VERSION_NAME)
            val currentCode = BuildConfig.VERSION_CODE

            Timber.d("current: $currentVersion, $currentCode")

            val result = s3.getVersionData()
            if (result.succeeded) {
                val response = (result as OTResult.Success).data
                val versions = response.associateBy { it.type }

                if (versions.containsKey(minimum)) {
                    val minimumVersion = Version(versions[minimum]!!.version)
                    val minimumCode = versions[minimum]!!.build

                    Timber.d("minimum: $minimumVersion, $minimumCode")

                    if (currentVersion < minimumVersion || currentCode < minimumCode) {
                        Timber.d("immediate")
                        return UpdateAction.Immediate
                    }
                }

                if (versions.containsKey(target)) {
                    val targetVersion = Version(versions[target]!!.version)
                    val targetCode = versions[target]!!.build

                    Timber.d("target: $targetVersion, $targetCode ${currentVersion < targetVersion}")

                    if (currentVersion < targetVersion || currentCode < targetCode) {
                        Timber.d("flexible")
                        return UpdateAction.Flexible
                    }
                }
                Timber.d("nothing")
                return UpdateAction.Nothing
            } else {
                Timber.e("Failed to get version data ${(result as OTResult.Error).exception}")
                return UpdateAction.Nothing
            }
        } catch (e: Exception) {
            Timber.e(e)
            return UpdateAction.Nothing
        }
    }

    companion object {
        const val minimum = "minimum"
        const val target = "target"
    }
}

class Version(
    name: String
) : Comparable<Version> {
    private val data = name.split('.').map { it.toInt() }
    val major = data[0]
    val minor = data[1]
    val patch = data[2]

    override fun compareTo(other: Version): Int {
        Timber.d("compare, current: $this, other: $other")
        return if (major > other.major) {
            1
        } else if (major == other.major){
            return if (minor > other.minor) {
                1
            } else if (minor == other.minor) {
                if (patch > other.patch) {
                    1
                } else if (patch == other.patch) {
                    0
                } else {
                    -1
                }
            } else {
                -1
            }
        } else {
            -1
        }
    }

    override fun toString(): String {
        return "$major.$minor,$patch"
    }
}