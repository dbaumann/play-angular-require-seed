import play.api.{Application, GlobalSettings}
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.api.Play.current 

object Global extends WithFilters(new GzipFilter(shouldGzip =
  (request, response) => {
    val contentType = response.headers.get("Content-Type")
    contentType.exists(_.startsWith("text/html")) || request.path.endsWith("jsroutes.js")
  }
)) with GlobalSettings {
  override def onStart(app: Application) = {
    Akka.system.actorOf(Props[agents.AgentFactory], name="AgentFactory")
    ()
  }
}
