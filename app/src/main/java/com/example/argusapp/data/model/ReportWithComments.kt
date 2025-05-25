package com.example.argusapp.data.model


data class ReportWithComments(
    val report: Report = Report(),
    val comments: List<ReportComment> = emptyList(),
    val updates: List<ReportUpdate> = emptyList()
)