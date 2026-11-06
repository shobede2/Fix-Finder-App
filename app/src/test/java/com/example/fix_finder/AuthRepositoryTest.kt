package com.example.fix_finder

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.fix_finder.data.api.FixFinderApiService
import com.example.fix_finder.data.local.SessionManager
import com.example.fix_finder.data.model.*
import com.example.fix_finder.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {

    private lateinit var mockApiService: FakeApiService
    private lateinit var mockSessionManager: FakeSessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        mockApiService = FakeApiService()
        mockSessionManager = FakeSessionManager()
        authRepository = AuthRepository(mockApiService, mockSessionManager)
    }

    @Test
    fun login_withBlankFields_returnsError() = runTest {
        val result = authRepository.login("", "")
        assertTrue(result is AuthResult.Error)
        assertEquals("Please enter both email/username and password.", (result as AuthResult.Error).message)
    }

    @Test
    fun login_withValidCredentials_returnsSuccessAndSavesSession() = runTest {
        mockApiService.loginResponse = Response.success(
            LoginResponse(
                success = true,
                message = "Success",
                token = "token_abc123",
                user = User("usr_1", "Test User", "test@example.com")
            )
        )

        val result = authRepository.login("test@example.com", "P@ssword123")
        assertTrue(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        assertEquals("Test User", successResult.user.name)
        assertTrue(mockSessionManager.isLoggedIn())
        assertEquals("token_abc123", mockSessionManager.tokenState)
    }

    @Test
    fun login_withShortPassword_returnsError() = runTest {
        val result = authRepository.login("test@example.com", "Ab1!")
        assertTrue(result is AuthResult.Error)
        assertEquals("Password must be greater than 6 characters.", (result as AuthResult.Error).message)
    }

    @Test
    fun register_withInvalidEmail_returnsError() = runTest {
        val result = authRepository.register("John", "notanemail", "P@ssword123")
        assertTrue(result is AuthResult.Error)
        assertEquals("Please enter a valid email address.", (result as AuthResult.Error).message)
    }

    @Test
    fun register_withShortPassword_returnsError() = runTest {
        val result = authRepository.register("John", "john@example.com", "Ab1!")
        assertTrue(result is AuthResult.Error)
        assertEquals("Password must be greater than 6 characters.", (result as AuthResult.Error).message)
    }

    @Test
    fun register_withMissingUppercase_returnsError() = runTest {
        val result = authRepository.register("John", "john@example.com", "p@ssword123")
        assertTrue(result is AuthResult.Error)
        assertEquals("Password must contain at least one uppercase letter.", (result as AuthResult.Error).message)
    }

    @Test
    fun register_withMissingSpecialChar_returnsError() = runTest {
        val result = authRepository.register("John", "john@example.com", "Password123")
        assertTrue(result is AuthResult.Error)
        assertEquals("Password must contain at least one special character.", (result as AuthResult.Error).message)
    }

    @Test
    fun register_withValidData_returnsSuccess() = runTest {
        mockApiService.registerResponse = Response.success(
            LoginResponse(
                success = true,
                message = "Registered",
                token = "token_reg123",
                user = User("usr_reg", "John", "john@example.com")
            )
        )

        val result = authRepository.register("John", "john@example.com", "P@ssword123")
        assertTrue(result is AuthResult.Success)
        assertEquals("usr_reg", (result as AuthResult.Success).user.id)
    }

    // Fake Api Service implementation for unit testing
    class FakeApiService : FixFinderApiService {
        var loginResponse: Response<LoginResponse>? = null
        var registerResponse: Response<LoginResponse>? = null

        override suspend fun login(request: LoginRequest): Response<LoginResponse> {
            return loginResponse ?: Response.error(500, "".toResponseBody())
        }

        override suspend fun register(request: RegisterRequest): Response<LoginResponse> {
            return registerResponse ?: Response.error(500, "".toResponseBody())
        }

        override suspend fun getCategories(): Response<List<ServiceCategory>> = Response.success(emptyList())
        override suspend fun getProviders(query: String?, category: String?, location: String?): Response<List<ServiceProvider>> = Response.success(emptyList())
        override suspend fun getProviderById(id: String): Response<ServiceProvider> = Response.error(404, "".toResponseBody())
        override suspend fun createServiceRequest(request: ServiceRequestCreate): Response<ServiceRequest> = Response.error(500, "".toResponseBody())
        override suspend fun getUserRequests(userId: String): Response<List<ServiceRequest>> = Response.success(emptyList())
        override suspend fun updateSettings(settings: UserSettings): Response<UserSettings> = Response.success(settings)
    }

    // Fake Session Manager implementation for unit testing
    class FakeSessionManager : SessionManager(DummyContext()) {
        private var loggedInState = false
        var tokenState: String? = null
        private var userState: User? = null

        override fun saveAuthSession(user: User, token: String) {
            this.loggedInState = true
            this.tokenState = token
            this.userState = user
        }

        override fun isLoggedIn(): Boolean = loggedInState
        override fun getUser(): User? = userState
        override fun logout(context: Context?) {
            loggedInState = false
            tokenState = null
            userState = null
        }
    }

    class DummyContext : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            return FakeSharedPreferences()
        }
    }

    class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValue: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor { map[key!!] = value; return this }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { return this }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor { map[key!!] = value; return this }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor { map[key!!] = value; return this }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { map[key!!] = value; return this }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { map[key!!] = value; return this }
            override fun remove(key: String?): SharedPreferences.Editor { map.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { map.clear(); return this }
            override fun apply() {}
            override fun commit(): Boolean = true
        }
    }
}
