// Project Update: Reporting Visual System Integration
package com.example.sqlaris

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import com.example.sqlaris.ui.theme.GreenSuccess
import com.example.sqlaris.ui.theme.RedError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class SessionDetailActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val apiService = ApiService.create()
    
    private var pendingRequest: QueryRequest? = null
    private var currentActiveSession: UserSession? = null

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { targetUri ->
            if (currentActiveSession != null && pendingRequest != null) {
                executeExcelDownload(currentActiveSession!!, pendingRequest!!, targetUri)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()

        setContent {
            SqlarisTheme {
                val sessions by tokenManager.sessions.collectAsState(initial = emptyList())
                val selectedName by tokenManager.selectedSessionName.collectAsState(initial = null)
                val session = sessions.find { it.name == selectedName }
                currentActiveSession = session

                var showTableSelector by remember { mutableStateOf(false) }
                var showQueryBuilder by remember { mutableStateOf(false) }
                var discoveredTables by remember { mutableStateOf<List<String>>(emptyList()) }
                var isLoadingDiscovery by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        session?.name ?: "Detalle", 
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (session != null) {
                                        SessionStatusBadge(session)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            FloatingActionButton(
                                onClick = {
                                    if (session != null && session.tables.isNotEmpty()) {
                                        showQueryBuilder = true
                                    } else {
                                        Toast.makeText(this@SessionDetailActivity, "Primero guarda algunas tablas", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Generar Reporte")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            FloatingActionButton(
                                onClick = {
                                    isLoadingDiscovery = true
                                    showTableSelector = true
                                    session?.let {
                                        fetchTables(it) { fetched ->
                                            discoveredTables = fetched
                                            isLoadingDiscovery = false
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isLoadingDiscovery) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Añadir Tablas")
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        if (session != null) {
                            StoredTablesList(
                                session = session,
                                onRemoveTable = { tableName ->
                                    lifecycleScope.launch {
                                        tokenManager.removeTable(session.name, tableName)
                                    }
                                },
                                onRemoveColumn = { tableName, columnName ->
                                    lifecycleScope.launch {
                                        val updatedTables = session.tables.map { table ->
                                            if (table.name == tableName) {
                                                table.copy(columns = table.columns.filter { it.name != columnName })
                                            } else table
                                        }
                                        tokenManager.updateSessionTables(session.name, updatedTables)
                                    }
                                }
                            )

                            if (showTableSelector) {
                                TableSelectorDialog(
                                    tables = discoveredTables,
                                    alreadySelected = session.tables.map { it.name },
                                    onDismiss = { showTableSelector = false },
                                    onConfirm = { selectedNames ->
                                        describeAndSaveTables(session, selectedNames)
                                        showTableSelector = false
                                    }
                                )
                            }

                            if (showQueryBuilder) {
                                QueryBuilderDialog(
                                    localTables = session.tables,
                                    onDismiss = { showQueryBuilder = false },
                                    onConfirm = { request ->
                                        if (request.format == "excel") {
                                            pendingRequest = request
                                            createDocumentLauncher.launch("reporte_${session.name}.xlsx")
                                        } else {
                                            handleJsonQuery(session, request)
                                        }
                                        showQueryBuilder = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SessionStatusBadge(session: UserSession) {
        var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30000)
                currentTime = System.currentTimeMillis()
            }
        }
        val secondsRemaining = session.expiresIn - (currentTime - session.issuedAt) / 1000
        val isActive = secondsRemaining > 0
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive) GreenSuccess.copy(alpha = 0.1f) else RedError.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) GreenSuccess else RedError)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) "Activa • ${secondsRemaining / 60} min" else "Inactiva",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) GreenSuccess else RedError,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StoredTablesList(
        session: UserSession, 
        onRemoveTable: (String) -> Unit,
        onRemoveColumn: (String, String) -> Unit
    ) {
        if (session.tables.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TableChart, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay tablas locales",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(session.tables, key = { it.name }) { table ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                onRemoveTable(table.name)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(RedError.copy(alpha = 0.8f))
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        },
                        modifier = Modifier.animateContentSize()
                    ) {
                        ExpandableTableItem(table = table, onRemoveColumn = { col -> onRemoveColumn(table.name, col) })
                    }
                }
            }
        }
    }

    @Composable
    fun ExpandableTableItem(table: LocalTable, onRemoveColumn: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))
                    table.columns.forEach { column ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = column.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(text = column.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            IconButton(
                                onClick = { onRemoveColumn(column.name) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp), tint = RedError.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TableSelectorDialog(
        tables: List<String>,
        alreadySelected: List<String>,
        onDismiss: () -> Unit,
        onConfirm: (List<String>) -> Unit
    ) {
        val selectedItems = remember { mutableStateListOf<String>().apply { addAll(alreadySelected) } }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Descubrir Tablas", fontWeight = FontWeight.Bold) },
            text = {
                if (tables.isEmpty()) {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(tables) { table ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (selectedItems.contains(table)) selectedItems.remove(table)
                                        else selectedItems.add(table)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedItems.contains(table),
                                    onCheckedChange = {
                                        if (it == true) selectedItems.add(table)
                                        else selectedItems.remove(table)
                                    }
                                )
                                Text(table, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(selectedItems.toList()) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar Esquemas")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun QueryBuilderDialog(
        localTables: List<LocalTable>,
        onDismiss: () -> Unit,
        onConfirm: (QueryRequest) -> Unit
    ) {
        var format by remember { mutableStateOf("excel") }
        val queryConfigs = remember { mutableStateMapOf<String, QueryConfigState>() }

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Configurar Reporte", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    item {
                        Text("Formato de Salida", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            FilterChip(
                                selected = format == "excel",
                                onClick = { format = "excel" },
                                label = { Text("Excel") },
                                leadingIcon = if (format == "excel") { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            FilterChip(
                                selected = format == "json",
                                onClick = { format = "json" },
                                label = { Text("JSON") },
                                leadingIcon = if (format == "json") { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Selección de Datos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(localTables) { table ->
                        val config = queryConfigs.getOrPut(table.name) { QueryConfigState() }
                        val isEnabled = config.isEnabled
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            border = if (isEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = config.isEnabled, onCheckedChange = { config.isEnabled = it })
                                    Text(table.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }

                                if (config.isEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Columnas:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    table.columns.forEach { col ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                            Checkbox(
                                                checked = config.selectedColumns.contains(col.name),
                                                onCheckedChange = {
                                                    if (it) config.selectedColumns.add(col.name)
                                                    else config.selectedColumns.remove(col.name)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text(col.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = config.limit,
                                            onValueChange = { config.limit = it },
                                            label = { Text("Límite") },
                                            modifier = Modifier.width(100.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { config.order = "newest" }) {
                                                RadioButton(selected = config.order == "newest", onClick = { config.order = "newest" })
                                                Text("Recientes", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { config.order = "oldest" }) {
                                                RadioButton(selected = config.order == "oldest", onClick = { config.order = "oldest" })
                                                Text("Antiguos", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalQueries = queryConfigs.filter { it.value.isEnabled && it.value.selectedColumns.isNotEmpty() }
                            .map { (tableName, state) ->
                                TableQuery(
                                    table = tableName,
                                    columns = state.selectedColumns.toList(),
                                    limit = state.limit.toIntOrNull() ?: 50,
                                    order = state.order
                                )
                            }
                        if (finalQueries.isNotEmpty()) {
                            onConfirm(QueryRequest(format = format, queries = finalQueries))
                        } else {
                            Toast.makeText(this@SessionDetailActivity, "Selecciona tablas y columnas", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Generar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    class QueryConfigState {
        var isEnabled by mutableStateOf(false)
        val selectedColumns = mutableStateListOf<String>()
        var limit by mutableStateOf("50")
        var order by mutableStateOf("newest")
    }

    private fun handleJsonQuery(session: UserSession, request: QueryRequest) {
        lifecycleScope.launch {
            try {
                Log.d("SQLARIS_DEBUG", "Query JSON: $request")
                val response = apiService.executeQuery("Bearer ${session.token}", request)
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@SessionDetailActivity, "JSON: ${response.body()?.totalQueries} consultas", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("SQLARIS_DEBUG", "Error JSON: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Excepción JSON", e)
            }
        }
    }

    private fun executeExcelDownload(session: UserSession, request: QueryRequest, uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d("SQLARIS_DEBUG", "Download Excel: $request")
                val response = apiService.downloadExcel("Bearer ${session.token}", request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val contentType = response.headers()["Content-Type"]
                    
                    if (contentType?.contains("application/json") == true) {
                        Log.e("SQLARIS_DEBUG", "Servidor envió JSON en lugar de Excel")
                        Toast.makeText(this@SessionDetailActivity, "El servidor devolvió un error JSON", Toast.LENGTH_LONG).show()
                    } else {
                        writeResponseBodyToUri(body, uri)
                    }
                } else {
                    Log.e("SQLARIS_DEBUG", "Error descarga: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Fallo descarga", e)
            }
        }
    }

    private fun writeResponseBodyToUri(body: ResponseBody, uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
                Toast.makeText(this, "Excel guardado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SQLARIS_DEBUG", "Error escritura", e)
        }
    }

    private fun describeAndSaveTables(session: UserSession, tableNames: List<String>) {
        lifecycleScope.launch {
            try {
                val response = apiService.describeTables("Bearer ${session.token}", DescribeRequest(tableNames))
                if (response.isSuccessful && response.body() != null) {
                    val localTables = response.body()!!.results.map { res ->
                        LocalTable(name = res.tableName, columns = res.columns)
                    }
                    tokenManager.updateSessionTables(session.name, localTables)
                    Toast.makeText(this@SessionDetailActivity, "Esquemas actualizados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Error describe", e)
            }
        }
    }

    private fun fetchTables(session: UserSession, onResult: (List<String>) -> Unit) {
        lifecycleScope.launch {
            try {
                val response = apiService.getTables("Bearer ${session.token}")
                if (response.isSuccessful && response.body() != null) {
                    onResult(response.body()!!.tables)
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Error fetch", e)
            }
        }
    }
}
