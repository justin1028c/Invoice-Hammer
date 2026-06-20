package com.fordham.toolbelt.util

/**
 * Localizes Foreman spoken messages and step labels for Spanish-preferred devices.
 */
object ForemanMessageLocalizer {
    fun localize(raw: String): String {
        if (AppLocale.fromSystem() != AppLocale.Spanish) return raw
        EXACT[raw]?.let { return it }
        for ((regex, translator) in PATTERNS) {
            val match = regex.find(raw)
            if (match != null) return translator(match)
        }
        return FailureMessageLocalizer.localize(raw)
    }

    private val EXACT = mapOf(
        "Done." to "Listo.",
        "Opened invoice PDF." to "Se abrió el PDF de la factura.",
        "Step complete" to "Paso completado",
        "Started draft invoice" to "Borrador de factura iniciado",
        "Approve sending this invoice by email?" to "¿Aprueba enviar esta factura por correo?",
        "Approve sending this invoice by text message?" to "¿Aprueba enviar esta factura por mensaje de texto?",
        "Approve sending the latest invoice?" to "¿Aprueba enviar la factura más reciente?",
        "Approve deleting this invoice?" to "¿Aprueba eliminar esta factura?",
        "This action needs your approval." to "Esta acción requiere su aprobación."
    )

    private val PATTERNS: List<Pair<Regex, (MatchResult) -> String>> = listOf(
        Regex("^Invoice saved for (.+)\\.$") to { m ->
            "Factura guardada para ${m.groupValues[1]}."
        },
        Regex("^Created (.+) and saved the invoice\\.$") to { m ->
            "Se creó ${m.groupValues[1]} y se guardó la factura."
        },
        Regex("^Invoiced (\\d+) receipt\\(s\\) for (.+)\\.$") to { m ->
            "Se facturaron ${m.groupValues[1]} recibo(s) para ${m.groupValues[2]}."
        },
        Regex("^Opened (.+)\\.$") to { m ->
            "Se abrió ${AppTabLabels.localizedNavLabel(m.groupValues[1])}."
        },
        Regex("^Loaded last invoice for (.+) with (\\d+) line\\(s\\)\\. Edit on New Invoice\\.$") to { m ->
            "Se cargó la última factura de ${m.groupValues[1]} con ${m.groupValues[2]} partida(s). Edite en Nueva factura."
        },
        Regex("^Draft ready for (.+) with (\\d+) line\\(s\\)\\.$") to { m ->
            "Borrador listo para ${m.groupValues[1]} con ${m.groupValues[2]} partida(s)."
        },
        Regex("^Saved invoice for (.+)$") to { m ->
            "Factura guardada para ${m.groupValues[1]}"
        },
        Regex("^Created client (.+)$") to { m ->
            "Cliente creado ${m.groupValues[1]}"
        },
        Regex("^Selected (.+)$") to { m ->
            "Seleccionado ${m.groupValues[1]}"
        },
        Regex("^Updated draft \\((\\d+) lines\\)$") to { m ->
            "Borrador actualizado (${m.groupValues[1]} partidas)"
        },
        Regex("^Created (.+) and saved invoice$") to { m ->
            "Se creó ${m.groupValues[1]} y se guardó la factura"
        },
        Regex("^Duplicated last invoice for (.+)$") to { m ->
            "Se duplicó la última factura de ${m.groupValues[1]}"
        },
        Regex("^Opened (.+)$") to { m ->
            "Se abrió ${AppTabLabels.localizedNavLabel(m.groupValues[1])}"
        },
        Regex("^Searched clients — none found$") to { _ ->
            "Clientes buscados — ninguno encontrado"
        },
        Regex("^Searched clients — (\\d+) match\\(es\\)$") to { m ->
            "Clientes buscados — ${m.groupValues[1]} coincidencia(s)"
        },
        Regex("^Failed: (.+)$") to { m ->
            "Error: ${FailureMessageLocalizer.localize(m.groupValues[1])}"
        },
        Regex("^(.+) Tell me what to change or try a different action\\.$") to { m ->
            "${FailureMessageLocalizer.localize(m.groupValues[1])} Dígame qué cambiar o pruebe otra acción."
        },
        Regex("^Approve saving (.+) invoice for (.+)\\?$") to { m ->
            "¿Aprueba guardar la factura de ${m.groupValues[1]} para ${m.groupValues[2]}?"
        },
        Regex("^Approve creating client and saving (.+) invoice for (.+)\\?$") to { m ->
            "¿Aprueba crear el cliente y guardar la factura de ${m.groupValues[1]} para ${m.groupValues[2]}?"
        }
    )
}
