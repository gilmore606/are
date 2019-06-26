package com.dlfsystems.are

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect

interface ARETileset {

    class Tile(texture: Bitmap, x: Int, y: Int, width: Int, height: Int)

    fun initialize(context: Context)

    fun getAllTextures(): List<Bitmap>

    val tileSize: Point

    fun getTile(tileCode: Int): Tile

    fun getTileTex(tileCode: Int): Bitmap
    fun getTileTexX(tileCode: Int): Int
    fun getTileTexY(tileCode: Int): Int
    fun getTileTexW(tileCode: Int): Int
    fun getTileTexH(tileCode: Int): Int
}