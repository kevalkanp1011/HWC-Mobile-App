## Secure Configuration with Native Code 🚀

This documentation provides a comprehensive guide to the `native-lib.cpp`, `CMakeLists.txt`, and `KeyUtils.kt` files in the HLW Mobile App repository. These files work together to securely handle sensitive configuration values by leveraging native code.

### 🛠️ Native Library (`native-lib.cpp`)

The `native-lib.cpp` file contains the native C++ code for interacting with JNI (Java Native Interface) to expose various utility methods to an Android application.

#### Included Headers 📁
```c++
#include <jni.h>
#include <string>
#include <android/log.h>
```

#### Macros 🏷️
```c++
#define LOG_TAG "KeyUtils"
```

#### Namespace 🌐
```c++
using namespace std;
```

### JNI Functions 🔧

#### `baseTmcUrl` 🔑
```c++
extern "C" JNIEXPORT jstring JNICALL
Java_org_piramalswasthya_cho_utils_KeyUtils_baseTmcUrl(JNIEnv *env, jobject thiz) {
    std::string baseTmcUrl = BASE_TMC_URL;
    return env->NewStringUTF(baseTmcUrl.c_str());
}
```

#### `baseAmritUrl` 🕵️‍♂️
```c++
extern "C" JNIEXPORT jstring JNICALL
Java_org_piramalswasthya_cho_utils_KeyUtils_baseAmritUrl(JNIEnv *env, jobject thiz) {
    std::string baseAmritUrl = BASE_AMRIT_URL;
    return env->NewStringUTF(baseAmritUrl.c_str());
}
```

#### `baseFlwUrl` 🆔
```c++
extern "C"
JNIEXPORT jstring JNICALL
Java_org_piramalswasthya_cho_utils_KeyUtils_baseFlwUrl(JNIEnv *env, jobject thiz) {
    std::string baseFlwUrl = BASE_FLW_URL;
    return env->NewStringUTF(baseFlwUrl.c_str());
}
```

#### `baseAbhaUrl` 🌐
```c++
extern "C"
JNIEXPORT jstring JNICALL
Java_org_piramalswasthya_cho_utils_KeyUtils_baseAbhaUrl(JNIEnv *env, jobject thiz) {
    std::string baseAbhaUrl = BASE_ABHA_URL;
    return env->NewStringUTF(baseAbhaUrl.c_str());
}
```



#### `sanjeevaniApiUrl` 💬
```c++
extern "C"
JNIEXPORT jstring JNICALL
Java_org_piramalswasthya_cho_utils_KeyUtils_sanjeevaniApiUrl(JNIEnv *env, jobject thiz) {
    std::string sanjeevaniApiUrl = SANJEEVANI_API_URL;
    return env->NewStringUTF(sanjeevaniApiUrl.c_str());
}
```

### Logging 📝
Each JNI function logs its respective key or URL using the `__android_log_print` function, which helps in debugging and ensures that the correct values are being accessed.

---

### ⚙️ CMake Configuration (`CMakeLists.txt`)

The `CMakeLists.txt` file configures the CMake build system for the Sakhi project. It sets up environment variables, passes them to the compiler, and defines the build targets and link libraries.

#### Minimum CMake Version 📅
```cmake
cmake_minimum_required(VERSION 3.11)
```

#### Project Definition 📋
```cmake
project(CHO LANGUAGES CXX)
```

#### Fetch Environment Variables 🌎
```cmake
set(BASE_TMC_URL "$ENV{BASE_TMC_URL}")
set(BASE_AMRIT_URL "$ENV{BASE_AMRIT_URL}")
set(BASE_FLW_URL "$ENV{BASE_FLW_URL}")
set(BASE_ABHA_URL "$ENV{BASE_ABHA_URL}")
set(SANJEEVANI_API_URL "$ENV{SANJEEVANI_API_URL}")
```

