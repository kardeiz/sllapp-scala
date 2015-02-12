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

import org.quartz.ee.servlet.QuartzInitializerListener

object SchedulerInit {

  lazy val config = {
    val props = new java.util.Properties
    props.put("org.quartz.threadPool.threadCount", "5")
    props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
    props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
    props.put("org.quartz.scheduler.skipUpdateCheck", "true")
    props.put("org.quartz.scheduler.jobFactory.class", "org.quartz.simpl.SimpleJobFactory")
    props
  }

  def buildScheduler = (new StdSchedulerFactory(config)).getScheduler

}


class ReservationCreateJob extends Job {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

  def execute(context: JobExecutionContext) {
    val userId = context.getMergedJobDataMap.getIntFromString("userId")
    val reservationId = context.getMergedJobDataMap.getIntFromString("userId")
    logger.info("Hello")
  }

}

object JobUtil {
  import Tables._
  import LocalDriver.simple._
  
  def createReservation(reservation: Reservation, user: User)(implicit scheduler: Scheduler) = {
    val job = JobBuilder.newJob(classOf[ReservationCreateJob]).build
    val trg = ( TriggerBuilder.newTrigger
      .withIdentity("create", s"res-${reservation.id.get}")
      .startAt(reservation.startTime.toDate)
      .usingJobData("reservationId", reservation.id.get.toString)
      .usingJobData("userId", user.id.get.toString)
      .build )
    scheduler.scheduleJob(job, trg)
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
