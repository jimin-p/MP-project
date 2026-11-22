package com.example.mp2026.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

object BackendErrors {
    fun message(error: Throwable): String = when (error) {
        is FirebaseNetworkException -> "인터넷 연결을 확인하고 다시 시도해 주세요."
        is FirebaseTooManyRequestsException -> "요청이 많습니다. 잠시 후 다시 시도해 주세요."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "이미 가입된 이메일입니다. 로그인해 주세요."
            "ERROR_WEAK_PASSWORD" -> "비밀번호가 보안 조건을 충족하지 않습니다. 더 강한 비밀번호를 입력해 주세요."
            "ERROR_INVALID_EMAIL" -> "이메일 형식을 확인해 주세요."
            "ERROR_OPERATION_NOT_ALLOWED" -> "이메일 로그인이 아직 활성화되지 않았습니다. 관리자에게 문의해 주세요."
            "ERROR_USER_DISABLED" -> "사용이 중지된 계정입니다."
            else -> "로그인 정보를 확인하고 다시 시도해 주세요."
        }
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "데이터 접근 권한이 없습니다. 로그인 상태와 서버 설정을 확인해 주세요."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
            else -> "데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
        }
        is IllegalArgumentException -> "학원 데이터 형식에 오류가 있습니다. 관리자에게 문의해 주세요."
        else -> "요청을 처리하지 못했습니다. 다시 시도해 주세요."
    }
}
