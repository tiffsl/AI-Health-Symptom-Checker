package com.tiffany.symptomchecker.network

import com.tiffany.symptomchecker.model.PredictRequest
import com.tiffany.symptomchecker.model.PredictResponse
import com.tiffany.symptomchecker.model.SymptomsResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SymptomApiService {
    @GET("api/symptoms")
    suspend fun getSymptoms(): Response<SymptomsResponse>

    @POST("api/predict")
    suspend fun predict(@Body request: PredictRequest): Response<PredictResponse>
}

object ApiClient {
    private const val BASE_URL = "http://127.0.0.1:5050/"

    val service: SymptomApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SymptomApiService::class.java)
    }
}
