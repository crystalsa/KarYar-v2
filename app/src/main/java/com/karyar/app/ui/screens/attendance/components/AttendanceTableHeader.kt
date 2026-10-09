package com.karyar.app.ui.screens.attendance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100

@Composable
fun AttendanceTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Slate100)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "نام کارگر",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Full Day (تمام روز)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(38.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "تمام روز",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent
                )
            }

            // 2. Half Day (نصف روز)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(38.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "نصف روز",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                )
            }

            // 3. Hourly (ساعتی) - بین نصف روز و غیبت
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(38.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ساعتی",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }

            // 4. Absent (غیبت)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(34.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "غیبت",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseAccent
                )
            }

            // Spacer for 3-dots menu button width
            Spacer(modifier = Modifier.width(26.dp))
        }
    }
}
