import com.github.kardeiz.sllapp._
import org.scalatra._
import javax.servlet.ServletContext


import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  
  val logger = LoggerFactory.getLogger(getClass)

  val cpds = DatabaseHelper.buildDataSource

  override def init(context: ServletContext) {
    val db = DatabaseHelper.setupDatabase(cpds)
    DatabaseHelper.buildDatabaseTables(db)
    context.mount(new MainServlet(db), "/*")
  }

  override def destroy(context: ServletContext) {
    super.destroy(context)
    cpds.close
  }

}
