package dk.youtec.zapr.backend.exception

class BackendException(val code: Int, errorMessage: String?) : Exception(errorMessage)