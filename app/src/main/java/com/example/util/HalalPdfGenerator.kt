package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentRecordEntity
import com.example.data.model.SaleEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HalalPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generateInvoicePdf(
        context: Context,
        sale: SaleEntity,
        customer: CustomerEntity?,
        previousOutstanding: Double
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.rgb(6, 78, 59) // Deep Emerald
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.rgb(75, 85, 99)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.rgb(17, 24, 39)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10.5f
            color = Color.rgb(17, 24, 39)
        }
        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.rgb(31, 41, 55)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(209, 213, 219)
            strokeWidth = 1f
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(243, 244, 246)
        }

        var y = 45f

        // Top Brand Banner
        canvas.drawText("HALAL CHICKEN SHOP HANTI", 40f, y, titlePaint)
        y += 16f
        canvas.drawText("Owner: Mr. Sajid | Hanti, Bihar, India | Phone: +91 9708099035", 40f, y, subTitlePaint)
        y += 14f
        canvas.drawText("Wholesale & Institutional Halal Poultry Supply", 40f, y, subTitlePaint)
        y += 16f
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        y += 24f

        // Invoice Header / Metadata
        canvas.drawText("INVOICE / DELIVERY BILL", 40f, y, headerPaint)
        val dateText = "Date: ${sale.saleDate}"
        val dateWidth = regularPaint.measureText(dateText)
        canvas.drawText(dateText, PAGE_WIDTH - 40f - dateWidth, y, regularPaint)
        y += 16f

        canvas.drawText("Invoice No: ${sale.billNumber}", 40f, y, boldPaint)
        val statusText = "Status: ${sale.paymentStatus}"
        val statusWidth = boldPaint.measureText(statusText)
        canvas.drawText(statusText, PAGE_WIDTH - 40f - statusWidth, y, boldPaint)
        y += 24f

        // Bill To Section Box
        paint.color = Color.rgb(249, 250, 251)
        canvas.drawRoundRect(40f, y, PAGE_WIDTH - 40f, y + 55f, 6f, 6f, paint)

        val custName = customer?.businessName?.ifBlank { customer.name } ?: sale.customerName
        canvas.drawText("BILL TO:", 52f, y + 18f, boldPaint)
        canvas.drawText(custName, 115f, y + 18f, headerPaint)

        val phoneStr = customer?.phone?.ifBlank { "N/A" } ?: "N/A"
        val addrStr = customer?.address?.ifBlank { "Hanti, Bihar" } ?: "Hanti, Bihar"
        canvas.drawText("Phone: $phoneStr", 52f, y + 36f, regularPaint)
        canvas.drawText("Address: $addrStr", 240f, y + 36f, regularPaint)
        y += 70f

        // Items Table Header
        canvas.drawRect(40f, y, PAGE_WIDTH - 40f, y + 24f, tableHeaderPaint)
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        canvas.drawLine(40f, y + 24f, PAGE_WIDTH - 40f, y + 24f, linePaint)

        canvas.drawText("ITEM DESCRIPTION", 50f, y + 16f, boldPaint)
        canvas.drawText("QTY", 280f, y + 16f, boldPaint)
        canvas.drawText("RATE", 370f, y + 16f, boldPaint)
        canvas.drawText("AMOUNT", 470f, y + 16f, boldPaint)
        y += 38f

        // Item Row
        val itemName = "${sale.productType} (${sale.chickenCut})"
        val qtyStr = "${String.format(Locale.US, "%.2f", sale.quantity)} ${sale.quantityUnit}"
        val rateStr = "Rs. ${String.format(Locale.US, "%.2f", sale.sellingRate)}/${sale.quantityUnit}"
        val amtStr = "Rs. ${String.format(Locale.US, "%.2f", sale.subtotal)}"

        canvas.drawText(itemName, 50f, y, regularPaint)
        canvas.drawText(qtyStr, 280f, y, regularPaint)
        canvas.drawText(rateStr, 370f, y, regularPaint)
        canvas.drawText(amtStr, 470f, y, regularPaint)
        y += 18f
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        y += 24f

        // Financials Summary Block
        val summaryLeft = 320f
        val summaryValueLeft = 460f

        canvas.drawText("Subtotal:", summaryLeft, y, regularPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.subtotal)}", summaryValueLeft, y, regularPaint)
        y += 18f

        if (sale.discount > 0) {
            canvas.drawText("Discount:", summaryLeft, y, regularPaint)
            canvas.drawText("-Rs. ${String.format(Locale.US, "%.2f", sale.discount)}", summaryValueLeft, y, regularPaint)
            y += 18f
        }

        canvas.drawText("Current Bill Amount:", summaryLeft, y, boldPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.finalAmount)}", summaryValueLeft, y, boldPaint)
        y += 18f

        canvas.drawText("Previous Outstanding:", summaryLeft, y, regularPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", previousOutstanding)}", summaryValueLeft, y, regularPaint)
        y += 18f

        canvas.drawText("Payment Received:", summaryLeft, y, regularPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", sale.amountPaid)}", summaryValueLeft, y, regularPaint)
        y += 20f

        canvas.drawLine(summaryLeft, y - 6f, PAGE_WIDTH - 40f, y - 6f, linePaint)
        val totalOutstanding = previousOutstanding + sale.balancePending
        canvas.drawText("Total Outstanding Balance:", summaryLeft, y + 10f, headerPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", totalOutstanding)}", summaryValueLeft, y + 10f, titlePaint)
        y += 40f

        // Notes if any
        if (sale.notes.isNotBlank()) {
            canvas.drawText("Notes / Instructions:", 40f, y, boldPaint)
            y += 14f
            canvas.drawText(sale.notes, 40f, y, regularPaint)
            y += 24f
        }

        // Footer / Terms
        val footerY = PAGE_HEIGHT - 75f
        canvas.drawLine(40f, footerY, PAGE_WIDTH - 40f, footerY, linePaint)
        canvas.drawText("Thank you for your business! Please settle pending balance within payment cycle.", 40f, footerY + 18f, subTitlePaint)
        canvas.drawText("HALAL CHICKEN SHOP HANTI • Hanti, Bihar • Designed & Developed by Mr. Sajid", 40f, footerY + 34f, subTitlePaint)

        document.finishPage(page)

        // Save PDF to documents directory
        val cleanCust = custName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanBill = sale.billNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanDate = sale.saleDate.replace("-", "_")
        val fileName = "HCS_${cleanCust}_Invoice_${cleanBill}_${cleanDate}.pdf"

        val dir = File(context.getExternalFilesDir(null), "Invoices")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun generatePendingStatementPdf(
        context: Context,
        customer: CustomerEntity,
        pendingSales: List<SaleEntity>,
        payments: List<PaymentRecordEntity>,
        totalPending: Double
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
            color = Color.rgb(6, 78, 59)
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.rgb(75, 85, 99)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.rgb(17, 24, 39)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.rgb(17, 24, 39)
        }
        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9.5f
            color = Color.rgb(31, 41, 55)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(209, 213, 219)
            strokeWidth = 1f
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(243, 244, 246)
        }

        var y = 45f

        // Brand Banner
        canvas.drawText("HALAL CHICKEN SHOP HANTI", 40f, y, titlePaint)
        y += 16f
        canvas.drawText("Owner: Mr. Sajid | Hanti, Bihar | Phone: +91 9708099035", 40f, y, subTitlePaint)
        y += 14f
        canvas.drawText("Pending Statement & Ledger Account", 40f, y, subTitlePaint)
        y += 16f
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        y += 24f

        // Statement Metadata
        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("OUTSTANDING BILLS STATEMENT", 40f, y, headerPaint)
        val dateText = "Statement Date: $todayStr"
        val dateWidth = regularPaint.measureText(dateText)
        canvas.drawText(dateText, PAGE_WIDTH - 40f - dateWidth, y, regularPaint)
        y += 22f

        // Customer Details Box
        val custBoxPaint = Paint().apply { color = Color.rgb(249, 250, 251) }
        canvas.drawRoundRect(40f, y, PAGE_WIDTH - 40f, y + 48f, 6f, 6f, custBoxPaint)

        val custName = customer.businessName.ifBlank { customer.name }
        canvas.drawText("CUSTOMER: $custName", 52f, y + 18f, headerPaint)
        canvas.drawText("Phone: ${customer.phone.ifBlank { "N/A" }} | Delivery: ${customer.deliveryDays.ifBlank { "Regular" }}", 52f, y + 34f, regularPaint)
        y += 65f

        // Table Header
        canvas.drawRect(40f, y, PAGE_WIDTH - 40f, y + 22f, tableHeaderPaint)
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        canvas.drawLine(40f, y + 22f, PAGE_WIDTH - 40f, y + 22f, linePaint)

        canvas.drawText("INVOICE NO", 48f, y + 15f, boldPaint)
        canvas.drawText("DATE", 145f, y + 15f, boldPaint)
        canvas.drawText("QTY / ITEM", 225f, y + 15f, boldPaint)
        canvas.drawText("BILL AMT", 345f, y + 15f, boldPaint)
        canvas.drawText("PAID", 420f, y + 15f, boldPaint)
        canvas.drawText("PENDING", 485f, y + 15f, boldPaint)
        y += 34f

        if (pendingSales.isEmpty()) {
            canvas.drawText("No pending bills. Account is fully cleared!", 50f, y, regularPaint)
            y += 20f
        } else {
            pendingSales.take(18).forEach { s ->
                canvas.drawText(s.billNumber, 48f, y, regularPaint)
                canvas.drawText(s.saleDate, 145f, y, regularPaint)
                val itemBrief = "${s.quantity}${s.quantityUnit} ${s.productType.take(4)}"
                canvas.drawText(itemBrief, 225f, y, regularPaint)
                canvas.drawText("Rs. ${s.finalAmount.toInt()}", 345f, y, regularPaint)
                canvas.drawText("Rs. ${s.amountPaid.toInt()}", 420f, y, regularPaint)
                canvas.drawText("Rs. ${s.balancePending.toInt()}", 485f, y, boldPaint)
                y += 18f
            }
        }
        y += 10f
        canvas.drawLine(40f, y, PAGE_WIDTH - 40f, y, linePaint)
        y += 24f

        // Outstanding Box
        val summaryBoxPaint = Paint().apply { color = Color.rgb(254, 242, 242) }
        canvas.drawRoundRect(280f, y, PAGE_WIDTH - 40f, y + 50f, 6f, 6f, summaryBoxPaint)
        val alertPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.rgb(185, 28, 28)
        }
        canvas.drawText("TOTAL PENDING AMOUNT:", 295f, y + 22f, boldPaint)
        canvas.drawText("Rs. ${String.format(Locale.US, "%.2f", totalPending)}", 295f, y + 42f, alertPaint)
        y += 75f

        // Payment note
        canvas.drawText("Payment Instructions: Settle via UPI / Cash to Mr. Sajid (+91 9708099035).", 40f, y, regularPaint)
        y += 14f
        canvas.drawText("Thank you for your valued partnership. HALAL CHICKEN SHOP HANTI", 40f, y, subTitlePaint)

        // Footer
        val footerY = PAGE_HEIGHT - 60f
        canvas.drawLine(40f, footerY, PAGE_WIDTH - 40f, footerY, linePaint)
        canvas.drawText("HALAL CHICKEN SHOP HANTI • Hanti, Bihar • Designed & Developed by Mr. Sajid", 40f, footerY + 20f, subTitlePaint)

        document.finishPage(page)

        val cleanCust = custName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val monthYear = SimpleDateFormat("MMMM_yyyy", Locale.getDefault()).format(Date())
        val fileName = "HCS_${cleanCust}_Pending_Statement_${monthYear}.pdf"

        val dir = File(context.getExternalFilesDir(null), "Statements")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File, subject: String = "Halal Chicken Shop Hanti - Bill") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Please find attached document from HALAL CHICKEN SHOP HANTI.\nDesigned & Developed by Mr. Sajid")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Bill PDF via WhatsApp / Drive")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openOrPrintPdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file, "Print / View Invoice")
        }
    }
}
