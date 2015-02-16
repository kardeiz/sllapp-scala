package com.github.kardeiz.sllapp

import org.scalatra.ScalatraBase
import org.scalatra.auth.{ScentryConfig, ScentrySupport, ScentryStrategy}

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.slf4j.LoggerFactory

import LocalDriver.simple._
import Models._

class Sip2Strategy(protected val app: ScalatraBase)(
  implicit request: HttpServletRequest, 
  response: HttpServletResponse
) extends ScentryStrategy[User] {
  
  val logger = LoggerFactory.getLogger(getClass)

  override def name: String = "Sip2Auth"
  
  lazy val uid = app.params.getOrElse("uid", "")
  lazy val pin = app.params.getOrElse("pin", "")

  lazy val sip2Response = Sip2Utils.makePatronInfoRequest(uid, pin)

  override def isValid(implicit request: HttpServletRequest) = 
    !uid.isEmpty && !pin.isEmpty

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
    if (sip2Response.isValidPatronPassword) {
      val user = User.findOrCreateByUid(uid) {
        val (e, l, f) = Sip2Utils.extractData(sip2Response)
        User(None, uid, Utils.passHash(pin), e, l, f)  
      }
      Some(user)
    } else None
  }

}

trait AuthenticationSupport 
  extends ScalatraBase with ScentrySupport[User] { 

  self: MainServlet =>

  object _scentryConfig extends ScentryConfig {
    override val login = url(authLoginGet)
    override val returnTo = url(rootPath)
    override val returnToKey = "returnTo"
    override val failureUrl = url(authLoginGet)
  }

  protected def fromSession = { 
    case id: String => User.findById(id.toInt).get
  }

  protected def toSession = { 
    case user: User => user.id.get.toString
  }

  def scentryConfig = _scentryConfig.asInstanceOf[ScentryConfiguration]

  val logger = LoggerFactory.getLogger(getClass)

  override protected def configureScentry = {
    scentry.unauthenticated {
      flash("danger") = "Login failed"
      redirect(scentryConfig.login)
    }
  }

  protected def requireLogin {
    if(!isAuthenticated) {
      session(scentryConfig.returnToKey) = {
        val qs = Option(request.getQueryString).map("?" + _).getOrElse("")
        request.getRequestURL.toString + qs
      }
      flash("info") = "Please login"
      redirect(scentryConfig.login)
    }
  }

  def afterAuthenticate(implicit request: HttpServletRequest, response: HttpServletResponse) {
    flash("success") = "Signed in successfully"
    val returnTo = session.get(scentryConfig.returnToKey) match {
      case Some(returnToVal: String) => {
        session -= scentryConfig.returnToKey
        returnToVal
      }
      case _ => url(rootPath)
    }
    redirect(returnTo)
  }

  def afterLogout(implicit request: HttpServletRequest, response: HttpServletResponse) {
    flash("success") = "Signed out successfully"
    redirect( url(rootPath) )
  }

  def checkAuthenticated {
    if (isAuthenticated) {
      flash("info") = "Already signed in"
      redirect( backPath )
    }
  }

  override protected def registerAuthStrategies = {
    scentry.register("Sip2Auth", app => new Sip2Strategy(app))
  }

}