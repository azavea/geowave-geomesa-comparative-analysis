package com.azavea.ca.server

import de.heikoseeberger.akkahttpcirce.CirceSupport

trait BaseService extends BaseComponent with CirceSupport with Config {
  // Check query option to see if this is a debug test run
  def checkIfIsTest(q: Option[String]) = q.map { x => if(x == "false") false else true }.getOrElse(true)

  // Check query option to see if we should do a loose bbox query for geomesa
  def checkIfIsLoose(q: Option[String]) = q.map { x => if(x == "true") true else false }.getOrElse(false)

  def looseSuffix(opt: Option[String]): String =
    if(checkIfIsLoose(opt)) "-LOOSE"
    else ""
}
