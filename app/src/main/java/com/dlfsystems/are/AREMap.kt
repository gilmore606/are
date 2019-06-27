package com.dlfsystems.are

interface AREMap {

    fun getMapWidth(): Int
    fun getMapHeight(): Int
    fun getLayerCount(): Int
    fun getTile(x: Int, y: Int, layer: Int = 0): Int?
    fun getLight(x: Int, y: Int): Float
}