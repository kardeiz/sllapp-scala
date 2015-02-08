package com.github.kardeiz.sllapp

import java.sql.Timestamp
import org.joda.time._

import com.mchange.v2.c3p0.ComboPooledDataSource
import scala.slick.jdbc.JdbcBackend.Database
import LocalDriver.simple._

import scala.collection.mutable.ArrayBuffer

trait DateConversions {
  implicit def dateTime  = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime)
  )
}

trait DatabaseExtensions extends DateConversions {

  implicit class ReservationExt[C[_]](q: Query[Tables.Reservations, Models.Reservation, C]) {
    def overlapping(st: DateTime, et: DateTime) = q.filter(x => x.startTime < et && x.endTime > st)
    def forResource(r: Models.Resource) = q.filter(_.resourceId === r.id)
    def forUser(u: Models.User) = q.filter(_.userId === u.id)
  }

}
object DatabaseExtensions extends DatabaseExtensions

object DatabaseAccess {

  import Props.Db
  import Tables._

  lazy val cpds = {
    val cpds = new ComboPooledDataSource
    cpds.setDriverClass( Db.Driver )
    cpds.setJdbcUrl( Db.Url )
    cpds.setUser( Db.User )
    cpds.setPassword( Db.Password )
    cpds
  }

  lazy val db = Database.forDataSource(cpds)

  def touchDb { db }

  def createTables {
    db.withSession { implicit session =>
      ( users.ddl ++ resources.ddl ++ reservations.ddl ).create
    }
  }

  def createSampleData {
    createTables
    val resource1 = Models.Resource(None, "something", "10001", "3000")
    val resource2 = Models.Resource(None, "somethingElse", "10001", "3000")
    val user     = Models.User(None, "user", "pin")
    val reservation = {
      val d1 = DateTime.now
      val d2 = d1.withTime(d1.getHourOfDay, 0, 0, 0)
      val d3 = d2.plus(Period.hours(2))
      Models.Reservation(None, 1, 1, d2, d3)
    }
    db.withSession { implicit session =>
      users.insert(user)
      resources.insertAll(resource1, resource2)
      reservations.insert(reservation)
    }
  }

}


object Models extends DateConversions with DatabaseExtensions {

  import DatabaseAccess._

  case class User(
    id: Option[Int],
    uid: String,
    encryptedPin: String,
    email: Option[String] = None,
    lastName: Option[String] = None,
    firstName: Option[String] = None
  ) {

    def reservationsWithResources = db.withSession { implicit session =>
      Reservation.scoped.forUser(this).join(Resource.scoped).on(_.resourceId === _.id).run
    }

  }

  object User {

    def scoped = Tables.users
    def all = db.withSession { implicit session => scoped.run }
    def findById(id: Int) = db.withSession { implicit session =>
      scoped.filter(_.id === id).take(1).firstOption
    }
    def findOrCreate(uid: String)(f: => User) = db.withSession { implicit session =>
      scoped.filter(_.uid === uid).firstOption match {
        case Some(user: User) => user
        case _ => {
          val user = f
          val id   = Tables.users.insert(user)
          user.copy(id = Some(id))
        }
      }
    }
  }

  case class Resource(
    id: Option[Int],
    name: String,
    ip: String,
    port: String,
    tpe: Option[String] = None,
    description: Option[String] = None
  ) {

    def reservations = db.withSession { implicit session => Reservation.scoped.forResource(this).run }

  }

  object Resource {
    def scoped = Tables.resources
    def all    = db.withSession { implicit session => scoped.run }
    def findById(id: Int) = db.withSession { implicit session =>
      scoped.filter(_.id === id).take(1).firstOption
    }
  }


  case class Reservation(
    id: Option[Int],
    userId: Int,
    resourceId: Int,
    startTime: DateTime,
    endTime: DateTime,
    description: Option[String] = None
  ) {

    def overlappingReservationsCount = db.withSession { implicit session => 
      Reservation.scoped.overlapping(startTime, endTime).filter(_.resourceId === resourceId).length.run
    }

    def errors = {
      val errors = ArrayBuffer[String]()
      if ( endTime.isBefore(DateTime.now) ) errors += "Reservations must end in the future"
      if ( Hours.hoursBetween(startTime, endTime).getHours > 3 )
        errors += "Reservations cannot last more than 3 hours"
      if ( overlappingReservationsCount > 0 )
        errors += "Reservation overlaps with another"
      errors.toSeq
    }

    def save = {
      val errs = errors
      if ( errs.isEmpty ) {
        val id = db.withSession { implicit session => Reservation.scoped.insert(this) }
        this.copy(id = Some(id))
      } else throw new Danger(errs)
    }

    def delete = {
      db.withSession { implicit session => 
        Reservation.scoped.filter(_.id === this.id).delete
      }
      this.copy(id = None)
    }

  }

  object Reservation {
    def scoped = Tables.reservations
    def all    = db.withSession { implicit session => scoped.run }
    def findById(id: Int) = db.withSession { implicit session =>
      scoped.filter(_.id === id).take(1).firstOption
    }
    def forSchedule(schedule: Schedule) = schedule.optResource.map( resource =>
      db.withSession { implicit session => 
        scoped.overlapping(schedule.startTime, schedule.endTime).forResource(resource).run
      }
    )
  }

}


object Tables extends DateConversions {

  import Models._

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

  val users = TableQuery[Users]

  class Resources(tag: Tag) extends Table[Resource](tag, "resources") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name        = column[String]("name")
    def ip          = column[String]("ip")
    def port        = column[String]("port")
    def tpe         = column[Option[String]]("type")
    def description = column[Option[String]]("description")
    
    def * = (id.?, name, ip, port, tpe, description) <>
      ( (Resource.apply _).tupled, Resource.unapply )
  }

  val resources = TableQuery[Resources]

  class Reservations(tag: Tag)
    extends Table[Reservation](tag, "reservations") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId      = column[Int]("user_id")
    def resourceId  = column[Int]("resource_id")
    def startTime   = column[DateTime]("start_time")
    def endTime     = column[DateTime]("end_time")
    def description = column[Option[String]]("description")

    def * = (id.?, userId, resourceId, startTime, endTime, description) <>
      ( (Reservation.apply _).tupled, Reservation.unapply )

    def resource = foreignKey("resource_id", resourceId, resources)(_.id)
    def user     = foreignKey("user_id", userId, users)(_.id)
  }

  val reservations = TableQuery[Reservations]

}
