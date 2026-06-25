package com.ruleup.network.di

import com.ruleup.network.image.AndroidImageReader
import com.ruleup.network.image.ImageReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageReaderModule {
    @Binds
    @Singleton
    abstract fun bindImageReader(impl: AndroidImageReader): ImageReader
}
