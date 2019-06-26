package com.dlfsystems.are

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AREView
    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : GLSurfaceView(context, attrs) {

    val maxTileWidth = 50
    val maxTileHeight = 100

    val renderer = ARERenderer()
    var map: AREMap? = null
    var tileset: ARETileset? = null
        set(value) {
            field = value
            renderer.loadTextures()
        }

    var zoom = 1.0
    private val center = PointF()
    private val centerTarget = PointF()

    private val screenSize = Point()
    private val displayMetrics = DisplayMetrics().also {
        (context as Activity).windowManager.defaultDisplay.getMetrics(it)
    }

    private var glProgram: Int = 0
    private var glPosition: Int = 0
    private var glTxposition: Int = 0
    private var glLight: Int = 0

    private val glTextureHandle = IntArray(1)

    class VBO(maxTiles: Int) {
        companion object {
            const val BYTES_PER_FLOAT = 4
            const val VERTS_PER_TILE = 6
            const val DATASIZE_POSITION = 2
            const val DATASIZE_TXPOSITION = 2
            const val DATASIZE_LIGHT = 1
            const val FLOATS_PER_VERT = DATASIZE_POSITION + DATASIZE_TXPOSITION + DATASIZE_LIGHT
            const val STRIDE = FLOATS_PER_VERT * BYTES_PER_FLOAT
        }
        private var cursor = 0

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
        setRenderer(renderer)
    }

    // override methods

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        screenSize.x = right - left
        screenSize.y = bottom - top
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

    // private methods


    // renderer

    inner class ARERenderer: Renderer {

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0f,0f,0f,1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            compileShaders()
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            buildFaces()
            drawFaces()
        }

        private fun compileShaders() {
            glProgram = GLES20.glCreateProgram().also { program ->
                GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also {
                    GLES20.glShaderSource(it, resources.getRawAsString(R.raw.shader_vertex))
                    GLES20.glCompileShader(it)
                    GLES20.glAttachShader(program, it)
                }
                GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also {
                    GLES20.glShaderSource(it, resources.getRawAsString(R.raw.shader_fragment))
                    GLES20.glCompileShader(it)
                    GLES20.glAttachShader(program, it)
                }
                GLES20.glLinkProgram(program)
                GLES20.glUseProgram(program)
            }
            glPosition = GLES20.glGetAttribLocation(glProgram, "position")
            GLES20.glEnableVertexAttribArray(glPosition)
            glTxposition = GLES20.glGetAttribLocation(glProgram, "txposition")
            GLES20.glEnableVertexAttribArray(glTxposition)
            glLight = GLES20.glGetAttribLocation(glProgram, "light")
            GLES20.glEnableVertexAttribArray(glLight)
        }

        fun loadTextures() {
            val bitmap = tileset!!.getAllTextures()[0]
            GLES20.glGenTextures(1, glTextureHandle, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureHandle[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }

        private fun Resources.getRawAsString(res: Int): String {
            return openRawResource(res).bufferedReader().readLines().joinToString("")
        }

        private fun buildFaces() {
            if ((tileset == null) || (map == null) || screenSize.x < 1)
                return

            if (layerVBOs.size < 1)
                allocateVBOs()

            val tileW = tileset!!.tileSize.x.toFloat()
            val tileH = tileset!!.tileSize.y.toFloat()
            val tilesAcross = screenSize.x / tileset!!.tileSize.x + 2
            val tilesDown = screenSize.y / tileset!!.tileSize.y + 2
            var offsetX = center.x - center.x.toInt()
            var offsetY = center.y - center.y.toInt()

            layerVBOs.forEachIndexed { layer, vbo ->
                vbo.clear()
                repeat (tilesDown) { y ->
                    repeat (tilesAcross) { x ->
                        map!!.getTile(x, y, layer)?.also { tilecode ->
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

        private fun allocateVBOs() {
            repeat (map!!.getLayerCount()) {
                layerVBOs.add(VBO(maxTileWidth * maxTileHeight))
            }
        }

        private fun drawFaces() {
            layerVBOs.forEach { vbo ->
                val buf = vbo.verts
                buf.position(0)
                GLES20.glVertexAttribPointer(glPosition, VBO.DATASIZE_POSITION, GLES20.GL_FLOAT, false, VBO.STRIDE, buf)
                buf.position(VBO.DATASIZE_POSITION)
                GLES20.glVertexAttribPointer(glTxposition, VBO.DATASIZE_TXPOSITION, GLES20.GL_FLOAT, false, VBO.STRIDE, buf)
                buf.position(VBO.DATASIZE_TXPOSITION)
                GLES20.glVertexAttribPointer(glLight, VBO.DATASIZE_LIGHT, GLES20.GL_FLOAT, false, VBO.STRIDE, buf)
            }
        }
    }
}