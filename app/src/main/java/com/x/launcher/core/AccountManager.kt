package com.x.launcher.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Account(
 val username: String,
 val uuid: String,
 val accessToken: String,
 val type: AccountType,
 val isLoggedIn: Boolean = false
)

enum class AccountType { MICROSOFT, OFFLINE }

object AccountManager {

 private val _currentAccount = MutableStateFlow<Account?>(null)
 val currentAccount: StateFlow<Account?> = _currentAccount

 private val _accounts = MutableStateFlow<List<Account>>(emptyList())
 val accounts: StateFlow<List<Account>> = _accounts

 fun loginOffline(username: String): Account {
 val account = Account(
 username = username,
 uuid = generateOfflineUuid(username),
 accessToken = "offline-token",
 type = AccountType.OFFLINE,
 isLoggedIn = true
 )

 _currentAccount.value = account
 addAccount(account)
 return account
 }

 fun loginMicrosoft(
 authCode: String,
 username: String,
 uuid: String,
 accessToken: String
 ): Account {
 val account = Account(
 username = username,
 uuid = uuid,
 accessToken = accessToken,
 type = AccountType.MICROSOFT,
 isLoggedIn = true
 )

 _currentAccount.value = account
 addAccount(account)
 return account
 }

 fun logout() {
 _currentAccount.value?.let { account ->
 _accounts.value = _accounts.value.map {
 if (it.uuid == account.uuid) it.copy(isLoggedIn = false) else it
 }
 }
 _currentAccount.value = null
 }

 fun switchAccount(uuid: String) {
 _accounts.value.find { it.uuid == uuid }?.let { account ->
 _currentAccount.value = account.copy(isLoggedIn = true)
 _accounts.value = _accounts.value.map {
 if (it.uuid == uuid) it.copy(isLoggedIn = true) else it.copy(isLoggedIn = false)
 }
 }
 }

 fun isLoggedIn(): Boolean = _currentAccount.value?.isLoggedIn == true

 fun getUsername(): String = _currentAccount.value?.username ?: "Player"

 private fun addAccount(account: Account) {
 _accounts.value = _accounts.value.filterNot { it.uuid == account.uuid } + account
 }

 private fun generateOfflineUuid(username: String): String {
 return "offline-" + username.hashCode().toString().padStart(8, '0')
 }
} 
