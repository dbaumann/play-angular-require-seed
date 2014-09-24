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
}

case class Project(
  id: Option[Long],
  name: String,
  tasks: Seq[Task] = Seq.empty[Task]
)

object Project {
  def default() = Project(None, "")
  implicit val projectFormat = Json.format[Project]
}
