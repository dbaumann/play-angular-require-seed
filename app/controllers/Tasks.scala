package controllers

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor._
import akka.util.Timeout
import play.api.mvc._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.json._

import julienrf.variants.Variants
import dbaumann.abstractcc.AbstractCaseClass._

import agents._
import models._

object Tasks extends Controller with Security {

  def initialize(stamp: Long) = Action.async {
    Project.findByDate(new Date(stamp)).map { tasks =>
      Ok(Json.obj(
        "projects" -> tasks,
        "defaults" -> Json.obj(
          "project" -> Project.default(),
          "task" -> Task.default(new Date(stamp))
        )
      ))
    }
  }

  def socket(stamp: Long, userId: Long) = WebSocket.tryAcceptWithActor[TasksAgent.Message, TasksAgent.Message] { implicit request =>

    // Future[Either[play.api.mvc.Result, ActorRef => Props]]
    User.findAsync(userId).map {
      case None => Left(Forbidden)
      case Some(user) =>
        Right { upstream: ActorRef =>
          ClientSubscriber.props(user, upstream) { subscriber: ActorRef => 
            AgentFactory.TasksSubscribe(subscriber, new Date(stamp))
          }
        }
    }
  }
}

object TasksAgent extends PubsubProtocol with AuthorsProtocol {
  def props(date: Date) = Props(new TasksAgent(date))

  sealed trait Message

  // PubsubProtocol messages
  @concrete case class ActionFailed(stamp: Long) extends Message with ClientSubscriber.Event

  // AuthorsProtocol messages
  @concrete case class ActionUnauthorized(message: String) extends Message with ClientSubscriber.Event

  // TasksAgent messages
  case class ProjectAdd(stamp: Long, project: Project) extends Message with ClientSubscriber.Command
  case class ProjectAddDone(user: User, stamp: Long, project: Project) extends Message with ClientSubscriber.Event

  case class TaskAdd(stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Command
  case class TaskAddDone(user: User, stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Event

  case class TaskUpdate(stamp: Long, task: Task) extends Message with ClientSubscriber.Command
  case class TaskUpdateDone(user: User, stamp: Long, task: Task) extends Message with ClientSubscriber.Event

  case class TaskDelete(stamp: Long, task: Task) extends Message with ClientSubscriber.Command
  case class TaskDeleteDone(user: User, stamp: Long, task: Task) extends Message with ClientSubscriber.Event


  // JSON serialization and WebSocket frames for above messages
  implicit val tasksAgentMessageFormat: Format[Message] = Variants.format[Message]

  implicit def tasksAgentMessageFormatter: FrameFormatter[Message] = FrameFormatter.jsonFrame.transform(
    clientEvent => Json.toJson(clientEvent),
    json => Json.fromJson[Message](json).fold(
      invalid => throw new RuntimeException("Bad client event on WebSocket: " + invalid),
      valid => valid
    )
  )
}

class TasksAgent(date: Date) extends Pubsub with Authors with ComposableActor with ActorLogging {
  import TasksAgent._

  // for Pubsub
  val title = date.toString
  val unsubscribe = AgentFactory.TasksUnsubscribe(self)
  val pubsubProtocol = TasksAgent

  // for Authors
  val defaultUser = None//Some(models.repositories.User.system)
  val authorsProtocol = TasksAgent

  implicit val timeout = Timeout(5.seconds)

  receiveBuilder += {
    case TaskAdd(stamp, task, projectId) =>
      withUser() { origin => user =>
        // todo store new task
        broadcast(TaskAddDone(user, stamp, task, projectId), origin)
      }
  }
}
