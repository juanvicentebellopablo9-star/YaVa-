package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WhatsAppButton(
    modifier: Modifier = Modifier,
    customMessage: String = "Hola YaVa!, quiero solicitar un envío.",
    phoneNumber: String = "529997431941",
    label: String = "Contactar por WhatsApp"
) {
    val context = LocalContext.current

    Button(
        onClick = {
            val encodedMsg = Uri.encode(customMessage)
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF25D366), // WhatsApp Green
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.testTag("whatsapp_contact_button")
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "WhatsApp"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
