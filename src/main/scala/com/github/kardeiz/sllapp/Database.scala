package com.github.kardeiz.sllapp

import java.sql.Timestamp
import org.joda.time._

import com.mchange.v2.c3p0.ComboPooledDataSource
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.meta.MTable

import LocalDriver.simple._

import scala.collection.mutable.ArrayBuffer

trait DateConversions {
  implicit def dateTime  = MappedColumnType.base[DateTime, Timestamp](
    dt => new Timestamp(dt.getMillis),
    ts => new DateTime(ts.getTime)
  )
}

object DateConversions extends DateConversions

object DatabaseAccess {

  private var _cpds: Option[ComboPooledDataSource] = None
  private var _db: Option[Database] = None

  def startup {
    val cpds = new ComboPooledDataSource(Settings.appEnv)
    _cpds = Some( cpds )
    _db   = Some( Database.forDataSource(cpds) )
    buildTables
    if ( Settings.appEnv == "dev" ) addSampleData
  }

  def db = _db.getOrElse( throw new Exception("Not initialized") )

  def shutdown {
    _cpds.foreach(_.close)
    _cpds = None
    _db = None
  }

  def buildTables { 
    db.withSession { implicit s =>
      Seq(Tables.users, Tables.resources, Tables.reservations).foreach { tbl =>
        if (MTable.getTables(tbl.baseTableRow.tableName).list.isEmpty)
          tbl.ddl.create
      }
    }
  }

  def addSampleData {
    db.withSession { implicit s =>
      val resource = Models.Resource(None, "ubuntu", "127.0.0.1", "5300")
      Tables.resources.insert(resource)
    }
  }
}

object Models {
  
  import DatabaseAccess.db
  import DateConversions._

  case class User(
    id: Option[Int],
    uid: String,
    encryptedPin: String,
    email: Option[String] = None,
    lastName: Option[String] = None,
    firstName: Option[String] = None
  ) {

    def reservationsWithResources = db.withSession { implicit s =>
      val query = for {
        re <- Tables.reservations if re.userId === id
        rs <- re.resource
      } yield (re, rs)
      query.run
    }

  }

  object User {
    def all = db.withSession { implicit s => Tables.users.run }

    def findOrCreateByUid(uid: String)(fn: => User) = 
      db.withSession { implicit s =>
        Tables.users.findByUid(uid).firstOption.getOrElse {
          val user = fn
          val id   = Tables.users.insert(user)
          user.copy(id = Some(id))      
        }
      }

    def findById(id: Int) = db.withSession { implicit s => 
      Tables.users.findById(id).firstOption
    }

  }

  case class Resource(
    id: Option[Int],
    name: String,
    ip: String,
    port: String,
    tpe: Option[String] = None,
    description: Option[String] = None
  )

  object Resource {
    def all = db.withSession { implicit s => Tables.resources.run }
    def findById(id: Int) = db.withSession { implicit s => 
      Tables.resources.findById(id).firstOption
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

    def user     = db.withSession { implicit s => User.findById(userId) }
    def resource = db.withSession { implicit s => Resource.findById(resourceId) }

    def overlappingReservationsCount = db.withSession { implicit s => 
      Tables.reservations.overlapping(startTime, endTime, resourceId).length.run
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

    def beforeSave {
      val errs = errors
      if (errs.nonEmpty) throw new Danger(errs)
    }

    def afterSave(persisted: Reservation) {
      JobUtil.createReservation(persisted)
      JobUtil.destroyReservation(persisted)
    }

    def afterDelete {
      JobUtil.unscheduleReservationJobs(this)
    }

    def save = {
      beforeSave
      val id = db.withSession { implicit s => Tables.reservations.insert(this) }
      val re = this.copy(id = Some(id))
      afterSave(re)
      re
    }

    def delete = db.withSession { implicit s => 
      Tables.reservations.filter(_.id === id).delete
      afterDelete
      this.copy(id = None)
    }

  }

  object Reservation {
    def all = db.withSession { implicit s => Tables.reservations.run }

    def overlapping(st: DateTime, et: DateTime, rId: Int) =
      db.withSession { implicit s => Tables.reservations.overlapping(st, et, rId).run }

    def findById(id: Int) = db.withSession { implicit s => 
      Tables.reservations.findById(id).firstOption
    }

    def findByIdPreload(id: Int) = db.withSession { implicit s => 
      val query  = for {
        re <- Tables.reservations.filter(_.id === id)
        rs <- re.resource
        us <- re.user
      } yield (re, rs, us)
      query.firstOption
    }

  }

}

object Tables extends DateConversions {

  class Users(tag: Tag) extends Table[Models.User](tag, "users") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def uid          = column[String]("uid")
    def encryptedPin = column[String]("encrypted_pin")
    def email        = column[Option[String]]("email")
    def lastName     = column[Option[String]]("last_name")
    def firstName    = column[Option[String]]("first_name")

    def idxUid = index("idx_uid", uid, unique = true)
    
    def * = (id.?, uid, encryptedPin, email, lastName, firstName) <> 
      ( (Models.User.apply _).tupled, Models.User.unapply )
  }

  object users extends TableQuery(new Users(_)) {
    val findById  = this.findBy(_.id)
    val findByUid = this.findBy(_.uid)
  }

  class Resources(tag: Tag) extends Table[Models.Resource](tag, "resources") {
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name        = column[String]("name")
    def ip          = column[String]("ip")
    def port        = column[String]("port")
    def tpe         = column[Option[String]]("type")
    def description = column[Option[String]]("description")
    
    def * = (id.?, name, ip, port, tpe, description) <>
      ( (Models.Resource.apply _).tupled, Models.Resource.unapply )
  }

  object resources extends TableQuery(new Resources(_)) {
    val findById  = this.findBy(_.id)
  }

  class Reservations(tag: Tag)
    extends Table[Models.Reservation](tag, "reservations") {
    
    def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId      = column[Int]("user_id")
    def resourceId  = column[Int]("resource_id")
    def startTime   = column[DateTime]("start_time")
    def endTime     = column[DateTime]("end_time")
    def description = column[Option[String]]("description")

    def * = (id.?, userId, resourceId, startTime, endTime, description) <>
      ( (Models.Reservation.apply _).tupled, Models.Reservation.unapply )

    def resource = foreignKey("resource_id", resourceId, resources)(_.id)
    def user     = foreignKey("user_id", userId, users)(_.id)
  }

  object reservations extends TableQuery(new Reservations(_)) {
    val findById  = this.findBy(_.id)

    def overlapping(st: Column[DateTime], et: Column[DateTime], rId: Column[Int]) = 
      this.filter( r => r.startTime < et && r.endTime > st && r.resourceId === rId )
  }

}

