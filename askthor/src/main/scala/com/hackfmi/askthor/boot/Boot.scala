package com.hackfmi.askthor.boot

import akka.io.IO
import akka.actor.{Props, ActorSystem}
import spray.can.Http
import com.hackfmi.askthor.config.Configuration
import com.hackfmi.askthor.rest.ActionServiceActor

object Boot extends App with Configuration {
 
 // create actor system 
 implicit val system = ActorSystem("action-rest-service")

 // initialize and start rest service actor
 val restService = system.actorOf(Props[ActionServiceActor], "rest-endpoint")

 // start HTTP server with rest service actor as a handler
 IO(Http) ! Http.Bind(restService, serviceHost, servicePort)
}
