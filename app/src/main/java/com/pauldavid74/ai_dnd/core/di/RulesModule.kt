package com.pauldavid74.ai_dnd.core.di

import com.pauldavid74.ai_dnd.core.rules.CombatEngine
import com.pauldavid74.ai_dnd.core.rules.DiceEngine
import com.pauldavid74.ai_dnd.core.rules.DiceParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RulesModule {

    @Provides
    @Singleton
    fun provideSecureRandom(): SecureRandom = SecureRandom()

    @Provides
    @Singleton
    fun provideDiceEngine(secureRandom: SecureRandom): DiceEngine = DiceEngine(secureRandom)

    @Provides
    @Singleton
    fun provideDiceParser(): DiceParser = DiceParser()

    @Provides
    @Singleton
    fun provideCombatEngine(diceEngine: DiceEngine): CombatEngine = CombatEngine(diceEngine)
}
