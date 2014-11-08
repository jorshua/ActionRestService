package com.hackfmi.askthor.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.hackfmi.askthor.domain._
import com.hackfmi.askthor.dao._
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import net.liftweb.json.Serialization._
import net.liftweb.json.{DateFormat, Formats}
import scala.Some
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._


class ActionServiceActor extends Actor with ActionService {

  implicit def actorRefFactory = context

  def receive = runRoute(rest)
}

trait ActionService extends HttpService with SLF4JLogging {

  val actionService = new ActionDAO

  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")

      def parse(s: String): Option[Date] = try {
        Some(sdf.parse(s))
      } catch {
        case e: Exception => None
      }

      def format(d: Date): String = sdf.format(d)
    }
  }

  implicit val string2Date = new FromStringDeserializer[Date] {
    def apply(value: String) = {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")
      try Right(sdf.parse(value))
      catch {
        case e: ParseException => {
          Left(MalformedContent("'%s' is not a valid Date value" format (value), e))
        }
      }
    }
  }

  implicit val customRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          write(Map("error" -> response.entity.asString))))
    } {
      RejectionHandler.Default(rejections)
    }
  }

  val rest = respondWithMediaType(MediaTypes.`application/json`) {
    path("action") {
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[Action](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          action: Action =>
            ctx: RequestContext =>
              handleRequest(ctx, StatusCodes.Created) {
                log.debug("Creating action: %s".format(action))
                actionService.create(action)
              }
        }
      } ~
        get {
          parameters('id.as[Long] ?, 'a.as[String] ?, 'b.as[String] ?).
            as(ActionSearchParameters) {
            searchParameters: ActionSearchParameters => {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Searching for actions with parameters: %s".format(searchParameters))
                  actionService.search(searchParameters)
                }
            }
          }
        }
    } ~
      path("action" / LongNumber) {
        actionId =>
          get {
            ctx: RequestContext =>
              handleRequest(ctx) {
                log.debug("Retrieving action with id %d".format(actionId))
                actionService.get(actionId)
              }
          }
      }
  }

  /**
   * Handles an incoming request and create valid response for it.
   *
   * @param ctx         request context
   * @param successCode HTTP Status code for success
   * @param action      action to perform
   */
  protected def handleRequest(ctx: RequestContext, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {
    action match {
      case Right(result: Object) =>
        ctx.complete(successCode, write(result))
      case Left(error: Failure) =>
        ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
      case _ =>
        ctx.complete(StatusCodes.InternalServerError)
    }
  }
}
