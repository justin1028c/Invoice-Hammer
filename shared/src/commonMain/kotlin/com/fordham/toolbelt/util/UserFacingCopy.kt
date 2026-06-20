package com.fordham.toolbelt.util

/**
 * Locale-aware user-facing strings for PDFs, notifications, and platform prompts
 * outside Compose resources (shared module).
 */
object UserFacingCopy {
    private val spanish: Boolean
        get() = AppLocale.fromSystem() == AppLocale.Spanish

    object Pdf {
        fun defaultBusinessName(): String = "INVOICE HAMMER"

        fun defaultSlogan(): String =
            if (spanish) "Servicios profesionales de campo" else "Professional Field Services"

        fun billedTo(): String = if (spanish) "FACTURAR A" else "BILLED TO"

        fun noJobAddress(): String =
            if (spanish) "Sin dirección de trabajo" else "No Job Address Provided"

        fun documentDetails(): String =
            if (spanish) "DETALLES DEL DOCUMENTO" else "DOCUMENT DETAILS"

        fun estimate(): String = if (spanish) "PRESUPUESTO" else "ESTIMATE"

        fun invoice(): String = if (spanish) "FACTURA" else "INVOICE"

        fun datePrefix(date: String): String =
            if (spanish) "FECHA: $date" else "DATE: $date"

        fun statusEstimate(): String = estimate()

        fun statusPaid(): String = if (spanish) "PAGADO" else "PAID"

        fun statusPartial(): String = if (spanish) "PARCIAL" else "PARTIAL"

        fun statusDue(): String = if (spanish) "PENDIENTE" else "DUE"

        fun description(): String = if (spanish) "DESCRIPCIÓN" else "DESCRIPTION"

        fun qty(): String = if (spanish) "CANT." else "QTY"

        fun unitPrice(): String = if (spanish) "PRECIO UNIT." else "UNIT PRICE"

        fun total(): String = if (spanish) "TOTAL" else "TOTAL"

        fun subtotal(): String = if (spanish) "Subtotal" else "Subtotal"

        fun tax(rate: Double): String =
            if (spanish) "Impuesto ($rate%)" else "Tax ($rate%)"

        fun depositPaid(): String =
            if (spanish) "Depósito pagado" else "Deposit Paid"

        fun totalDue(): String =
            if (spanish) "TOTAL A PAGAR" else "TOTAL DUE"

        fun thankYou(): String =
            if (spanish) "¡GRACIAS POR SU CONFIANZA!" else "THANK YOU FOR YOUR BUSINESS!"

        fun estimateFilePrefix(): String =
            if (spanish) "Presupuesto" else "Estimate"

        fun invoiceFilePrefix(): String =
            if (spanish) "Factura" else "Invoice"

        fun galleryPageTitle(page: Int): String =
            if (spanish) "GALERÍA DEL LUGAR - PÁGINA $page" else "JOB SITE GALLERY - PAGE $page"

        fun beforeWorkCaption(): String =
            if (spanish) "[ ANTES DEL TRABAJO ]" else "[ BEFORE WORK ]"

        fun afterWorkCaption(): String =
            if (spanish) "[ DESPUÉS DEL TRABAJO ]" else "[ AFTER WORK ]"

        fun docNumber(isEstimate: Boolean, invoiceId: String): String {
            val type = if (isEstimate) estimate() else invoice()
            return "$type #${invoiceId.take(8).uppercase()}"
        }
    }

    object Bento {
        fun reportTitle(): String =
            if (spanish) "Informe Bento de negocio" else "Bento Business Report"

        fun financialYearYtd(): String =
            if (spanish) "AÑO FISCAL YTD" else "FINANCIAL YEAR YTD"

        fun generated(date: String): String =
            if (spanish) "Generado: $date" else "Generated: $date"

        fun netProfitYtd(): String =
            if (spanish) "BENEFICIO NETO YTD" else "NET PROFIT YTD"

        fun profitableStatus(): String =
            if (spanish) "Estado: Rentable YTD" else "Status: Profitable YTD"

        fun grossIncome(): String =
            if (spanish) "INGRESOS BRUTOS" else "GROSS INCOME"

        fun profitMargin(): String =
            if (spanish) "MARGEN DE BENEFICIO" else "PROFIT MARGIN"

        fun totalExpenses(): String =
            if (spanish) "GASTOS TOTALES" else "TOTAL EXPENSES"

        fun operationsSummary(): String =
            if (spanish) "RESUMEN DE OPERACIONES" else "OPERATIONS SUMMARY"

        fun operationsValue(invoiceCount: Int, receiptCount: Int): String =
            if (spanish) "$invoiceCount facturas | $receiptCount recibos"
            else "$invoiceCount Invoices | $receiptCount Receipts"

