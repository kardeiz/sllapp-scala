package com.github.kardeiz.sllapp

import org.scalatra._
import javax.servlet.http.HttpServletRequest
import collection.mutable

trait SllappStack 
  extends ScalatraServlet 
  with FlashMapSupport 
  with UrlGeneratorSupport {

  notFound {
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }
}
