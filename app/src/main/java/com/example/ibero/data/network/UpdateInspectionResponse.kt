package com.example.ibero.data.network

import com.google.gson.annotations.SerializedName

/**
 * Data class to parse the response from the Google Apps Script API when updating an inspection record.
 * @param status The status of the API call, e.g., "SUCCESS" or "ERROR".
 * @param message An optional message from the API, e.g., a success message or an error description.
 */
data class UpdateInspectionResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null
)