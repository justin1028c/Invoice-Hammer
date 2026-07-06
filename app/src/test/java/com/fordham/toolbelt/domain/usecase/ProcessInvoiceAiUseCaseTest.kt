package com.fordham.toolbelt.domain.usecase

import com.fordham.toolbelt.domain.model.AiInvoiceResult
import com.fordham.toolbelt.domain.model.FailureMessage
import com.fordham.toolbelt.domain.model.InvoiceTextOutcome
import com.fordham.toolbelt.domain.model.LineItem
import com.fordham.toolbelt.domain.model.ItemsSummary
import com.fordham.toolbelt.domain.model.MoneyAmount
import com.fordham.toolbelt.domain.model.subscription.SubscriptionFeature
import com.fordham.toolbelt.domain.repository.GeminiRepository
import com.fordham.toolbelt.domain.repository.SubscriptionRepository
import com.fordham.toolbelt.domain.usecase.subscription.HasSubscriptionFeatureUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessInvoiceAiUseCaseTest {
    private val geminiRepository = mockk<GeminiRepository>()
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val hasSubscriptionFeature = HasSubscriptionFeatureUseCase(subscriptionRepository)
    private val parser = ParseVoiceInvoiceDeterministicallyUseCase()
    private val validator = ValidateVoiceInvoiceResultUseCase()

    @Test
    fun `uses deterministic parser for simple invoice command`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("Invoice John Smith for standard drywall repair, 450 dollars.", emptyList())

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("John Smith", result.result.clientName)
        assertEquals(1, result.result.items.size)
        assertEquals(450.0, result.result.items.first().amount.value, 0.01)
    }

    @Test
    fun `deterministic parser handles multi line contractor voice invoice`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "let's make an invoice for Justin Fordham at 1941 Norwalk Court Jonesboro Georgia 30236 " +
                "we did 15 ft of drywall we put mud on it we taped it and we skimmed it at $400 " +
                "we replace 3 light fixtures at $200 we also replace 3 receptacles at $200 " +
                "and did a general repair outside the house on vinyl siding for $200 " +
                "add 10 hours of labor at $50 an hour",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Justin Fordham", result.result.clientName)
        assertEquals("1941 Norwalk Court Jonesboro GA 30236", result.result.clientAddress)
        assertEquals(5, result.result.items.size)
        assertEquals(1500.0, result.result.items.sumOf { it.amount.value }, 0.01)
        assertEquals("Labor", result.result.items.last().category)
        assertEquals(10.0, result.result.items.last().quantity ?: 0.0, 0.01)
        assertEquals(50.0, result.result.items.last().unitPrice?.value ?: 0.0, 0.01)
    }

    @Test
    fun `deterministic parser handles create invoice to client phrasing`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "create an invoice to Scott Fordham at 3132 Brookhollow Drive Rex Georgia 30238 " +
                "installed drywall for $500",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Scott Fordham", result.result.clientName)
        assertEquals("3132 Brookhollow Drive Rex GA 30238", result.result.clientAddress)
        assertEquals(500.0, result.result.items.first().amount.value, 0.01)
    }

    @Test
    fun `deterministic parser handles client name at address phrasing`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "Scott Fordham at 3132 Brookhollow Drive Rex Georgia 30238 " +
                "invoice installed drywall for $500",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Scott Fordham", result.result.clientName)
        assertEquals("3132 Brookhollow Drive Rex GA 30238", result.result.clientAddress)
        assertEquals(500.0, result.result.items.first().amount.value, 0.01)
    }

    @Test
    fun `deterministic parser treats explicit each pricing as quantity times unit price`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for Justin Fordham at 1941 Norwalk Court Jonesboro Georgia 30236 " +
                "replace 3 light fixtures at $200 each",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(1, result.result.items.size)
        assertEquals(600.0, result.result.items.first().amount.value, 0.01)
        assertEquals(3.0, result.result.items.first().quantity ?: 0.0, 0.01)
        assertEquals(200.0, result.result.items.first().unitPrice?.value ?: 0.0, 0.01)
    }

    @Test
    fun `deterministic parser splits bare dollar contractor line prices`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "let's make an invoice for new client just in Fordham at 1941 Norwalk Court Jonesboro Georgia 30236 " +
                "we did a drywall job 15 ft of drywall mudded taped and skimmed $100 " +
                "we replaced two light fixtures $200 " +
                "we removed an entire room of carpet and put new carpet down for $500 " +
                "we mow the front and back yard for $300 " +
                "we took three receptacles out of the back bedroom for $200 and replaced them with new ones",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Justin Fordham", result.result.clientName)
        assertEquals("1941 Norwalk Court Jonesboro GA 30236", result.result.clientAddress)
        assertEquals(5, result.result.items.size)
        assertEquals(1300.0, result.result.items.sumOf { it.amount.value }, 0.01)
        assertEquals("Drywall", result.result.items[0].category)
        assertEquals("Electrical", result.result.items[1].category)
        assertEquals("Flooring", result.result.items[2].category)
        assertEquals("Electrical", result.result.items[4].category)
    }

    @Test
    fun `deterministic parser keeps labor hours as quantity when preceded by at`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for Cammy Fordham at 1941 Brook Court Rex Georgia 303 " +
                "we did 10 ft of plumbing underneath the house cast iron for $400 " +
                "replaced four light fixtures at $200 " +
                "we cut the front and back yard for $300 " +
                "we bought lamps from Home Depot at $200 " +
                "at 10 hours of labor at $100 an hour",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        val labor = result.result.items.last()
        assertEquals("Labor", labor.category)
        assertEquals(10.0, labor.quantity ?: 0.0, 0.01)
        assertEquals(100.0, labor.unitPrice?.value ?: 0.0, 0.01)
        assertEquals(1000.0, labor.amount.value, 0.01)
        assertEquals(2100.0, result.result.items.sumOf { it.amount.value }, 0.01)
    }

    @Test
    fun `deterministic parser splits per foot each and deposit contractor lines`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210 " +
                "installed 42 linear feet of baseboard at $6.50 per foot painted a hallway trim for 225 " +
                "repair 2 nail pops at $30 each apply at $50 deposit already paid",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Marcus Hill", result.result.clientName)
        assertEquals("1189 West Briar Court Macon GA 31210", result.result.clientAddress)
        assertEquals(3, result.result.items.size)
        assertTrue(result.result.items[0].description.value.equals("Installed 42 linear feet of baseboard", ignoreCase = true))
        assertEquals(273.0, result.result.items[0].amount.value, 0.01)
        assertEquals(42.0, result.result.items[0].quantity ?: 0.0, 0.01)
        assertEquals(6.5, result.result.items[0].unitPrice?.value ?: 0.0, 0.01)
        assertEquals("Painted hallway trim", result.result.items[1].description.value)
        assertEquals(225.0, result.result.items[1].amount.value, 0.01)
        assertTrue(result.result.items[2].description.value.equals("Repaired 2 nail pops", ignoreCase = true))
        assertEquals(60.0, result.result.items[2].amount.value, 0.01)
        assertEquals(50.0, result.result.depositAmount, 0.01)
    }

    @Test
    fun `deterministic parser stops address at zip before work details`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210 " +
                "42 linear feet of baseboard is $6.50 per foot painted the hallway trim at 225 " +
                "repaired two nail pops at $30 each and apply a $50 deposit already paid",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Marcus Hill", result.result.clientName)
        assertEquals("1189 West Briar Court Macon GA 31210", result.result.clientAddress)
        assertEquals(3, result.result.items.size)
        assertEquals(273.0, result.result.items[0].amount.value, 0.01)
        assertEquals(225.0, result.result.items[1].amount.value, 0.01)
        assertEquals(60.0, result.result.items[2].amount.value, 0.01)
        assertEquals(50.0, result.result.depositAmount, 0.01)
    }

    @Test
    fun `deterministic parser keeps cents per square foot and post price descriptions clean`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "create an invoice for Eleanor Brooks at 7426 Maple Ridge Lane Savannah Georgia 31405 " +
                "replaced three damaged deck boards at $48 each go wash the 32 ft at 75 cents per square foot " +
                "sealed back steps for 185 dollars and add a $35 material pickup fee",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Eleanor Brooks", result.result.clientName)
        assertEquals(4, result.result.items.size)
        assertTrue(result.result.items[0].description.value.equals("Replaced 3 damaged deck boards", ignoreCase = true))
        assertEquals(144.0, result.result.items[0].amount.value, 0.01)
        assertEquals("Pressure washed the 32 ft", result.result.items[1].description.value)
        assertEquals(24.0, result.result.items[1].amount.value, 0.01)
        assertEquals("Sealed back steps", result.result.items[2].description.value)
        assertEquals(185.0, result.result.items[2].amount.value, 0.01)
        assertEquals("Material pickup fee", result.result.items[3].description.value)
        assertEquals(35.0, result.result.items[3].amount.value, 0.01)
    }

    @Test
    fun `deterministic parser normalizes colon address and trailing percent tax`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "start an invoice for Nadia Coleman at 5:06 Lakeview Drive Augusta Georgia 30907 " +
                "replace one garbage disposal for $275 installed two shut off valves at $65 each " +
                "hauled away old unit for $40 charge 8% tax",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Nadia Coleman", result.result.clientName)
        assertEquals("506 Lakeview Drive Augusta GA 30907", result.result.clientAddress)
        assertEquals(3, result.result.items.size)
        assertEquals(275.0, result.result.items[0].amount.value, 0.01)
        assertEquals(130.0, result.result.items[1].amount.value, 0.01)
        assertEquals(40.0, result.result.items[2].amount.value, 0.01)
        assertEquals(8.0, result.result.taxRatePercent, 0.01)
    }

    @Test
    fun `deterministic parser repairs shut off valve stt quantity confusion`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "start an invoice for Nadia Coleman at 506 Lakeview Drive Augusta Georgia 30907 " +
                "replace one garbage disposal at 275 to shut off valve at 65 each " +
                "hauled away the old unit for $40 in charge 8% tax",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(3, result.result.items.size)
        assertTrue(result.result.items[1].description.value.equals("Installed 2 shut off valve", ignoreCase = true))
        assertEquals(130.0, result.result.items[1].amount.value, 0.01)
        assertEquals(2.0, result.result.items[1].quantity ?: 0.0, 0.01)
        assertEquals("Hauled away the old unit", result.result.items[2].description.value)
    }

    @Test
    fun `deterministic parser keeps noisy deck and wash line descriptions coherent`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "create an invoice for Eleanor Brooks at 71426 Maple Ridge Lane Savannah Georgia 31405 " +
                "I replaced three damaged deck boards at $48 each to washed 120 square feet at 75 cents per square foot " +
                "back the steps at 185 dollars at a 35 deer at a $35 markup fee",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(4, result.result.items.size)
        assertEquals("Replaced 3 damaged deck boards", result.result.items[0].description.value)
        assertEquals(144.0, result.result.items[0].amount.value, 0.01)
        assertEquals("Pressure washed 120 square feet", result.result.items[1].description.value)
        assertEquals(90.0, result.result.items[1].amount.value, 0.01)
        assertEquals("Sealed back steps", result.result.items[2].description.value)
        assertEquals(185.0, result.result.items[2].amount.value, 0.01)
        assertEquals("Markup fee", result.result.items[3].description.value)
        assertEquals(35.0, result.result.items[3].amount.value, 0.01)
    }

    @Test
    fun `deterministic parser repairs logged cents and sealed stt noise`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for Denny's Carter at 5:08 Brookstone Circle Stockbridge Georgia 30281 " +
                "pressure wash 320 ft of driveway at 75 50 cents per square foot silver front steps for $180 " +
                "replaced three cracked deck boards at $48 each",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Denny's Carter", result.result.clientName)
        assertEquals("508 Brookstone Circle Stockbridge GA 30281", result.result.clientAddress)
        assertEquals(3, result.result.items.size)
        assertEquals("Pressure washed 320 ft of driveway", result.result.items[0].description.value)
        assertEquals(240.0, result.result.items[0].amount.value, 0.01)
        assertEquals(320.0, result.result.items[0].quantity ?: 0.0, 0.01)
        assertEquals(0.75, result.result.items[0].unitPrice?.value ?: 0.0, 0.01)
        assertEquals("Sealed front steps", result.result.items[1].description.value)
        assertEquals(180.0, result.result.items[1].amount.value, 0.01)
        assertEquals("Replaced 3 cracked deck boards", result.result.items[2].description.value)
        assertEquals(144.0, result.result.items[2].amount.value, 0.01)
    }

    @Test
    fun `deterministic parser keeps action verbs in generated descriptions`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "let's make an invoice for Justin Fordham at 1941 Norwalk Court Jonesboro Georgia 30236 " +
                "we did 15 ft of drywall for $400 we replaced a ceiling fan for $200 " +
                "we replaced a toilet for $200",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(3, result.result.items.size)
        assertEquals("15 ft of drywall", result.result.items[0].description.value)
        assertEquals("Replaced ceiling fan", result.result.items[1].description.value)
        assertEquals("Replaced toilet", result.result.items[2].description.value)
    }

    @Test
    fun `deterministic parser repairs electrical stt noise from logged transcript`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "start an invoice for Denise Carter at 77 Maple Bend Court Savannah Georgia 31419 " +
                "around 35 ft of wire for a new microwave circuit at $6 per foot " +
                "installed one dedicated Outlet at $150 replaced one bad gsci breaker at $80 " +
                "and apply $1 deposit already paid",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Denise Carter", result.result.clientName)
        assertEquals("77 Maple Bend Court Savannah GA 31419", result.result.clientAddress)
        assertEquals(3, result.result.items.size)
        assertEquals("Ran 35 ft of wire for a new microwave circuit", result.result.items[0].description.value)
        assertEquals("Electrical", result.result.items[0].category)
        assertEquals(210.0, result.result.items[0].amount.value, 0.01)
        assertEquals(35.0, result.result.items[0].quantity ?: 0.0, 0.01)
        assertEquals(6.0, result.result.items[0].unitPrice?.value ?: 0.0, 0.01)
        assertEquals("Installed 1 dedicated Outlet", result.result.items[1].description.value)
        assertEquals("Electrical", result.result.items[1].category)
        assertEquals("Replaced 1 bad GFCI breaker", result.result.items[2].description.value)
        assertEquals("Electrical", result.result.items[2].category)
    }

    @Test
    fun `deterministic parser repairs incomplete electrical stt fragments from logged transcript`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "start an invoice for Denise Carter at 77 Maple Bend Court Savannah Georgia 31419 " +
                "ran 35 ft of wire for a new microwave circuit is $6 per foot " +
                "and start one dedicated La $150 for replace one break dollar deposit already paid",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Denise Carter", result.result.clientName)
        assertEquals("77 Maple Bend Court Savannah GA 31419", result.result.clientAddress)
        assertEquals(2, result.result.items.size)
        assertEquals("Ran 35 ft of wire for a new microwave circuit", result.result.items[0].description.value)
        assertEquals("Electrical", result.result.items[0].category)
        assertEquals(210.0, result.result.items[0].amount.value, 0.01)
        assertEquals("Installed 1 dedicated outlet", result.result.items[1].description.value)
        assertEquals("Electrical", result.result.items[1].category)
        assertEquals(150.0, result.result.items[1].amount.value, 0.01)
    }

    @Test
    fun `deterministic parser repairs to nail pops stt quantity confusion`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "make an invoice for client Marcus Hill at 1189 West Briar Court Macon Georgia 31210 " +
                "installed 42 linear feet of baseboard at 6.50 per foot painted the hallway trim for $225 " +
                "repaired to nail pops at $30 each and apply $50 deposit already paid",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(3, result.result.items.size)
        assertEquals("Installed 42 linear feet of baseboard", result.result.items[0].description.value)
        assertEquals(273.0, result.result.items[0].amount.value, 0.01)
        assertEquals("Painted hallway trim", result.result.items[1].description.value)
        assertEquals(225.0, result.result.items[1].amount.value, 0.01)
        assertEquals("Repaired 2 nail pops", result.result.items[2].description.value)
        assertEquals(60.0, result.result.items[2].amount.value, 0.01)
        assertEquals(2.0, result.result.items[2].quantity ?: 0.0, 0.01)
        assertEquals(50.0, result.result.depositAmount, 0.01)
    }

    @Test
    fun `deterministic parser does not put job location phrase in address field`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "let's make an invoice for new client Michael Jackson to his mansion " +
                "and replaced all of the fencing at the gate for $600 " +
                "at 10 hours of labor at $50 an hour",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Michael Jackson", result.result.clientName)
        assertEquals("", result.result.clientAddress)
        assertEquals(2, result.result.items.size)
        assertEquals(600.0, result.result.items.first().amount.value, 0.01)
        assertEquals(500.0, result.result.items.last().amount.value, 0.01)
        assertTrue(result.result.validationIssues.contains("MISSING_CLIENT_ADDRESS"))
    }

    @Test
    fun `validates repository result before returning`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        coEvery { geminiRepository.processInvoiceText(any(), any()) } returns InvoiceTextOutcome.Success(
            AiInvoiceResult(
                clientName = "  Acme  ",
                clientAddress = " ",
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Paint walls"),
                        amount = MoneyAmount(100.0),
                        category = "Service",
                        quantity = 2.0,
                        unitPrice = MoneyAmount(40.0)
                    )
                ),
                taxRatePercent = 101.0,
                confidenceScore = 5.0
            )
        )
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("complex transcript", emptyList())

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Acme", result.result.clientName)
        assertEquals(80.0, result.result.items.first().amount.value, 0.01)
        assertEquals(100.0, result.result.taxRatePercent, 0.01)
        assertEquals(1.0, result.result.confidenceScore, 0.01)
        assertTrue(result.result.validationIssues.contains("MISSING_CLIENT_ADDRESS"))
        assertTrue(result.result.validationIssues.contains("MATH_MISMATCH"))
    }

    @Test
    fun `moves address out of client name when llm combines fields`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        coEvery { geminiRepository.processInvoiceText(any(), any()) } returns InvoiceTextOutcome.Success(
            AiInvoiceResult(
                clientName = "Scott Fordham 3132 Workhollow Drive Wedgewood Georgia 30236",
                clientAddress = "3132 Brookhollow Drive Rex GA 30238",
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Drywall"),
                        amount = MoneyAmount(100.0),
                        category = "Service",
                        quantity = 1.0,
                        unitPrice = MoneyAmount(100.0)
                    )
                )
            )
        )
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("complex transcript", emptyList())

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Scott Fordham", result.result.clientName)
        assertEquals("3132 Workhollow Drive Wedgewood Georgia 30236", result.result.clientAddress)
        assertTrue(result.result.validationIssues.contains("CLIENT_NAME_CONTAINED_ADDRESS"))
    }

    @Test
    fun `removes stale missing client warning when repository provides client name`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        coEvery { geminiRepository.processInvoiceText(any(), any()) } returns InvoiceTextOutcome.Success(
            AiInvoiceResult(
                clientName = "Scott Fordham",
                clientAddress = "3132 Brookhollow Drive Rex GA 30238",
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Drywall"),
                        amount = MoneyAmount(100.0),
                        category = "Service",
                        quantity = 1.0,
                        unitPrice = MoneyAmount(100.0)
                    )
                ),
                validationIssues = listOf("MISSING_CLIENT_NAME")
            )
        )
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("complex transcript", emptyList())

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Scott Fordham", result.result.clientName)
        assertTrue("MISSING_CLIENT_NAME" !in result.result.validationIssues)
    }

    @Test
    fun `flags LLM result when spoken money is missing from parsed items`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        coEvery { geminiRepository.processInvoiceText(any(), any()) } returns InvoiceTextOutcome.Success(
            AiInvoiceResult(
                clientName = "Acme",
                clientAddress = "12 Main Street",
                items = listOf(
                    LineItem(
                        description = ItemsSummary("Drywall repair"),
                        amount = MoneyAmount(125.0),
                        category = "Service",
                        quantity = 1.0,
                        unitPrice = MoneyAmount(125.0)
                    )
                ),
                confidenceScore = 0.9
            )
        )
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("complex transcript: drywall repair for $125 and outlet repair for $80", emptyList())

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertTrue(result.result.validationIssues.contains("UNMATCHED_MONEY_AMOUNT"))
    }

    @Test
    fun `normalizes spoken number money before evidence verification`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "create an invoice for John Smith at 112 Oak Drive installed 2 ceiling fans for one hundred fifty dollars each",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(300.0, result.result.items.first().amount.value, 0.01)
        assertEquals(150.0, result.result.items.first().unitPrice?.value ?: 0.0, 0.01)
    }

    @Test
    fun `deterministic parser does not crash on painting stt material fragment`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        coEvery { geminiRepository.processInvoiceText(any(), any()) } returns
            InvoiceTextOutcome.Failure(FailureMessage("offline"))
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "created invoice for Laura Bennett at 804 Cedar Ridge Lane Augusta Georgia 30907 " +
                "2 bedrooms at $650 ate nail holes for $15 each painted hallway trim for $225 " +
                "and at $85 for materials",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals("Laura Bennett", result.result.clientName)
        assertEquals("804 Cedar Ridge Lane Augusta GA 30907", result.result.clientAddress)
        assertTrue(result.result.items.isNotEmpty())
        assertTrue(result.result.items.none { it.description.value.isBlank() })
    }

    @Test
    fun `deterministic parser assigns carpentry category to carpentry voice items`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns true
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase(
            "create an invoice for Daniel Reed at 642 Pine Hollow Road Athens Georgia 30606 " +
                "replaced 8 damaged deck boards at $45 each reinforced 2 loose stair treads at $75 each " +
                "installed one new handrail for $280",
            emptyList()
        )

        assertTrue(result is InvoiceTextOutcome.Success)
        result as InvoiceTextOutcome.Success
        assertEquals(3, result.result.items.size)
        assertTrue(result.result.items.all { it.category == "Carpentry" })
    }

    @Test
    fun `returns paywall failure when feature unavailable`() = runTest {
        every { subscriptionRepository.hasFeature(SubscriptionFeature.AiAgent) } returns false
        val useCase = ProcessInvoiceAiUseCase(geminiRepository, hasSubscriptionFeature, parser, validator)

        val result = useCase("anything", emptyList())

        assertTrue(result is InvoiceTextOutcome.Failure)
        result as InvoiceTextOutcome.Failure
        assertEquals(
            "Pro subscription required for invoice AI parsing.",
            result.error.value
        )
    }
}
