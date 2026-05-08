package com.example.sqlaris

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TablesActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val apiService = ApiService.create()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()
        
        val tables = mutableStateListOf<String>()
        val isLoading = mutableStateOf(true)

        lifecycleScope.launch {
            try {
                // Se usa el Flow currentToken definido en el TokenManager
                val token = tokenManager.currentToken.first()
                if (token != null) {
                    val response = apiService.getTables("Bearer $token")
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            tables.addAll(body.tables)
                        }
                    } else {
                        Toast.makeText(this@TablesActivity, "Error al obtener tablas: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@TablesActivity, "No hay sesión activa seleccionada", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TablesActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }

        setContent {
            SqlarisTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Tablas de la BD") }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    TablesScreen(
                        tables = tables,
                        isLoading = isLoading.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun TablesScreen(
        tables: List<String>,
        isLoading: Boolean,
        modifier: Modifier = Modifier
    ) {
        if (isLoading) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tables.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron tablas.")
            }
        } else {
            LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
                items(tables) { table ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = table,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
