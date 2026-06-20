package com.fordham.toolbelt.util

/**
 * Maps canonical English [FailureMessage] strings to Spanish when the device locale is Spanish.
 * Unknown messages pass through unchanged so English users see no difference.
 */
object FailureMessageLocalizer {
    fun localize(raw: String): String {
        if (AppLocale.fromSystem() != AppLocale.Spanish) return raw
        EXACT[raw]?.let { return it }
        for ((prefix, translator) in PREFIXES) {
            if (raw.startsWith(prefix)) return translator(raw)
        }
        for ((regex, translator) in REGEX) {
            val match = regex.find(raw)
            if (match != null) return translator(match)
        }
        return raw
    }

    private val EXACT = mapOf(
        "Pro subscription required for invoice AI parsing." to
            "Se requiere suscripción Pro para el análisis de facturas con IA.",
        "Pro subscription required." to "Se requiere suscripción Pro.",
        "Invoice must have a positive total before charging." to
            "La factura debe tener un total positivo antes de cobrar.",
        "PowerPay is not configured (baseUrl + appId required)." to
            "PowerPay no está configurado (se requiere baseUrl + appId).",
        "PowerPay is not configured." to "PowerPay no está configurado.",
        "PowerPay signing secret is not configured." to
            "El secreto de firma de PowerPay no está configurado.",
        "Missing PowerPay webhook signature headers." to
            "Faltan encabezados de firma del webhook de PowerPay.",
        "Invalid x-powerpay-timestamp header." to
            "Encabezado x-powerpay-timestamp no válido.",
        "Webhook timestamp is too old (replay protection)." to
            "La marca de tiempo del webhook es demasiado antigua (protección contra repetición).",
        "Webhook signature mismatch." to "La firma del webhook no coincide.",
        "Webhook event id header does not match payload." to
            "El encabezado de id del evento del webhook no coincide con la carga útil.",
        "Agent command cannot be blank." to "El comando del agente no puede estar vacío.",
        "Unexpected outcome from LLM gateway." to
            "Resultado inesperado del gateway LLM.",
        "Biometric authentication failed or was cancelled." to
            "La autenticación biométrica falló o se canceló.",
        "Tool arguments do not match requested tool." to
            "Los argumentos de la herramienta no coinciden con la herramienta solicitada.",
        "Foreman needs Pro or Hammer credits. Open Settings → Subscription." to
            "Foreman requiere Pro o créditos Hammer. Abra Ajustes → Suscripción.",
        "Failed to generate PDF" to "No se pudo generar el PDF",
        "Unknown error saving invoice" to "Error desconocido al guardar la factura",
        "Foreman requires native function calling; no tools were registered." to
            "Foreman requiere llamadas a funciones nativas; no se registraron herramientas.",
        "Can't reach Foreman — add foreman.gemini.backend.url in local.properties." to
            "No se puede contactar a Foreman — agregue foreman.gemini.backend.url en local.properties.",
        "Foreman auth failed. Check foreman.backend.api.key matches Supabase." to
            "Error de autenticación de Foreman. Verifique que foreman.backend.api.key coincida con Supabase.",
        "Google Gemini is temporarily overloaded (503 Service Unavailable). Please try again in a few seconds." to
            "Google Gemini está temporalmente sobrecargado (503). Inténtelo de nuevo en unos segundos.",
        "Foreman is busy — try again in a minute." to
            "Foreman está ocupado — inténtelo de nuevo en un minuto.",
        "Can't reach Foreman — check your connection." to
            "No se puede contactar a Foreman — verifique su conexión.",
        "Foreman returned no tool call." to "Foreman no devolvió una llamada a herramienta.",
        "Foreman tools are unavailable. Restart the app and try again." to
            "Las herramientas de Foreman no están disponibles. Reinicie la app e inténtelo de nuevo.",
        "Enter a valid card number." to "Ingrese un número de tarjeta válido.",
        "Card number failed verification." to "El número de tarjeta no pasó la verificación.",
        "Expiry must be MM/YY and not in the past." to
            "La fecha de vencimiento debe ser MM/AA y no estar vencida.",
        "Enter the cardholder name." to "Ingrese el nombre del titular de la tarjeta.",
        "No items could be extracted from this receipt. Please ensure the image is clear and contains readable text." to
            "No se pudieron extraer artículos de este recibo. Asegúrese de que la imagen sea clara y contenga texto legible.",
        "Pro subscription required for Bento reports." to
            "Se requiere suscripción Pro para informes Bento.",
        "Pro subscription required for tax bundles." to
            "Se requiere suscripción Pro para paquetes fiscales.",
        "Sign in to back up to Supabase." to "Inicie sesión para respaldar en Supabase.",
        "Supabase is not configured." to "Supabase no está configurado.",
        "Sign in to restore from Supabase." to "Inicie sesión para restaurar desde Supabase.",
        "No Supabase backup found for this account. Run SYNC NOW first." to
            "No se encontró respaldo de Supabase para esta cuenta. Ejecute SINCRONIZAR AHORA primero.",
        "No Drive backup found. Run SYNC NOW first." to
            "No se encontró respaldo en Drive. Ejecute SINCRONIZAR AHORA primero.",
        "Drive backup file ID missing." to "Falta el ID del archivo de respaldo en Drive.",
        "Could not remove saved logo." to "No se pudo eliminar el logo guardado.",
        "Could not save logo image." to "No se pudo guardar la imagen del logo.",
        "Logo saved locally but settings update failed." to
            "Logo guardado localmente pero falló la actualización de ajustes.",
        "Invoice must have a positive total before requesting payment." to
            "La factura debe tener un total positivo antes de solicitar el pago.",
        "Mock payment not found." to "Pago simulado no encontrado.",
        "Invalid terminal charge request." to "Solicitud de cobro en terminal no válida.",
        "Card declined (beta test)." to "Tarjeta rechazada (prueba beta).",
        "Add stripe.publishable.key and stripe.payment.backend.url to enable secure checkout." to
            "Agregue stripe.publishable.key y stripe.payment.backend.url para habilitar el pago seguro.",
        "Payment backend is not configured." to "El backend de pagos no está configurado.",
        "Stripe did not return a payment secret for on-site checkout." to
            "Stripe no devolvió un secreto de pago para el cobro en sitio.",
        "Configure Stripe keys and payment backend to enable Tap to Pay." to
            "Configure las claves de Stripe y el backend de pagos para habilitar Pago sin contacto.",
        "Stripe did not return a payment secret for Tap to Pay." to
            "Stripe no devolvió un secreto de pago para Pago sin contacto.",
        "Stripe Connect backend required for physical readers." to
            "Se requiere backend de Stripe Connect para lectores físicos.",
        "Stripe did not return a payment secret for the card reader." to
            "Stripe no devolvió un secreto de pago para el lector de tarjetas.",
        "Unsupported payment provider." to "Proveedor de pago no compatible.",
        "Stripe did not return a checkout link. Try again or use Card Terminal." to
            "Stripe no devolvió un enlace de pago. Inténtelo de nuevo o use Terminal de tarjeta.",
        "Subscription tier not found." to "Nivel de suscripción no encontrado.",
        "Stripe onboarding URL was empty." to "La URL de incorporación de Stripe estaba vacía.",
        "Client not found." to "Cliente no encontrado.",
        "Draft needs a client name before line items can be added." to
            "El borrador necesita un nombre de cliente antes de agregar partidas.",
        "Could not save job note." to "No se pudo guardar la nota del trabajo.",
        "Draft needs a client and at least one line item before saving." to
            "El borrador necesita un cliente y al menos una partida antes de guardar.",
        "Client name is required." to "El nombre del cliente es obligatorio.",
        "Could not create client." to "No se pudo crear el cliente.",
        "No receipt photo ready. Open Receipts and capture an image first." to
            "No hay foto de recibo lista. Abra Recibos y capture una imagen primero.",
        "Pro subscription required for receipt scan." to
            "Se requiere suscripción Pro para escanear recibos.",
        "Client not found. Say the client name or select a client first." to
            "Cliente no encontrado. Diga el nombre del cliente o seleccione uno primero.",
        "No line items to append." to "No hay partidas para agregar.",
        "Invoice not found." to "Factura no encontrada.",
        "Invoice has no PDF yet." to "La factura aún no tiene PDF.",
        "Delete failed." to "Error al eliminar.",
        "No saved invoices found to open." to "No se encontraron facturas guardadas para abrir.",
        "Supplier not found." to "Proveedor no encontrado.",
        "Client not found for details lookup." to
            "Cliente no encontrado para consulta de detalles.",
        "Client not found for receipt lookup." to
            "Cliente no encontrado para consulta de recibos.",
        "No invoice found to send. Save an invoice or provide invoiceId from COMPLETED STEPS." to
            "No se encontró factura para enviar. Guarde una factura o proporcione invoiceId de PASOS COMPLETADOS.",
        "Could not parse line items from AI response. Please try rephrasing your request." to
            "No se pudieron analizar las partidas de la respuesta de IA. Intente reformular su solicitud.",
        "Cryptographic purchase verification token is missing." to
            "Falta el token de verificación criptográfica de la compra."
    )

