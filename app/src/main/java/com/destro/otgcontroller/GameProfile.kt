package com.destro.otgcontroller

data class ControlItem(val label: String, val x: Float, val y: Float)
data class GameProfile(val name: String, val sensitivity: Int = 50, val controls: List<ControlItem> = emptyList())
