import com.github.kardeiz.sllapp._
import org.scalatra._
import javax.servlet.ServletContext


import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  
  val logger = LoggerFactory.getLogger(getClass)

  val cpds = DatabaseInit.buildCpds

  val scheduler = SchedulerInit.buildScheduler

  override def init(context: ServletContext) {
    val db  = DatabaseInit.buildDbFor(cpds)
    scheduler.start
    context.mount(new MainServlet()(db, scheduler), "/*")
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    scheduler.shutdown
    cpds.close
  }

}
