package com.example.argusapp.data.model


data class Category(
    val id: String = "",
    val name: String = "",
    val icon: String = "", // Ресурс іконки або URL
    val description: String = "",
    val sortOrder: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        // Заготовлені категорії для використання в додатку
        fun getDefaultCategories(): List<Category> {
            return listOf(
                Category("theft", "Крадіжка", "ic_theft", "Повідомити про крадіжку майна", 1),
                Category("vandalism", "Вандалізм", "ic_vandalism", "Пошкодження громадського майна", 2),
                Category("traffic", "Порушення ПДР", "ic_traffic", "Порушення правил дорожнього руху", 3),
                Category("noise", "Шум", "ic_noise", "Порушення тиші та спокою", 4),
                Category("other", "Інше", "ic_other", "Інші правопорушення", 5)
            )
        }
    }
}