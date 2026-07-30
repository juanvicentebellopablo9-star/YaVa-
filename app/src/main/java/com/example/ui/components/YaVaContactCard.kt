package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YaVaYellowPrimary

@Composable
fun YaVaContactCard(
    modifier: Modifier = Modifier,
    customMessage: String = "Hola YaVa!, me interesa información sobre sus servicios de envío."
) {
    val context = LocalContext.current
    val whatsappPhone = "529997431941"
    val emailAddress = "yavaenvios@gmail.com"
    val facebookName = "YaVa Envíos"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Contacto Directo YaVa!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Atención personalizada para cotizaciones, seguimiento y socios:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. WhatsApp Row
            Surface(
                onClick = {
                    val encodedMsg = Uri.encode(customMessage)
                    val url = "https://api.whatsapp.com/send?phone=$whatsappPhone&text=$encodedMsg"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF25D366),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_whatsapp_row")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WhatsApp Directo",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "999 743 1941",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Direct Phone Call Row
            Surface(
                onClick = {
                    val callUri = Uri.parse("tel:$whatsappPhone")
                    val intent = Intent(Intent.ACTION_DIAL, callUri)
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_phone_call_row")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Llamada Directa",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Llamada Directa de Soporte",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "999 743 1941",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Email Row
            Surface(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$emailAddress")
                        putExtra(Intent.EXTRA_SUBJECT, "Consulta YaVa! Envíos")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback browser mailto
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$emailAddress")))
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_email_row")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Correo Electrónico",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = emailAddress,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Facebook Row
            Surface(
                onClick = {
                    val fbUri = Uri.parse("https://www.facebook.com/yavaenvios")
                    val intent = Intent(Intent.ACTION_VIEW, fbUri)
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1877F2), // Facebook Blue
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_facebook_row")
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Facebook",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Página Oficial de Facebook",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = facebookName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
