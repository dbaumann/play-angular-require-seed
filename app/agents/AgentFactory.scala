package agents

import java.util.Date

import scala.concurrent.duration._
import collection.mutable.{HashMap, MultiMap, Set}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

object AgentFactory {
  sealed trait Message

  case class Result(agent: ActorRef) extends Message
  case object Inactive extends Message


  /**
   * Obtain an agent for the given date, creating if no other clients exist.
   */
  case class TasksSubscribe(subscriber: ActorRef, date: Date) extends Message
  
  /**
   * Release an agent for the given date, retiring if no other clients exist.
   */
  case class TasksUnsubscribe(agent: ActorRef) extends Message

  /**
   * Obtain an ActorRef for the given date, but don't subscribe.
   */
  case class TasksObtain(date: Date) extends Message
}

/**
 * Supervises and manages Agent lifecycles.
 */
class AgentFactory extends Actor with ActorLogging with AkkaHelpers {
  import AgentFactory._

  // each date has an agent actor
  var tasksAgents = Map.empty[Date, ActorRef]

  // each agent actor has a set of users; when all users quit, the agent is retired
  val tasksAgentUsers = new HashMap[ActorRef, Set[ActorRef]] with MultiMap[ActorRef, ActorRef]

  def receive = {
    case TasksSubscribe(subscriber, date) =>
      val agent: ActorRef = tasksAgents.get(date).getOrElse {
        val newAgent = context.actorOf(
          props = controllers.TasksAgent.props(date),
          name = "TasksAgent" + date.getTime
        )
        log.info("TasksAgent initialized for {}", df.format(date))
        tasksAgents += date -> newAgent
        newAgent
      }
      tasksAgentUsers.addBinding(agent, subscriber)
      log.info("{} subscriber added for {}", subscriber, df.format(date))
      sender ! Result(agent)

    case TasksUnsubscribe(agent) =>
      tasksAgentUsers.removeBinding(agent, sender)

      val agentDate = tasksAgents.find(_._2 == agent).map(_._1).get
      log.info("{} subscriber removed for {}", sender, df.format(agentDate))

      // if no remaining users, retire the agent
      if(!tasksAgentUsers.contains(agent)) {
        tasksAgents -= agentDate
        log.info("TasksAgent retired for {}", df.format(agentDate))
        agent ! PoisonPill
      }

    case TasksObtain(date) =>
      tasksAgents.get(date).map(sender ! Result(_)).getOrElse(sender ! Inactive)
  }
}
