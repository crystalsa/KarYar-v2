package com.karyar.app.ui.screens.workers.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate400
import com.karyar.app.util.Formatters

@Composable
fun DayAllowanceDeductionItem(
    title: String,
    allowance: Long,
    deduction: Long
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate100,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (allowance == 0L && deduction == 0L) {
                    Text(
                        text = "۰ تومان",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    if (allowance > 0L) {
                        Text(
                            text = "+ ${Formatters.formatCurrency(allowance)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                    if (allowance > 0L && deduction > 0L) {
                        Text(
                            text = "|",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                    if (deduction > 0L) {
                        Text(
                            text = "- ${Formatters.formatCurrency(deduction)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseAccent
                        )
                    }
                }
            }
        }
    }
}
