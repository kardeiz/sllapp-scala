package com.github.kardeiz.sllapp

import org.scalatra._

import org.joda.time._

import scala.slick.jdbc.JdbcBackend.Database
import org.quartz.Scheduler

import LocalDriver.simple._
import Tables._

class MainServlet(implicit val db: Database, val scheduler: Scheduler) extends SllappStack {

  val rootPath = get("/") { 
    JobRunner(scheduler)
    layouts.html.default()()(this)
  }

  val reservationsGet = get("/reservations") {
    requireLogin
    views.html.reservations(user.reservationsWithResources)(this)
  }

  val reservationDelete = delete("/reservations/:id") {
    params.getAs[Int]("id").foreach( id =>
      db.withSession { implicit s => reservations.findById(id).delete }
    )
    flash("success") = "Reservation deleted"
    redirect( url(reservationsGet) )
  }

  val reservationsPost = post("/reservations") {
    requireLogin
    val reservation = (for {
      resourceId  <- params.getAs[Int]("resource_id")
      startTime   <- params.get("start_time").map( Schedule.formatIso.parseDateTime(_) )
      endTime     <- params.getAs[Int]("duration").map( d => startTime.plus( Period.hours(d) ) )
      userId      <- user.id
      reservation =  Reservation(None, userId, resourceId, startTime, endTime)
    } yield reservation).getOrElse( throw new Danger ).save

    db.withSession { implicit s => reservations.insert(reservation) }    

    flash("success") = "Reservation created successfully"
    redirect( url(reservationsGet))
  }

  val availableReservationsPath = get("/reservations/available") {
    val schedule = {
      val date = params.get("date").map( Schedule.formatDate.parseDateTime(_) ).getOrElse(DateTime.now)
      val resource = params.getAs[Int]("resource_id").flatMap( rId => 
        db.withSession { implicit s => resources.filter(_.id === rId).firstOption }
      )
      Schedule(date, resource)
    }
    views.html.availableReservations(schedule)(this)
  }

  val authLoginGet = get("/auth/login") {
    checkAuthenticated
    views.html.authLogin(this)
  }

  val authLoginPost = post("/auth/login") { authenticate; afterAuthenticate }

  val authLogoutPath = get("/auth/logout") { logOut; afterLogout }

}
