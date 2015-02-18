package com.github.kardeiz.sllapp

import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import org.quartz.impl.matchers.GroupMatcher
import scala.collection.JavaConverters._

import Models._

object SchedulerAccess {

  private var _scheduler: Option[Scheduler] = None

  def scheduler = _scheduler.getOrElse(throw new Exception("Not initialized"))

  def startup {
    val factory   = new StdSchedulerFactory(Settings.quartzProperties)
    val scheduler = factory.getScheduler
    scheduler.start
    _scheduler = Some(scheduler)
  }

  def shutdown {
    _scheduler.foreach( _.shutdown(false) )
    _scheduler = None
  }

}


class ReservationCreateJob extends Job {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

  def execute(context: JobExecutionContext) {
    val reservationId = context.getMergedJobDataMap.getIntFromString("reservationId")
    Reservation.findByIdPreload(reservationId) match {
      case Some( (re, rs, us ) )  => {
        VboxUtils.createReservation(us, rs)
      }
      case _ => logger.info("Reservation not found")
    }    
  }
}

class ReservationDestroyJob extends Job {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

  def execute(context: JobExecutionContext) {
    val reservationId = context.getMergedJobDataMap.getIntFromString("reservationId")
    Reservation.findByIdPreload(reservationId) match {
      case Some( (re, rs, us ) )  => {
        VboxUtils.destroyReservation(us, rs)
      }
      case _ => logger.info("Reservation not found")
    }    
  }
}

object JobUtil {
  
  def createReservation(reservation: Reservation) {
    val job = JobBuilder.newJob(classOf[ReservationCreateJob]).build
    val trg = ( TriggerBuilder.newTrigger
      .withIdentity("create", s"res-${reservation.id.get}")
      .startAt(reservation.startTime.toDate)
      .usingJobData("reservationId", reservation.id.get.toString)
      .build )
    SchedulerAccess.scheduler.scheduleJob(job, trg)
  }

  def destroyReservation(reservation: Reservation) {
    val job = JobBuilder.newJob(classOf[ReservationDestroyJob]).build
    val trg = ( TriggerBuilder.newTrigger
      .withIdentity("destroy", s"res-${reservation.id.get}")
      .startAt(reservation.endTime.toDate)
      .usingJobData("reservationId", reservation.id.get.toString)
      .build )
    SchedulerAccess.scheduler.scheduleJob(job, trg)
  }
  
  def unscheduleReservationJobs(reservation: Reservation) {
    val gm   = GroupMatcher.triggerGroupEquals(s"res-${reservation.id.get}")
    val trgs = SchedulerAccess.scheduler.getTriggerKeys(gm).asScala
    if ( !trgs.exists(_.getName == "create") && trgs.exists(_.getName == "destroy") )
      VboxUtils.destroyReservation(reservation)
    trgs.foreach( SchedulerAccess.scheduler.unscheduleJob(_) ) 
  }

}

object JobRunner {

  def job = JobBuilder.newJob(classOf[ReservationCreateJob]).
    withIdentity("dummyJobName", "group1").build

  def trigger = TriggerBuilder.newTrigger.
    withIdentity("dummyTriggerName", "group1").withSchedule(
      SimpleScheduleBuilder.simpleSchedule.withIntervalInSeconds(5).repeatForever
    ).build

  def apply(scheduler: Scheduler) = {
    scheduler.scheduleJob(job, trigger)
  }

}
