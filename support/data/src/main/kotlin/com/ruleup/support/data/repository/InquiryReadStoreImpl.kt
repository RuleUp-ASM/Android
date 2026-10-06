package com.ruleup.support.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.support.domain.repository.InquiryReadStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Qualifier

/** 문의 확인 기록 전용 DataStore 식별자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupportPrefs

/** 문의 id 를 키로 확인 당시 답변 시각을 둔다. 문의 id 는 계정마다 달라 로그아웃해도 비우지 않는다. */
class InquiryReadStoreImpl
    @Inject
    constructor(
        @SupportPrefs private val dataStore: DataStore<Preferences>,
    ) : InquiryReadStore {
        override suspend fun seenAnswers(): Map<String, String> =
            dataStore.data
                .first()
                .asMap()
                .mapNotNull { (key, value) ->
                    if (!key.name.startsWith(KEY_PREFIX) || value !is String) return@mapNotNull null
                    key.name.removePrefix(KEY_PREFIX) to value
                }.toMap()

        override suspend fun markSeen(
            inquiryId: String,
            answeredAt: String,
        ) {
            dataStore.edit { it[stringPreferencesKey(KEY_PREFIX + inquiryId)] = answeredAt }
        }

        private companion object {
            const val KEY_PREFIX = "answer_seen_"
        }
    }
