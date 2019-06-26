package com.dlfsystems.are

import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.DisplayMetrics
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AREView(context: Context): GLSurfaceView(context) {

    val maxTileWidth = 50
    val maxTileHeight = 100

    val renderer = ARERenderer()
    var level: ARELevel? = null
    var tileset: ARETileset? = null

    var zoom = 1.0
    private val center = PointF()
    private val centerTarget = PointF()

    private val screenSize = Rect()
    private val displayMetrics = DisplayMetrics().also {
        (context as Activity).windowManager.defaultDisplay.getMetrics(it)
    }

    class VBO(maxTiles: Int) {
        companion object {
            const val BYTES_PER_FLOAT = 4
            const val VERTS_PER_TILE = 6
            const val FLOATS_PER_VERT = 5
        }
        var cursor = 0
        val verts = ByteBuffer.allocateDirect(maxTiles * BYTES_PER_FLOAT * FLOATS_PER_VERT * VERTS_PER_TILE)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

        fun clear() {
            cursor = 0
            verts.position(0)
        }

        private val addV = FloatArray(VERTS_PER_TILE * FLOATS_PER_VERT)
        fun add(x: Float, y: Float, w: Float, h: Float, tx: Float, ty: Float, tw: Float, th: Float, light: Float) {
            addV[0] = x
            addV[1] = y
            addV[2] = tx
            addV[3] = ty
            addV[4] = light

            addV[5] = x + w
            addV[6] = y
            addV[7] = tx + tw
            addV[8] = ty
            addV[9] = light

            addV[10] = x + w
            addV[11] = y + h
            addV[12] = tx + tw
            addV[13] = ty + th
            addV[14] = light

            addV[15] = x
            addV[16] = y
            addV[17] = tx
            addV[18] = ty
            addV[19] = light

            addV[20] = x + w
            addV[21] = y + h
            addV[22] = tx + tw
            addV[23] = ty + th
            addV[24] = light

            addV[25] = x
            addV[26] = y + h
            addV[27] = tx
            addV[28] = ty + th
            addV[29] = light

            verts.position(cursor * FLOATS_PER_VERT * VERTS_PER_TILE)
            verts.put(addV)
            cursor++
        }
    }

    private val layerVBOs: ArrayList<VBO> = ArrayList(0)

    init {
        setEGLContextClientVersion(2)

        GLES20.glCreateProgram().also { program ->
            GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also {
                GLES20.glShaderSource(it, resources.openRawResource(R.raw.shader_vertex).toString())
                GLES20.glCompileShader(it)
                GLES20.glAttachShader(program, it)
            }
            GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also {
                GLES20.glShaderSource(it, resources.openRawResource(R.raw.shader_fragment).toString())
                GLES20.glCompileShader(it)
                GLES20.glAttachShader(program, it)
            }
            GLES20.glLinkProgram(program)
            GLES20.glUseProgram(program)
        }

        setRenderer(renderer)
    }

    // override methods

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        screenSize.right = right - left
        screenSize.bottom = bottom - top
        super.onLayout(changed, left, top, right, bottom)
    }
    // public api

    fun moveCenter(x: Int, y: Int, animate: Boolean = false) {
        centerTarget.x = x.toFloat()
        centerTarget.y = y.toFloat()
        if (animate) {

        } else {
            center.x = centerTarget.x
            center.y = centerTarget.y
        }
    }

    inner class ARERenderer: Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f,0f,0f,1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            buildFaces()
        }

        private fun buildFaces() {
            if ((tileset == null) || (level == null) || screenSize.width() < 1)
                return

            if (layerVBOs.size < 1)
                buildVBOs()

            val tileW = tileset!!.tileSize.width().toFloat()
            val tileH = tileset!!.tileSize.height().toFloat()
            val tilesAcross = screenSize.width() / tileset!!.tileSize.width() + 2
            val tilesDown = screenSize.height() / tileset!!.tileSize.height() + 2
            var offsetX = center.x - center.x.toInt()
            var offsetY = center.y - center.y.toInt()

            layerVBOs.forEachIndexed { layer, vbo ->
                vbo.clear()
                repeat (tilesDown) { y ->
                    repeat (tilesAcross) { x ->
                        level!!.getTile(x, y, layer)?.also { tilecode ->
                            vbo.add(
                                x * tileW + offsetX, y * tileH + offsetY,
                                tileW, tileH,
                                tileset!!.getTileTexX(tilecode).toFloat(), tileset!!.getTileTexY(tilecode).toFloat(),
                                tileset!!.getTileTexW(tilecode).toFloat(), tileset!!.getTileTexH(tilecode).toFloat(),
                                1f
                            )
                        }
                    }
                }
            }
        }

        private fun buildVBOs() {
            repeat (level!!.getLayerCount()) {
                layerVBOs.add(VBO(maxTileWidth * maxTileHeight))
            }
        }
    }
}