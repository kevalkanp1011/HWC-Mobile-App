package org.piramalswasthya.cho.repositories

import androidx.lifecycle.LiveData
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.piramalswasthya.cho.crypt.CryptoUtil
import org.piramalswasthya.cho.database.room.dao.UserDao
import org.piramalswasthya.cho.database.shared_preferences.PreferenceDao
import org.piramalswasthya.cho.model.FingerPrint
import org.piramalswasthya.cho.model.UserCache
import org.piramalswasthya.cho.model.UserDomain
import org.piramalswasthya.cho.model.UserNetwork
import org.piramalswasthya.cho.model.fhir.SelectedOutreachProgram
import org.piramalswasthya.cho.network.AmritApiService
import org.piramalswasthya.cho.network.interceptors.TokenInsertTmcInterceptor
import org.piramalswasthya.cho.ui.login_activity.cho_login.outreach.OutreachViewModel
import org.piramalswasthya.cho.network.TmcAuthUserRequest
import org.piramalswasthya.cho.network.TmcUserVanSpDetailsRequest
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class UserRepo @Inject constructor(
    private val userDao: UserDao,
    private val preferenceDao: PreferenceDao,
    private val tmcNetworkApiService: AmritApiService
) {


    private var user: UserNetwork? = null

    suspend fun getLoggedInUser(): UserDomain? {
        return withContext(Dispatchers.IO) {
            userDao.getLoggedInUser()?.asDomainModel()
        }
    }

    private suspend fun setOutreachProgram(selectedOption: String, timestamp: String) {
        var userId = userDao.getLoggedInUser()?.userId
        val selectedOutreachProgram = SelectedOutreachProgram(
            userId = userId,
            option = selectedOption, timestamp = timestamp
        )
        userDao.insertOutreachProgram(selectedOutreachProgram)
    }

    suspend fun authenticateUser(
        userName: String,
        password: String,
        selectedOption: String,
        timestamp: String
    ): OutreachViewModel.State {
        return withContext(Dispatchers.IO) {
            val loggedInUser = userDao.getUser(userName, password)
            Timber.d("user", loggedInUser.toString())
            loggedInUser?.let {
                if (it.userName.lowercase() == userName.lowercase() && it.password == password) {
                    preferenceDao.setUserRoles(loggedInUser.roles);
                    val tokenB = preferenceDao.getPrimaryApiToken()
                    TokenInsertTmcInterceptor.setToken(
                        tokenB
                            ?: throw IllegalStateException("User logging offline without pref saved token B!")
                    )
                    it.userName = userName
                    it.loggedIn = true
                    userDao.update(loggedInUser)
                    setOutreachProgram(selectedOption, timestamp)
                    return@withContext OutreachViewModel.State.SUCCESS
                }
            }

            try {
                getTokenTmc(userName, password)
                if (user != null) {
                    Timber.d("User Auth Complete!!!!")
                    user?.loggedIn = true
                    if (userDao.getUser(userName, password)?.userName == userName) {
                        userDao.update(user!!.asCacheModel())
                    } else {
                        userDao.resetAllUsersLoggedInState()
                        userDao.insert(user!!.asCacheModel())
                    }
                    preferenceDao.registerUser(user!!)
                    setOutreachProgram(selectedOption, timestamp)
                    return@withContext OutreachViewModel.State.SUCCESS
//                        }
                }
                return@withContext OutreachViewModel.State.ERROR_SERVER
//                }
//                return@withContext OutreachViewModel.State.ERROR_INPUT
            } catch (se: SocketTimeoutException) {
                return@withContext OutreachViewModel.State.ERROR_SERVER
            } catch (ce: ConnectException) {
                return@withContext OutreachViewModel.State.ERROR_NETWORK
            } catch (ue: UnknownHostException) {
                return@withContext OutreachViewModel.State.ERROR_NETWORK
            } catch (ce: ConnectException) {
                return@withContext OutreachViewModel.State.ERROR_NETWORK
            }
        }
    }


//    suspend fun createHealthIdWithUid(createHealthIdRequest: CreateHealthIdRequest): NetworkResult<CreateHIDResponse> {
//
//        JSONObject()
//        return withContext((Dispatchers.IO)) {
//            try {
//                val response = amritApiService.createHid(createHealthIdRequest)
//                val responseBody = response.body()?.string()
//                JSONObject(responseBody)
//                when (responseBody?.let { JSONObject(it).getInt("statusCode") }) {
//                    200 -> {
//                        val data = responseBody.let { JSONObject(it).getString("data") }
//                        val result = Gson().fromJson(data, CreateHIDResponse::class.java)
//                        NetworkResult.Success(result)
//                    }
//                    5000 -> {
//                        if (JSONObject(responseBody).getString("errorMessage")
//                                .contentEquals("Invalid login key or session is expired")) {
//                            val user = userRepo.getLoggedInUser()!!
//                            userRepo.refreshTokenTmc(user.userName, user.password)
//                            createHealthIdWithUid(createHealthIdRequest)
//                        } else {
//                            NetworkResult.Error(0,JSONObject(responseBody).getString("errorMessage"))
//                        }
//                    }
//                    else -> {
//                        NetworkResult.Error(0, responseBody.toString())
//                    }
//                }
//            } catch (e: IOException) {
//                NetworkResult.Error(-1, "Unable to connect to Internet!")
//            } catch (e: JSONException) {
//                NetworkResult.Error(-2, "Invalid response! Please try again!")
//            } catch (e: SocketTimeoutException) {
//                NetworkResult.Error(-3, "Request Timed out! Please try again!")
//            } catch (e: java.lang.Exception) {
//                NetworkResult.Error(-4, e.message ?: "Unknown Error")
//            }
//        }
//    }


    private suspend fun getUserVanSpDetails(): Boolean {
        return withContext(Dispatchers.IO) {
            val response = tmcNetworkApiService.getUserVanSpDetails(
                TmcUserVanSpDetailsRequest(
                    user!!.userId,
                    user!!.serviceMapId
                )
            )
            Timber.d("User Van Sp Details : $response")
            val statusCode = response.code()
            if (statusCode == 200) {
                val responseString = response.body()?.string() ?: return@withContext false
                val responseJson = JSONObject(responseString)
                val data = responseJson.getJSONObject("data")
                val vanSpDetailsArray = data.getJSONArray("UserVanSpDetails")

                for (i in 0 until vanSpDetailsArray.length()) {
                    val vanSp = vanSpDetailsArray.getJSONObject(i)
                    val vanId = vanSp.getInt("vanID")
                    user?.vanId = vanId
                    //val name = vanSp.getString("vanNoAndType")
                    val servicePointId = vanSp.getInt("servicePointID")
                    user?.servicePointId = servicePointId
                    val servicePointName = vanSp.getString("servicePointName")
                    user?.servicePointName = servicePointName
                    val facilityId = vanSp.getInt("facilityID")
                    user?.facilityID = facilityId
                    user?.parkingPlaceId = vanSp.getInt("parkingPlaceID")

                }
                true
            } else {
                false
            }
        }
    }


    private suspend fun getTokenTmc(userName: String, password: String) {
        withContext(Dispatchers.IO) {
            try {
                val encryptedPassword = encrypt(password)

                val response =
                    tmcNetworkApiService.getJwtToken(
                        TmcAuthUserRequest(
                            userName,
                            encryptedPassword
                        )
                    )
                Timber.d("msg", response.toString())
                if (!response.isSuccessful) {
                    return@withContext
                }

                val responseBody = JSONObject(
                    response.body()?.string()
                        ?: throw IllegalStateException("Response success but data missing @ $response")
                )
                val responseStatusCode = responseBody.getInt("statusCode")
                if (responseStatusCode == 200) {
                    val data = responseBody.getJSONObject("data")
                    val token = data.getString("key")
                    val userId = data.getInt("userID")
                    Timber.d("Token", token.toString())
                    val privilegesArray = data.getJSONArray("previlegeObj")
                    val privilegesObject = privilegesArray.getJSONObject(0)
                    val rolesArray = extractRoles(privilegesObject);
//                    val roles = rolesArray;
//                    Log.i("roles are ", roles);
                    val name = data.getString("fullName")
                    user = UserNetwork(userId, userName, password, name, rolesArray)
                    val serviceId = privilegesObject.getInt("serviceID")
                    user?.serviceId = serviceId
                    val serviceMapId =
                        privilegesObject.getInt("providerServiceMapID")
                    user?.serviceMapId = serviceMapId
                    TokenInsertTmcInterceptor.setToken(token)
                    preferenceDao.registerPrimaryApiToken(token)
                    getUserVanSpDetails()
                } else {
                    val errorMessage = responseBody.getString("errorMessage")
                    Timber.d("Error Message $errorMessage")
                }
            } catch (e: retrofit2.HttpException) {
                Timber.d("Auth Failed!")
            }

        }

    }

    fun extractRoles(privilegesObject : JSONObject) : String{
        val rolesObjectArray = privilegesObject.getJSONArray("roles")
        var roles = ""
        for (i in 0 until rolesObjectArray.length()) {
            val roleObject = rolesObjectArray.getJSONObject(i)
            roles += roleObject.getString("RoleName") + ","
        }
        return roles.substring(0, roles.length - 1)
    }

     fun encrypt(password: String): String {
        val util = CryptoUtil()
        return util.encrypt(password)
    }

    suspend fun refreshTokenTmc(userName: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val encryptedPassword = encrypt(password)
                val response =
                    tmcNetworkApiService.getJwtToken(TmcAuthUserRequest(userName, encryptedPassword))
                Timber.d("JWT : $response")
                if (!response.isSuccessful) {
                    return@withContext false
                }
                val responseBody = JSONObject(
                    response.body()?.string()
                        ?: throw IllegalStateException("Response success but data missing @ $response")
                )
                val responseStatusCode = responseBody.getInt("statusCode")
                if (responseStatusCode == 200) {
                    val data = responseBody.getJSONObject("data")
                    val token = data.getString("key")
                    TokenInsertTmcInterceptor.setToken(token)
                    preferenceDao.registerPrimaryApiToken(token)
                    return@withContext true
                } else {
                    val errorMessage = responseBody.getString("errorMessage")
                    Timber.d("Error Message $errorMessage")
                }
                return@withContext false
            } catch (se: SocketTimeoutException) {
                return@withContext refreshTokenTmc(userName, password)
            } catch (e: HttpException) {
                Timber.d("Auth Failed!")
                return@withContext false
            }


        }

    }
    suspend fun getUserCacheDetails(): UserCache?{
        return withContext(Dispatchers.IO){
            try {
                return@withContext userDao.getLoggedInUser()
            } catch (e: Exception) {
                Timber.d("Error in finding loggedIn user $e")
                return@withContext null
            }
        }
    }

    suspend fun insertFPDataToLocalDB(fpList: List<FingerPrint>){
        return withContext(Dispatchers.IO){
            try{
                for(item in fpList){
                    userDao.insertFpData(item)
                }
            } catch (e: Exception){
                Timber.d("Error in inserting Finger Print Data $e")
            }
        }
    }

    fun getFPDataFromLocalDB(): LiveData<List<FingerPrint>>{
        return  userDao.getAllFpData()
    }

}