package com.dfinn.wallet.feature_crowdloan_impl.data.network.api.bifrost

class GetAccountByReferralCodeResponse(
    val getAccountByInvitationCode: GetAccountByReferralCode
) {

    class GetAccountByReferralCode(val account: String?)
}
