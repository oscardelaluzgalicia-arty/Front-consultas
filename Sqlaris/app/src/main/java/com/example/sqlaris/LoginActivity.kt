package com.example.sqlaris

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.sqlaris.ui.theme.SqlarisTheme
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager
    private val apiService = ApiService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)
        enableEdgeToEdge()
        setContent {
            SqlarisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                        finish() // Regresar a ConnectionsActivity
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

        var hostVisible by remember { mutableStateOf(false) }
        var userVisible by remember { mutableStateOf(false) }
        var passVisible by remember { mutableStateOf(false) }
        var dbVisible by remember { mutableStateOf(false) }

        val loading by isLoading

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nueva Conexión",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la Conexión (Local)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            SensitiveField(
                value = host,
                onValueChange = { host = it },
                label = "Host (IP o URL)",
                isVisible = hostVisible,
                onToggleVisibility = { hostVisible = !hostVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SensitiveField(
                value = user,
                onValueChange = { user = it },
                label = "Usuario",
                isVisible = userVisible,
                onToggleVisibility = { userVisible = !userVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SensitiveField(
                value = pass,
                onValueChange = { pass = it },
                label = "Contraseña",
                isVisible = passVisible,
                onToggleVisibility = { passVisible = !passVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SensitiveField(
                value = dbName,
                onValueChange = { dbName = it },
                label = "Nombre de Base de Datos",
                isVisible = dbVisible,
                onToggleVisibility = { dbVisible = !dbVisible }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { onConnectClick(name, host, user, pass, dbName) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && host.isNotBlank() && user.isNotBlank() && pass.isNotBlank() && dbName.isNotBlank()
                ) {
                    Text("Conectar y Validar")
                }
            }
        }
    }

    @Composable
    fun SensitiveField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        isVisible: Boolean,
        onToggleVisibility: () -> Unit
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            singleLine = true
        )
    }
}
