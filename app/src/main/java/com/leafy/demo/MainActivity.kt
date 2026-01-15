package com.leafy.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leafy.composetreeview.GraphConfig
import com.leafy.composetreeview.GTWNode
import com.leafy.composetreeview.GraphTreeView
import com.leafy.composetreeview.LineStyle
import com.leafy.composetreeview.gtwNode
import com.leafy.composetreeview.rememberZoomPanState
import com.leafy.demo.ui.theme.LeafyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeafyTheme {
                OrgChartScreen()
            }
        }
    }
}

// Data class for employee nodes
data class Employee(val name: String, val title: String)

// Color palette for different departments
object DepartmentColors {
    val CEO = Color(0xFF1A237E)       // Dark Blue
    val Technology = Color(0xFF2E7D32) // Green
    val Finance = Color(0xFF6A1B9A)    // Purple
    val Operations = Color(0xFF00838F) // Teal
}

/**
 * Creates an org chart with dynamic per-node styling.
 */
fun createStyledOrgChart(): GTWNode<Employee> = gtwNode(
    data = Employee("Sarah Johnson", "CEO"),
    color = DepartmentColors.CEO
) {
    // CTO Branch - Green theme
    gtwNode(
        data = Employee("John Smith", "CTO"),
        color = DepartmentColors.Technology
    ) {
        gtwNode(
            data = Employee("Alice Chen", "Dev Lead"),
            color = DepartmentColors.Technology
        ) {
            gtwNode(Employee("Bob Wilson", "Senior Dev"), color = Color(0xFF43A047))
            gtwNode(Employee("Carol Davis", "Developer"), color = Color(0xFF66BB6A))
        }
        // Special node with gradient background
        gtwNode(
            data = Employee("Dave Lee", "⭐ Star Engineer"),
            backgroundRes = R.drawable.bg_gradient_special
        )
    }

    // CFO Branch - Purple theme
    gtwNode(
        data = Employee("Emma Brown", "CFO"),
        color = DepartmentColors.Finance
    ) {
        gtwNode(Employee("Frank Miller", "Controller"), color = Color(0xFF8E24AA))
        gtwNode(Employee("Grace Kim", "Analyst"), color = Color(0xFFAB47BC))
    }

    // COO Branch - Teal theme
    gtwNode(
        data = Employee("Henry Taylor", "COO"),
        color = DepartmentColors.Operations
    ) {
        gtwNode(Employee("Ivy Martinez", "HR Director"), color = Color(0xFF00ACC1))
        gtwNode(Employee("Jack Anderson", "Facilities"), color = Color(0xFF26C6DA))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgChartScreen() {
    val orgData = createStyledOrgChart()
    val zoomPanState = rememberZoomPanState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dynamic Styled OrgChart")
                        Text("Per-node colors & backgrounds", style = MaterialTheme.typography.bodySmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pinch to zoom • Drag to pan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(onClick = { zoomPanState.reset() }) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                GraphTreeView(
                    root = orgData,
                    modifier = Modifier.fillMaxSize(),
                    zoomPanState = zoomPanState,
                    config = GraphConfig(
                        levelSpacing = 60.dp,
                        nodeSpacing = 25.dp,
                        lineColor = Color(0xFF78909C),
                        lineWidth = 2.dp,
                        lineStyle = LineStyle.Bezier
                    )
                ) { nodeData, color, backgroundRes ->
                    // Render node with dynamic styling
                    StyledNodeCard(
                        employee = nodeData,
                        color = color,
                        backgroundRes = backgroundRes
                    )
                }
            }
        }
    }
}

/**
 * A styled node card that uses either a solid color or a drawable background.
 */
@Composable
fun StyledNodeCard(
    employee: Employee,
    color: Color?,
    backgroundRes: Int?
) {
    val shape = RoundedCornerShape(8.dp)

    when {
        // Use drawable background if provided
        backgroundRes != null -> {
            Box(
                modifier = Modifier
                    .clip(shape)
            ) {
                // Draw the gradient/drawable background using AndroidView
                // This supports ALL drawable types (Shapes, Vectors, Bitmaps, etc.)
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.view.View(context).apply {
                            setBackgroundResource(backgroundRes)
                        }
                    },
                    modifier = Modifier.matchParentSize()
                )
                // Content on top
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        employee.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        employee.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Use solid color if provided
        color != null -> {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = color),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        employee.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        employee.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Fallback to default styling
        else -> {
            Card(
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF607D8B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        employee.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        employee.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
