package com.example.ui.components.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightSurface = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE2E8F0)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)
private val TextSubtle = Color(0xFF94A3B8)
private val TealPrimary = Color(0xFF0D9488)

@Composable
fun ReportTeamNoteCard(
    noteText: String,
    onNoteChange: (String) -> Unit,
    dateStr: String,
    timeStr: String,
    focusManager: FocusManager
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LightSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Ekip Notu (İsteğe Bağlı)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                text = "Reyon sorumlusu veya ekip arkadaşlarınız için kısa bir talimat yazabilirsiniz:",
                fontSize = 11.5.sp,
                color = TextSecondary
            )

            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    if (it.length <= 120) {
                        onNoteChange(it)
                    }
                },
                placeholder = {
                    Text(
                        text = "Bu rapor hakkında mağaza ekibine kısa bir not ekleyin...",
                        color = TextSubtle,
                        fontSize = 12.5.sp
                    )
                },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${noteText.length}/120",
                            color = TextSubtle,
                            fontSize = 11.sp
                        )
                    }
                },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = BorderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preview_note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSubtle,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Oluşturulma: $dateStr $timeStr  •  SKT & Stok Takip Sistemi",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
