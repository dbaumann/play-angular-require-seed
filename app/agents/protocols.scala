package agents

import java.util.Date

import akka.actor._

import dbaumann.abstractcc.AbstractCaseClass._

import models.User

trait PubsubProtocol {
  @`abstract` case class ActionFailed(stamp: Long) extends ClientSubscriber.Event
}

trait Pubsub {
  composed: ComposableActor with ActorLogging =>

  val title: String
  val unsubscribe: AgentFactory.Message

  val pubsubProtocol: PubsubProtocol
  import pubsubProtocol._

  var clients = Map.empty[ActorRef, User]

  def broadcast(message: ClientSubscriber.Event, origin: ActorRef) =
    clients.foreach(_._1 ! message)

  def subscribeClient(user: User): Unit = {
    log.info("{} connected to {}", user.name, title)
    clients += sender -> user
    context.watch(sender)
    ()
  }

  def unsubscribeClient(): Unit =
    clients.get(sender).foreach { user =>
      log.info("{} disconnected from {}", user.name, title)
      clients -= sender
    }

  receiveBuilder += {
    case ClientSubscriber.Subscribe(user) => subscribeClient(user)
    case Terminated(subscriber) =>
      unsubscribeClient
      context.parent.tell(unsubscribe, subscriber)
  }
}


trait AuthorsProtocol {
  @`abstract` case class ActionUnauthorized(message: String) extends ClientSubscriber.Event
}

trait Authors {
  composed: ComposableActor with ActorLogging with Pubsub =>

  val defaultUser: Option[User]

  val pubsubProtocol: PubsubProtocol
  import pubsubProtocol._

  val authorsProtocol: AuthorsProtocol
  import authorsProtocol._

  override def broadcast(message: ClientSubscriber.Event, origin: ActorRef) = {
    clients.foreach(_._1 ! message)
    if(!clients.contains(origin)) origin ! message
  }
  
  def withUser(userFilter: User => Boolean = Function.const(true))(f: ActorRef => User => Unit): Unit = {
    val user = clients.get(sender).orElse(defaultUser)
    user.filter(userFilter).map(f(sender)(_)).getOrElse {
      log.warning("ActionUnauthorized for {}", user)
      sender ! ActionUnauthorized("Action not permitted.")
    }
  }
}