    private val PREFIXES: List<Pair<String, (String) -> String>> = listOf(
        "PowerPay events poll failed with HTTP " to { raw ->
            raw.replace(
                "PowerPay events poll failed with HTTP ",
                "Error al consultar eventos de PowerPay con HTTP "
            )
        },
        "PowerPay create payment failed with HTTP " to { raw ->
            raw.replace(
                "PowerPay create payment failed with HTTP ",
                "Error al crear pago de PowerPay con HTTP "
            )
        },
        "PowerPay status failed with HTTP " to { raw ->
            raw.replace(
                "PowerPay status failed with HTTP ",
                "Error de estado de PowerPay con HTTP "
            )
        },
        "PowerPay transaction history failed with HTTP " to { raw ->
            raw.replace(
                "PowerPay transaction history failed with HTTP ",
                "Error en historial de transacciones de PowerPay con HTTP "
            )
        },
        "PowerPay request failed with HTTP " to { raw ->
            raw.replace(
                "PowerPay request failed with HTTP ",
                "Solicitud de PowerPay falló con HTTP "
            )
        },
        "Supabase connection failed with HTTP " to { raw ->
            raw.replace(
                "Supabase connection failed with HTTP ",
                "Conexión a Supabase falló con HTTP "
            )
        },
        "Supabase tiers failed with HTTP " to { raw ->
            raw.replace(
                "Supabase tiers failed with HTTP ",
                "Niveles de Supabase fallaron con HTTP "
            )
        },
        "Supabase entitlement fetch failed with HTTP " to { raw ->
            raw.replace(
                "Supabase entitlement fetch failed with HTTP ",
                "Error al obtener derechos de Supabase con HTTP "
            )
        },
        "Drive sign-in required: " to { raw ->
            raw.replace("Drive sign-in required: ", "Se requiere inicio de sesión en Drive: ")
        },
        "Drive listing failed with HTTP " to { raw ->
            raw.replace(
                "Drive listing failed with HTTP ",
                "Error al listar Drive con HTTP "
            )
        },
        "Drive download failed with HTTP " to { raw ->
            raw.replace(
                "Drive download failed with HTTP ",
                "Error al descargar de Drive con HTTP "
            )
        },
        "Drive upload failed with HTTP " to { raw ->
            raw.replace(
                "Drive upload failed with HTTP ",
                "Error al subir a Drive con HTTP "
            )
        },
        "Error saving estimate: " to { raw ->
            raw.replace("Error saving estimate: ", "Error al guardar presupuesto: ")
        },
        "Error saving invoice: " to { raw ->
            raw.replace("Error saving invoice: ", "Error al guardar factura: ")
        },
        "Foreman returned an unknown tool: " to { raw ->
            raw.replace(
                "Foreman returned an unknown tool: ",
                "Foreman devolvió una herramienta desconocida: "
            )
        },
        "Enter a " to { raw ->
            if (raw.endsWith("-digit security code.")) {
                raw.replace("Enter a ", "Ingrese un código de seguridad de ")
                    .replace("-digit security code.", " dígitos.")
            } else raw
        },
        "Unknown tab: " to { raw ->
            raw.replace("Unknown tab: ", "Pestaña desconocida: ")
        },
        "Client not found: " to { raw ->
            raw.replace("Client not found: ", "Cliente no encontrado: ")
        },
        "No prior invoice for " to { raw ->
            raw.replace("No prior invoice for ", "No hay factura previa para ")
                .removeSuffix(".") + "."
        },
        "No unbilled receipts for " to { raw ->
            raw.replace("No unbilled receipts for ", "No hay recibos sin facturar para ")
                .removeSuffix(".") + "."
        },
        "Failed to load suppliers list: " to { raw ->
            raw.replace("Failed to load suppliers list: ", "Error al cargar la lista de proveedores: ")
        }
    )

    private val REGEX: List<Pair<Regex, (MatchResult) -> String>> = listOf(
        Regex("^(.+) is collected on-site — use Card Terminal or Tap to Pay from the picker\\.$") to { m ->
            "${m.groupValues[1]} se cobra en sitio — use Terminal de tarjeta o Pago sin contacto desde el selector."
        },
        Regex("^No (.+) product id configured for (.+)\\.$") to { m ->
            "No hay id de producto ${m.groupValues[1]} configurado para ${m.groupValues[2]}."
        }
    )
}
