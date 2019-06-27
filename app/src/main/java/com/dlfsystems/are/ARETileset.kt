package com.dlfsystems.are

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect

interface ARETileset {

    class Tile(texture: Bitmap, x: Float, y: Float, width: Float, height: Float)

    fun initialize(context: Context)

    fun getAllTextures(): List<Bitmap>

    val tileSize: Point

    fun getTile(tileCode: Int): Tile

    fun getTileTex(tileCode: Int): Bitmap
    fun getTileTexX(tileCode: Int): Float
    fun getTileTexY(tileCode: Int): Float
    fun getTileTexW(tileCode: Int): Float
    fun getTileTexH(tileCode: Int): Float
}