#### Pass Values to the Compiler 🚀
```cmake
add_definitions(
        -DBASE_TMC_URL=\"${BASE_TMC_URL}\"
        -DBASE_AMRIT_URL=\"${BASE_AMRIT_URL}\"
        -DBASE_FLW_URL=\"${BASE_FLW_URL}\"
        -DBASE_ABHA_URL=\"${BASE_ABHA_URL}\"
        -DSANJEEVANI_API_URL=\"${SANJEEVANI_API_URL}\"
)
```

#### Define Library Name 📚
```cmake
set(LIBRARY_NAME "cho")
```

#### Add Source File for Shared Library 🛠️
```cmake
add_library(
    ${LIBRARY_NAME}
    SHARED
    native-lib.cpp
)
```

#### Find Log Library 🔍
```cmake
find_library(log-lib log)
```

#### Link Libraries 🔗
```cmake
target_link_libraries(
    ${LIBRARY_NAME}
    ${log-lib}
)
```

### Guidelines for Adding New Environment Variables 📝

To add new environment variables for use in `native-lib.cpp` or other native functions, follow these steps:

1. **Define the Environment Variable:**
   ```cmake
   set(NEW_VARIABLE_NAME "$ENV{NEW_VARIABLE_NAME}")
   ```

2. **Pass the Value to the Compiler:**
   ```cmake
   add_definitions(
       -DNEW_VARIABLE_NAME=\"${NEW_VARIABLE_NAME}\"
   )
   ```

3. **Use the Variable in `native-lib.cpp`:**
   ```c++
   std::string newVariable = NEW_VARIABLE_NAME;
   __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "New Variable: %s", newVariable.c_str());
   return env->NewStringUTF(newVariable.c_str());
   ```

4. **Ensure the Environment Variable is Set:**
   ```bash
   export NEW_VARIABLE_NAME="your_value"
   ```

---

### 🔒 Secure Configuration with `KeyUtils.kt`

The `KeyUtils.kt` file is a Kotlin class that provides a secure interface to access sensitive information such as keys, secrets, and URLs by leveraging native code.

#### Overview 🌐
The `KeyUtils` object securely handles sensitive configuration values. It relies on a native library called `cho` to retrieve these values, ensuring they are not exposed in the Kotlin code.

#### Initialization 🚀
```kotlin
private const val NATIVE_JNI_LIB_NAME = "cho"

init {
    try {
        System.loadLibrary(NATIVE_JNI_LIB_NAME)
        Timber.tag("KeyUtils").d(encryptedPassKey())
        Timber.tag("KeyUtils").d(abhaClientSecret())
        Timber.tag("KeyUtils").d(abhaClientID())
    } catch (e: UnsatisfiedLinkError) {
        Timber.tag("KeyUtils").e(e, "Failed to load native library")
        throw RuntimeException("Failed to load native library: $NATIVE_JNI_LIB_NAME")
    }
}
```

#### Native Methods 🔧
The `KeyUtils` object declares several external functions that are implemented in native code.

```kotlin
external fun baseTmcUrl(): String

external fun baseAmritUrl(): String

external fun baseFlwUrl(): String

external fun baseAbhaUrl(): String

external fun sanjeevaniApiUrl(): String
```

### Guidelines for Adding New Native Methods 📝

1. **Define the Native Method:**
   ```kotlin
   external fun newSensitiveData(): String
   ```

2. **Implement the Native Method:**
   ```c++
   extern "C" JNIEXPORT jstring JNICALL
   Java_org_piramalswasthya_sakhi_utils_KeyUtils_newSensitiveData(JNIEnv *env, jobject thiz) {
       std::string newData = NEW_SENSITIVE_DATA;
       __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "New Sensitive Data: %s", newData.c_str());
       return env->NewStringUTF(newData.c_str());
   }
   ```

3. **Update CMakeLists.txt:**
   ```cmake
   set(NEW_SENSITIVE_DATA "$ENV{NEW_SENSITIVE_DATA}")
   add_definitions(-DNEW_SENSITIVE_DATA=\"${NEW_SENSITIVE_DATA}\")
   ```

4. **Ensure the Environment Variable is Set:**
   ```bash
   export NEW_SENSITIVE_DATA="your_value"
   ```
