package com.quietgrid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.appDataStore

    @Provides
    @Singleton
    fun provideQuietGridDatabase(@ApplicationContext context: Context): QuietGridDatabase =
        Room.databaseBuilder(context, QuietGridDatabase::class.java, "quiet_grid.db").build()

    @Provides
    fun providePlayHistoryDao(database: QuietGridDatabase): PlayHistoryDao = database.playHistoryDao()
}
