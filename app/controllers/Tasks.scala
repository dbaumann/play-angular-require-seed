package controllers

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{HashMap, MultiMap, Set}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import play.api.mvc._
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.json._

import julienrf.variants.Variants
import dbaumann.abstractcc.AbstractCaseClass._

import agents._
import models._

object Tasks extends Controller with Security with AkkaHelpers {
  val agentFactory = blockForActor("user/AgentFactory")

  def initialize(stamp: Long) = Action.async {
    val date = new Date(stamp)
    implicit val timeout = Timeout(5.seconds)

    val result = Json.obj(
      "defaults" -> Json.obj(
        "project" -> Project.default(),
        "task" -> Task.default(date)
      )
    )

    (for {
      AgentFactory.Result(agent) <- agentFactory ? AgentFactory.TasksObtain(date)
      TasksAgent.AllProjects(projects) <- agent ? TasksAgent.GetAllProjects
    } yield {
      Ok(result ++ Json.obj("projects" -> projects))
    }) fallbackTo {
      Future(Ok(result))
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

  // backend messages, not shared with client
  case object GetAllProjects
  case class AllProjects(projects: Seq[Project])


  sealed trait Message

  // PubsubProtocol messages
  @concrete case class ActionFailed(stamp: Long) extends Message with ClientSubscriber.Event

  // AuthorsProtocol messages
  @concrete case class ActionUnauthorized(message: String) extends Message with ClientSubscriber.Event

  // TasksAgent messages
  case class ProjectAdd(stamp: Long, project: Project) extends Message with ClientSubscriber.Command
  case class ProjectAddDone(user: User, stamp: Long, project: Project) extends Message with ClientSubscriber.Event

  case class ProjectUpdate(stamp: Long, project: Project) extends Message with ClientSubscriber.Command
  case class ProjectUpdateDone(user: User, stamp: Long, project: Project) extends Message with ClientSubscriber.Event

  case class TaskAdd(stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Command
  case class TaskAddDone(user: User, stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Event

  case class TaskUpdate(stamp: Long, task: Task) extends Message with ClientSubscriber.Command
  case class TaskUpdateDone(user: User, stamp: Long, task: Task) extends Message with ClientSubscriber.Event

  case class TaskDelete(stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Command
  case class TaskDeleteDone(user: User, stamp: Long, task: Task, projectId: Long) extends Message with ClientSubscriber.Event


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

  // agent state
  var projects = Seq.empty[Project]
  var tasks = Seq.empty[Task]
  val projectTasks = new HashMap[Long, Set[Long]] with MultiMap[Long, Long]

  receiveBuilder += {
    case GetAllProjects =>
      val joinedProjects = projects.map { project =>
        projectTasks.get(project.id.get).map { taskIds =>
          project.copy(
            tasks = this.tasks.filter(task => taskIds.contains(task.id.get))
          )
        }.getOrElse(project)
      }

      sender ! AllProjects(joinedProjects)

    case ProjectAdd(stamp, project) =>
      withUser() { origin => user =>
        val newProject = project.copy(id = Some(projects.size))
        projects :+= newProject
        
        log.info("Broadcasting ProjectAddDone to {} clients", clients.size)
        broadcast(ProjectAddDone(user, stamp, newProject), origin)
      }

    case ProjectUpdate(stamp, project) =>
      withUser() { origin => user =>
        val index = projects.indexWhere(_.id == project.id)
        projects = projects.updated(index, project)
        
        log.info("Broadcasting ProjectUpdateDone to {} clients", clients.size)
        broadcast(ProjectUpdateDone(user, stamp, project), origin)
      }

    case TaskAdd(stamp, task, projectId) =>
      withUser() { origin => user =>
        val newTask = task.copy(id = Some(tasks.size), assignedTo = Some(user.name))
        tasks :+= newTask
        projectTasks.addBinding(projectId, newTask.id.get)

        log.info("Broadcasting TaskAddDone to {} clients", clients.size)
        broadcast(TaskAddDone(user, stamp, newTask, projectId), origin)
      }

    case TaskUpdate(stamp, task) =>
      withUser() { origin => user =>
        val index = tasks.indexWhere(_.id == task.id)
        tasks = tasks.updated(index, task)

        log.info("Broadcasting TaskUpdateDone to {} clients", clients.size)
        broadcast(TaskUpdateDone(user, stamp, task), origin)
      }

    case TaskDelete(stamp, task, projectId) =>
      withUser() { origin => user =>
        tasks = tasks.filterNot(_.id == task.id)
        projectTasks.removeBinding(projectId, task.id.get)

        log.info("Broadcasting TaskDeleteDone to {} clients", clients.size)
        broadcast(TaskDeleteDone(user, stamp, task, projectId), origin)
      }
  }
}
