package com.dfinn.wallet.feature_staking_impl.data.repository

import com.dfinn.wallet.common.data.network.runtime.binding.NonNullBinderWithType
import com.dfinn.wallet.common.data.network.runtime.binding.returnType
import com.dfinn.wallet.common.utils.Modules
import com.dfinn.wallet.common.utils.babe
import com.dfinn.wallet.common.utils.balances
import com.dfinn.wallet.common.utils.constant
import com.dfinn.wallet.common.utils.hasModule
import com.dfinn.wallet.common.utils.numberConstant
import com.dfinn.wallet.common.utils.session
import com.dfinn.wallet.common.utils.staking
import com.dfinn.wallet.core_db.dao.AccountStakingDao
import com.dfinn.wallet.core_db.model.AccountStakingLocal
import com.dfinn.wallet.feature_staking_api.domain.api.AccountIdMap
import com.dfinn.wallet.feature_staking_api.domain.api.StakingRepository
import com.dfinn.wallet.feature_staking_api.domain.model.EraIndex
import com.dfinn.wallet.feature_staking_api.domain.model.Nominations
import com.dfinn.wallet.feature_staking_api.domain.model.SlashingSpans
import com.dfinn.wallet.feature_staking_api.domain.model.StakingLedger
import com.dfinn.wallet.feature_staking_api.domain.model.StakingState
import com.dfinn.wallet.feature_staking_api.domain.model.StakingStory
import com.dfinn.wallet.feature_staking_api.domain.model.ValidatorPrefs
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindActiveEra
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindCurrentEra
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindCurrentIndex
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindCurrentSlot
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindErasStartSessionIndex
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindExposure
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindHistoryDepth
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindMaxNominators
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindMinBond
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindNominations
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindNominatorsCount
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindRewardDestination
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindSlashDeferDuration
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindSlashingSpans
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedger
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindTotalInsurance
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings.bindValidatorPrefs
import com.dfinn.wallet.feature_staking_impl.data.network.blockhain.updaters.activeEraStorageKey
import com.dfinn.wallet.feature_staking_impl.data.repository.datasource.StakingStoriesDataSource
import com.dfinn.wallet.feature_wallet_api.domain.interfaces.WalletConstants
import com.dfinn.wallet.runtime.multiNetwork.ChainRegistry
import com.dfinn.wallet.runtime.multiNetwork.chain.model.Chain
import com.dfinn.wallet.runtime.multiNetwork.chain.model.ChainId
import com.dfinn.wallet.runtime.multiNetwork.getRuntime
import com.dfinn.wallet.runtime.storage.source.StorageDataSource
import com.dfinn.wallet.runtime.storage.source.observeNonNull
import com.dfinn.wallet.runtime.storage.source.query.wrapSingleArgumentKeys
import com.dfinn.wallet.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageOrNull
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.math.BigInteger

