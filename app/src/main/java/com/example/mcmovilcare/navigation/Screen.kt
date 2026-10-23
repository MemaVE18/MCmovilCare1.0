package com.example.mcmovilcare.navigation

sealed class Screen(val route: String) {
    object AdminDashboard : Screen("admin_dashboard_screen")
    object AdminGestionUsuarios : Screen("admin_gestion_usuarios_screen")
    object TecnicoDashboard : Screen("tecnico_dashboard_screen")
    object TecnicoDetalleOrden : Screen("tecnico_detalle_orden_screen/{ordenId}") {
        fun createRoute(ordenId: Int) = "tecnico_detalle_orden_screen/$ordenId"
    }
}