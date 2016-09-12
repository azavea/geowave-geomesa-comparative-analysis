package com.azavea.ca.server

import de.heikoseeberger.akkahttpcirce.CirceSupport

trait BaseService extends BaseComponent with CirceSupport with Config {
  def checkIfIsTest(q: Option[String]) = q.map { x => if(x == "false") false else true }.getOrElse(true)
}
