package com.hackfmi.askthor.domain

/**
 * Actions search parameters.
 *
 * @param id id
 * @param a  a
 * @param b  b
 */
case class ActionSearchParameters(id: Option[Long] = None,
                                    a: Option[String] = None,
                                    b: Option[String] = None)