package com.github.kardeiz.sllapp

import org.scalatra._

import org.joda.time._

import Models._

class MainServlet extends SllappStack {

  val rootPath = get("/") { 
    layouts.html.default()()(this)
  }

  val reservationsGet = get("/reservations") {
    requireLogin
    val reservationsWithResources = 
      userOption.map(_.reservationsWithResources).getOrElse(List.empty)
    views.html.reservations(reservationsWithResources)(this)
  }

  val reservationDelete = delete("/reservations/:id") {
    params.getAs[Int]("id").foreach( Reservation.findById(_).foreach( _.delete ) )
    flash("success") = "Reservation deleted"
    redirect( url(reservationsGet) )
  }

  val reservationsPost = post("/reservations") {
    requireLogin
    val optReservation = for {
      resourceId <- params.getAs[Int]("resource_id")
      resource   <- Resource.findById(resourceId)
      startTime  <- params.get("start_time").map( Schedule.formatIso.parseDateTime(_) )
      endTime    <- params.getAs[Int]("duration").map( d => startTime.plus( Period.hours(d) ) )
      userId     <- userOption.flatMap(_.id)
    } yield Reservation(None, userId, resourceId, startTime, endTime)
    
    optReservation.getOrElse( throw new Danger ).save

    flash("success") = "Reservation created successfully"
    redirect( url(reservationsGet))
  }

  val availableReservationsPath = get("/reservations/available") {
    val schedule = {
      val date = params.get("date").map( Schedule.formatDate.parseDateTime(_) ).getOrElse(DateTime.now)
      val resource = params.getAs[Int]("resource_id").flatMap( Resource.findById(_) )
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
