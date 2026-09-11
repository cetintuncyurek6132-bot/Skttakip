package com.example.ui.screens.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun NotificationSettingsCard(
    morningReminderEnabled: Boolean,
    criticalAlertEnabled: Boolean,
    highStockAlertEnabled: Boolean,
    onToggleMorningReminder: () -> Unit,
    onToggleCriticalAlert: () -> Unit,
    onToggleHighStockAlert: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "⚙️ BİLDİRİM VE UYARI AYARLARI",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Günlük SKT Hatırlatma Uyarısı (08:30)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = morningReminderEnabled,
                    onCheckedChange = { onToggleMorningReminder() },
                    modifier = Modifier.scale(0.75f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kritik SKT Anlık Sesli Bildirimi", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = criticalAlertEnabled,
                    onCheckedChange = { onToggleCriticalAlert() },
                    modifier = Modifier.scale(0.75f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yüksek Adet (≥10) Risk Uyarısı", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                Switch(
                    checked = highStockAlertEnabled,
                    onCheckedChange = { onToggleHighStockAlert() },
                    modifier = Modifier.scale(0.75f)
                )
            }
        }
    }
}
