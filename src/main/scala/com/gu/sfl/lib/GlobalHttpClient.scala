package com.gu.sfl.lib

import java.util.concurrent.TimeUnit

import okhttp3.{ConnectionPool, MediaType, OkHttpClient}

object GlobalHttpClient {
  val defaultHttpClient: OkHttpClient = new OkHttpClient.Builder()
      .connectTimeout(240, TimeUnit.SECONDS)
      .readTimeout(240, TimeUnit.SECONDS)
      .writeTimeout(240, TimeUnit.SECONDS)
      .connectionPool(new ConnectionPool(30, 30, TimeUnit.MINUTES))
      .build()

  private val applicationJsonContentType: String = "application/json; charset=utf-8"
  val applicationJsonMediaType: MediaType = MediaType.parse(applicationJsonContentType)


}
