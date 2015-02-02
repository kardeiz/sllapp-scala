package com.github.kardeiz.sllapp

import org.scalatra._
import scala.slick.jdbc.JdbcBackend.Database

class MainServlet(val db: Database) extends SllappStack {

  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Say <a href="hello-scalate">hello to Scalate</a>.
      </body>
    </html>
  }

}
