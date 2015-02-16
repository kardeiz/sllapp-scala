package com.github.kardeiz.sllapp

import org.joda.time._
import org.joda.time.format._

import scala.slick.jdbc.JdbcBackend.Database

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object LocalDriver extends scala.slick.driver.H2Driver

import Models._

object Settings {

  lazy val config = ConfigFactory.load
  lazy val appEnv = config.getString("app.env")
  lazy val sip2   = config.getConfig("sip2")
  lazy val vbox   = config.getConfig("vbox")

  lazy val quartzProperties = {
    val props  = new java.util.Properties
    config.getConfig("org.quartz").entrySet.asScala.foreach( k =>
      props.put("org.quartz." + k.getKey, k.getValue.unwrapped)
    )
    props
  }


}

case class Danger(
  errors: Seq[String] = List("Bad request"), 
  backTo: Option[String] = None
) extends Exception

case class TimeSlot(startTime: DateTime, endTime: DateTime, available: Boolean)

case class Schedule(
  date: DateTime = DateTime.now, 
  optResource: Option[Resource] = None
) {

  val currentDate = DateTime.now

  val startTime = if (date.toLocalDate == currentDate.toLocalDate)
    currentDate.withTime(currentDate.getHourOfDay, 0, 0, 0)
  else 
    date.withTimeAtStartOfDay

  val endTime = date.withTimeAtStartOfDay.plus(Period.days(1))

  val hoursOfDay = Schedule.iterateBetween(startTime, endTime)

  val reservationsOfDay = optResource.flatMap(_.id).map(
    Reservation.overlapping(startTime, endTime, _)
  ).getOrElse(List.empty)

  val timeSlots = hoursOfDay.map { hour =>
    val available = !reservationsOfDay.exists( res =>
      Schedule.iterateBetween(res.startTime, res.endTime).contains(hour)
    )
    TimeSlot(hour, hour.plus(Period.hours(1)), available)
  }
}

object Schedule {
  
  def iterateBetween(
    st: DateTime, et: DateTime, it: List[DateTime] = List.empty
  ): List[DateTime] = if (st.isBefore(et)) 
    st :: iterateBetween(st.plus(Period.hours(1)), et, it) 
  else it

  val formatDate = DateTimeFormat.forPattern("yyyy-MM-dd")
  val formatTime = DateTimeFormat.forPattern("hh:mm a")
  val formatBoth = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm a")
  val formatIso = ISODateTimeFormat.dateTime

}


object Utils {

  val msgDigest = java.security.MessageDigest.getInstance("SHA-256")

  def passHash(text: String) = {
    val bts = msgDigest.digest(text.getBytes("UTF-8"))
    bts.map("%02x".format(_)).mkString
  }

}

object Sip2Utils {
  
  import com.pkrete.jsip2.connection.SIP2SocketConnection
  import com.pkrete.jsip2.messages._
  import com.pkrete.jsip2.messages.requests._
  import com.pkrete.jsip2.messages.responses._
  import com.pkrete.jsip2.variables._

  val Host = Settings.sip2.getString("host")
  val Port = Settings.sip2.getInt("port")
  val Ao   = Settings.sip2.getString("ao")

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

  def makePatronInfoRequest(uid: String, pin: String) = {    
    val msgRequest = new SIP2PatronInformationRequest(Ao, uid, pin)
    msgRequest.setErrorDetectionEnabled(true)
    processRequest(msgRequest) match {
      case response: SIP2PatronInformationResponse => response
      case _ => throw new Exception("Sip2 Error")
    }
  }

}

object VboxUtils {

  import org.virtualbox_4_3._

  val Url      = Settings.vbox.getString("url")
  val User     = Settings.vbox.getString("user")
  val Password = Settings.vbox.getString("password")

  def buildManager = VirtualBoxManager.createInstance(null)

  def process[T]( fun: (VirtualBoxManager) => T ) = {
    val manager = buildManager
    manager.connect(Url, User, Password)
    val result = fun(manager)
    manager.disconnect
    manager.cleanup
    result
  }

  // def createReservation(user: Tables.User, resource: Tables.Resource) = {
  //   process { manager => 
  //     val machine = manager.getVBox.findMachine(resource.name)
  //     val session = manager.getSessionObject
  //     machine.lockMachine(session, LockType.Write)
  //     val mutable = session.getMachine
  //     mutable.setExtraData(
  //       s"VBoxAuthSimple/users/${user.uid}",
  //       user.encryptedPin
  //     )
  //     mutable.saveSettings
  //     session.unlockMachine
  //     val progress = machine.launchVMProcess(session, "headless", null)
  //     progress.waitForCompletion(-1)
  //     session.unlockMachine
  //   }
  // }

}