package com.hackfmi.askthor.domain

import scala.slick.driver.PostgresDriver.simple._

case class Action (id: Option[Long],
                   a : String,
                   b : String)



object Actions extends Table[Action]("actions") {

  def id = column[Long]("id", O.AutoInc)

  def a = column[String]("a")

  def b = column[String]("b")

  def * = id.? ~ a ~ b <>(Action, Action.unapply _)

  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Date](
  {
    ud => new java.sql.Date(ud.getTime)
  }, {
    sd => new java.util.Date(sd.getTime)
  })

  val findById = for {
    id <- Parameters[Long]
    c <- this if c.id is id
  } yield c

}


