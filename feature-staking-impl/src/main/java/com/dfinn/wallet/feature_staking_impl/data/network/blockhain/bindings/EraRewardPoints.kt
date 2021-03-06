package com.dfinn.wallet.feature_staking_impl.data.network.blockhain.bindings

import com.dfinn.wallet.common.data.network.runtime.binding.*
import com.dfinn.wallet.common.utils.second
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import java.math.BigInteger

typealias RewardPoint = BigInteger

class EraRewardPoints(
    val totalPoints: RewardPoint,
    val individual: List<Individual>
) {
    class Individual(val accountId: AccountId, val rewardPoints: RewardPoint)
}

@UseCaseBinding
fun bindEraRewardPoints(
    scale: String?,
    runtime: RuntimeSnapshot,
    type: Type<*>,
): EraRewardPoints {
    val dynamicInstance = scale?.let { type.fromHexOrNull(runtime, it) }.cast<Struct.Instance>()

    return EraRewardPoints(
        totalPoints = bindRewardPoint(dynamicInstance["total"]),
        individual = dynamicInstance.getList("individual").map {
            requireType<List<*>>(it) // (AccountId, RewardPoint)

            EraRewardPoints.Individual(
                accountId = bindAccountId(it.first()),
                rewardPoints = bindRewardPoint(it.second())
            )
        }
    )
}

@HelperBinding
fun bindRewardPoint(dynamicInstance: Any?): RewardPoint = bindNumber(dynamicInstance)
