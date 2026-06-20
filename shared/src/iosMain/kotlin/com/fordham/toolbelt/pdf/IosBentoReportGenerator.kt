package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
import com.fordham.toolbelt.util.UserFacingCopy
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.drawAtPoint
import platform.UIKit.systemFontOfSize
import platform.UIKit.boldSystemFontOfSize
import platform.UIKit.setFill
import platform.UIKit.setStroke

@OptIn(ExperimentalForeignApi::class)
class IosBentoReportGenerator : BentoReportGenerator {
    override fun generate(data: BentoReportData, outputPath: String): Boolean {
        val parent = outputPath.substringBeforeLast('/', missingDelimiterValue = "")
        if (parent.isNotEmpty() && parent != outputPath) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                parent,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }

        val format = UIGraphicsPDFRendererFormat()
        val pageRect = CGRectMake(0.0, 0.0, 595.0, 842.0)
        val renderer = UIGraphicsPDFRenderer(bounds = pageRect, format = format)
        val pdfData = renderer.PDFDataWithActions { context ->
            context.beginPage()

            // Draw solid white background
            UIColor.whiteColor.setFill()
            val bgPath = platform.UIKit.UIBezierPath.bezierPathWithRect(pageRect)
            bgPath.fill()

            // Header Background (Slate-800)
            val headerRect = CGRectMake(45.0, 40.0, 505.0, 70.0)
            colorFromHex("#1E293B").setFill()
            val headerPath = platform.UIKit.UIBezierPath.bezierPathWithRoundedRect(headerRect, cornerRadius = 8.0)
            headerPath.fill()

            val bento = UserFacingCopy.Bento
            val dateStr = com.fordham.toolbelt.util.DateTimeUtil.getNowFormatted()

            // Header Text - Left
            val titleLabelAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(10.0),
                platform.UIKit.NSForegroundColorAttributeName to UIColor.whiteColor
            )
            (UserFacingCopy.Pdf.defaultBusinessName() as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(60.0, 50.0),
                withAttributes = titleLabelAttrs
            )

