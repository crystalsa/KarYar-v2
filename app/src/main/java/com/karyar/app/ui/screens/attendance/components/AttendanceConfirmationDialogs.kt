package com.karyar.app.ui.screens.attendance.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate400

@Composable
fun HourlyProfileRequiredDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تکمیل پروفایل کارگر ساعتی",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "باید اول داخل پروفایل قسمت دستمزد ساعتی پر شود تا اجازه زدن تیک دستمزد ساعتی در قسمت حضور غیاب داده شود.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("متوجه شدم", fontWeight = FontWeight.Bold, color = AmberAccent)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun MustCreateDayDialog(
    onDismiss: () -> Unit,
    onCreateDay: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "تعریف روز کاری الزامی است",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "قبل از افزودن کارگر، ابتدا باید یک روز کاری ثبت نمایید تا وضعیت کارکرد و حضور و غیاب کارگر در آن روز ثبت شود.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            LoadingButton(
                text = "ساخت روز کاری",
                icon = Icons.Default.Add,
                onClick = onCreateDay,
                containerColor = AmberAccent,
                height = 38.dp,
                fontSize = 12.sp
            )
        },
        dismissButton = {
            LoadingOutlinedButton(
                text = "انصراف",
                onClick = onDismiss,
                height = 38.dp,
                fontSize = 12.sp
            )
        }
    )
}

@Composable
fun DeleteWorkerConfirmDialog(
    workerName: String,
    dayInfo: String? = null,
    deletionImpact: com.karyar.app.domain.model.DeletionImpact = com.karyar.app.domain.model.DeletionImpact(),
    onDismiss: () -> Unit,
    onRemoveFromDay: (() -> Unit)? = null,
    onDeactivateWorker: (() -> Unit)? = null,
    onDeletePermanently: () -> Unit = onDismiss
) {
    val daysCountPersian = com.karyar.app.util.Formatters.toPersianDigits(deletionImpact.attendanceDaysCount)
    val settlementStr = deletionImpact.formatSettlementText()
    val hasHistory = deletionImpact.hasHistory

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف کارگر از این روز",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "با حذف این کارگر، $daysCountPersian روز حضور و مبلغ $settlementStr از سوابق پاک می‌شود.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseAccent,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (hasHistory && onDeactivateWorker != null) {
                    Text(
                        text = "پیشنهاد: برای حفظ کامل سوابق حضور و دستمزد «$workerName» در گزارش‌های مالی، گزینه «غیرفعال کردن» را انتخاب کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = if (dayInfo != null) {
                        "با «حذف از این روز»، «$workerName» فقط از روز کاری $dayInfo حذف می‌شود و سوابق سایر روزها محفوظ می‌ماند."
                    } else {
                        "با «حذف از این روز»، «$workerName» فقط از این روز کاری حذف می‌شود و سوابق سایر روزها محفوظ می‌ماند."
                    },
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                if (hasHistory && onDeactivateWorker != null) {
                    // دکمه اول (پیشنهادی): غیرفعال کردن (isActive = false)
                    LoadingButton(
                        text = "غیرفعال کردن (پیشنهادی)",
                        onClick = onDeactivateWorker,
                        containerColor = com.karyar.app.ui.theme.EmeraldAccent,
                        modifier = Modifier.fillMaxWidth(),
                        height = 38.dp,
                        fontSize = 12.sp,
                        testTag = "deactivate_worker_button"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                // جایگزینی حذف دائمی با «حذف از این روز»
                LoadingButton(
                    text = "حذف از این روز",
                    onClick = onRemoveFromDay ?: onDeletePermanently,
                    containerColor = RoseAccent,
                    modifier = Modifier.fillMaxWidth(),
                    height = 38.dp,
                    fontSize = 12.sp,
                    testTag = "confirm_remove_worker_from_day_button"
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "انصراف",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun CopyWorkerConfirmDialog(
    workerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "کپی کارگر (ساخت کارگر جدید)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "آیا از کپی کردن مشخصات «$workerName» اطمینان دارید؟",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "یک کارگر جدید با شناسه جدید و نام «$workerName (کپی)» ساخته می‌شود و اسم می‌تواند بعداً در بخش ویرایش تغییر کند.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_copy_worker_button")
            ) {
                Text(
                    text = "تأیید و ساخت کارگر جدید",
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "انصراف",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
