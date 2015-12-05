package com.busyjay.scrock

import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.util.Try

/**
  * A simple cron rule validator and time generator written in pure scala.
  *
  * This simple parser only support pure number definition, so text such like ''Jan'', ''Mon'' won't get
  * recognized. For detail definition of cron configuration, please refer [[https://en.wikipedia.org/wiki/Cron Cron]].
  *
  * Created by jay on 15-12-5.
  */
object Scrock {
  /**
    * Int list matcher, support format like ''1'' or ''1,2,3''.
    */
  private val IntList =
    """^[0-9,]+$""".r
  /**
    * Int range matcher, support format like ''0-10''.
    */
  private val IntRange =
    """^([0-9]+)-([0-9]+)$""".r
  /**
    * Wildcard value matcher.
    */
  private val AnyValue = "*"
  /**
    * Divide matcher, support format like ''*\/2''.
    */
  private val Divide =
    """^\*/([0-9]+)$""".r

  /**
    * Allowed values in minute field.
    */
  private val MinuteAllowed = 0 to 59
  /**
    * Allowed values in hour field.
    */
  private val HourAllowed = 0 to 23
  /**
    * Allowed values in day field.
    */
  private val DayAllowed = 1 to 31
  /**
    * Allowed values in weekday field, 0 is Sunday, 1 is Monday, 6 is Saturday, 7 is Sunday.
    */
  private val WeekDayAllowed = 0 to 7
  /**
    * Allowed values in month field.
    */
  private val MonthAllowed = 1 to 12
  /**
    * Allowed values in year field.
    */
  private val YearAllowed = Int.MinValue to Int.MaxValue

  /**
    * Throws when cron format is invalid.
    * @param message error message.
    */
  case class ParseException(message: String) extends Exception(message)

  /**
    * A cron object that provides basic functions.
    * @param minutes expression in minute field.
    * @param hours expression in hour field.
    * @param dayOfMonth expression in day field.
    * @param month expression in month field.
    * @param dayOfWeek expression in weekday field.
    * @param year expression in year field.
    */
  case class Cron(minutes: String, hours: String, dayOfMonth: String, month: String,
                  dayOfWeek: String, year: Option[String] = Some("1970-9999")) {

    /**
      * Possible value in minute field.
      */
    private val minuteHolder: Stream[Int] = buildHolder(minutes, MinuteAllowed)
    /**
      * Possible value in hour field.
      */
    private val hourHolder: Stream[Int] = buildHolder(hours, HourAllowed)
    /**
      * Possible value in day field.
      */
    private val dayHolder: Stream[Int] = buildHolder(dayOfMonth, DayAllowed)
    /**
      * Possible value in month field.
      */
    private val monthHolder: Stream[Int] = buildHolder(month, MonthAllowed)

    if (monthHolder.head == 2 && monthHolder.tail.isEmpty && dayHolder.exists(a => a == 30 || a == 31)) {
      throw ParseException("Day of Feb doesn't contain 30 or 31.")
    }

    /**
      * Possible value in weekday field.
      */
    private val weekDayHolder: Stream[Int] = buildHolder(dayOfWeek, WeekDayAllowed)
    /**
      * Possible value in year field.
      */
    private val yearHolder: Stream[Int] = if (year.isDefined) buildHolder(year.get, YearAllowed) else YearAllowed.toStream

    /**
      * Validate if time in valid to expressions defined in all field.
      * @param d time to be validate.
      * @return whether the time is valid.
      */
    def validate(d: DateTime): Boolean = (
      minuteHolder.contains(d.getMinuteOfHour) && hourHolder.contains(d.getHourOfDay) && dayHolder.contains(d.getDayOfMonth)
        && monthHolder.contains(d.getMonthOfYear) && weekDayHolder.contains(d.getDayOfWeek) && yearHolder.contains(d.getYear)
      )

    /**
      * Find out possible values of a field according expression and values that allowed.
      * @param field expression of a field.
      * @param allowIntRange values that allowed to appear in a field.
      * @return all possible values.
      */
    private def buildHolder(field: String, allowIntRange: Range): Stream[Int] = {
      val res = field match {
        case IntList() =>
          val list = field.split(",").toList.distinct.map(_.toInt).sorted
          list.foreach(i => {
            if (!allowIntRange.contains(i))
              throw ParseException(s"$i is not allowed.")
          })
          list
        case IntRange(b, e) =>
          val (ib, ie) = (b.toInt, e.toInt)
          if (!allowIntRange.contains(ib) || !allowIntRange.contains(ie)) {
            throw ParseException(s"$b or $e is beyond range limit.")
          }
          if (ib > ie) ie to ib else ib to ie
        case Divide(by) =>
          val iby = by.toInt
          if (iby == 0) {
            throw ParseException("should not divide by 0!!!")
          }
          if (allowIntRange.end == 7) {
            throw ParseException(s"Please use list instead of divide when specify weekdays.")
          }
          Range.inclusive(allowIntRange.start, allowIntRange.end, iby)
        case AnyValue => allowIntRange
        case s => throw ParseException("format is not supprted.")
      }
      if (allowIntRange.end == 7 && res.contains(0)) {
        res.view.filter(_ != 0).filter(_ != 7).:+(7).toStream
      } else {
        res.toStream
      }
    }

    /**
      * Find next valid time according to the time given.
      *
      * Please note that no matter if the time given is valid or not, the return value should either be None or greater
      * than the former.
      * @param beginTime time to check.
      * @return next valid time.
      */
    def next(beginTime: DateTime): Option[DateTime] = {
      val tmpDate = beginTime.withSecondOfMinute(0).withMillisOfSecond(0)
      val (ty, tm, td, th, tminute) = (tmpDate.getYear, tmpDate.getMonthOfYear, tmpDate.getDayOfMonth, tmpDate.getHourOfDay, tmpDate.getMinuteOfHour + 1)
      (for (
        y <- yearHolder; if y >= ty;
        m <- monthHolder; if y > ty || m >= tm;
        d <- dayHolder; if (y > ty || m > tm || d >= td) && !Try(tmpDate.withYear(y).withMonthOfYear(m).withDayOfMonth(d).getDayOfWeek).toOption.exists(!weekDayHolder.contains(_));
        h <- hourHolder; if y > ty || m > tm || d > td || h >= th;
        minute <- minuteHolder; if y > ty || m > tm || d > td || h > th || minute >= tminute
      ) yield Try(tmpDate.withYear(y).withMonthOfYear(m).withDayOfMonth(d).withHourOfDay(h).withMinuteOfHour(minute)).toOption
        ).find(_.nonEmpty).flatten
    }
  }

  object Cron {
    private val Logger = LoggerFactory.getLogger(Cron.getClass)

    /**
      * Generate cron object from an expression.
      * @param s expression to be parsed.
      * @return either error message or a valid cron object.
      */
    def apply(s: String): Either[String, Cron] = {
      val arr = s.toUpperCase.split(" ").filter(_.length != 0)
      try {
        arr match {
          case Array(a1, a2, a3, a4, a5) =>
            Right(Cron(a1, a2, a3, a4, a5))
          case Array(a1, a2, a3, a4, a5, a6) =>
            Right(Cron(a1, a2, a3, a4, a5, Some(a6)))
          case _ => Left("there should be only 5 or 6 column for cron expression.")
        }
      } catch {
        case e: ParseException =>
          Logger.debug(e.message, e)
          Left(e.message)
      }
    }
  }

}
