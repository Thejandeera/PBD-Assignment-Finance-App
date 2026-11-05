package com.clarity.finance.viewmodel

import com.clarity.finance.data.repository.FakeAccountRepository
import com.clarity.finance.data.repository.FakeExchangeRateProvider
import com.clarity.finance.data.repository.FakePrimaryAccountProvider
import com.clarity.finance.model.AccountType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var primaryProvider: FakePrimaryAccountProvider
    private lateinit var viewModel: AccountViewModel

    @Before
    fun setUp() {
        accountRepository = FakeAccountRepository()
        primaryProvider = FakePrimaryAccountProvider()
        viewModel = AccountViewModel(
            accountRepository,
            FakeExchangeRateProvider(rate = 300.0),
            primaryProvider
        )
    }

    @Test
    fun addEditDeleteAccount_updatesStateAndComputesMultiCurrencyTotal() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertTrue(viewModel.addAccount("Commercial Bank", AccountType.BANK, CurrencyType.LKR, "100000"))
        assertTrue(viewModel.addAccount("USD Savings", AccountType.SAVINGS, CurrencyType.USD, "100"))

        val stateAfterAdd = viewModel.uiState.value
        assertEquals(2, stateAfterAdd.accounts.size)
        // 100,000 LKR + 100 USD * 300 = 130,000 LKR
        assertEquals(130_000.0, stateAfterAdd.totalBalanceLkr, 0.01)

        val bank = stateAfterAdd.accounts.first { it.name == "Commercial Bank" }
        assertTrue(viewModel.updateAccount(bank, "Commercial Bank Main", AccountType.BANK, CurrencyType.LKR, "125000"))

        val updatedBank = viewModel.uiState.value.accounts.first { it.id == bank.id }
        assertEquals("Commercial Bank Main", updatedBank.name)
        assertEquals(125_000.0, updatedBank.currentBalance, 0.01)
        assertEquals(155_000.0, viewModel.uiState.value.totalBalanceLkr, 0.01)

        viewModel.deleteAccount(updatedBank)
        assertEquals(1, viewModel.uiState.value.accounts.size)
        assertEquals(30_000.0, viewModel.uiState.value.totalBalanceLkr, 0.01)

        job.cancel()
    }

    @Test
    fun togglePrimaryAccount_setsAndUnsetsPrimaryAndClearsOnDelete() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.addAccount("Cash", AccountType.CASH, CurrencyType.LKR, "5000")
        viewModel.addAccount("Commercial Bank", AccountType.BANK, CurrencyType.LKR, "100000", isPrimary = true)

        val bank = viewModel.uiState.value.accounts.first { it.name == "Commercial Bank" }
        assertEquals(bank.id, viewModel.uiState.value.primaryAccountId)
        // Primary account is sorted first
        assertEquals("Commercial Bank", viewModel.uiState.value.accounts.first().name)

        // Toggle off
        viewModel.togglePrimaryAccount(bank)
        assertEquals(null, viewModel.uiState.value.primaryAccountId)

        // Toggle back on, then delete -> primary cleared
        viewModel.togglePrimaryAccount(bank)
        assertEquals(bank.id, viewModel.uiState.value.primaryAccountId)
        viewModel.deleteAccount(bank)
        assertEquals(null, viewModel.uiState.value.primaryAccountId)

        job.cancel()
    }

    @Test
    fun invalidAccountInput_returnsFalseAndSavesNothing() {
        assertFalse(viewModel.addAccount("", AccountType.CASH, CurrencyType.LKR, "5000"))
        assertFalse(viewModel.addAccount("Wallet", AccountType.CASH, CurrencyType.LKR, "abc"))
        assertTrue(accountRepository.accountsState.value.isEmpty())
    }
}
