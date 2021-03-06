package com.dfinn.wallet.feature_wallet_impl.data.network.blockchain.updaters.balance

import android.util.Log
import com.dfinn.wallet.common.utils.LOG_TAG
import com.dfinn.wallet.core.updater.SubscriptionBuilder
import com.dfinn.wallet.core.updater.Updater
import com.dfinn.wallet.core_db.dao.OperationDao
import com.dfinn.wallet.core_db.model.OperationLocal
import com.dfinn.wallet.feature_account_api.domain.model.accountIdIn
import com.dfinn.wallet.feature_account_api.domain.updaters.AccountUpdateScope
import com.dfinn.wallet.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import com.dfinn.wallet.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import com.dfinn.wallet.feature_wallet_api.data.network.blockhain.assets.history.AssetHistory
import com.dfinn.wallet.runtime.ext.addressOf
import com.dfinn.wallet.runtime.multiNetwork.chain.model.Chain
import com.dfinn.wallet.runtime.multiNetwork.runtime.repository.ExtrinsicStatus
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.*

class PaymentUpdaterFactory(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val scope: AccountUpdateScope,
) {

    fun create(chain: Chain): PaymentUpdater {
        return PaymentUpdater(
            operationDao = operationDao,
            assetSourceRegistry = assetSourceRegistry,
            scope = scope,
            chain = chain,
        )
    }
}

class PaymentUpdater(
    private val operationDao: OperationDao,
    private val assetSourceRegistry: AssetSourceRegistry,
    override val scope: AccountUpdateScope,
    private val chain: Chain,
) : Updater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val metaAccount = scope.getAccount()

        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        val assetSyncs = chain.assets.mapNotNull { chainAsset ->
            val assetSource = assetSourceRegistry.sourceFor(chainAsset)

            val assetUpdateFlow = runCatching {
                assetSource.balance.startSyncingBalance(chain, chainAsset, metaAccount, accountId, storageSubscriptionBuilder)
            }
                .onFailure { logSyncError(chain, chainAsset, error = it) }
                .getOrNull()

            assetUpdateFlow
                ?.filterNotNull()
                ?.catch { logSyncError(chain, chainAsset, error = it) }
                ?.onEach { Log.d(LOG_TAG, "Starting block fetching for ${chain.name}.${chainAsset.symbol}") }
                ?.onEach { blockHash -> assetSource.history.syncOperationsForBalanceChange(chainAsset, blockHash, accountId) }
        }

        val chainSyncingFlow = if (assetSyncs.size == 1) {
            // skip unnecessary flows merges
            assetSyncs.first()
        } else {
            assetSyncs.merge()
        }

        return chainSyncingFlow
            .noSideAffects()
    }

    private fun logSyncError(chain: Chain, chainAsset: Chain.Asset, error: Throwable) {
        Log.e(LOG_TAG, "Failed to sync balance for ${chainAsset.symbol} in ${chain.name}", error)
    }

    private suspend fun AssetHistory.syncOperationsForBalanceChange(chainAsset: Chain.Asset, blockHash: String, accountId: AccountId) {
        fetchOperationsForBalanceChange(chain, blockHash, accountId)
            .onSuccess { blockTransfers ->
                val localOperations = blockTransfers.map { transfer -> createTransferOperationLocal(chainAsset, transfer, accountId) }

                operationDao.insertAll(localOperations)
            }.onFailure {
                Log.e(LOG_TAG, "Failed to retrieve transactions from block (${chain.name}.${chainAsset.symbol}): ${it.message}")
            }
    }

    private suspend fun createTransferOperationLocal(
        chainAsset: Chain.Asset,
        extrinsic: TransferExtrinsic,
        accountId: ByteArray,
    ): OperationLocal {
        val localStatus = when (extrinsic.status) {
            ExtrinsicStatus.SUCCESS -> OperationLocal.Status.COMPLETED
            ExtrinsicStatus.FAILURE -> OperationLocal.Status.FAILED
            ExtrinsicStatus.UNKNOWN -> OperationLocal.Status.PENDING
        }

        val localCopy = operationDao.getOperation(extrinsic.hash)

        return OperationLocal.manualTransfer(
            hash = extrinsic.hash,
            chainId = chain.id,
            address = chain.addressOf(accountId),
            chainAssetId = chainAsset.id,
            amount = extrinsic.amountInPlanks,
            senderAddress = chain.addressOf(extrinsic.senderId),
            receiverAddress = chain.addressOf(extrinsic.recipientId),
            fee = localCopy?.fee,
            status = localStatus,
            source = OperationLocal.Source.BLOCKCHAIN,
        )
    }
}
