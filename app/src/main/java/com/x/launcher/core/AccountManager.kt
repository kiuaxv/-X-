 package com.x.launcher.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

data class Account(
 val username: String,
 val uuid: String,
 val accessToken: String,
 val type: AccountType,
 val isLoggedIn: Boolean = false,
 val xboxProfile: XboxProfile? = null
)

data class XboxProfile(
 val xboxUserId: String,
 val gamertag: String,
 val xuid: String
)

enum class AccountType { MICROSOFT, OFFLINE }

object AccountManager {

 // Microsoft OAuth config
 private const val CLIENT_ID = "00000000-0000-0000-0000-000000000000"
 private const val REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"
 private const val AUTH_URL = "https://login.live.com/oauth20_authorize.srf"
 private const val TOKEN_URL = "https://login.live.com/oauth20_token.srf"
 private const val SCOPE = "XboxLive.signin XboxLive.offline_access"

 private val _currentAccount = MutableStateFlow<Account?>(null)
 val currentAccount: StateFlow<Account?> = _currentAccount

 private val _accounts = MutableStateFlow<List<Account>>(emptyList())
 val accounts: StateFlow<List<Account>> = _accounts

 private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
 val authState: StateFlow<AuthState> = _authState

 sealed class AuthState {
 object Idle : AuthState()
 object WaitingForCode : AuthState()
 object ExchangingCode : AuthState()
 object AuthenticatingXbox : AuthState()
 object AuthenticatingMinecraft : AuthState()
 data class Success(val account: Account) : AuthState()
 data class Error(val message: String) : AuthState()
 }

 fun getAuthUrl(): String {
 val params = mapOf(
 "client_id" to CLIENT_ID,
 "response_type" to "code",
 "redirect_uri" to REDIRECT_URI,
 "scope" to SCOPE
 )

 val query = params.entries.joinToString("&") {
 "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
 }

 return "$AUTH_URL?$query"
 }

 suspend fun loginMicrosoft(authCode: String): AuthState = withContext(Dispatchers.IO) {
 try {
 // Step 1: Exchange auth code for access token
 _authState.value = AuthState.ExchangingCode
 val msToken = exchangeCodeForToken(authCode)

 // Step 2: Authenticate with Xbox Live
 _authState.value = AuthState.AuthenticatingXbox
 val xboxToken = authenticateXbox(msToken)

 // Step 3: Get Minecraft token from Xbox token
 _authState.value = AuthState.AuthenticatingMinecraft
 val mcToken = authenticateMinecraft(xboxToken)

 // Step 4: Get Minecraft profile
 val profile = getMinecraftProfile(mcToken)

 val account = Account(
 username = profile.first,
 uuid = profile.second,
 accessToken = mcToken,
 type = AccountType.MICROSOFT,
 isLoggedIn = true,
 xboxProfile = XboxProfile(
 xboxUserId = xboxToken.xboxUserId,
 gamertag = xboxToken.gamertag,
 xuid = xboxToken.xuid
 )
 )

 _currentAccount.value = account
 addAccount(account)
 _authState.value = AuthState.Success(account)

 AuthState.Success(account)
 } catch (e: Exception) {
 _authState.value = AuthState.Error(e.message ?: "Authentication failed")
 AuthState.Error(e.message ?: "Authentication failed")
 }
 }

 private data class MSToken(
 val accessToken: String,
 val refreshToken: String,
 val expiresIn: Long
 )

 private data class XboxToken(
 val token: String,
 val xboxUserId: String,
 val gamertag: String,
 val xuid: String
 )

 private fun exchangeCodeForToken(code: String): MSToken {
 val params = mapOf(
 "client_id" to CLIENT_ID,
 "code" to code,
 "grant_type" to "authorization_code",
 "redirect_uri" to REDIRECT_URI
 )

 val body = params.entries.joinToString("&") {
 "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}"
 }

 val conn = URL(TOKEN_URL).openConnection() as HttpURLConnection
 conn.requestMethod = "POST"
 conn.doOutput = true
 conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

 conn.outputStream.use { it.write(body.toByteArray()) }

 val response = conn.inputStream.bufferedReader().readText()
 val json = JSONObject(response)

 return MSToken(
 accessToken = json.getString("access_token"),
 refreshToken = json.optString("refresh_token"),
 expiresIn = json.optLong("expires_in")
 )
 }

 private fun authenticateXbox(msToken: MSToken): XboxToken {
 val requestBody = JSONObject()
 .put("Properties", JSONObject()
 .put("AuthMethod", "RPS")
 .put("SiteName", "user.auth.xboxlive.com")
 .put("RpsTicket", "d=${msToken.accessToken}")
 )
 .put("RelyingParty", "https://auth.xboxlive.com")
 .put("TokenType", "JWT")
 .toString()

 val conn = URL("https://user.auth.xboxlive.com/user/authenticate").openConnection() as HttpURLConnection
 conn.requestMethod = "POST"
 conn.doOutput = true
 conn.setRequestProperty("Content-Type", "application/json")
 conn.setRequestProperty("Accept", "application/json")

 conn.outputStream.use { it.write(requestBody.toByteArray()) }

 val response = conn.inputStream.bufferedReader().readText()
 val json = JSONObject(response)
 val token = json.getString("Token")
 val claims = json.optJSONObject("DisplayClaims")
 ?.optJSONArray("xui")
 ?.optJSONObject(0)

 return XboxToken(
 token = token,
 xboxUserId = claims?.optString("uhs") ?: "",
 gamertag = claims?.optString("gtg") ?: "",
 xuid = claims?.optString("xid") ?: ""
 )
 }

 private fun authenticateMinecraft(xboxToken: XboxToken): String {
 val requestBody = JSONObject()
 .put("identityToken", "XBL3.0 x=${xboxToken.xboxUserId};${xboxToken.token}")
 .toString()

 val conn = URL("https://api.minecraftservices.com/authentication/login_with_xbox").openConnection() as HttpURLConnection
 conn.requestMethod = "POST"
 conn.doOutput = true
 conn.setRequestProperty("Content-Type", "application/json")

 conn.outputStream.use { it.write(requestBody.toByteArray()) }

 val response = conn.inputStream.bufferedReader().readText()
 val json = JSONObject(response)

 return json.getString("access_token")
 }

 private fun getMinecraftProfile(mcToken: String): Pair<String, String> {
 val conn = URL("https://api.minecraftservices.com/minecraft/profile").openConnection() as HttpURLConnection
 conn.requestMethod = "GET"
 conn.setRequestProperty("Authorization", "Bearer $mcToken")

 val response = conn.inputStream.bufferedReader().readText()
 val json = JSONObject(response)

 val username = json.getString("name")
 val uuid = json.getString("id")

 return Pair(username, uuid)
 }

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
 _authState.value = AuthState.Success(account)
 return account
 }

 fun logout() {
 _currentAccount.value?.let { account ->
 _accounts.value = _accounts.value.map {
 if (it.uuid == account.uuid) it.copy(isLoggedIn = false) else it
 }
 }
 _currentAccount.value = null
 _authState.value = AuthState.Idle
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
