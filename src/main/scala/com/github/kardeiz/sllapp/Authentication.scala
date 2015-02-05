package com.github.kardeiz.sllapp

import org.scalatra.ScalatraBase
import org.scalatra.auth.{ScentryConfig, ScentrySupport, ScentryStrategy}

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.slf4j.LoggerFactory

import Tables._

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

class Sip2Strategy(protected val app: ScalatraBase)(
  implicit request: HttpServletRequest, 
  response: HttpServletResponse
) extends ScentryStrategy[User] {
  
  val logger = LoggerFactory.getLogger(getClass)

  override def name: String = "Sip2Auth"
  
  lazy val uid = app.params.getOrElse("uid", "")
  lazy val pin = app.params.getOrElse("pin", "")

  lazy val sip2Response = Sip2Utils.makePatronInfoRequest(uid, pin)
  lazy val encryptedPin = Utils.passHash(pin)

  override def isValid(implicit request: HttpServletRequest) = 
    !uid.isEmpty && !pin.isEmpty

  def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] = {
    if (sip2Response.isValidPatronPassword) {
      val user = msApp.db.withDynSession {
        msApp.findOrCreateUser(uid) {
          val (e, l, f) = Sip2Utils.extractData(sip2Response)
          User(None, uid, encryptedPin, e, l, f)
        }
      }
      Some(user)
    } else None
  }

  def msApp = app.asInstanceOf[MainServlet]

  // override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {
  //   app.redirect("/auth/login")
  // }

}

trait AuthenticationSupport 
  extends ScalatraBase with ScentrySupport[User] { 

  self: ScalatraBase =>

  def msApp = self.asInstanceOf[MainServlet]

  protected def fromSession = { 
    case id: String => msApp.db.withDynSession { 
      users.findById(id.toInt).first
    }
  }

  protected def toSession = { 
    case user: User => user.id.getOrElse(-1).toString
  }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  val logger = LoggerFactory.getLogger(getClass)

  // protected def requireLogin() = {
  //   if(!isAuthenticated) {
  //     msApp.flash("info") = "Please login"
  //     redirect(scentryConfig.login)
  //   }
  // }

  /**
   * If an unauthenticated user attempts to access a route which is protected by Scentry,
   * run the unauthenticated() method on the UserPasswordStrategy.
   */
  // override protected def configureScentry = {
  //   scentry.unauthenticated {
  //     scentry.strategies("Sip2Auth").unauthenticated()
  //   }
  // }

  /**
   * Register auth strategies with Scentry. Any controller with this trait mixed in will attempt to
   * progressively use all registered strategies to log the user in, falling back if necessary.
   */
  override protected def registerAuthStrategies = {
    scentry.register("Sip2Auth", app => new Sip2Strategy(app))
  }

}