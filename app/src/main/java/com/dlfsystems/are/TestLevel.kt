package com.dlfsystems.are

class TestLevel: ARELevel {

    override fun getMapWidth() = 100
    override fun getMapHeight() = 100

    override fun getTile(x: Int, y: Int, layer: Int): Int? {
        if (layer == 0) {
            return (x + y) % 7
        }
        return null
    }

    override fun getLayerCount(): Int {
        return 1
    }
}