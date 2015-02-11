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
    props.setProperty("org.quartz.threadPool.threadCount", "5")
    props.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool")
    props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore")
    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true")
    props.setProperty("org.quartz.scheduler.jobFactory.class", "org.quartz.simpl.SimpleJobFactory")
    props
  }

  def buildScheduler = (new StdSchedulerFactory(config)).getScheduler

}


class ReservationCreateJob extends Job {

  import org.slf4j.LoggerFactory

  val logger = LoggerFactory.getLogger(getClass)

  def execute(context: JobExecutionContext) {
    logger.info("Hello")
  }

}

object JobUtil {
  import Tables._
  import LocalDriver.simple._
  
  def createReservation(reservation: Reservation, user: User)(implicit scheduler: Scheduler) = {
    val job = JobBuilder.newJob(classOf[ReservationCreateJob]).build
    val trg = TriggerBuilder.newTrigger.withIdentity(
      "create", s"${user.id.get}-${reservation.id.get}"
    ).startAt(reservation.startTime.toDate).build
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
