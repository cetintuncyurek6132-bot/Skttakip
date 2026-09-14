package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.data.Product
import com.example.util.image.BitmapSharingHelper
import com.example.util.image.ProductReportImageRenderer
import com.example.util.image.SharePreviewReportRenderer

data class ExpiryReportInfo(
    val statusText: String,        // "SÜRESİ GEÇTİ", "BUGÜN DOLUYOR", "2 GÜN KALDI", "16 GÜN VAR"
    val subDetailText: String,     // "2 GÜN ÖNCE" if expired, else ""
    val badgeTopText: String,      // "GEÇTİ", "BUGÜN", "2", "16"
    val badgeBottomText: String,   // "X GÜN ÖNCE", "DOLUYOR", "GÜN KALDI", "GÜN VAR"
    val colorHex: String,          // Main risk color
    val bgHex: String,             // Soft card tint
    val borderHex: String          // Border stroke color
)

/**
 * Façade object for generating, sharing, and saving report images.
 * Delegates rendering and sharing logic to modular sub-components in com.example.util.image.
 */
object ProductImageGenerator {

    fun getExpiryReportInfo(days: Long): ExpiryReportInfo {
        return when {
            days < 0L -> {
                val passedDays = -days
                ExpiryReportInfo(
                    statusText = "SÜRESİ GEÇTİ",
                    subDetailText = "$passedDays GÜN ÖNCE",
                    badgeTopText = "GEÇTİ",
                    badgeBottomText = "$passedDays GÜN ÖNCE",
                    colorHex = "#DC2626", // Kırmızı
                    bgHex = "#FEF2F2",
                    borderHex = "#FECACA"
                )
            }
            days == 0L -> {
                ExpiryReportInfo(
                    statusText = "BUGÜN DOLUYOR",
                    subDetailText = "BUGÜN",
                    badgeTopText = "BUGÜN",
                    badgeBottomText = "DOLUYOR",
                    colorHex = "#991B1B", // Koyu Kırmızı
                    bgHex = "#FEF2F2",
                    borderHex = "#FCA5A5"
                )
            }
            days in 1L..3L -> {
                ExpiryReportInfo(
                    statusText = "$days GÜN KALDI",
                    subDetailText = "$days GÜN KALDI",
                    badgeTopText = "$days",
                    badgeBottomText = "GÜN KALDI",
                    colorHex = "#EA580C", // Turuncu/Kırmızı
                    bgHex = "#FFF7ED",
                    borderHex = "#FED7AA"
                )
            }
            days in 4L..7L -> {
                ExpiryReportInfo(
                    statusText = "$days GÜN KALDI",
                    subDetailText = "$days GÜN KALDI",
                    badgeTopText = "$days",
                    badgeBottomText = "GÜN KALDI",
                    colorHex = "#F59E0B", // Turuncu
                    bgHex = "#FFFBEB",
                    borderHex = "#FDE68A"
                )
            }
            days in 8L..15L -> {
                ExpiryReportInfo(
                    statusText = "$days GÜN KALDI",
                    subDetailText = "$days GÜN KALDI",
                    badgeTopText = "$days",
                    badgeBottomText = "GÜN KALDI",
                    colorHex = "#D97706", // Sarı
                    bgHex = "#FEFCE8",
                    borderHex = "#FEF08A"
                )
            }
            else -> {
                ExpiryReportInfo(
                    statusText = "$days GÜN VAR",
                    subDetailText = "$days GÜN VAR",
                    badgeTopText = "$days",
                    badgeBottomText = "GÜN VAR",
                    colorHex = "#16A34A", // Yeşil
                    bgHex = "#F0FDF4",
                    borderHex = "#BBF7D0"
                )
            }
        }
    }

    fun createProductsBitmap(
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ): Bitmap? = ProductReportImageRenderer.createProductsBitmap(filterLabel, searchQuery, productList)

    fun shareProductsAsImage(
        context: Context,
        filterLabel: String,
        searchQuery: String,
        productList: List<Product>
    ) = ProductReportImageRenderer.shareProductsAsImage(context, filterLabel, searchQuery, productList)

    fun createSharePreviewReportBitmap(
        title: String = "KONTROL EDİLECEK ÜRÜNLER",
        note: String = "",
        productList: List<Product>
    ): Bitmap = SharePreviewReportRenderer.createSharePreviewReportBitmap(title, note, productList)

    fun shareBitmap(context: Context, bitmap: Bitmap, fileNamePrefix: String = "skt_rapor") {
        BitmapSharingHelper.shareBitmap(context, bitmap, fileNamePrefix)
    }

    fun shareBitmapWithText(
        context: Context,
        bitmap: Bitmap,
        textMessage: String = "",
        fileNamePrefix: String = "skt_onizleme"
    ) {
        BitmapSharingHelper.shareBitmapWithText(context, bitmap, textMessage, fileNamePrefix)
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileNamePrefix: String = "skt_rapor") {
        BitmapSharingHelper.saveBitmapToGallery(context, bitmap, fileNamePrefix)
    }
}
