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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.OutputStream

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
                        TopAppBar(
                            title = {
                                Column {
                                    Text(session?.name ?: "Detalle", style = MaterialTheme.typography.titleLarge)
                                    if (session != null) {
                                        SessionStatusRow(session)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        Column(horizontalAlignment = Alignment.End) {
                            SmallFloatingActionButton(
                                onClick = {
                                    if (session != null && session.tables.isNotEmpty()) {
                                        showQueryBuilder = true
                                    } else {
                                        Toast.makeText(this@SessionDetailActivity, "Primero guarda algunas tablas", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Generar Consulta")
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
                                }
                            ) {
                                if (isLoadingDiscovery) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Añadir Tablas")
                                }
                            }
                        }
                    },
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

    private fun handleJsonQuery(session: UserSession, request: QueryRequest) {
        lifecycleScope.launch {
            try {
                Log.d("SQLARIS_DEBUG", "Enviando query JSON: $request")
                val response = apiService.executeQuery("Bearer ${session.token}", request)
                if (response.isSuccessful && response.body() != null) {
                    Log.d("SQLARIS_DEBUG", "Respuesta JSON exitosa: ${response.body()}")
                    Toast.makeText(this@SessionDetailActivity, "Resultados JSON: ${response.body()?.totalQueries}", Toast.LENGTH_LONG).show()
                } else {
                    val error = response.errorBody()?.string()
                    Log.e("SQLARIS_DEBUG", "Error JSON del servidor: $error")
                    Toast.makeText(this@SessionDetailActivity, "Error: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Excepción en query JSON", e)
                Toast.makeText(this@SessionDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun executeExcelDownload(session: UserSession, request: QueryRequest, uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d("SQLARIS_DEBUG", "Iniciando descarga Excel para: ${session.name}")
                Log.d("SQLARIS_DEBUG", "Payload: $request")
                
                val response = apiService.downloadExcel("Bearer ${session.token}", request)
                
                if (response.isSuccessful && response.body() != null) {
                    val contentType = response.headers()["Content-Type"]
                    Log.d("SQLARIS_DEBUG", "Headers recibidos: ${response.headers()}")
                    Log.d("SQLARIS_DEBUG", "Content-Type: $contentType")
                    
                    if (contentType?.contains("application/json") == true) {
                        // El servidor envió JSON en lugar de Excel (posiblemente un error 200 con mensaje de error interno)
                        val errorJson = response.body()!!.string()
                        Log.e("SQLARIS_DEBUG", "¡Error! El servidor respondió con JSON en lugar de binario: $errorJson")
                        Toast.makeText(this@SessionDetailActivity, "Error del servidor (JSON): $errorJson", Toast.LENGTH_LONG).show()
                    } else {
                        writeResponseBodyToUri(response.body()!!, uri)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SQLARIS_DEBUG", "Error HTTP ${response.code()}: $errorBody")
                    Toast.makeText(this@SessionDetailActivity, "Error HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SQLARIS_DEBUG", "Fallo crítico en descarga", e)
                Toast.makeText(this@SessionDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun writeResponseBodyToUri(body: ResponseBody, uri: Uri) {
        try {
            Log.d("SQLARIS_DEBUG", "Abriendo flujo de escritura para URI: $uri")
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d("SQLARIS_DEBUG", "Escritura completada. Bytes copiados: $bytesCopied")
                }
                Toast.makeText(this, "Archivo Excel guardado con éxito", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("SQLARIS_DEBUG", "Error al escribir en el almacenamiento", e)
            Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_LONG).show()
        }
    }

    private fun describeAndSaveTables(session: UserSession, tableNames: List<String>) {
        lifecycleScope.launch {
            try {
                val response = apiService.describeTables("Bearer ${session.token}", DescribeRequest(tableNames))
                if (response.isSuccessful && response.body() != null) {
                    val describeResults = response.body()!!
                    val localTables = describeResults.results.map { res ->
                        LocalTable(name = res.tableName, columns = res.columns)
                    }
                    tokenManager.updateSessionTables(session.name, localTables)
                    Toast.makeText(this@SessionDetailActivity, "Esquemas actualizados", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SessionDetailActivity, "Error al obtener esquemas", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SessionDetailActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Composable
    fun SessionStatusRow(session: UserSession) {
        var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(30000)
                currentTime = System.currentTimeMillis()
            }
        }
        val secondsRemaining = session.expiresIn - (currentTime - session.issuedAt) / 1000
        val isActive = secondsRemaining > 0
        val statusText = if (isActive) "Activa • ${secondsRemaining / 60} min" else "Inactiva"
        val statusColor = if (isActive) Color(0xFF4CAF50) else Color.Red

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
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
                Text("No hay tablas guardadas. Pulsa + para añadir.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.5f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        },
                        modifier = Modifier.padding(vertical = 4.dp)
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
                .animateContentSize()
                .clickable { expanded = !expanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = table.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Columnas locales:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    table.columns.forEach { column ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = column.name, style = MaterialTheme.typography.bodyMedium)
                                Text(text = column.type, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = { onRemoveColumn(column.name) }) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar", modifier = Modifier.size(16.dp))
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
            title = { Text("Descubrir Tablas") },
            text = {
                if (tables.isEmpty()) {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(tables) { table ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (selectedItems.contains(table)) selectedItems.remove(table)
                                    else selectedItems.add(table)
                                }
                            ) {
                                Checkbox(
                                    checked = selectedItems.contains(table),
                                    onCheckedChange = {
                                        if (it == true) selectedItems.add(table)
                                        else selectedItems.remove(table)
                                    }
                                )
                                Text(table)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(selectedItems.toList()) }) {
                    Text("Guardar")
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
            title = { Text("Configurar Reporte") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    item {
                        Text("Formato:", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = format == "excel", onClick = { format = "excel" })
                            Text("Excel", modifier = Modifier.clickable { format = "excel" })
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = format == "json", onClick = { format = "json" })
                            Text("JSON", modifier = Modifier.clickable { format = "json" })
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    items(localTables) { table ->
                        var isTableSelected by remember { mutableStateOf(false) }
                        val config = queryConfigs.getOrPut(table.name) { QueryConfigState() }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTableSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isTableSelected, onCheckedChange = { isTableSelected = it })
                                    Text(table.name, fontWeight = FontWeight.Bold)
                                }

                                if (isTableSelected) {
                                    Text("Columnas:", style = MaterialTheme.typography.labelSmall)
                                    table.columns.forEach { col ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = config.selectedColumns.contains(col.name),
                                                onCheckedChange = {
                                                    if (it) config.selectedColumns.add(col.name)
                                                    else config.selectedColumns.remove(col.name)
                                                }
                                            )
                                            Text(col.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = config.limit,
                                            onValueChange = { config.limit = it },
                                            label = { Text("Límite") },
                                            modifier = Modifier.width(90.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = config.order == "newest", onClick = { config.order = "newest" })
                                                Text("Recientes", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = config.order == "oldest", onClick = { config.order = "oldest" })
                                                Text("Antiguos", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        config.isEnabled = isTableSelected
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
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
                        Toast.makeText(this@SessionDetailActivity, "Selecciona al menos una tabla y una columna", Toast.LENGTH_SHORT).show()
                    }
                }) {
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

    private fun fetchTables(session: UserSession, onResult: (List<String>) -> Unit) {
        lifecycleScope.launch {
            try {
                val response = apiService.getTables("Bearer ${session.token}")
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.connection == "Exitosa") {
                        onResult(body.tables)
                    } else {
                        Toast.makeText(this@SessionDetailActivity, "Error BD: ${body.connection}", Toast.LENGTH_LONG).show()
                        onResult(emptyList())
                    }
                } else {
                    Toast.makeText(this@SessionDetailActivity, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                Toast.makeText(this@SessionDetailActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                onResult(emptyList())
            }
        }
    }
}
