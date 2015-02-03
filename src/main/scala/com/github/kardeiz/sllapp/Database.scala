package com.github.kardeiz.sllapp

import java.sql.Time

import com.mchange.v2.c3p0.ComboPooledDataSource

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.JdbcBackend.Database

object DatabaseHelper {

  import Props.Db

  def buildDataSource = {
    val cpds = new ComboPooledDataSource
    cpds.setDriverClass( Db.Driver )
    cpds.setJdbcUrl( Db.Url )
    cpds.setUser( Db.User )
    cpds.setPassword( Db.Password )
    cpds
  }

  def buildDatabase(cpds: ComboPooledDataSource) =
    Database.forDataSource(cpds)

}


object Tables {

  case class User(
    id: Option[Int],
    uid: String,
    encryptedPin: String,
    email: Option[String] = None,
    lastName: Option[String] = None,
    firstName: Option[String] = None
  ) 

  object User {
    def build(uid: String, pin: String) = 
      new User( None, uid, Utils.passHash(pin) )
  }

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def uid          = column[String]("uid")
    def encryptedPin = column[String]("encrypted_pin")
    def email        = column[Option[String]]("email")
    def lastName     = column[Option[String]]("last_name")
    def firstName    = column[Option[String]]("first_name")

    def idxUid = index("idx_uid", uid, unique = true)
    
    def * = (id.?, uid, encryptedPin, email, lastName, firstName) <> 
      ( (User.apply _).tupled, User.unapply )

  }

  object users extends TableQuery(new Users(_)) {
    val findById  = this.findBy(_.id)
    val findByUid = this.findBy(_.uid)
  }

  implicit class UserExtensions[C[_]](q: Query[Users, User, C]) {

    def withReservations = q.join(reservations).on(_.id === _.userId)
  
  }

  case class Resource(
    id: Option[Int],
    name: String,
    ip: String,
    port: String,
    tpe: Option[String] = None,
    description: Option[String] = None
  )

  class Resources(tag: Tag) extends Table[Resource](tag, "resources") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name        = column[String]("name")
    def ip          = column[String]("ip")
    def port        = column[String]("port")
    def tpe         = column[Option[String]]("type")
    def description = column[Option[String]]("description")
    

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id.?, name, ip, port, tpe, description) <>
      ( Resource.tupled, Resource.unapply )
  }

  val resources = TableQuery[Resources]

  case class Reservation(
    id: Option[Int],
    userId: Int,
    resourceId: Int,
    startTime: Time,
    endTime: Time,
    description: Option[String] = None
  )

  class Reservations(tag: Tag)
    extends Table[Reservation](tag, "reservations") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId      = column[Int]("user_id")
    def resourceId  = column[Int]("resource_id")
    def startTime   = column[Time]("start_time")
    def endTime     = column[Time]("end_time")
    def description = column[Option[String]]("description")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id.?, userId, resourceId, startTime, endTime, description) <>
      ( Reservation.tupled, Reservation.unapply )

    def resource = foreignKey("resource_id", resourceId, resources)(_.id)
    def user     = foreignKey("user_id", userId, users)(_.id)
  }

  val reservations = TableQuery[Reservations]

}

trait SlickSupport {
  
  import Tables._
  import scala.slick.jdbc.JdbcBackend.Database.dynamicSession

  def db: Database

  def findOrCreateUser(uid: String)(f: => User) = 
    db.withDynSession {
      users.findByUid(uid).firstOption match {
        case Some(user: User) => user
        case _ => {
          val user = f
          val id   = users.insert(user)
          user.copy(id = Some(id))
        }
      }
    }
  
}

case class SlickObject(db: Database) extends SlickSupport