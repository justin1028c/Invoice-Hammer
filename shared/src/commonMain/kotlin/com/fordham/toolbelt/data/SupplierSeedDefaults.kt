package com.fordham.toolbelt.data

import com.fordham.toolbelt.domain.model.SupplierCategory

/**
 * Default supplier catalog. [packageName] values are Android app IDs used by
 * [com.fordham.toolbelt.util.PlatformActions.launchApp]; on iOS only [webUrl] is opened.
 */
internal object SupplierAndroidLaunchPackages {
    const val HOME_DEPOT = "com.thehomedepot"
    const val LOWES = "com.lowes.android"
    const val ACE = "com.acehardware.rewards"
    const val MENARDS = "com.menards.mobile"
    const val FERGUSON = "com.ferguson.mobile"
    const val SHERWIN = "com.sherwin.probuyplus"
    const val GRAINGER = "com.grainger.graingerapp"
    const val ABC_SUPPLY = "com.abcsupply.myabcsupply"
    const val GRAYBAR = "com.graybar.mobile"
    const val SITEONE = "com.siteone.mobile"
    const val AMAZON_BUSINESS = "com.amazon.mShop.android.business.shopping"
    const val NORTHERN_TOOL = "com.multiservice.ntca"
    const val SUNBELT = "com.sunbeltrentals.app"
    const val HILTI = "com.hilti.mobile.hiltionline"
    const val MCMASTER = "com.mcmaster.android"
}

internal fun defaultSupplierEntities(): List<SupplierEntity> = listOf(
    SupplierEntity(
        id = "home_depot",
        name = "Home Depot",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.HOME_DEPOT,
        webUrl = "https://www.homedepot.com",
        displayOrder = 0,
        logoResName = "logo_home_depot",
        isDefault = true
    ),
    SupplierEntity(
        id = "lowes",
        name = "Lowe's",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.LOWES,
        webUrl = "https://www.lowes.com",
        displayOrder = 1,
        logoResName = "logo_lowes",
        isDefault = true
    ),
    SupplierEntity(
        id = "ace",
        name = "Ace Hardware",
        category = SupplierCategory.HARDWARE.name,
        packageName = SupplierAndroidLaunchPackages.ACE,
        webUrl = "https://www.acehardware.com",
        displayOrder = 2,
        logoResName = "logo_ace",
        isDefault = true
    ),
    SupplierEntity(
        id = "menards",
        name = "Menards",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.MENARDS,
        webUrl = "https://www.menards.com",
        displayOrder = 3,
        logoResName = "logo_menards",
        isDefault = true
    ),
    SupplierEntity(
        id = "ferguson",
        name = "Ferguson",
        category = SupplierCategory.PLUMBING.name,
        packageName = SupplierAndroidLaunchPackages.FERGUSON,
        webUrl = "https://www.ferguson.com",
        displayOrder = 4,
        logoResName = "logo_ferguson",
        isDefault = true
    ),
    SupplierEntity(
        id = "sherwin",
        name = "Sherwin-Williams",
        category = SupplierCategory.PAINT.name,
        packageName = SupplierAndroidLaunchPackages.SHERWIN,
        webUrl = "https://www.sherwin-williams.com",
        displayOrder = 5,
        logoResName = "logo_sherwin",
        isDefault = true
    ),
    SupplierEntity(
        id = "grainger",
        name = "Grainger",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.GRAINGER,
        webUrl = "https://www.grainger.com",
        displayOrder = 6,
        logoResName = "logo_grainger",
        isDefault = true
    ),
    SupplierEntity(
        id = "abc_supply",
        name = "ABC Supply",
        category = SupplierCategory.ROOFING.name,
        packageName = SupplierAndroidLaunchPackages.ABC_SUPPLY,
        webUrl = "https://www.abcsupply.com",
        displayOrder = 7,
        logoResName = "logo_abc",
        isDefault = true
    ),
    SupplierEntity(
        id = "graybar",
        name = "Graybar",
        category = SupplierCategory.ELECTRICAL.name,
        packageName = SupplierAndroidLaunchPackages.GRAYBAR,
        webUrl = "https://www.graybar.com",
        displayOrder = 8,
        logoResName = "logo_graybar",
        isDefault = true
    ),
    SupplierEntity(
        id = "siteone",
        name = "SiteOne",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.SITEONE,
        webUrl = "https://www.siteone.com",
        displayOrder = 9,
        logoResName = "logo_siteone",
        isDefault = true
    ),
    SupplierEntity(
        id = "amazon_biz",
        name = "Amazon Business",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.AMAZON_BUSINESS,
        webUrl = "https://www.amazon.com/business",
        displayOrder = 10,
        logoResName = "logo_amazon",
        isDefault = true
    ),
    SupplierEntity(
        id = "northern_tool",
        name = "Northern Tool",
        category = SupplierCategory.HARDWARE.name,
        packageName = SupplierAndroidLaunchPackages.NORTHERN_TOOL,
        webUrl = "https://www.northerntool.com",
        displayOrder = 11,
        logoResName = "logo_northern",
        isDefault = true
    ),
    SupplierEntity(
        id = "sunbelt",
        name = "Sunbelt Rentals",
        category = SupplierCategory.GENERAL_SUPPLY.name,
        packageName = SupplierAndroidLaunchPackages.SUNBELT,
        webUrl = "https://www.sunbeltrentals.com",
        displayOrder = 12,
        logoResName = "logo_sunbelt",
        isDefault = true
    ),
    SupplierEntity(
        id = "hilti",
        name = "Hilti",
        category = SupplierCategory.FASTENERS.name,
        packageName = SupplierAndroidLaunchPackages.HILTI,
        webUrl = "https://www.hilti.com",
        displayOrder = 13,
        logoResName = "logo_hilti",
        isDefault = true
    ),
    SupplierEntity(
        id = "mcmaster",
        name = "McMaster-Carr",
        category = SupplierCategory.FASTENERS.name,
        packageName = SupplierAndroidLaunchPackages.MCMASTER,
        webUrl = "https://www.mcmaster.com",
        displayOrder = 14,
        logoResName = "logo_mcmaster",
        isDefault = true
    )
)
