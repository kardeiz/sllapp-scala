package com.github.kardeiz.sllapp

import org.scalatra._
// import javax.servlet.http.HttpServletRequest
// import collection.mutable

trait SllappStack 
  extends ScalatraServlet 
  with FlashMapSupport 
  with UrlGeneratorSupport
  with AuthenticationSupport 
  with MethodOverride { self: MainServlet => 

  notFound {
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case e: Danger => {
      if (e.errors.nonEmpty) flash("danger") = e.errors
      redirect( e.backTo.getOrElse(backPath) )
    }
    case e: Throwable => { redirect("/") }
  }

  def simpleUrl(
    path: String,
    params: Iterable[(String, Any)] = Iterable.empty
  ) = url(path, params, withSessionId = false)

  def backPath = request.referrer match {
    case Some(s) => s
    case _ => url(rootPath)
  }

  object viewHelpers {
    def isAuthenticated = self.isAuthenticated
  }

}

