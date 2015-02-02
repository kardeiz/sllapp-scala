package com.github.kardeiz.sllapp

import org.scalatra._
import javax.servlet.http.HttpServletRequest
import collection.mutable

import scala.slick.jdbc.JdbcBackend.Database

trait SllappStack 
  extends ScalatraServlet 
  with FlashMapSupport 
  with UrlGeneratorSupport
  with SlickSupport {
  notFound {
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }
}

// trait SlickSupport extends ScalatraBase {
//   def db: Database
// }