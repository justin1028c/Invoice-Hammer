package com.fordham.toolbelt.pdf

import com.fordham.toolbelt.domain.model.BentoReportData
import com.fordham.toolbelt.domain.repository.BentoReportGenerator
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
            val titleAttributes = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.boldSystemFontOfSize(24.0),
                platform.UIKit.NSForegroundColorAttributeName to UIColor.blackColor
            )
            val bodyAttributes = mapOf<Any?, Any?>(
                platform.UIKit.NSFontAttributeName to UIFont.systemFontOfSize(14.0),
                platform.UIKit.NSForegroundColorAttributeName to UIColor.blackColor
            )

            ("Bento Business Report" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 50.0),
                withAttributes = titleAttributes
            )
            ("Gross Income: ${data.grossIncome}" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 100.0),
                withAttributes = bodyAttributes
            )
            ("Expenses: ${data.expenses}" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 120.0),
                withAttributes = bodyAttributes
            )
            ("Net Profit: ${data.netProfit}" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 140.0),
                withAttributes = bodyAttributes
            )
            ("Paid invoices: ${data.invoices.size}" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 160.0),
                withAttributes = bodyAttributes
            )
            ("Receipts tracked: ${data.receiptCount}" as NSString).drawAtPoint(
                platform.CoreGraphics.CGPointMake(50.0, 180.0),
                withAttributes = bodyAttributes
            )
        }

        return pdfData.writeToFile(outputPath, atomically = true)
    }
}
