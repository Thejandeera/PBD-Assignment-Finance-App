package com.clarity.finance.util

import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun parseBocDebitPosAtmTransaction_extractsAmountAndCardChannel() {
        val bocSms = "POS/ATM Transaction Rs 1020.00 From A/C No XXXXXXXXX838. Balance available Rs 3878.41 - Thank you for banking with BOC"
        val draft = BankSmsParser.parseSms("BOC", bocSms)

        assertNotNull(draft)
        assertEquals(DraftTransactionType.EXPENSE, draft!!.type)
        assertEquals(1020.00, draft.amount, 0.01)
        assertEquals("BOC", draft.sender)
        assertEquals("XXXXXXXXX838", draft.accountNumber)
        assertEquals(PaymentMethod.CARD, draft.paymentMethod)
    }

    @Test
    fun parseBocCreditOnlineTransfer_extractsIncomeAndOnlineChannel() {
        val bocSms = "Online Transfer Credit Rs 1010.00 To A/C No XXXXXXXXX838. Balance available Rs 2015.41 - Thank you for banking with BOC"
        val draft = BankSmsParser.parseSms("BOC", bocSms)

        assertNotNull(draft)
        assertEquals(DraftTransactionType.INCOME, draft!!.type)
        assertEquals(1010.00, draft.amount, 0.01)
        assertEquals("BOC", draft.sender)
        assertEquals("XXXXXXXXX838", draft.accountNumber)
        assertEquals(PaymentMethod.ONLINE, draft.paymentMethod)
    }

    @Test
    fun parseBocBalanceInquiry_isIgnored() {
        val balanceAlert = "XXXXXXXXXX557. Balance available Rs 15040.99 - Thank you for banking with BOC"
        val draft = BankSmsParser.parseSms("BOC", balanceAlert)

        assertNull(draft)
    }

    @Test
    fun parseComBankCardPurchase_extractsMerchantCategoryAndCard() {
        val combankSms = "Dear Cardholder, Purchase at CHATHURA BAKERS MULLERIYAWA N LK for LKR 1,690.00 on 23/10/26 01:56 PM has been authorised on your debit card ending #4261."
        val draft = BankSmsParser.parseSms("COMBANK", combankSms)

        assertNotNull(draft)
        assertEquals(DraftTransactionType.EXPENSE, draft!!.type)
        assertEquals(1690.00, draft.amount, 0.01)
        assertEquals("COMBANK", draft.sender)
        assertEquals("Card #4261", draft.accountNumber)
        assertEquals("Chathura Bakers Mulleriyawa", draft.merchantOrTitle)
        assertEquals("FOOD_AND_COFFEE", draft.suggestedCategoryId)
        assertEquals("Food & Coffee", draft.suggestedCategoryName)
        assertEquals(SpendingType.DISCRETIONARY, draft.spendingType)
        assertEquals("2026-10-23", draft.date)
        assertEquals("01:56 PM", draft.time)
    }

    @Test
    fun parseComBankOtpAndMaintenance_areIgnoredForSecurity() {
        val otpSms = "ComBank Digital-Transfer within ComBank LKR 900.00 attempted. Please use code 204351 to approve. Do NOT share this number with anyone. PbgqGoDg8jk"
        val otpDraft = BankSmsParser.parseSms("COMBANK", otpSms)
        assertNull(otpDraft)

        val maintenanceSms = "Dear Customer, a scheduled system maintenance will take place on Saturday, October 24th, from 01:00 AM to 3:00 AM SL time."
        val maintenanceDraft = BankSmsParser.parseSms("COMBANK", maintenanceSms)
        assertNull(maintenanceDraft)
    }

    @Test
    fun parseSampleList_allEntriesAreValidDrafts() {
        val samples = BankSmsParser.getSampleBankSmsList()
        assertTrue(samples.isNotEmpty())

        val parsed = samples.mapNotNull { (sender, body) ->
            BankSmsParser.parseSms(sender, body)
        }

        assertEquals(samples.size, parsed.size)
    }
}
