package com.dlfsystems.are

interface ARELevel {

    fun getMapWidth(): Int
    fun getMapHeight(): Int
    fun getLayerCount(): Int
    fun getTile(x: Int, y: Int, layer: Int = 0): Int?

}