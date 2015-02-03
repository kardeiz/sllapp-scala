package com.github.kardeiz.sllapp

import org.scalatra._
import scala.slick.jdbc.JdbcBackend.Database

class MainServlet(val db: Database) extends SllappStack {

  val rootPath = get("/") { 
    layouts.html.default(this)
  }

  // val testPath = get("/test") {
  //   flash("info") = "hi"
  //   redirect("/")
  // }

  val reservationsPath = get("/reservations") {}

  val authLoginGet = get("/auth/login") {}

  val authLoginPost = post("/auth/login") {}

  val authLogoutPath = get("/auth/logout") {}

}
