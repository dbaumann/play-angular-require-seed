package agents

import scala.concurrent.{Future, Await, TimeoutException}
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import models.User

object ClientSubscriber extends AkkaHelpers {
  type FactoryCommand = ActorRef => AgentFactory.Message

  def props(user: User, upstream: ActorRef)(subscribe: FactoryCommand) =
    Props(new ClientSubscriber(user, upstream, subscribe))

  val agentFactory = blockForActor("/user/AgentFactory")

  sealed trait Message
  case class Subscribe(user: User) extends Message
  case object Terminate extends Message

  /** Incoming messages from clients */
  trait Command

  /** Outgoing messages to clients */
  trait Event
}

/**
 * Facilitates management of WebSocket connections as pubsub subscribers.
 *
 * @param user        An authenticated user represented by this subscriber.
 * @param upstream    Actor representing the websocket connection.
 * @param subscribe   The message to use when obtaining an agent ActorRef from AgentFactory.
 */
class ClientSubscriber(user: User, upstream: ActorRef, subscribe: ClientSubscriber.FactoryCommand) extends Actor with ActorLogging {
  import ClientSubscriber._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  implicit val askTimeout = Timeout(1.second)

  var agent: ActorRef = _

  (ClientSubscriber.agentFactory ? subscribe(self)).map {
    case AgentFactory.Result(agent) =>
      this.agent = agent
      agent ! Subscribe(user)
      context.become(active)
  }.onFailure(exceptions.actorLookup)

  def receive = PartialFunction.empty

  def active: Actor.Receive = {
    case msg: Command => agent ! msg
    case msg: Event => upstream ! msg
    case Terminate => context.stop(self)
  }
}
