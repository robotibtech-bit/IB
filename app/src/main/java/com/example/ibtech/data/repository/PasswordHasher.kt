package com.example.ibtech.data.repository

import java.security.MessageDigest

/** 관리자 비밀번호 해시 (로드맵 10단계 "비밀번호 보호"). 평문을 저장하지 않는다. */
object PasswordHasher {
    fun hash(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
