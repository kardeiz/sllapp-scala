package com.github.kardeiz.sllapp

import org.scalatra._
import javax.servlet.http.HttpServletRequest
import collection.mutable

import scala.slick.jdbc.JdbcBackend.Database

trait SllappStack 
  extends ScalatraServlet 
  with FlashMapSupport 
  with UrlGeneratorSupport
  with SlickSupport
  with AuthenticationSupport { self: MainServlet => 

  notFound {
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  def simpleUrl(
    path: String,
    params: Iterable[(String, Any)] = Iterable.empty
  ) = url(path, params, withSessionId = false)

  def requireLogin() = {
    if( !isAuthenticated ) {
      flash("info") = "Please login"
      redirect( url(authLoginGet) )
    }
  }

  object viewHelpers {
    def isAuthenticated = self.isAuthenticated
  }

}

// trait SlickSupport extends ScalatraBase {
//   def db: Database
// }