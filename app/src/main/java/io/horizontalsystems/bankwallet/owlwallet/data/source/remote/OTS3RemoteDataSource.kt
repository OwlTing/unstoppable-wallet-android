package io.horizontalsystems.bankwallet.owlwallet.data.source.remote

import io.horizontalsystems.bankwallet.owlwallet.data.OTResult
import io.horizontalsystems.bankwallet.owlwallet.data.source.OTS3DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OTS3RemoteDataSource(
    private val apiClient: OTS3ApiClient = OTS3ApiClient.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OTS3DataSource {

    override suspend fun getVersionData(): OTResult<List<VersionData>> = withContext(ioDispatcher) {
        try {
            OTResult.Success(apiClient.getVersionData())
//            OTResult.Success(listOf(
//                VersionData("target", "1.0.0", 3, ""),
//                VersionData("minimum", "0.0.9", 4, "")
//            ))
        } catch (e: Exception) {
            OTResult.Error(e)
        }
    }
}