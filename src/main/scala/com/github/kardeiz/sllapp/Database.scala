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

object DateConversions extends DateConversions

object DatabaseInit {

  import Props.Db._

  def buildCpds = {
    val cpds = new ComboPooledDataSource
    cpds.setDriverClass( Driver )
    cpds.setJdbcUrl( Url )
    cpds.setUser( User )
    cpds.setPassword( Password )
    cpds
  }

  def buildDbFor(cpds: ComboPooledDataSource) = Database.forDataSource(cpds)

  def buildTables = {
    import Tables._
    val ds = buildCpds
    val db = buildDbFor(ds)
    db.withSession { implicit s => 
      ( reservations.ddl ++ users.ddl ++ resources.ddl ).create
    }
    ds.close
  }
}

object Tables extends DateConversions {

  case class User(
    id: Option[Int],
    uid: String,
    encryptedPin: String,
    email: Option[String] = None,
    lastName: Option[String] = None,
    firstName: Option[String] = None
  )

  object User {
    def all(implicit db: Database) = db.withSession { implicit s => users.run }
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

  case class Resource(
    id: Option[Int],
    name: String,
    ip: String,
    port: String,
    tpe: Option[String] = None,
    description: Option[String] = None
  )

  object Resource {
    def all(implicit db: Database) = db.withSession { implicit s => resources.run }
  }

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

  object resources extends TableQuery(new Resources(_)) {
    val findById  = this.findBy(_.id)
  }

  case class Reservation(
    id: Option[Int],
    userId: Int,
    resourceId: Int,
    startTime: DateTime,
    endTime: DateTime,
    description: Option[String] = None
  )

  object Reservation {
    def all(implicit db: Database) = db.withSession { implicit s => reservations.run }
  }

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

  object reservations extends TableQuery(new Reservations(_)) {
    val findById  = this.findBy(_.id)

    def overlapping(st: Column[DateTime], et: Column[DateTime], rId: Column[Int]) = this.filter( r => 
      r.startTime < et && r.endTime > st && r.resourceId === rId
    )


  }


  trait RichModels extends DateConversions {

    def db: Database

    implicit class RichUser(user: User) {
      def reservationsWithResources = user.id.map( id =>
        db.withSession { implicit s =>
          val query = for {
            re <- reservations if re.userId === id
            rs <- re.resource
          } yield (re, rs)
          query.run
        }
      ).getOrElse(List.empty)      
    }

    implicit class RichReservation(rsv: Reservation) {

      def overlappingReservationsCount = db.withSession { implicit s => 
        reservations.overlapping(rsv.startTime, rsv.endTime, rsv.resourceId).length.run
      }

      def errors = {
        val errors = ArrayBuffer[String]()
        if ( rsv.endTime.isBefore(DateTime.now) ) errors += "Reservations must end in the future"
        if ( Hours.hoursBetween(rsv.startTime, rsv.endTime).getHours > 3 )
          errors += "Reservations cannot last more than 3 hours"
        if ( rsv.overlappingReservationsCount > 0 )
          errors += "Reservation overlaps with another"
        errors.toSeq
      }

      def save = {
        val errs = rsv.errors
        if ( errs.isEmpty ) {
          val id = db.withSession { implicit s => reservations.insert(rsv) }
          rsv.copy(id = Some(id))
        } else throw new Danger(errs)
      }

      def delete = db.withSession { implicit session => 
        reservations.filter(_.id === rsv.id).delete
      }
    }
  }


}

