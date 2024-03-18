package com.owlting.app.stellarkit.exception

enum class ErrorType {
    Transactions_Failed
}
class KitException (val errorType: ErrorType,message:String) : Exception()