package com.dfinn.wallet.feature_staking_impl.presentation.validators.change.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dfinn.wallet.common.base.BaseFragment
import com.dfinn.wallet.common.di.FeatureUtils
import com.dfinn.wallet.common.utils.setVisible
import com.dfinn.wallet.feature_staking_api.di.StakingFeatureApi
import com.dfinn.wallet.feature_staking_impl.R
import com.dfinn.wallet.feature_staking_impl.di.StakingFeatureComponent
import com.dfinn.wallet.feature_staking_impl.presentation.validators.ValidatorsAdapter
import com.dfinn.wallet.feature_staking_impl.presentation.validators.change.ValidatorModel
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsAccounts
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsContent
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsList
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsNext
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsProgress
import kotlinx.android.synthetic.main.fragment_recommended_validators.recommendedValidatorsToolbar

class RecommendedValidatorsFragment : BaseFragment<RecommendedValidatorsViewModel>(), ValidatorsAdapter.ItemHandler {

    lateinit var adapter: ValidatorsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommended_validators, container, false)
    }

    override fun initViews() {
        adapter = ValidatorsAdapter(this)
        recommendedValidatorsList.adapter = adapter

        recommendedValidatorsList.setHasFixedSize(true)

        recommendedValidatorsToolbar.setHomeButtonListener { viewModel.backClicked() }
        onBackPressed { viewModel.backClicked() }

        recommendedValidatorsNext.setOnClickListener {
            viewModel.nextClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(
            requireContext(),
            StakingFeatureApi::class.java
        )
            .recommendedValidatorsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: RecommendedValidatorsViewModel) {
        viewModel.recommendedValidatorModels.observe {
            adapter.submitList(it)

            recommendedValidatorsProgress.setVisible(false)
            recommendedValidatorsContent.setVisible(true)
        }

        viewModel.selectedTitle.observe(recommendedValidatorsAccounts::setText)
    }

    override fun validatorInfoClicked(validatorModel: ValidatorModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }

    override fun validatorClicked(validatorModel: ValidatorModel) {
        viewModel.validatorInfoClicked(validatorModel)
    }
}