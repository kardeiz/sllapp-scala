package com.github.kardeiz.sllapp

object Props {
	
  object Sip2 {
    val Host = sys.env.getOrElse("SIP2_HOST", "localhost")
    val Ao   = sys.env.getOrElse("SIP2_AO", "undefined")
    val Port = sys.env.getOrElse("SIP2_PORT", "6006").toInt
  }
  
  object Db {
    val Driver   = "org.h2.Driver"
    val Url      = sys.env.getOrElse("DB_URL", "jdbc:h2:mem:test")
    val User     = sys.env.getOrElse("DB_USER", "root")
    val Password = sys.env.getOrElse("DB_PASSWORD", "pass")
  }

  object Demo {
    val UserUid = sys.env.getOrElse("USER_UID", null)
    val UserPin = sys.env.getOrElse("USER_PIN", null)
  }

  object Vbox {
    val Url      = sys.env.getOrElse("VBOX_URL", null)
    val User     = sys.env.getOrElse("VBOX_USER", null)
    val Password = sys.env.getOrElse("VBOX_PASSWORD", null)
  }

}

object Utils {

  // def startRepl = {
  //   import com.github.kardeiz.sllapp._
  //   import Tables._
  //   import scala.slick.driver.H2Driver.simple._
  //   val cpds = DatabaseHelper.buildDataSource
  //   val db   = DatabaseHelper.buildDatabase(cpds)
  //   implicit def session = db.createSession
  //   ( users.ddl ++ resources.ddl ++ reservations.ddl ).create
  //   val u = User.build( Props.Demo.UserUid, Props.Demo.UserPin )


  //   cpds.close
  // }

  val msgDigest = java.security.MessageDigest.getInstance("SHA-256")

  def passHash(text: String) = {
    val bts = msgDigest.digest(text.getBytes("UTF-8"))
    bts.map("%02x".format(_)).mkString
  }

}

object Sip2Utils {
  
  import Props.Sip2._

  import com.pkrete.jsip2.connection.SIP2SocketConnection
  import com.pkrete.jsip2.messages._
  import com.pkrete.jsip2.messages.requests._
  import com.pkrete.jsip2.messages.responses._
  import com.pkrete.jsip2.variables._

  def buildConnection = new SIP2SocketConnection(Host, Port)

  def processRequest(msgRequest: SIP2MessageRequest): SIP2MessageResponse = {
    val connection = buildConnection
    connection.connect
    val response = connection.send(msgRequest)
    connection.close
    response
  }

  def extractData(response: SIP2PatronInformationResponse) = {
    val email = Option(response.getEmail).flatMap(
      _.split("\\s*,\\s*").toList.headOption
    )
    Option(response.getPersonalName).map(
      _.split("\\s*,\\s*").toList
    ) match {
      case Some(ln :: fn :: Nil) => ( email, Option(ln), Option(fn))
      case _ => ( email, None, None ) 
    }   
  }

  def patronInfoRequest(uid: String, pin: String) = {    
    val msgRequest = new SIP2PatronInformationRequest(Ao, uid, pin)
    msgRequest.setErrorDetectionEnabled(true)
    processRequest(msgRequest) match {
      case response: SIP2PatronInformationResponse => response
      case _ => throw new Exception("Sip2 Error")
    }
  }

}

object VboxUtils {

  import Props.Vbox._
  import org.virtualbox_4_3._

  def buildManager = VirtualBoxManager.createInstance(null)

  def process[T]( fun: (VirtualBoxManager) => T ) = {
    val manager = buildManager
    manager.connect(Url, User, Password)
    val result = fun(manager)
    manager.disconnect
    manager.cleanup
    result
  }

  def createReservation(user: Tables.User, resource: Tables.Resource) = {
    process { manager => 
      val machine = manager.getVBox.findMachine(resource.name)
      val session = manager.getSessionObject
      machine.lockMachine(session, LockType.Write)
      val mutable = session.getMachine
      mutable.setExtraData(
        s"VBoxAuthSimple/users/${user.uid}",
        user.encryptedPin
      )
      mutable.saveSettings
      session.unlockMachine
      val progress = machine.launchVMProcess(session, "headless", null)
      progress.waitForCompletion(-1)
      session.unlockMachine
    }
  }

}