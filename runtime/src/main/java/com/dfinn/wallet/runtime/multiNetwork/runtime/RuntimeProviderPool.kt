package com.dfinn.wallet.runtime.multiNetwork.runtime

import com.dfinn.wallet.runtime.ext.typesUsage
import com.dfinn.wallet.runtime.multiNetwork.chain.model.Chain
import com.dfinn.wallet.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import java.util.concurrent.ConcurrentHashMap

class RuntimeProviderPool(
    private val runtimeFactory: RuntimeFactory,
    private val runtimeSyncService: RuntimeSyncService,
    private val baseTypeSynchronizer: BaseTypeSynchronizer
) {

    private val pool = ConcurrentHashMap<String, RuntimeProvider>()

    fun getRuntimeProvider(chainId: String) = pool.getValue(chainId)

    fun setupRuntimeProvider(chain: Chain): RuntimeProvider {
        val provider = pool.getOrPut(chain.id) {
            RuntimeProvider(runtimeFactory, runtimeSyncService, baseTypeSynchronizer, chain)
        }

        provider.considerUpdatingTypesUsage(chain.typesUsage)

        return provider
    }

    fun removeRuntimeProvider(chainId: String) {
        pool.remove(chainId)?.apply { finish() }
    }
}
