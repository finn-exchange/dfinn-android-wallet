package com.dfinn.wallet.feature_account_impl.domain.account.export.json.validations

import com.dfinn.wallet.common.validation.Validation
import com.dfinn.wallet.common.validation.ValidationSystem

typealias ExportJsonPasswordValidationSystem = ValidationSystem<ExportJsonPasswordValidationPayload, ExportJsonPasswordValidationFailure>
typealias ExportJsonPasswordValidation = Validation<ExportJsonPasswordValidationPayload, ExportJsonPasswordValidationFailure>
