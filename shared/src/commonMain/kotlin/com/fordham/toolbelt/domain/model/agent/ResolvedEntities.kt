package com.fordham.toolbelt.domain.model.agent

import com.fordham.toolbelt.domain.model.ClientId
import com.fordham.toolbelt.domain.model.InvoiceId
import com.fordham.toolbelt.domain.model.ReceiptId

sealed interface ResolvedEntityId

data class ResolvedClient(
    val id: ClientId
) : ResolvedEntityId

data class ResolvedInvoice(
    val id: InvoiceId
) : ResolvedEntityId

data class ResolvedReceipt(
    val id: ReceiptId
) : ResolvedEntityId

data class ResolvedEntityAlias(
    val alias: NaturalLanguage,
    val entity: ResolvedEntityId
)

class ResolvedEntities private constructor(
    private val aliases: Map<NaturalLanguage, ResolvedEntityId>
) {
    fun resolve(alias: NaturalLanguage): ResolvedEntityId? {
        return aliases[alias]
    }

    fun remember(alias: NaturalLanguage, entity: ResolvedEntityId): ResolvedEntities {
        return ResolvedEntities(aliases + (alias to entity))
    }

    fun forget(alias: NaturalLanguage): ResolvedEntities {
        return ResolvedEntities(aliases - alias)
    }

    fun entries(): List<ResolvedEntityAlias> {
        return aliases.map { (alias, entity) ->
            ResolvedEntityAlias(alias = alias, entity = entity)
        }
    }

    companion object {
        fun empty(): ResolvedEntities = ResolvedEntities(emptyMap())
    }
}
