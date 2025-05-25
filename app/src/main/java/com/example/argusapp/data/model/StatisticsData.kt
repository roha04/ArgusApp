package com.example.argusapp.data.model


data class StatisticsData(
    var totalUsers: Int = 0,
    var citizenCount: Int = 0,
    var policeCount: Int = 0,
    var adminCount: Int = 0,

    var totalReports: Int = 0,
    var newReports: Int = 0,
    var inProgressReports: Int = 0,
    var resolvedReports: Int = 0,
    var closedReports: Int = 0,

    var todayReports: Int = 0,
    var todayUsers: Int = 0,
    var averageResolutionTimeHours: Double = 0.0,

    // Статистика за категоріями
    var categoryStats: Map<String, Int> = emptyMap(),

    // Статистика по регіонах/адресах
    var locationStats: Map<String, Int> = emptyMap()
)