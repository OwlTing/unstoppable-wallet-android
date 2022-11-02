package io.horizontalsystems.bankwallet.owlwallet.data.source

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.remote.VersionData

interface OTS3DataSource {

    suspend fun getVersionData(): OTResult<List<VersionData>>
}