package com.karyar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karyar.app.ui.WorkerViewModel
import com.karyar.app.ui.screens.attendance.AttendanceScreen
import com.karyar.app.ui.screens.dashboard.DashboardScreen
import com.karyar.app.ui.screens.folders.FoldersScreen
import com.karyar.app.ui.screens.reports.ReportsScreen
import com.karyar.app.ui.screens.workers.WorkersScreen
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.EmeraldAccent
import com.karyar.app.ui.theme.MyApplicationTheme
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100
import com.karyar.app.ui.theme.Slate200
import com.karyar.app.ui.theme.Slate400
import com.karyar.app.ui.theme.Slate50

enum class MainTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("داشبورد", Icons.Default.Dashboard, "tab_dashboard"),
    WORKERS("کارگران", Icons.Default.People, "tab_workers"),
    ATTENDANCE("ورود/خروج", Icons.Default.AccessTime, "tab_attendance"),
    REPORTS("گزارشات", Icons.Default.Assessment, "tab_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Clean up old export files from cache/exports on app startup
        try {
            val exportDir = java.io.File(cacheDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}

        enableEdgeToEdge()
        setContent {
            // Strictly Light Theme as requested by user: "از تم دارک استفاده نکن تم باید روشن باشه"
            MyApplicationTheme(darkTheme = false) {
                // Persian RTL Layout Support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    WorkerManagementApp()
                }
            }
        }
    }
}

@Composable
fun WorkerManagementApp(
    viewModel: WorkerViewModel = viewModel()
) {
    val currentFolder by viewModel.currentFolder.collectAsState()
    val workerSearchQuery by viewModel.workerSearchQuery.collectAsState()
    val isWorkerSearchVisible by viewModel.isWorkerSearchVisible.collectAsState()
    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSampleConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Intercept hardware back button when inside a folder to return to FoldersScreen
    BackHandler(enabled = currentFolder != null) {
        viewModel.selectFolder(null)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            // Sleek Light Top App Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentFolder != null) {
                                // Back to folders button
                                IconButton(
                                    onClick = { viewModel.selectFolder(null) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("back_to_folders_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "بازگشت به پوشه‌ها",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AmberAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Engineering,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                if (currentFolder != null) {
                                    // Workplace name
                                    Text(
                                        text = currentFolder!!.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // FOREMAN NAME DIRECTLY UNDER WORKPLACE NAME (Mandatory user rule)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Engineering,
                                            contentDescription = null,
                                            tint = AmberAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "سرکارگر: ${if (currentFolder!!.foremanName.isNotBlank()) currentFolder!!.foremanName else "تعیین نشده"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AmberAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "مدیریت کارگران و کارگاه",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "پوشه‌ها، ثبت تردد، دستمزد و هزینه‌ها",
                                        fontSize = 10.5.sp,
                                        color = AmberAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Right side icons: Magnifying glass beside 3-dots button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentFolder != null) {
                                IconButton(
                                    onClick = {
                                        if (selectedTab != MainTab.WORKERS) {
                                            selectedTab = MainTab.WORKERS
                                        }
                                        viewModel.toggleWorkerSearch()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("app_top_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "جستجوی کارگر",
                                        tint = if (isWorkerSearchVisible) AmberAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // 3-DOT MENU BUTTON AT TOP
                            Box {
                                IconButton(
                                    onClick = { showTopMenu = true },
                                    modifier = Modifier.testTag("app_top_3dot_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "منوی گزینه‌ها",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بارگذاری اطلاعات نمونه (پروژه نمونه)", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = EmeraldAccent)
                                },
                                onClick = {
                                    showTopMenu = false
                                    showSampleConfirm = true
                                },
                                modifier = Modifier.testTag("top_menu_load_sample_data")
                            )

                            if (currentFolder != null) {
                                DropdownMenuItem(
                                    text = { Text("تغییر یا مدیریت پوشه‌ها", fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AmberAccent)
                                    },
                                    onClick = {
                                        showTopMenu = false
                                        viewModel.selectFolder(null)
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("حذف تمام اطلاعات (شروع از صفر)", fontSize = 13.sp, color = RoseAccent) },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RoseAccent)
                                },
                                onClick = {
                                    showTopMenu = false
                                    showClearConfirm = true
                                },
                                modifier = Modifier.testTag("top_menu_clear_all_data")
                            )
                        }
                    }
                }
            }

            // کادر موقت برای جست و جو زیر سرکارگر
            AnimatedVisibility(
                visible = isWorkerSearchVisible && currentFolder != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = workerSearchQuery,
                            onValueChange = { viewModel.setWorkerSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("temporary_worker_search_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (workerSearchQuery.isEmpty()) {
                                    Text(
                                        text = "جست و جوی کارگر",
                                        color = Slate400,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (workerSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setWorkerSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک کردن",
                                    tint = Slate400,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.toggleWorkerSearch(false) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن جستجو",
                                tint = RoseAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
},
        bottomBar = {
            // Show bottom navigation bar only when a folder is actively opened
            if (currentFolder != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp)
                            .border(
                                width = 1.dp,
                                color = Slate200,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(21.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AmberAccent,
                                    indicatorColor = AmberAccent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val folder = currentFolder
            if (folder == null) {
                // Initial Screen: Folders Screen (starts empty unless user creates folder or loads sample data)
                FoldersScreen(
                    viewModel = viewModel,
                    onFolderSelected = { selected ->
                        viewModel.selectFolder(selected)
                    }
                )
            } else {
                Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
                    when (tab) {
                        MainTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onChangeFolder = { viewModel.selectFolder(null) },
                            onNavigateToTab = { targetTag ->
                                when (targetTag) {
                                    "WORKERS" -> selectedTab = MainTab.WORKERS
                                    "ATTENDANCE" -> selectedTab = MainTab.ATTENDANCE
                                    "REPORTS" -> selectedTab = MainTab.REPORTS
                                }
                            }
                        )
                        MainTab.WORKERS -> WorkersScreen(viewModel = viewModel)
                        MainTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                        MainTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("حذف تمام اطلاعات") },
            text = { Text("آیا مطمئن هستید که می‌خواهید تمام پوشه‌ها و اطلاعات کارگران را پاک کنید؟ برنامه کاملاً خالی خواهد شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent)
                ) {
                    Text("حذف همه چیز", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showSampleConfirm) {
        AlertDialog(
            onDismissRequest = { showSampleConfirm = false },
            title = { Text("بارگذاری داده‌های نمونه") },
            text = { Text("همه داده‌های فعلی پاک می‌شود. آیا مطمئن هستید که می‌خواهید داده‌های نمونه را بارگذاری کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadSampleData(clearFirst = true)
                        showSampleConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                ) {
                    Text("بارگذاری داده نمونه", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSampleConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