class StakingRepositoryImpl(
    private val accountStakingDao: AccountStakingDao,
    private val remoteStorage: StorageDataSource,
    private val localStorage: StorageDataSource,
    private val walletConstants: WalletConstants,
    private val chainRegistry: ChainRegistry,
    private val stakingStoriesDataSource: StakingStoriesDataSource,
) : StakingRepository {

    override suspend fun sessionLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.babe().numberConstant("EpochDuration", runtime) // How many blocks per session
    }

    override suspend fun currentSessionIndex(chainId: ChainId) = remoteStorage.queryNonNull(
        // Current session index
        keyBuilder = { it.metadata.session().storage("CurrentIndex").storageKey() },
        binding = ::bindCurrentIndex,
        chainId = chainId
    )

    override suspend fun currentSlot(chainId: ChainId) = remoteStorage.queryNonNull(
        keyBuilder = { it.metadata.babe().storage("CurrentSlot").storageKey() },
        binding = ::bindCurrentSlot,
        chainId = chainId
    )

    override suspend fun genesisSlot(chainId: ChainId) = remoteStorage.queryNonNull(
        keyBuilder = { it.metadata.babe().storage("GenesisSlot").storageKey() },
        binding = ::bindCurrentSlot,
        chainId = chainId
    )

    override suspend fun eraStartSessionIndex(chainId: ChainId, currentEra: BigInteger): EraIndex {
        val runtime = runtimeFor(chainId)
        return remoteStorage.queryNonNull( // Index of session from with the era started
            keyBuilder = { it.metadata.staking().storage("ErasStartSessionIndex").storageKey(runtime, currentEra) },
            binding = ::bindErasStartSessionIndex,
            chainId = chainId
        )
    }

    override suspend fun eraLength(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.staking().numberConstant("SessionsPerEra", runtime) // How many sessions per era
    }

    override suspend fun blockCreationTime(chainId: ChainId): BigInteger {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.babe().numberConstant("ExpectedBlockTime", runtime)
    }

    override fun stakingAvailableFlow(chainId: ChainId): Flow<Boolean> {
        return chainRegistry.getRuntimeProvider(chainId).observe().map { it.metadata.hasModule(Modules.STAKING) }
    }

    override suspend fun getTotalIssuance(chainId: ChainId): BigInteger = localStorage.queryNonNull(
        keyBuilder = { it.metadata.balances().storage("TotalIssuance").storageKey() },
        binding = ::bindTotalInsurance,
        chainId = chainId
    )

    override suspend fun getActiveEraIndex(chainId: ChainId): EraIndex = localStorage.queryNonNull(
        keyBuilder = { it.metadata.activeEraStorageKey() },
        binding = ::bindActiveEra,
        chainId = chainId
    )

    override suspend fun getCurrentEraIndex(chainId: ChainId): EraIndex = localStorage.queryNonNull(
        keyBuilder = { it.metadata.staking().storage("CurrentEra").storageKey() },
        binding = ::bindCurrentEra,
        chainId = chainId
    )

    override suspend fun getHistoryDepth(chainId: ChainId): BigInteger = localStorage.queryNonNull(
        keyBuilder = { it.metadata.staking().storage("HistoryDepth").storageKey() },
        binding = ::bindHistoryDepth,
        chainId = chainId
    )

    override fun observeActiveEraIndex(chainId: String) = localStorage.observeNonNull(
        chainId = chainId,
        keyBuilder = { it.metadata.activeEraStorageKey() },
        binding = { scale, runtime -> bindActiveEra(scale, runtime) }
    )

    override fun electedExposuresInActiveEra(chainId: ChainId) = observeActiveEraIndex(chainId).mapLatest {
        getElectedValidatorsExposure(chainId, it)
    }

    override suspend fun getElectedValidatorsExposure(chainId: ChainId, eraIndex: EraIndex) = localStorage.query(chainId) {
        runtime.metadata.staking().storage("ErasStakers").entries(
            eraIndex,
            keyExtractor = { (_: BigInteger, accountId: ByteArray) -> accountId.toHexString() },
            binding = { scale, _ -> bindExposure(scale!!, runtime) }
        )
    }

    override suspend fun getValidatorPrefs(
        chainId: ChainId,
        accountIdsHex: List<String>,
    ): AccountIdMap<ValidatorPrefs?> {
        return remoteStorage.query(chainId) {
            runtime.metadata.staking().storage("Validators").entries(
                keysArguments = accountIdsHex.map(String::fromHex).wrapSingleArgumentKeys(),
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { scale, _ -> scale?.let { bindValidatorPrefs(scale, runtime) } }
            )
        }
    }

    override suspend fun getSlashes(chainId: ChainId, accountIdsHex: List<String>): AccountIdMap<Boolean> = withContext(Dispatchers.Default) {
        remoteStorage.query(chainId) {
            val storage = runtime.metadata.staking().storage("SlashingSpans")
            val returnType = storage.type.value!!

            val activeEraIndex = getActiveEraIndex(chainId)

            val slashDeferDurationConstant = runtime.metadata.staking().constant("SlashDeferDuration")
            val slashDeferDuration = bindSlashDeferDuration(slashDeferDurationConstant, runtime)

            runtime.metadata.staking().storage("SlashingSpans").entries(
                keysArguments = accountIdsHex.map(String::fromHex).wrapSingleArgumentKeys(),
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { scale, _ ->
                    val span = scale?.let { bindSlashingSpans(it, runtime, returnType) }

                    isSlashed(span, activeEraIndex, slashDeferDuration)
                }
            )
        }
    }

    override suspend fun getSlashingSpan(chainId: ChainId, accountId: AccountId): SlashingSpans? {
        return remoteStorage.query(
            keyBuilder = { it.metadata.staking().storage("SlashingSpans").storageKey(it, accountId) },
            binding = { scale, runtimeSnapshot -> scale?.let { bindSlashingSpans(it, runtimeSnapshot) } },
            chainId = chainId
        )
    }

    override fun stakingStateFlow(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): Flow<StakingState> {
        return accountStakingDao.observeDistinct(chain.id, chainAsset.id, accountId)
            .flatMapLatest { accountStaking ->
                val accessInfo = accountStaking.stakingAccessInfo

                if (accessInfo == null) {
                    flowOf(StakingState.NonStash(chain, accountStaking.accountId))
                } else {
                    observeStashState(chain, accessInfo, accountId)
                }
            }
    }

    override suspend fun getRewardDestination(stakingState: StakingState.Stash) = localStorage.queryNonNull(
        keyBuilder = { it.metadata.staking().storage("Payee").storageKey(it, stakingState.stashId) },
        binding = { scale, runtime -> bindRewardDestination(scale, runtime, stakingState.stashId, stakingState.controllerId) },
        chainId = stakingState.chain.id
    )

    override suspend fun minimumNominatorBond(chainId: ChainId): BigInteger {
        val minBond = queryStorageIfExists(
            storageName = "MinNominatorBond",
            binder = ::bindMinBond,
            chainId = chainId
        ) ?: BigInteger.ZERO

        val existentialDeposit = walletConstants.existentialDeposit(chainId)

        return minBond.max(existentialDeposit)
    }

    override suspend fun maxNominators(chainId: ChainId): BigInteger? = queryStorageIfExists(
        storageName = "MaxNominatorsCount",
        binder = ::bindMaxNominators,
        chainId = chainId
    )

    override suspend fun nominatorsCount(chainId: ChainId): BigInteger? = queryStorageIfExists(
        storageName = "CounterForNominators",
        binder = ::bindNominatorsCount,
        chainId = chainId
    )

    private suspend fun <T> queryStorageIfExists(
        chainId: ChainId,
        storageName: String,
        binder: NonNullBinderWithType<T>,
    ): T? {
        val runtime = runtimeFor(chainId)

        return runtime.metadata.staking().storageOrNull(storageName)?.let { storageEntry ->
            localStorage.query(
                keyBuilder = { storageEntry.storageKey() },
                binding = { scale, _ -> scale?.let { binder(scale, runtime, storageEntry.returnType()) } },
                chainId = chainId
            )
        }
    }

    override fun stakingStoriesFlow(): Flow<List<StakingStory>> {
        return stakingStoriesDataSource.getStoriesFlow()
    }

    override suspend fun ledgerFlow(stakingState: StakingState.Stash): Flow<StakingLedger> {
        return localStorage.observe(
            keyBuilder = { it.metadata.staking().storage("Ledger").storageKey(it, stakingState.controllerId) },
            binder = { scale, runtime -> scale?.let { bindStakingLedger(it, runtime) } },
            chainId = stakingState.chain.id
        ).filterNotNull()
    }

    override suspend fun ledger(chainId: ChainId, address: String) = remoteStorage.query(
        keyBuilder = { it.metadata.staking().storage("Ledger").storageKey(it, address.toAccountId()) },
        binding = { scale, runtime -> scale?.let { bindStakingLedger(it, runtime) } },
        chainId = chainId
    )

    private fun observeStashState(
        chain: Chain,
        accessInfo: AccountStakingLocal.AccessInfo,
        accountId: AccountId,
    ): Flow<StakingState.Stash> {
        val stashId = accessInfo.stashId
        val controllerId = accessInfo.controllerId

        return combine(
            observeAccountNominations(chain.id, stashId),
            observeAccountValidatorPrefs(chain.id, stashId)
        ) { nominations, prefs ->
            when {
                prefs != null -> StakingState.Stash.Validator(
                    chain, accountId, controllerId, stashId, prefs
                )
                nominations != null -> StakingState.Stash.Nominator(
                    chain, accountId, controllerId, stashId, nominations
                )

                else -> StakingState.Stash.None(chain, accountId, controllerId, stashId)
            }
        }
    }

    private fun observeAccountValidatorPrefs(chainId: ChainId, stashId: AccountId): Flow<ValidatorPrefs?> {
        return localStorage.observe(
            chainId = chainId,
            keyBuilder = { it.metadata.staking().storage("Validators").storageKey(it, stashId) },
            binder = { scale, runtime ->
                scale?.let { bindValidatorPrefs(it, runtime) }
            }
        )
    }

    private fun observeAccountNominations(chainId: ChainId, stashId: AccountId): Flow<Nominations?> {
        return localStorage.observe(
            chainId = chainId,
            keyBuilder = { it.metadata.staking().storage("Nominators").storageKey(it, stashId) },
            binder = { scale, runtime -> scale?.let { bindNominations(it, runtime) } }
        )
    }

    private fun isSlashed(
        slashingSpans: SlashingSpans?,
        activeEraIndex: BigInteger,
        slashDeferDuration: BigInteger,
    ) = slashingSpans != null && activeEraIndex - slashingSpans.lastNonZeroSlash < slashDeferDuration

    private suspend fun runtimeFor(chainId: String) = chainRegistry.getRuntime(chainId)
}
