package com.azavea.ca.core

import java.util.Date

object Time {
  val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

  val s = "yyyy-mm-hhThh:mm:ss"
}

case class TimeQuery(from: Option[Date], to: Option[Date]) {
  val fromStr = from.map(Time.dateFormat.format)

  // Subtract a second from the 'to' time;
  // GeoWave seems to have issues if the date range is right on a periodicity border
  val toStr =
    to
      .map { t => new Date(t.getTime - 1000) }
      .map(Time.dateFormat.format)

  def toCQL(dateField: String): String =
    (fromStr, toStr) match {
      case (Some(f), Some(t)) if f == t =>
        s"($dateField TEQUALS ${t})"
      case (Some(f), Some(t)) =>
        s"($dateField DURING ${f}/${t})"
      case  (Some(f), None) =>
        s"($dateField AFTER $f)"
      case  (None, Some(t)) =>
        s"($dateField BEFORE $t)"
      case _ => ""
    }
}

object TimeQuery {
  def apply(time: Date): TimeQuery =
    new TimeQuery(Some(time), Some(time))

  def apply(from: Date, to: Date): TimeQuery =
    new TimeQuery(Some(from), Some(to))

  def apply(from: String, to: String): TimeQuery =
    new TimeQuery(Some(Time.dateFormat.parse(from)), Some(Time.dateFormat.parse(to)))
}
