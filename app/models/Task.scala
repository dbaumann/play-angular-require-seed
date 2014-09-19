package models

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json._

case class Task(
  id: Option[Long],
  date: Date,
  title: String,
  description: String,
  done: Boolean,
  dueDate: Option[Date],
  assignedTo: Option[String]
)

object Task {
  def default(date: Date) = Task(None, date, "", "", false, None, None)
  implicit val taskFormat = Json.format[Task]

  def findByDate(date: Date): Future[Seq[Task]] =
    Future(Seq(
      Task(
        id = Some(1l),
        date = date,
        title = "task 1",
        description = "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Sunt suscipit iusto distinctio eaque. Necessitatibus culpa ullam ratione, fugit, dolorem est in sint harum provident iusto laboriosam praesentium eius debitis possimus.",
        done = false,
        dueDate = Some(date),
        assignedTo = Some("user@user.com")
      )
    ))
}

case class Project(
  id: Option[Long],
  name: String,
  tasks: Seq[Task] = Seq.empty[Task]
)

object Project {
  def default() = Project(None, "")
  implicit val projectFormat = Json.format[Project]

  def findByDate(date: Date): Future[Seq[Project]] =
    Task.findByDate(date).map { tasks =>
      Seq(
        Project(
          id = Some(1l),
          name = "Project 1",
          tasks = tasks
        )
      )
    }

}
