package com.clarity.finance.di

import com.clarity.finance.data.repository.DraftRepository
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.data.repository.ExchangeRateRepository
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.data.repository.FirestoreDraftRepository
import com.clarity.finance.data.repository.FirestoreExpenseRepository
import com.clarity.finance.data.repository.FirestoreGoalRepository
import com.clarity.finance.data.repository.GoalRepository
import com.clarity.finance.data.repository.FirestoreIncomeRepository
import com.clarity.finance.data.repository.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: FirestoreExpenseRepository
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindDraftRepository(
        impl: FirestoreDraftRepository
    ): DraftRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        impl: FirestoreGoalRepository
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: FirestoreIncomeRepository): IncomeRepository

    @Binds
    abstract fun bindExchangeRateProvider(impl: ExchangeRateRepository): ExchangeRateProvider
}