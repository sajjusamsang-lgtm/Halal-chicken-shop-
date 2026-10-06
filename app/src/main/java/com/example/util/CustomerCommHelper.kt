package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.util.Locale

object CustomerCommHelper {

    fun cleanPhoneNumber(phone: String): String {
        return phone.replace("[^0-9+]".toRegex(), "").trim()
    }

    fun callCustomer(context: Context, phone: String) {
        val cleanPhone = cleanPhoneNumber(phone)
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun chatWhatsApp(context: Context, phone: String, message: String = "") {
        val cleanPhone = cleanPhoneNumber(phone).removePrefix("+")
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val url = if (cleanPhone.isNotBlank()) {
                "https://wa.me/$cleanPhone?text=$encodedMsg"
            } else {
                "https://wa.me/?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback generic send
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via WhatsApp"))
        }
    }

    fun sendSms(context: Context, phone: String, message: String) {
        val cleanPhone = cleanPhoneNumber(phone)
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "SMS composer opened.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open SMS composer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildPendingWhatsAppMessage(
        customerName: String,
        totalPending: Double,
        billsSummary: String
    ): String {
        return """
            *HALAL CHICKEN SHOP HANTI*
            *PAYMENT REMINDER*

            Customer: $customerName
            Pending Amount: ₹${String.format(Locale.US, "%.2f", totalPending)}

            Bill Details:
            $billsSummary

            Total Pending: ₹${String.format(Locale.US, "%.2f", totalPending)}

            Please clear the pending amount at your convenience.
            Thank you.

            *HALAL CHICKEN SHOP HANTI*
            _Designed & Developed by Mr. Sajid_
        """.trimIndent()
    }

    fun buildPendingSmsMessage(
        customerName: String,
        totalPending: Double
    ): String {
        return "HALAL CHICKEN SHOP HANTI: Dear $customerName, your pending amount is ₹${String.format(Locale.US, "%.2f", totalPending)}. Please check your latest bill/statement. Thank you. - Mr. Sajid"
    }
}
