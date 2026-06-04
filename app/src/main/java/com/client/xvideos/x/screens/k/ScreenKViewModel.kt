package com.client.xvideos.screens.k

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.hilt.ScreenModelKey
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Inject

class ScreenKScreenModel @Inject constructor(): ScreenModel {



}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleK {

    @Binds
    @IntoMap
    @ScreenModelKey(ScreenKScreenModel::class)
    abstract fun bindScreenKScreenModel(hiltListScreenModel: ScreenKScreenModel): ScreenModel

}