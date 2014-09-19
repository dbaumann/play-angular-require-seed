package agents

import akka.actor._

trait AkkaHelpers {
  import scala.concurrent.duration._
  import scala.concurrent.{Await, TimeoutException}

  import akka.actor.{ActorRef, ActorNotFound}

  import play.api.libs.concurrent.Akka
  import play.api.libs.concurrent.Execution.Implicits.defaultContext
  import play.api.Play.current

  /**
   * Replaces `akka.actor.ActorSystem.actorFor` with a blocking call to `akka.actor.ActorSystem.actorSelection`, which is necessary for consistent behavior with distributed actor systems.
   * @note See http://doc.akka.io/docs/akka/2.2.3/project/migration-guide-2.1.x-2.2.x.html#use-actorselection-instead-of-actorfor
   */
  def blockForActor(path: String, duration: FiniteDuration = 5.seconds): ActorRef = try {
    Await.result(Akka.system.actorSelection(path).resolveOne(duration), duration)
  } catch exceptions.actorLookup

  val df = new java.text.SimpleDateFormat("yyyy-MM-dd")
}

// http://doc.akka.io/docs/akka/2.2.0/scala/actors.html
class PartialFunctionBuilder[A, B] {
  import scala.collection.immutable.Vector

  // Abbreviate to make code fit
  type PF = PartialFunction[A, B]

  private var pfsOption: Option[Vector[PF]] = Some(Vector.empty)

  private def mapPfs[C](f: Vector[PF] => (Option[Vector[PF]], C)): C = {
    pfsOption.fold(throw new IllegalStateException("Already built"))(f) match {
      case (newPfsOption, result) => {
        pfsOption = newPfsOption
        result
      }
    }
  }

  def +=(pf: PF): Unit =
    mapPfs { case pfs => (Some(pfs :+ pf), ()) }

  def result(): PF =
    mapPfs { case pfs => (None, pfs.foldLeft[PF](Map.empty) { _ orElse _ }) }
}

trait ComposableActor extends Actor {
  protected lazy val receiveBuilder = new PartialFunctionBuilder[Any, Unit]
  final def receive = receiveBuilder.result()
}
