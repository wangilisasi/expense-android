package com.example.expensemanager.models

import java.util.Locale

const val DEFAULT_EXPENSE_CATEGORY = "Other"
const val DEFAULT_EXPENSE_SELECTION_CATEGORY = "Food"
const val DEFAULT_EXPENSE_DESCRIPTION = "Expense"
const val LEGACY_SHOPPING_CATEGORY = "Shopping"
const val LEGACY_EDUCATION_CATEGORY = "Education"
const val LEGACY_HEALTH_CATEGORY = "Health"
const val INVESTING_CATEGORY = "Investing"
const val COMMUNICATIONS_CATEGORY = "Communications"
const val SUBSCRIPTIONS_CATEGORY = "Subscriptions"

val FALLBACK_EXPENSE_CATEGORIES = listOf(
    DEFAULT_EXPENSE_SELECTION_CATEGORY,
    "Transport",
    "Entertainment",
    SUBSCRIPTIONS_CATEGORY,
    "Utilities",
    INVESTING_CATEGORY,
    COMMUNICATIONS_CATEGORY,
    "Housing",
    DEFAULT_EXPENSE_CATEGORY
)

private val PREFERRED_EXPENSE_CATEGORY_ORDER = listOf(
    DEFAULT_EXPENSE_SELECTION_CATEGORY,
    "Transport",
    "Entertainment",
    SUBSCRIPTIONS_CATEGORY,
    "Utilities",
    INVESTING_CATEGORY,
    COMMUNICATIONS_CATEGORY
)

fun normalizeExpenseCategory(rawCategory: String): String {
    val normalized = rawCategory.trim()
    if (normalized.isBlank()) return DEFAULT_EXPENSE_CATEGORY

    return when (normalized.lowercase(Locale.US)) {
        LEGACY_SHOPPING_CATEGORY.lowercase(Locale.US),
        INVESTING_CATEGORY.lowercase(Locale.US) -> INVESTING_CATEGORY
        LEGACY_EDUCATION_CATEGORY.lowercase(Locale.US),
        "communication",
        COMMUNICATIONS_CATEGORY.lowercase(Locale.US) -> COMMUNICATIONS_CATEGORY
        DEFAULT_EXPENSE_SELECTION_CATEGORY.lowercase(Locale.US) -> DEFAULT_EXPENSE_SELECTION_CATEGORY
        "transport" -> "Transport"
        "housing" -> "Housing"
        "entertainment" -> "Entertainment"
        LEGACY_HEALTH_CATEGORY.lowercase(Locale.US),
        "subscription",
        SUBSCRIPTIONS_CATEGORY.lowercase(Locale.US) -> SUBSCRIPTIONS_CATEGORY
        "utilities" -> "Utilities"
        DEFAULT_EXPENSE_CATEGORY.lowercase(Locale.US) -> DEFAULT_EXPENSE_CATEGORY
        else -> normalized
    }
}

fun normalizeExpenseCategories(categories: List<String>): List<String> {
    val normalized = categories
        .map(::normalizeExpenseCategory)
        .distinct()

    val otherPresent = DEFAULT_EXPENSE_CATEGORY in normalized
    val preferred = PREFERRED_EXPENSE_CATEGORY_ORDER.filter { it in normalized }
    val remaining = normalized
        .filterNot { it in PREFERRED_EXPENSE_CATEGORY_ORDER || it == DEFAULT_EXPENSE_CATEGORY }
        .sorted()

    return buildList {
        addAll(preferred)
        addAll(remaining)
        if (otherPresent) add(DEFAULT_EXPENSE_CATEGORY)
    }
}
