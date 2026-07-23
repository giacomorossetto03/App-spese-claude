package com.personal.spese.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val EXPENSES = "expenses"
    const val INSTALLMENTS = "installments"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"

    // Milestone successive: form unico add/edit e dettaglio piano.
    const val ADD_EDIT = "addEdit?type={type}&id={id}"
    fun addEdit(type: String = "SINGLE", id: Long? = null): String =
        "addEdit?type=$type&id=${id ?: -1L}"

    const val ADD_PLAN = "addPlan"

    const val INSTALLMENT_DETAIL = "installmentDetail/{planId}"
    fun installmentDetail(planId: Long): String = "installmentDetail/$planId"

    const val RECURRING = "recurring"
    const val RECURRING_EDIT = "recurringEdit?id={id}"
    fun recurringEdit(id: Long? = null): String = "recurringEdit?id=${id ?: -1L}"

    val mainRoutes = setOf(DASHBOARD, EXPENSES, INSTALLMENTS, SETTINGS)
}
