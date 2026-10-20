package com.client.xvideos.common.rotation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Блокировка поворота для [RotationLockKeeper] — системные настройки устройства. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RotationModule {

    @Binds
    abstract fun bindRotationSettings(impl: SystemRotationSettings): RotationSettings
}
