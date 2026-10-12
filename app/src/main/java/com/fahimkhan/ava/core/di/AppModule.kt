package com.fahimkhan.ava.core.di

import android.content.Context
import androidx.room.Room
import com.fahimkhan.ava.admin.AdminRepository
import com.fahimkhan.ava.ai.brain.AiBrainRepository
import com.fahimkhan.ava.ai.memory.AvaDatabase
import com.fahimkhan.ava.ai.memory.MemoryRepository
import com.fahimkhan.ava.ai.personality.PersonalityEngine
import com.fahimkhan.ava.core.utils.ApiKeyStore
import com.fahimkhan.ava.security.owner.OwnerSecuritySystem
import com.fahimkhan.ava.voice.engine.VoiceEngine
import com.fahimkhan.ava.voice.wake.WakeWordDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun provideAvaDatabase(@ApplicationContext ctx: Context): AvaDatabase = Room.databaseBuilder(ctx, AvaDatabase::class.java, "ava_neural_memory.db").fallbackToDestructiveMigration().build()
    @Provides @Singleton fun provideMemoryRepository(db: AvaDatabase) = MemoryRepository(db)
    @Provides @Singleton fun providePersonalityEngine(@ApplicationContext ctx: Context) = PersonalityEngine(ctx)
    @Provides @Singleton fun provideApiKeyStore(@ApplicationContext ctx: Context) = ApiKeyStore(ctx)
    @Provides @Singleton fun provideAiBrainRepository(@ApplicationContext ctx: Context, p: PersonalityEngine, m: MemoryRepository, keys: ApiKeyStore) = AiBrainRepository(ctx, p, m, keys)
    @Provides @Singleton fun provideVoiceEngine(@ApplicationContext ctx: Context, keys: ApiKeyStore) = VoiceEngine(ctx, keys)
    @Provides @Singleton fun provideWakeWordDetector(@ApplicationContext ctx: Context) = WakeWordDetector(ctx)
    @Provides @Singleton fun provideOwnerSecuritySystem(@ApplicationContext ctx: Context, m: MemoryRepository) = OwnerSecuritySystem(ctx, m)
    @Provides @Singleton fun provideAdminRepository(m: MemoryRepository, @ApplicationContext ctx: Context) = AdminRepository(m, ctx)
}
