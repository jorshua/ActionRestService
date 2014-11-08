package com.hackfmi.askthor.dao

import com.hackfmi.askthor.config.Configuration
import com.hackfmi.askthor.domain._
import java.sql._
import scala.Some
import scala.slick.driver.PostgresDriver.simple.Database.threadLocalSession
import scala.slick.driver.PostgresDriver.simple._
import slick.jdbc.meta.MTable

class ActionDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = "jdbc:postgresql://%s:%d/%s".format(dbHost, dbPort, dbName),
    user = dbUser, password = dbPassword, driver = "org.postgresql.Driver")


  // create tables if not exist
  db.withSession {
    if (MTable.getTables("actions").list().isEmpty) {
      Actions.ddl.create
    }
  }

  /**
   * Saves action entity into database.
   *
   * @param action action entity to
   * @return saved action entity
   */
  def create(action: Action): Either[Failure, Action] = {
    try {
      val id = db.withSession {
        Actions returning Actions.id insert action
      }
      Right(action.copy(id = Some(id)))
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Retrieves specific action from database.
   *
   * @param id id of the action to retrieve
   * @return action entity with specified id
   */
  def get(id: Long): Either[Failure, Action] = {
    try {
      db.withSession {
        Actions.findById(id).firstOption match {
          case Some(action: Action) =>
            Right(action)
          case _ =>
            Left(notFoundError(id))
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Retrieves list of actions with specified parameters from database.
   *
   * @param params search parameters
   * @return list of actions that match given parameters
   */
  def search(params: ActionSearchParameters): Either[Failure, List[Action]] = {
    implicit val typeMapper = Actions.dateTypeMapper

    try {
      db.withSession {
        val query = for {
          action <- Actions if {
          Seq(
            params.a.map(action.a is _),
            params.b.map(action.b is _)
          ).flatten match {
            case Nil => ConstColumn.TRUE
            case seq => seq.reduce(_ && _)
          }
        }
        } yield action

        Right(query.run.toList)
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Produce database error description.
   *
   * @param e SQL Exception
   * @return database error description
   */
  protected def databaseError(e: SQLException) =
    Failure("%d: %s".format(e.getErrorCode, e.getMessage), FailureType.DatabaseFailure)

  /**
   * Produce action not found error description.
   *
   * @param actionId id of the action
   * @return not found error description
   */
  protected def notFoundError(actionId: Long) =
    Failure("action with id=%d does not exist".format(actionId), FailureType.NotFound)
}