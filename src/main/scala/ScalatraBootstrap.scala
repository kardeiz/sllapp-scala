import com.github.kardeiz.sllapp._
import org.scalatra._
import javax.servlet.ServletContext


import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  
  val logger = LoggerFactory.getLogger(getClass)

  val cpds = DatabaseInit.buildCpds

  override def init(context: ServletContext) {
    implicit val db  = DatabaseInit.buildDbFor(cpds)
    implicit val sch = null // SchedulerInit.extractScheduler(context)
    context.mount(new MainServlet, "/*")
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    cpds.close
  }

}
