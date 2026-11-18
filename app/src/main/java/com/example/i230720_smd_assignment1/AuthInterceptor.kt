package com.example.i230720_smd_assignment1

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.token()

        val newRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)
        if (response.code == 401) {
            tokenStore.clear()
        }
        return response
    }
}
