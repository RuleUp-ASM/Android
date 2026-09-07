package com.ruleup.profile.data.di

import com.ruleup.profile.data.repository.AccountRepositoryImpl
import com.ruleup.profile.data.repository.MyPageRepositoryImpl
import com.ruleup.profile.data.repository.ProfileRepositoryImpl
import com.ruleup.profile.domain.repository.AccountRepository
import com.ruleup.profile.domain.repository.MyPageRepository
import com.ruleup.profile.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMyPageRepository(impl: MyPageRepositoryImpl): MyPageRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
