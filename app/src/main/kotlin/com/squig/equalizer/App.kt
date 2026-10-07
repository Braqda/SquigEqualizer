package com.squig.equalizer

import com.squig.equalizer.web.WebServer

fun main() {
    val server = WebServer(port = 8080)
    server.start()
}
