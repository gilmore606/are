package com.dlfsystems.are

import android.content.res.Resources

fun Resources.getRawAsString(res: Int): String {
    return openRawResource(res).bufferedReader().readLines().joinToString("")
}

fun Resources.getRawAsLines(res: Int): List<String> {
    return openRawResource(res).bufferedReader().readLines()
}