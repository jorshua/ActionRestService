package com.hackfmi.askthor.rest

import akka.actor.Actor
import net.liftweb.json.Serialization._
import net.liftweb.json.{DateFormat, Formats}
import spray.routing._
import spray.http._
import MediaTypes._
import spray.httpx.unmarshalling.Unmarshaller
import com.hackfmi.askthor.domain.Action

class ActionServiceActor extends Actor with ActionService {

  implicit def actorRefFactory = context

  def receive = runRoute(rest)
}

trait ActionService extends HttpService {

  val rest = respondWithMediaType(MediaTypes.`text/plain`) {
    path("action") {
      get {
        complete {
          "test"
        }
      } ~
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[Action](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          action: Action =>
            ctx: RequestContext =>
              handleRequest(ctx, StatusCodes.Created) {
                actionService.create(action)
              }
        }
      }
    } ~
    path("action" / IntNumber ) {
      actionId =>
        get {
          complete {
            "test ".concat(actionId.toString)
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
