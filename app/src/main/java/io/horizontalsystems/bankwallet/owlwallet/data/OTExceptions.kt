package io.horizontalsystems.bankwallet.owlwallet.data

class WrongPasswordException(message: String): Exception(message)

class AccountDeletedException(message: String): Exception(message)

class NotExistException(message: String): Exception(message)

class UnknownException(message: String): Exception(message)

class RefreshTokenExpiredException(message: String): Exception(message)