import com.github.kardeiz.sllapp._
import org.scalatra._
import javax.servlet.ServletContext


import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  
  val logger = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.initParameters("org.scalatra.environment") = Settings.appEnv
    DatabaseAccess.startup
    SchedulerAccess.startup
    context.mount(new MainServlet, "/*")
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    SchedulerAccess.shutdown
    DatabaseAccess.shutdown
  }

}
