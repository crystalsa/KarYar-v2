package com.karyar.app.ui.screens.attendance.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.data.local.entity.WorkerEntity
import com.karyar.app.ui.components.LoadingButton
import com.karyar.app.ui.components.LoadingOutlinedButton
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.RoseAccent

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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حذف",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Text(
                text = "آیا از حذف «$workerName» اطمینان دارید؟",
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "حذف",
                    color = RoseAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
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
                text = "تکثیر",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        },
        text = {
            Text(
                text = "آیا از تکثیر «$workerName» اطمینان دارید؟",
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "تکثیر",
                    color = AmberAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
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
