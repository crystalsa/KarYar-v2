package com.karyar.app.ui.screens.workers.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.IconicsBox
import com.karyar.app.ui.components.IconicsSize
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters

@Composable
fun WorkerDateBadgeCard(
    workDate: String,
    dayOfWeek: String,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = AmberAccent.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconicsBox(
                icon = Icons.Default.CalendarMonth,
                color = AmberAccent,
                size = IconicsSize.TINY
            )
            Spacer(modifier = Modifier.width(8.dp))
            val cleanDay = dayOfWeek.replace("روز", "").trim()
            Text(
                text = "$cleanDay  ${Formatters.toPersianDigits(workDate)}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = AmberAccent
            )
        }
    }
}

@Composable
fun OptionalWorkerFieldRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    accentColor: Color,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Slate100,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = CheckboxDefaults.colors(checkedColor = accentColor),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconicsBox(
                    icon = icon,
                    color = accentColor,
                    size = IconicsSize.TINY
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(3.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(title, fontSize = 11.sp) },
                    placeholder = { Text(placeholder, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }
}
