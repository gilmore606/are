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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.core.view.ScaleGestureDetectorCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class AREView
    @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
    ) : GLSurfaceView(context, attrs), ScaleGestureDetector.OnScaleGestureListener {

    val maxTileWidth = 50
    val maxTileHeight = 100

    val renderer = ARERenderer()
    var map: AREMap? = null
    var tileset: ARETileset? = null
        set(value) {
            field = value
            renderer.loadTextures()
        }
    var texturesLoaded = false
    var glInitialized = false

    var zoom = 1.5f
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
    private var glTexture: Int = 0
    private var glUniformScreenSize: Int = 0

    private val glTextureHandle = IntArray(1)

    private val lastTouch = PointF()
    private val scaleGestureDetector = ScaleGestureDetector(context, this)

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
        var tileCount = 0
            private set


        val verts = ByteBuffer.allocateDirect(maxTiles * BYTES_PER_FLOAT * FLOATS_PER_VERT * VERTS_PER_TILE)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()

        fun clear() {
            tileCount = 0
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

            verts.position(tileCount * addV.size)
            verts.put(addV)
            tileCount++
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

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(e)
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress) {
                    val dx = e.x - lastTouch.x
                    val dy = e.y - lastTouch.y
                    moveCenter(
                        center.x - dx * (1f / tileset!!.tileSize.x) / zoom,
                        center.y - dy * (1f / tileset!!.tileSize.y) / zoom,
                        false
                    )
                }
            }
        }
        lastTouch.x = e.x
        lastTouch.y = e.y
        return true
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        detector?.also {
            val scale = it.scaleFactor
            Log.e("TOUCH", "scalefactor " + scale)
            zoom *= scale
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean { return true }

    override fun onScaleEnd(detector: ScaleGestureDetector?) { }

    // public api

    fun moveCenter(x: Float, y: Float, animate: Boolean = false) {
        centerTarget.x = x
        centerTarget.y = y
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
            Log.e("DEBUG", "onSurfaceCreated")
            glInitialized = true
            GLES20.glClearColor(0f,1f,0f,1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.e("DEBUG", "onSurfaceChanged")
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0f,0f,0f,1f)
            compileShaders()
            loadTextures()
            Log.e("DEBUG", "sending gl screensize " + width + " by " + height)
            GLES20.glUniform2f(glUniformScreenSize, width.toFloat(), height.toFloat())
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            buildFaces()
            drawFaces()
        }

        private fun compileShaders() {
            Log.e("DEBUG", "compileShaders")
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
            glTexture = GLES20.glGetUniformLocation(glProgram, "texture")
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTexture)
            glUniformScreenSize = GLES20.glGetUniformLocation(glProgram, "screenSize")

            Log.e("DEBUG", "shader param handles " + glPosition + " " + glTxposition + " " + glLight + " u " + glUniformScreenSize)
        }

        fun loadTextures() {
            if (!texturesLoaded && glInitialized) {
                Log.e("DEBUG", "loadTextures")
                val bitmap = tileset!!.getAllTextures()[0] // TODO: load all textures, split up VBOs by texture, etc
                GLES20.glGenTextures(1, glTextureHandle, 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureHandle[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

                texturesLoaded = true
            }
        }

        private fun buildFaces() {
            if ((tileset == null) || (map == null) || screenSize.x < 1)
                return

            if (layerVBOs.size < 1)
                allocateVBOs()

            val tileW = tileset!!.tileSize.x * zoom
            val tileH = tileset!!.tileSize.y * zoom
            val tilesAcross = (screenSize.x / tileW).toInt() + 2
            val tilesDown = (screenSize.y / tileH).toInt() + 2
            var offsetX = center.x - center.x.toInt().toFloat()
            var offsetY = center.y - center.y.toInt().toFloat()
            var upperleftX = center.x.toInt() - tilesAcross / 2
            var upperleftY = center.y.toInt() - tilesDown / 2

            layerVBOs.forEachIndexed { layer, vbo ->
                vbo.clear()
                repeat (tilesDown) { dy ->
                    repeat (tilesAcross) { dx ->
                        val mapX = upperleftX + dx + 1
                        val mapY = upperleftY + dy + 1
                        val light = map!!.getLight(mapX, mapY)
                        map!!.getTile(mapX, mapY, layer)?.also { tilecode ->
                            vbo.add(
                                (dx - offsetX) * tileW, (dy - offsetY) * tileH,
                                tileW, tileH,
                                tileset!!.getTileTexX(tilecode), tileset!!.getTileTexY(tilecode),
                                tileset!!.getTileTexW(tilecode), tileset!!.getTileTexH(tilecode),
                                light
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
                buf.position(VBO.DATASIZE_TXPOSITION + VBO.DATASIZE_POSITION)
                GLES20.glVertexAttribPointer(glLight, VBO.DATASIZE_LIGHT, GLES20.GL_FLOAT, false, VBO.STRIDE, buf)

                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vbo.tileCount * VBO.VERTS_PER_TILE)
            }
        }
    }
}