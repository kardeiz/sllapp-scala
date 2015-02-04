package com.github.kardeiz.sllapp

import org.scalatra._
import scala.slick.jdbc.JdbcBackend.Database

class MainServlet(val db: Database) extends SllappStack {

  val rootPath = get("/") { 
    layouts.html.default()()(this)
  }

  // val testPath = get("/test") {
  //   flash("info") = "hi"
  //   redirect("/")
  // }

  val reservationsPath = get("/reservations") {}

  val authLoginGet = get("/auth/login") {
    if (isAuthenticated) {
      flash("info") = "Already signed in"
      redirect( url(rootPath) )
    }
    views.html.auth_login(this)
  }

  val authLoginPost = post("/auth/login") {
    scentry.authenticate()
    if (isAuthenticated) {
      flash("success") = "Signed in successfully"
      redirect( url(rootPath) )
    } else {
      flash("danger") = "Log in failed"
      redirect( url(authLoginGet) )
    }
  }

  val authLogoutPath = get("/auth/logout") {
    scentry.logout()
    flash("success") = "Signed out successfully"
    redirect( url(rootPath) )
  }

}