        fun cleanAuditTrail(): String =
            if (spanish) "Pista de auditoría activa" else "Clean audit trail active"

        fun recentPaidInvoices(): String =
            if (spanish) "FACTURAS PAGADAS RECIENTES" else "RECENT PAID INVOICES"

        fun tableDate(): String = if (spanish) "Fecha" else "Date"

        fun tableClient(): String = if (spanish) "Cliente" else "Client"

        fun tableDescription(): String = if (spanish) "Descripción" else "Description"

        fun tableAmount(): String = if (spanish) "Importe" else "Amount"

        fun morePaidInvoices(count: Int): String =
            if (spanish) "+ $count facturas pagadas más registradas YTD"
            else "+ $count more paid invoices recorded YTD"
    }

    object Notifications {
        fun reminderTitle(): String =
            if (spanish) "Recordatorio de facturas pendientes" else "Unpaid Invoices Reminder"

        fun reminderBody(): String =
            if (spanish) "Toque para revisar facturas pendientes en Invoice Hammer."
            else "Tap to review unpaid invoices in Invoice Hammer."

        fun paymentReminderTitle(clientName: String): String =
            if (spanish) "Recordatorio de pago: $clientName"
            else "Payment Reminder: $clientName"

        fun tapToReview(): String =
            if (spanish) "Toque para revisar facturas pendientes"
            else "Tap to review unpaid invoices"

        fun channelName(): String =
            if (spanish) "Facturas pendientes" else "Unpaid Invoices"

        fun channelDescription(): String =
            if (spanish) "Recordatorios de facturas pendientes de pago"
            else "Reminders for unpaid invoices"
    }

    object Common {
        fun cancel(): String = if (spanish) "Cancelar" else "Cancel"
    }

    object Platform {
        fun signInLauncherNotRegistered(): String =
            if (spanish) "Inicio de sesión no registrado en la actividad"
            else "Sign-in launcher not registered"

        fun noActiveActivity(): String =
            if (spanish) "No hay actividad activa"
            else "No active activity context"

        fun cryptographicAuthFailed(): String =
            if (spanish) "Error de autenticación criptográfica"
            else "Cryptographic authentication failed"

        fun biometricVerificationFailed(detail: String?): String =
            if (spanish) {
                detail?.takeIf { it.isNotBlank() }?.let { "Error de verificación biométrica: $it" }
                    ?: "Error de verificación biométrica"
            } else {
                detail?.takeIf { it.isNotBlank() }?.let { "Biometric verification failed: $it" }
                    ?: "Biometric verification failed"
            }

        fun cameraUnavailable(): String =
            if (spanish) "Cámara no disponible" else "Camera unavailable"

        fun cameraPermissionRequired(): String =
            if (spanish) "Se requiere permiso de cámara para fotografiar recibos"
            else "Camera permission is required to snap receipts"
    }

    object Llm {
        fun transcribeInstruction(): String =
            if (spanish) {
                "El audio está en español. Transcriba verbatim en español. " +
                    "Salida únicamente la transcripción literal. Sin descripciones ni comentarios. " +
                    "Si hay ruido de fondo, céntrese en la voz principal."
            } else {
                "The audio is in English. Transcribe verbatim in English. " +
                    "Output only the literal transcription. No descriptions, no comments, no meta-text. " +
                    "If there is background noise, focus solely on the primary speaker's voice."
            }
    }

    object ForemanApproval {
        fun sendEmail(): String =
            if (spanish) "¿Aprueba enviar esta factura por correo?"
            else "Approve sending this invoice by email?"

        fun sendSms(): String =
            if (spanish) "¿Aprueba enviar esta factura por mensaje de texto?"
            else "Approve sending this invoice by text message?"

        fun quickSend(): String =
            if (spanish) "¿Aprueba enviar la factura más reciente?"
            else "Approve sending the latest invoice?"

        fun deleteInvoice(): String =
            if (spanish) "¿Aprueba eliminar esta factura?"
            else "Approve deleting this invoice?"

        fun saveInvoice(total: String, clientName: String): String =
            if (spanish) "¿Aprueba guardar la factura de $total para $clientName?"
            else "Approve saving $total invoice for $clientName?"

        fun createClientAndSave(total: String, clientName: String): String =
            if (spanish) "¿Aprueba crear el cliente y guardar la factura de $total para $clientName?"
            else "Approve creating client and saving $total invoice for $clientName?"

        fun defaultPrompt(): String =
            if (spanish) "Esta acción requiere su aprobación."
            else "This action needs your approval."
    }
}
