package com.dlfsystems.are

class TestLevel(text: List<String>): AREMap {

    val tiles = ArrayList<ArrayList<Int>>(0)

    override fun getMapHeight() = tiles.size
    override fun getMapWidth() = if (tiles.size < 1) 0 else tiles[0].size

    init {
        text.forEach { line ->
            val row = ArrayList<Int>(0)
            line.forEach { c ->
                row.add(when (c) {
                    'X' -> 1
                    '.' -> 2
                    'a' -> 3
                    else -> 0
                })
            }
            tiles.add(row)
        }
    }

    override fun getTile(x: Int, y: Int, layer: Int): Int? {
        try {
            return tiles[y][x]
        } catch (e: Exception) {}
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