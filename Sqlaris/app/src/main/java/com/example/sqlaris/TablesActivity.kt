// Project Update: Reporting System Visual Integration
package com.example.sqlaris

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TablesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }

        setContent {
            SqlarisTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Esquema de BD", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background,
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
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(tables) { table ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = table,
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
