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

  // def extractScheduler(context: javax.servlet.ServletContext) = {
  //   Option(context.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY) match {
  //     case Some(factory: StdSchedulerFactory) => Option(factory.getScheduler("LocalScheduler"))
  //     case _ => None
  //   }.getOrElse(throw new Danger)
  // }

}


class ReservationCreateJob extends Job {

  def execute(context: JobExecutionContext) {
    println("Hello Quartz!");
  }

}

object JobRunner {

  def job = JobBuilder.newJob(classOf[ReservationCreateJob]).
    withIdentity("dummyJobName", "group1").build

  def trigger = TriggerBuilder.newTrigger.
    withIdentity("dummyTriggerName", "group1").withSchedule(
      SimpleScheduleBuilder.simpleSchedule.withIntervalInSeconds(5).repeatForever
    ).build

  def apply = {
    val scheduler = (new StdSchedulerFactory).getScheduler
    scheduler.start
    scheduler.scheduleJob(job, trigger)
  }

}