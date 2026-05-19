package com.fordham.toolbelt.util

import kotlinx.coroutines.delay

data class PlaceSuggestion(
    val name: String,
    val address: String,
    val phone: String = "",
    val website: String = ""
)

class PlacesService {
    
    // Simulating zero-trust local search for common contractor suppliers.
    private val mockPlaces = listOf(
        PlaceSuggestion("Sherwin-Williams Pro Center", "123 Paint St, Industrial Park", "(555) 012-3456", "https://www.sherwin-williams.com"),
        PlaceSuggestion("Sherwin-Williams Commercial", "456 Commercial Way", "(555) 987-6543"),
        PlaceSuggestion("Home Depot Pro Desk", "789 Contractor Ln", "(555) 111-2222"),
        PlaceSuggestion("Ferguson Plumbing Supply", "321 Pipe Rd", "(555) 333-4444"),
        PlaceSuggestion("ABC Supply Co. Inc.", "555 Roofing Ave", "(555) 444-5555"),
        PlaceSuggestion("Graybar Electric", "888 Voltage Blvd", "(555) 777-8888")
    )

    suspend fun searchPlaces(query: String): List<PlaceSuggestion> {
        if (query.isBlank() || query.length < 2) return emptyList()
        delay(200)
        return mockPlaces.filter { it.name.contains(query, ignoreCase = true) }
    }
}
