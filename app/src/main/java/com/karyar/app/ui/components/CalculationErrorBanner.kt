package com.karyar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.ui.ProblematicRecordInfo
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.util.Formatters

@Composable
fun CalculationErrorBanner(
    errorMessage: String,
    problematicRecords: List<ProblematicRecordInfo>,
    modifier: Modifier = Modifier
) {
    var showRecordsDialog by rememberSaveable { mutableStateOf(false) }

    if (showRecordsDialog) {
        AlertDialog(
            onDismissRequest = { showRecordsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = RoseAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "رکوردهای دارای مقدار غیرمجاز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "رکوردهای زیر دارای دستمزد بالاتر از سقف مجاز (${Formatters.formatCurrency(Formatters.MAX_WAGE_AMOUNT)}) یا ساعت کارکرد بیشتر از ۲۴ ساعت هستند و برای جلوگیری از سرریز محاسباتی کنار گذاشته شده‌اند. لطفاً در بخش حضور و غیاب آن‌ها را ویرایش یا اصلاح کنید:",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (problematicRecords.isEmpty()) {
                        Text(
                            text = "مقدار سرریز در جمع کل شناسایی شد.",
                            fontSize = 12.sp,
                            color = RoseAccent,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(problematicRecords, key = { it.attendanceId }) { record ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate100,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("problematic_record_${record.attendanceId}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = record.workerName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = Formatters.toPersianDigits(record.date),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = RoseAccent
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            thickness = 0.5.dp,
                                            color = Slate200
                                        )
                                        Text(
                                            text = record.issues.joinToString("، "),
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showRecordsDialog = false },
                    modifier = Modifier.testTag("close_problematic_records_dialog")
                ) {
                    Text("متوجه شدم", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculation_error_banner"),
        shape = RoundedCornerShape(14.dp),
        color = RoseAccent.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "هشدار محاسباتی",
                    tint = RoseAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseAccent,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showRecordsDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("view_problematic_records_button")
                ) {
                    Text(
                        text = "مشاهده رکوردهای مشکل‌دار",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
