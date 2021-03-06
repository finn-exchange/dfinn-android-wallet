package com.dfinn.wallet.feature_crowdloan_impl.domain.contribute.custom.bifrost

import com.dfinn.wallet.common.data.network.HttpExceptionHandler
import com.dfinn.wallet.feature_crowdloan_api.data.network.blockhain.binding.ParaId
import com.dfinn.wallet.feature_crowdloan_impl.data.network.api.bifrost.BifrostApi
import com.dfinn.wallet.feature_crowdloan_impl.data.network.api.bifrost.getAccountByReferralCode
import com.dfinn.wallet.feature_crowdloan_impl.data.network.blockhain.extrinsic.addMemo
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder

class BifrostContributeInteractor(
    val novaReferralCode: String,
    private val bifrostApi: BifrostApi,
    private val httpExceptionHandler: HttpExceptionHandler,
) {

    suspend fun isCodeValid(code: String): Boolean {
        val response = httpExceptionHandler.wrap { bifrostApi.getAccountByReferralCode(code) }

        return response.data.getAccountByInvitationCode.account.isNullOrEmpty().not()
    }

    fun submitOnChain(
        paraId: ParaId,
        referralCode: String,
        extrinsicBuilder: ExtrinsicBuilder,
    ) = extrinsicBuilder.addMemo(paraId, referralCode)
}
