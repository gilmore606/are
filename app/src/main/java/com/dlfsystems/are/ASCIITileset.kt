package com.dlfsystems.are

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ASCIITileset(val tileDefRes: Int, val tileWidth: Int, val tileHeight: Int, val font: Typeface, val fontSize: Float): ARETileset {

    private class TileDef(
        val id: Int,
        val char: Int,
        val fgColor: String,
        val bgColor: String,
        var texX: Int? = null,
        var texY: Int? = null,
        var texW: Int? = null,
        var texH: Int? = null
    )

    private val tileDefs: ArrayList<TileDef> = ArrayList(0)
    private var texture: Bitmap? = null
    override val tileSize = Point().apply {
        x = tileWidth
        y = tileHeight
    }

    override fun initialize(context: Context) {

        tileDefs.clear()
        val rawTileDefs = ArrayList(
            Gson().fromJson<List<TileDef>>(
                context.resources.openRawResource(tileDefRes).bufferedReader().use { it.readText() },
                object : TypeToken<List<TileDef>>() {}.type
            ))
        rawTileDefs.forEach { tileDefs.add(it.id, it) }

        val bgPaint = Paint()
        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fgPaint.typeface = font
        fgPaint.textSize = fontSize

        texture = Bitmap.createBitmap(tileWidth * rawTileDefs.size, tileHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(texture)
        val scale = 1f

        tileDefs.forEach {
            it.texX = tileWidth * it.id
            it.texY = 0
            it.texW = tileWidth
            it.texH = tileHeight

            fgPaint.color = Color.parseColor(it.fgColor)
            bgPaint.color = Color.parseColor(it.bgColor)
            canvas.drawRect(it.texX!! * scale, it.texY!! * scale, (it.texW!! + it.texX!!) * scale, (it.texH!! + it.texY!!)* scale, bgPaint)
            val str = String(Character.toChars(it.char))
            val baseline = (it.texH!! * 2) / 3
            canvas.drawText(str, it.texX!!.toFloat(), baseline.toFloat(), fgPaint)
        }
    }

    override fun getAllTextures(): List<Bitmap> {
        texture?.also { return listOf(it) }
        throw IllegalStateException("getAllTextures called before texture initialize")
    }

    override fun getTile(tileCode: Int): ARETileset.Tile {
        return ARETileset.Tile(
            texture!!,
            tileDefs[tileCode].texX!!,
            tileDefs[tileCode].texY!!,
            tileDefs[tileCode].texW!!,
            tileDefs[tileCode].texH!!
        )
    }
    override fun getTileTex(tileCode: Int) = texture!!
    override fun getTileTexX(tileCode: Int) = tileDefs[tileCode].texX!!
    override fun getTileTexY(tileCode: Int) = tileDefs[tileCode].texY!!
    override fun getTileTexW(tileCode: Int) = tileDefs[tileCode].texW!!
    override fun getTileTexH(tileCode: Int) = tileDefs[tileCode].texH!!

}