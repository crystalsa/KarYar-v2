package com.karyar.app.ui.screens.workers.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.karyar.app.ui.components.HairlineCard
import com.karyar.app.ui.theme.AmberAccent
import com.karyar.app.ui.theme.CyanAccent
import com.karyar.app.ui.theme.IndigoAccent
import com.karyar.app.ui.theme.RoseAccent
import com.karyar.app.ui.theme.Slate100

@Composable
fun WorkerExpensesSection(
    transitEnabled: Boolean,
    onTransitEnabledChange: (Boolean) -> Unit,
    transitAllowance: String,
    onTransitAllowanceChange: (String) -> Unit,
    transitImpact: String,
    onTransitImpactChange: (String) -> Unit,

    accommodationEnabled: Boolean,
    onAccommodationEnabledChange: (Boolean) -> Unit,
    accommodationAllowance: String,
    onAccommodationAllowanceChange: (String) -> Unit,
    accommodationImpact: String,
    onAccommodationImpactChange: (String) -> Unit,

    foodEnabled: Boolean,
    onFoodEnabledChange: (Boolean) -> Unit,
    foodAllowance: String,
    onFoodAllowanceChange: (String) -> Unit,
    foodImpact: String,
    onFoodImpactChange: (String) -> Unit,

    medicalEnabled: Boolean,
    onMedicalEnabledChange: (Boolean) -> Unit,
    medicalAllowance: String,
    onMedicalAllowanceChange: (String) -> Unit,
    medicalImpact: String,
    onMedicalImpactChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HairlineCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = Slate100,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "هزینه‌های فردی",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "+ اضافه / - کسر",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Transit
            ExpenseSettingRow(
                title = "ایاب و ذهاب",
                icon = Icons.Default.DirectionsBus,
                iconColor = CyanAccent,
                enabled = transitEnabled,
                onEnabledChange = onTransitEnabledChange,
                amount = transitAllowance,
                onAmountChange = onTransitAllowanceChange,
                impact = transitImpact,
                onImpactChange = onTransitImpactChange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Accommodation
            ExpenseSettingRow(
                title = "مسکن",
                icon = Icons.Default.Home,
                iconColor = IndigoAccent,
                enabled = accommodationEnabled,
                onEnabledChange = onAccommodationEnabledChange,
                amount = accommodationAllowance,
                onAmountChange = onAccommodationAllowanceChange,
                impact = accommodationImpact,
                onImpactChange = onAccommodationImpactChange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Food
            ExpenseSettingRow(
                title = "خوراک",
                icon = Icons.Default.LocalDining,
                iconColor = AmberAccent,
                enabled = foodEnabled,
                onEnabledChange = onFoodEnabledChange,
                amount = foodAllowance,
                onAmountChange = onFoodAllowanceChange,
                impact = foodImpact,
                onImpactChange = onFoodImpactChange
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 4. Medical
            ExpenseSettingRow(
                title = "درمان",
                icon = Icons.Default.MedicalServices,
                iconColor = RoseAccent,
                enabled = medicalEnabled,
                onEnabledChange = onMedicalEnabledChange,
                amount = medicalAllowance,
                onAmountChange = onMedicalAllowanceChange,
                impact = medicalImpact,
                onImpactChange = onMedicalImpactChange
            )
        }
    }
}
