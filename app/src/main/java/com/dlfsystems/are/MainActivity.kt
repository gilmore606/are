package com.dlfsystems.are

import android.content.Context
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val tileset = ASCIITileset(R.raw.ascii_tiles, 96, 96, Typeface.DEFAULT_BOLD, 72f)
    val level = TestLevel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tileset.initialize(this as Context)

        areView.tileset = tileset
        areView.map = level
        areView.moveCenter(20f, 20f)
    }

    override fun onPause() {
        super.onPause()
        areView.onPause()
    }
    override fun onResume() {
        super.onResume()
        areView.onResume()
    }
}
