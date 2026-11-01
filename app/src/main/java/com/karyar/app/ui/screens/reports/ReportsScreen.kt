package com.karyar.app.ui.screens.reports

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karyar.app.domain.model.WorkerPerformance
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.components.AddExpenseDialog
import com.karyar.app.ui.components.ExpensesManagementSection
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.components.StatusBadge
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.IndigoAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.util.ExcelExportUtil
import com.karyar.app.util.Formatters
import com.karyar.app.util.JalaliCalendar
import com.karyar.app.util.PdfExportUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportsScreen(
    viewModel: WorkerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val folder by viewModel.currentFolder.collectAsState()
    val performances by viewModel.workerPerformances.collectAsState()
    val analytics by viewModel.analytics.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val selectedDailyDate by viewModel.selectedDailyDate.collectAsState()
    val calculationError by viewModel.calculationError.collectAsState()
    val problematicRecords by viewModel.problematicRecords.collectAsState()
    var showAddExpenseDialog by rememberSaveable { mutableStateOf(false) }

    val activeProject = folder?.name ?: "پروژه کارگاهی"
    val activeEmployer = folder?.employerName ?: "کارفرما"
    val activeForeman = folder?.foremanName ?: "سرکارگر"
    var lastSavedPathMessage by remember { mutableStateOf<String?>(null) }

    // انتخابگر مسیر دلخواه در حافظه داخلی برای فایل PDF
    val createPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelExportUtil.MIME_TYPE_PDF)
    ) { targetUri ->
        if (targetUri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = PdfExportUtil.exportToPdf(
                        context = context,
                        projectName = activeProject,
                        employerName = activeEmployer,
                        foremanName = activeForeman,
                        analytics = analytics,
                        performances = performances,
                        expenses = expenses
                    )
                    ExcelExportUtil.writeFileToUri(context, tempFile, targetUri)
                    withContext(Dispatchers.Main) {
                        lastSavedPathMessage = "فایل PDF در مسیر انتخاب‌شده در حافظه داخلی ذخیره شد"
                        viewModel.showSnackbarMessage("فایل PDF با موفقیت در حافظه داخلی گوشی ذخیره شد")
                    }
                } catch (e: Exception) {
                    Log.e("ReportsScreen", "Error saving PDF to custom URI", e)
                    withContext(Dispatchers.Main) {
                        viewModel.showSnackbarMessage("خطا در ذخیره فایل PDF در حافظه گوشی")
                    }
                }
            }
        }
    }

    // انتخابگر مسیر دلخواه در حافظه داخلی برای فایل اکسل (.xlsx)
    val createExcelDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExcelExportUtil.MIME_TYPE_XLSX)
    ) { targetUri ->
        if (targetUri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = ExcelExportUtil.exportToExcelXlsx(
                        context = context,
                        workers = workers,
                        attendanceList = attendanceList,
                        performances = performances,
                        projectName = activeProject,
                        expenses = expenses,
                        analytics = analytics
                    )
                    ExcelExportUtil.writeFileToUri(context, tempFile, targetUri)
                    withContext(Dispatchers.Main) {
                        lastSavedPathMessage = "فایل اکسل در مسیر انتخاب‌شده در حافظه داخلی ذخیره شد"
                        viewModel.showSnackbarMessage("فایل اکسل با موفقیت در حافظه داخلی گوشی ذخیره شد")
                    }
                } catch (e: Exception) {
                    Log.e("ReportsScreen", "Error saving Excel to custom URI", e)
                    withContext(Dispatchers.Main) {
                        viewModel.showSnackbarMessage("خطا در ذخیره فایل اکسل در حافظه گوشی")
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        if (calculationError != null) {
            item {
                com.karyar.app.ui.components.CalculationErrorBanner(
                    errorMessage = calculationError!!,
                    problematicRecords = problematicRecords
                )
            }
        }

        // 1. Header & Export Buttons Banner
        item {
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "گزارشات و خروجی رسمی این کارگاه",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ذخیره مستقیم در حافظه داخلی گوشی (پوشه Download/KarYar) یا اشتراک‌گذاری",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = "${performances.size} کارگر",
                            dotColor = AmberAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ردیف ۱: ذخیره مستقیم در حافظه داخلی گوشی (پوشه Download/KarYar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Direct Save PDF to Internal Storage
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = PdfExportUtil.exportToPdf(
                                            context = context,
                                            projectName = activeProject,
                                            employerName = activeEmployer,
                                            foremanName = activeForeman,
                                            analytics = analytics,
                                            performances = performances,
                                            expenses = expenses
                                        )
                                        val exportFileName = ExcelExportUtil.buildExportFileName(
                                            prefix = "گزارش_PDF",
                                            projectName = activeProject,
                                            extension = "pdf"
                                        )
                                        val saved = ExcelExportUtil.saveFileToInternalStorage(
                                            context = context,
                                            sourceFile = file,
                                            desiredFileName = exportFileName,
                                            mimeType = ExcelExportUtil.MIME_TYPE_PDF
                                        )
                                        withContext(Dispatchers.Main) {
                                            lastSavedPathMessage = "ذخیره شد: ${saved.displayPath}"
                                            viewModel.showSnackbarMessage("فایل PDF در حافظه داخلی (${saved.displayPath}) ذخیره شد")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ReportsScreen", "Error saving PDF to internal storage", e)
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("خطا در ذخیره فایل PDF در حافظه گوشی")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_pdf_internal_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ذخیره PDF در گوشی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Direct Save Excel (.xlsx) to Internal Storage
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = ExcelExportUtil.exportToExcelXlsx(
                                            context = context,
                                            workers = workers,
                                            attendanceList = attendanceList,
                                            performances = performances,
                                            projectName = activeProject,
                                            expenses = expenses,
                                            analytics = analytics
                                        )
                                        val exportFileName = ExcelExportUtil.buildExportFileName(
                                            prefix = "گزارش_اکسل",
                                            projectName = activeProject,
                                            extension = "xlsx"
                                        )
                                        val saved = ExcelExportUtil.saveFileToInternalStorage(
                                            context = context,
                                            sourceFile = file,
                                            desiredFileName = exportFileName,
                                            mimeType = ExcelExportUtil.MIME_TYPE_XLSX
                                        )
                                        withContext(Dispatchers.Main) {
                                            lastSavedPathMessage = "ذخیره شد: ${saved.displayPath}"
                                            viewModel.showSnackbarMessage("فایل اکسل در حافظه داخلی (${saved.displayPath}) ذخیره شد")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ReportsScreen", "Error saving Excel to internal storage", e)
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("خطا در ذخیره فایل اکسل در حافظه گوشی")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_excel_internal_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ذخیره اکسل در گوشی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ردیف ۲: انتخاب پوشه دلخواه در حافظه داخلی (SAF)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fileName = ExcelExportUtil.buildExportFileName(
                                    prefix = "گزارش_PDF",
                                    projectName = activeProject,
                                    extension = "pdf"
                                )
                                createPdfDocumentLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("save_pdf_custom_path_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسیر دلخواه PDF", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val fileName = ExcelExportUtil.buildExportFileName(
                                    prefix = "گزارش_اکسل",
                                    projectName = activeProject,
                                    extension = "xlsx"
                                )
                                createExcelDocumentLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("save_excel_custom_path_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسیر دلخواه اکسل", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ردیف ۳: اشتراک‌گذاری مستقیم فایل‌ها (PDF و Excel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PDF Share Button
                        OutlinedButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = PdfExportUtil.exportToPdf(
                                            context = context,
                                            projectName = activeProject,
                                            employerName = activeEmployer,
                                            foremanName = activeForeman,
                                            analytics = analytics,
                                            performances = performances,
                                            expenses = expenses
                                        )
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("فایل PDF با موفقیت آماده شد")
                                            ExcelExportUtil.shareFile(context, file, ExcelExportUtil.MIME_TYPE_PDF, "ارسال گزارش رسمی PDF")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ReportsScreen", "Error exporting PDF", e)
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("خطا در تولید فایل PDF")
                                        }
                                    }
                                 }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("export_pdf_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اشتراک‌گذاری PDF", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }

                        // Excel (.xlsx) Share Button
                        OutlinedButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val file = ExcelExportUtil.exportToExcelXlsx(
                                            context = context,
                                            workers = workers,
                                            attendanceList = attendanceList,
                                            performances = performances,
                                            projectName = activeProject,
                                            expenses = expenses,
                                            analytics = analytics
                                        )
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("فایل اکسل با موفقیت آماده شد")
                                            ExcelExportUtil.shareFile(
                                                context = context,
                                                file = file,
                                                mimeType = ExcelExportUtil.MIME_TYPE_XLSX,
                                                title = "ارسال گزارش اکسل (Excel)"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ReportsScreen", "Error exporting Excel", e)
                                        withContext(Dispatchers.Main) {
                                            viewModel.showSnackbarMessage("خطا در صدور فایل اکسل")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("export_excel_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اشتراک‌گذاری اکسل", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }

                    if (lastSavedPathMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("saved_export_path_banner"),
                            color = EmeraldAccent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = lastSavedPathMessage!!,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }
        }

        // Project Grand Totals & Worker Debt Summary
        item {
            val totalDebt = performances.sumOf { it.debtAmount }
            HairlineCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع کل مخارج و هزینه‌های پروژه:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (analytics.grandTotalProjectCost < 0L) "بدهکار: ${Formatters.formatCurrency(-analytics.grandTotalProjectCost)}" else Formatters.formatCurrency(analytics.grandTotalProjectCost),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analytics.grandTotalProjectCost < 0L) RoseAccent else EmeraldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع بدهی کارگران:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDebt > 0L) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalDebt > 0L) "بدهکار: ${Formatters.formatCurrency(totalDebt)}" else "۰ تومان",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalDebt > 0L) RoseAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // بخش مدیریت و ثبت هزینه‌های فردی و گروهی کارگاه
        if (folder != null) {
            item {
                ExpensesManagementSection(
                    expenses = expenses,
                    workers = workers,
                    onAddExpenseClick = { showAddExpenseDialog = true },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
            }
        }

        // 2. Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ریز محاسبات کارکرد و دستمزد هر کارگر",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ساعات کارکرد، اضافه کاری، کمک‌هزینه‌ها و کسورات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (performances.isEmpty()) {
            item {
                HairlineCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "هیچ کارگری برای محاسبه در این کارگاه وجود ندارد.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Worker Performance Detailed Pay Slips
        items(performances, key = { it.worker.id }) { perf ->
            WorkerPaySlipCard(perf)
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            workers = workers,
            initialDate = selectedDailyDate.ifBlank { JalaliCalendar.todayString() },
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { newExpense ->
                viewModel.addExpense(newExpense)
                showAddExpenseDialog = false
            }
        )
    }
}

@Composable
private fun WorkerPaySlipCard(perf: WorkerPerformance) {
    HairlineCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(perf.worker.colorTag)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = perf.worker.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perf.worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = perf.worker.role,
                            fontSize = 11.5.sp,
                            color = AmberAccent,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (perf.debtAmount > 0L) "وضعیت حساب" else "خالص دریافتی",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (perf.debtAmount > 0L) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RoseAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "بدهکار: ${Formatters.formatCurrency(perf.debtAmount)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = Formatters.formatCurrency(perf.netPayout),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate100)
                    .padding(10.dp)
            ) {
                if (perf.worker.isHourlyEnabled) {
                    ReportRow("ساعات کارکرد عادی:", Formatters.formatHours(perf.hourlyHours))
                } else {
                    ReportRow("وضعیت کارکرد:", perf.formatWorkSummary())
                    ReportRow("دستمزد پایه تجمیعی:", Formatters.formatCurrency(perf.baseWageTotal))
                }

                if (perf.hourlyPayTotal > 0 || perf.hourlyHours > 0) {
                    ReportRow("دستمزد ساعتی (${Formatters.formatHours(perf.hourlyHours)}):", "+${Formatters.formatCurrency(perf.hourlyPayTotal)}", CyanAccent)
                }
                if (perf.overtimeHours > 0) {
                    ReportRow("اضافه کاری (${Formatters.formatHours(perf.overtimeHours)}):", "+${Formatters.formatCurrency(perf.overtimePayTotal)}", AmberAccent)
                }
                if (perf.totalAllowances > 0) {
                    ReportRow("(+) مجموع کمک‌هزینه‌های افزایشی:", "+${Formatters.formatCurrency(perf.totalAllowances)}", EmeraldAccent)
                }
                if (perf.otherAllowanceTotal > 0L) {
                    ReportRow("(+) سایر (افزایشی):", "+${Formatters.formatCurrency(perf.otherAllowanceTotal)}", EmeraldAccent)
                }
                if (perf.totalDeductions > 0) {
                    ReportRow("(-) مجموع کسورات:", "-${Formatters.formatCurrency(perf.totalDeductions)}", RoseAccent)
                }
                if (perf.otherDeductionTotal > 0L) {
                    ReportRow("(-) سایر (کسورات):", "-${Formatters.formatCurrency(perf.otherDeductionTotal)}", RoseAccent)
                }
                if (perf.debtAmount > 0L) {
                    ReportRow("وضعیت بدهی کارگر:", "بدهکار: ${Formatters.formatCurrency(perf.debtAmount)}", RoseAccent)
                }
            }
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
