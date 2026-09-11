package com.ruleup.tti.data

import android.content.Context
import androidx.room.Room
import com.ruleup.tti.data.room.TtiDao
import com.ruleup.tti.data.room.TtiDatabase
import com.ruleup.tti.domain.TtiClock
import com.ruleup.tti.domain.TtiRecordStore
import com.ruleup.tti.domain.TtiRecorder
import com.ruleup.tti.domain.TtiShooter
import com.ruleup.tti.domain.createTtiRecorder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * `:tti:domain` 의 포트에 안드로이드 구현을 꽂는 곳.
 *
 * **[TtiShooter] 는 여기서 묶지 않는다** — 어디로 보낼지는 앱이 정할 일이라 `:app` 이 바인딩한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class TtiDataModule {
    @Binds
    @Singleton
    abstract fun bindTtiRecordStore(impl: RoomTtiRecordStore): TtiRecordStore

    @Binds
    abstract fun bindTtiClock(impl: AndroidTtiClock): TtiClock

    companion object {
        /**
         * 기록기는 도메인이 만든다. 구현 클래스를 밖으로 내보내지 않으려는 것이다.
         *
         * 디스패처를 여기서 고르는 이유는 `:tti:domain` 이 순수 JVM 이라 Android 의 IO 선택을
         * 자기가 하지 않기 때문이다.
         */
        @Provides
        @Singleton
        fun provideTtiRecorder(
            store: TtiRecordStore,
            shooter: TtiShooter,
            clock: TtiClock,
        ): TtiRecorder = createTtiRecorder(store, shooter, clock, Dispatchers.IO)

        @Provides
        @Singleton
        fun provideTtiDatabase(
            @ApplicationContext context: Context,
        ): TtiDatabase =
            Room
                .databaseBuilder(context, TtiDatabase::class.java, DATABASE_NAME)
                // 계측 데이터라 스키마가 바뀌면 마이그레이션 대신 버린다.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun provideTtiDao(database: TtiDatabase): TtiDao = database.ttiDao()

        private const val DATABASE_NAME = "tti.db"
    }
}
