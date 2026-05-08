// Project Update: Reporting Visual System Integration
package com.example.sqlaris

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val apiService = ApiService.create()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()
        setContent {
            SqlarisTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Nueva Conexión", fontWeight = FontWeight.Bold) },
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
                    containerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        onConnectClick = { name, host, user, pass, db ->
                            performConnection(name, host, user, pass, db)
                        }
                    )
                }
            }
        }
    }

    private var isLoading = mutableStateOf(false)

    private fun performConnection(name: String, host: String, user: String, pass: String, db: String) {
        lifecycleScope.launch {
            isLoading.value = true
            try {
                val request = ConnectionRequest(host, user, pass, db)
                val response = apiService.verifyConnection(request)
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.connection == "Exitosa") {
                        val session = UserSession(
                            name = name,
                            token = body.token,
                            expiresIn = 3600,
                            issuedAt = System.currentTimeMillis(),
                            status = "ACTIVE",
                            dbName = db
                        )
                        tokenManager.saveSession(session)
                        Toast.makeText(this@LoginActivity, "Conexión Exitosa", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: ${body.connection}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Error de servidor: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }
    }

    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
        onConnectClick: (String, String, String, String, String) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var host by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var dbName by remember { mutableStateOf("") }

        var passVisible by remember { mutableStateOf(false) }

        val loading by isLoading

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Credenciales SQL",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Configura el acceso a tu base de datos remota.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo de Nombre
            StyledTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre Local (ej. Ventas)",
                placeholder = "Para identificar esta conexión"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Host
            StyledTextField(
                value = host,
                onValueChange = { host = it },
                label = "Host (IP o URL)",
                placeholder = "mysql.ejemplo.com"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Usuario
            StyledTextField(
                value = user,
                onValueChange = { user = it },
                label = "Usuario",
                placeholder = "root"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Password
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                placeholder = { Text("••••••••") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passVisible = !passVisible }) {
                        Icon(
                            imageVector = if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo DB Name
            StyledTextField(
                value = dbName,
                onValueChange = { dbName = it },
                label = "Base de Datos",
                placeholder = "mi_ecommerce"
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = { onConnectClick(name, host, user, pass, dbName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = name.isNotBlank() && host.isNotBlank() && user.isNotBlank() && pass.isNotBlank() && dbName.isNotBlank()
                ) {
                    Text("Validar y Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    @Composable
    fun StyledTextField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        placeholder: String = ""
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}