            val titleAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(18.0),
                platform.UIKit.NSForegroundColorAttributeName to UIColor.whiteColor
            )
            (bento.reportTitle() as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(60.0, 72.0),
                withAttributes = titleAttrs
            )

            // Header Text - Right
            val rightLabelAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(8.0),
                platform.UIKit.NSForegroundColorAttributeName to colorFromHex("#94A3B8")
            )
            (bento.financialYearYtd() as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(400.0, 50.0),
                withAttributes = rightLabelAttrs
            )

            val dateAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(10.0),
                platform.UIKit.NSForegroundColorAttributeName to UIColor.whiteColor
            )
            (bento.generated(dateStr) as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(400.0, 70.0),
                withAttributes = dateAttrs
            )

            // Calculations
            val marginPercent = if (data.grossIncome > 0.0) {
                ((data.netProfit / data.grossIncome) * 100).toInt()
            } else {
                0
            }

            // Card 1: Net Profit (Large)
            drawCard(
                left = 45.0, top = 130.0, width = 240.0, height = 120.0,
                bgColor = colorFromHex("#DCFCE7"), borderColor = colorFromHex("#86EFAC"),
                title = bento.netProfitYtd(), titleColor = colorFromHex("#166534"),
                value = formatCurrency(data.netProfit), valueColor = colorFromHex("#15803D"), valueSize = 20.0,
                subtext = bento.profitableStatus(), subtextColor = colorFromHex("#15803D")
            )

            // Card 2: Gross Income
            drawCard(
                left = 310.0, top = 130.0, width = 240.0, height = 55.0,
                bgColor = colorFromHex("#DBEAFE"), borderColor = colorFromHex("#BFDBFE"),
                title = bento.grossIncome(), titleColor = colorFromHex("#1E3A8A"),
                value = formatCurrency(data.grossIncome), valueColor = colorFromHex("#1D4ED8"), valueSize = 14.0,
                subtext = null, subtextColor = null
            )

            // Card 3: Margin
            drawCard(
                left = 310.0, top = 195.0, width = 240.0, height = 55.0,
                bgColor = colorFromHex("#F3E8FF"), borderColor = colorFromHex("#E9D5FF"),
                title = bento.profitMargin(), titleColor = colorFromHex("#581C87"),
                value = "$marginPercent%", valueColor = colorFromHex("#7E22CE"), valueSize = 14.0,
                subtext = null, subtextColor = null
            )

            // Card 4: Total Expenses
            drawCard(
                left = 45.0, top = 265.0, width = 240.0, height = 70.0,
                bgColor = colorFromHex("#FEE2E2"), borderColor = colorFromHex("#FCA5A5"),
                title = bento.totalExpenses(), titleColor = colorFromHex("#991B1B"),
                value = formatCurrency(data.expenses), valueColor = colorFromHex("#B91C1C"), valueSize = 14.0,
                subtext = null, subtextColor = null
            )

            // Card 5: Operations Summary
            drawCard(
                left = 310.0, top = 265.0, width = 240.0, height = 70.0,
                bgColor = colorFromHex("#F8FAFC"), borderColor = colorFromHex("#E2E8F0"),
                title = bento.operationsSummary(), titleColor = colorFromHex("#475569"),
                value = bento.operationsValue(data.invoices.size, data.receiptCount), valueColor = colorFromHex("#1E293B"), valueSize = 12.0,
                subtext = bento.cleanAuditTrail(), subtextColor = colorFromHex("#64748B")
            )

            // Recent Invoices Table Header
            val tableTitleAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(11.0),
                platform.UIKit.NSForegroundColorAttributeName to colorFromHex("#334155")
            )
            (bento.recentPaidInvoices() as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(45.0, 355.0),
                withAttributes = tableTitleAttrs
            )

            // Draw Table Header background
            val tableHeaderRect = CGRectMake(45.0, 375.0, 505.0, 20.0)
            colorFromHex("#F1F5F9").setFill()
            val tableHeaderPath = platform.UIKit.UIBezierPath.bezierPathWithRect(tableHeaderRect)
            tableHeaderPath.fill()

            val tableHeaderAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(9.0),
                platform.UIKit.NSForegroundColorAttributeName to colorFromHex("#475569")
            )
            (bento.tableDate() as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(55.0, 380.0), withAttributes = tableHeaderAttrs)
            (bento.tableClient() as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(140.0, 380.0), withAttributes = tableHeaderAttrs)
            (bento.tableDescription() as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(260.0, 380.0), withAttributes = tableHeaderAttrs)
            (bento.tableAmount() as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(480.0, 380.0), withAttributes = tableHeaderAttrs)

            // Table Rows
            var y = 405.0
            val rowTextAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(9.0),
                platform.UIKit.NSForegroundColorAttributeName to colorFromHex("#334155")
            )

            val sortedInvoices = data.invoices.sortedByDescending { it.date }
            val maxRows = 15
            val displayInvoices = sortedInvoices.take(maxRows)

            for (invoice in displayInvoices) {
                (invoice.date.take(10) as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(55.0, y), withAttributes = rowTextAttrs)
                
                val clientDisplay = if (invoice.clientName.length > 18) invoice.clientName.take(16) + ".." else invoice.clientName
                (clientDisplay as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(140.0, y), withAttributes = rowTextAttrs)
                
                val summaryDisplay = if (invoice.itemsSummary.length > 40) invoice.itemsSummary.take(37) + "..." else invoice.itemsSummary
                (summaryDisplay as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(260.0, y), withAttributes = rowTextAttrs)
                
                (formatCurrency(invoice.totalAmount) as NSString).drawAtPoint(platform.CoreGraphics.CGPointMake(480.0, y), withAttributes = rowTextAttrs)

                // Separator line
                val linePath = platform.UIKit.UIBezierPath()
                linePath.moveToPoint(platform.CoreGraphics.CGPointMake(45.0, y + 16.0))
                linePath.addLineToPoint(platform.CoreGraphics.CGPointMake(550.0, y + 16.0))
                colorFromHex("#F1F5F9").setStroke()
                linePath.lineWidth = 1.0
                linePath.stroke()

                y += 24.0
            }

            if (sortedInvoices.size > maxRows) {
                val footerAttrs = mapOf<Any?, Any?>(
                    platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(8.0),
                    platform.UIKit.NSForegroundColorAttributeName to colorFromHex("#64748B")
                )
                (bento.morePaidInvoices(sortedInvoices.size - maxRows) as NSString).drawAtPoint(
                    platform.CoreGraphics.CGPointMake(45.0, y + 10.0),
                    withAttributes = footerAttrs
                )
            }
        }

        return pdfData.writeToFile(outputPath, atomically = true)
    }

    private fun drawCard(
        left: Double,
        top: Double,
        width: Double,
        height: Double,
        bgColor: UIColor,
        borderColor: UIColor,
        title: String,
        titleColor: UIColor,
        value: String,
        valueColor: UIColor,
        valueSize: Double,
        subtext: String?,
        subtextColor: UIColor?
    ) {
        val rect = CGRectMake(left, top, width, height)
        bgColor.setFill()
        val path = platform.UIKit.UIBezierPath.bezierPathWithRoundedRect(rect, cornerRadius = 8.0)
        path.fill()

        borderColor.setStroke()
        path.lineWidth = 1.0
        path.stroke()

        val titleAttrs = mapOf<Any?, Any?>(
            platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(8.0),
            platform.UIKit.NSForegroundColorAttributeName to titleColor
        )
        (title as NSString).drawAtPoint(
            platform.CoreGraphics.CGPointMake(left + 15.0, top + 12.0),
            withAttributes = titleAttrs
        )

        val valueAttrs = mapOf<Any?, Any?>(
            platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(valueSize),
            platform.UIKit.NSForegroundColorAttributeName to valueColor
        )
        val isDoubleRow = height > 60.0
        val valY = if (isDoubleRow) top + 42.0 else top + 28.0
        (value as NSString).drawAtPoint(
            platform.CoreGraphics.CGPointMake(left + 15.0, valY),
            withAttributes = valueAttrs
        )

        if (subtext != null && subtextColor != null && isDoubleRow) {
            val subAttrs = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(8.0),
                platform.UIKit.NSForegroundColorAttributeName to subtextColor
            )
            (subtext as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(left + 15.0, top + height - 20.0),
                withAttributes = subAttrs
            )
        }
    }

    private fun colorFromHex(hex: String): UIColor {
        val cleanHex = hex.removePrefix("#")
        val rgb = cleanHex.toLong(16)
        val r = ((rgb shr 16) and 0xFF).toDouble() / 255.0
        val g = ((rgb shr 8) and 0xFF).toDouble() / 255.0
        val b = (rgb and 0xFF).toDouble() / 255.0
        return UIColor.colorWithRed(r, green = g, blue = b, alpha = 1.0)
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = platform.Foundation.NSNumberFormatter().apply {
            numberStyle = platform.Foundation.NSNumberFormatterCurrencyStyle
            locale = platform.Foundation.NSLocale.localeWithLocaleIdentifier("en_US")
        }
        return formatter.stringFromNumber(platform.Foundation.NSNumber(amount)) ?: "$${amount}"
    }
}

