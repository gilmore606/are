package com.dlfsystems.are

class TestLevel: AREMap {

    override fun getMapWidth() = 83
    override fun getMapHeight() = 83

    override fun getTile(x: Int, y: Int, layer: Int): Int? {
        if ((x >= 0) && (x < getMapWidth()) && (y >= 0) && (y < getMapHeight())) {
            if (layer == 0) {
                return (x + y) % 6
                //return 1
            }
        }
        return null
    }

    override fun getLight(x: Int, y: Int): Float {
        //return (((x+y) % 4) - 1) * 0.25f + 0.5f
        return 1f
    }

    override fun getLayerCount(): Int {
        return 1
    }
}