package com.fordham.toolbelt.domain.model

/**
 * Where an artifact lives on a user's device once produced by the app.
 *
 * The split between [shareablePath] and [userVisiblePath] is needed because some platforms
 * (Android API 29+ scoped storage) only expose user-visible Documents files through
 * MediaStore content URIs, while existing share/print flows depend on a path that can
 * be granted via the app's FileProvider. On iOS both fields point at the same sandboxed
 * Documents path.
 */
data class DocumentLocation(
    /** Path the app keeps and feeds into share/print/email flows. Always reachable by the app. */
    val shareablePath: String,
    /** Human-friendly path users will recognise on their device (e.g. `Documents/InvoiceHammer/Invoices/...`). */
    val userVisiblePath: String
)

sealed interface DocumentExportOutcome {
    data class Success(val location: DocumentLocation) : DocumentExportOutcome
    data class Failure(val error: FailureMessage) : DocumentExportOutcome
}

/**
 * The user-recognisable buckets we present under `Documents/InvoiceHammer/` on every platform.
 * Repository/use case code talks only in [DocumentCategory]; the actual filesystem layout is
 * decided by each platform's [com.fordham.toolbelt.domain.repository.DocumentExporter].
 */
enum class DocumentCategory(val subdirectoryName: String) {
    Invoices("Invoices"),
    Reports("Reports"),
    TaxBundles("TaxBundles")
}
