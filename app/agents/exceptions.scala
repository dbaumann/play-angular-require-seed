package agents

import scala.concurrent.TimeoutException

import akka.actor.ActorNotFound

object exceptions {
  case class AgentException(msg: String) extends RuntimeException(msg)

  def actorLookup: PartialFunction[java.lang.Throwable, Nothing] = {
    case e: TimeoutException =>
      throw new AgentException("error looking up actor: future timed out: " + e.toString)
    case e: ActorNotFound =>
      throw new AgentException("error looking up actor: actor not found: " + e.toString)
  }
}


