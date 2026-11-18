package com.example.i230720_smd_assignment1

import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

data class ApiResponse<T>(
    val ok: Boolean,
    val data: T?,
    val error: String?
)

fun <T> apiErrorMessage(response: Response<ApiResponse<T>>, fallback: String): String {
    response.body()?.error?.takeIf { it.isNotBlank() }?.let { return it }
    val errorBody = response.errorBody()?.string() ?: return fallback
    return try {
        JSONObject(errorBody).optString("error").ifBlank { fallback }
    } catch (_: JSONException) {
        fallback
    }
}
