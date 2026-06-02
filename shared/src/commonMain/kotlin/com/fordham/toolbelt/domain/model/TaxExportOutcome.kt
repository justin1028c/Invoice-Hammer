package com.fordham.toolbelt.domain.model

sealed interface TaxExportOutcome {
    /**
     * @param path Shareable file path for [PlatformActions.shareFile].
     * @param savedTo User-visible location when published to Documents/InvoiceHammer (e.g. Reports or TaxBundles).
     */
    data class Success(
        val path: String,
        val savedTo: String? = null
    ) : TaxExportOutcome
    data class Failure(val error: FailureMessage) : TaxExportOutcome
    data object Loading : TaxExportOutcome
}
