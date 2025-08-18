package com.example.ibero.data.network

import com.google.gson.annotations.SerializedName

data class CheckHojaRutaResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: CheckHojaRutaData
)

data class CheckHojaRutaData(
    @SerializedName("exists")
    val exists: Boolean
)