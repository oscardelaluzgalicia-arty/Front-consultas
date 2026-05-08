// Project Update: Reporting Visual System Integration
package com.example.sqlaris

import com.google.gson.annotations.SerializedName

data class ConnectionRequest(
    val dbHost: String,
    val dbUser: String,
    val dbPass: String,
    val dbName: String
)

data class ConnectionResponse(
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("TOKEN")
    val token: String
)

data class TablesResponse(
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("TABLES")
    val tables: List<String>
)

// Describe API Models
data class DescribeRequest(
    @SerializedName("table_names")
    val tableNames: List<String>
)

data class DescribeResponse(
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("RESULTS")
    val results: List<TableDescription>,
    @SerializedName("TOTAL_TABLES")
    val totalTables: Int,
    @SerializedName("SUCCESSFUL_TABLES")
    val successfulTables: Int
)

data class TableDescription(
    @SerializedName("table")
    val tableName: String,
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("COLUMNS")
    val columns: List<ColumnInfo>,
    @SerializedName("PRIMARY_KEY")
    val primaryKey: List<String>,
    @SerializedName("COLUMN_COUNT")
    val columnCount: Int
)

data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val default: Any?
)

// Query API Models
data class QueryRequest(
    val format: String, // "excel" o "json"
    val queries: List<TableQuery>
)

data class TableQuery(
    val table: String,
    val columns: List<String>,
    val limit: Int,
    val order: String // "newest" o "oldest"
)

data class QueryJsonResponse(
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("RESULTS")
    val results: List<QueryResult>,
    @SerializedName("TOTAL_QUERIES")
    val totalQueries: Int,
    @SerializedName("SUCCESSFUL_QUERIES")
    val successfulQueries: Int
)

data class QueryResult(
    val table: String,
    @SerializedName("ORDER")
    val order: String,
    @SerializedName("CONECCION")
    val connection: String,
    @SerializedName("COLUMNS")
    val columns: List<String>,
    @SerializedName("ROWS")
    val rows: List<Map<String, Any>>,
    @SerializedName("COUNT")
    val count: Int
)

// Local Data Storage Model
data class LocalTable(
    val name: String,
    val columns: List<ColumnInfo>
)

data class UserSession(
    val name: String,
    val token: String,
    val expiresIn: Long, // en segundos
    val issuedAt: Long,  // en milisegundos
    val status: String = "ACTIVE",
    val dbName: String = "",
    val tables: List<LocalTable> = emptyList()
)
