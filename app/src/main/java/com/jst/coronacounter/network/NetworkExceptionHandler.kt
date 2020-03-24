package com.jst.coronacounter.network

import retrofit2.HttpException
import java.net.SocketTimeoutException

open class NetworkExceptionHandler {

    fun handleException(exception: Exception): String =
        when (exception) {
            is HttpException -> getErrorMessage(exception.code())
            is SocketTimeoutException -> getErrorMessage(408)
            else -> getErrorMessage(null)
        }

    private fun getErrorMessage(code: Int?): String =
        when (code) {
            401 -> "Unauthorised"
            404 -> "Not found"
            408 -> "Timeout"
            else -> "Something went wrong"
        }
}