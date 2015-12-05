package com.busyjay.scrock

import org.joda.time.DateTime
import org.scalatest._
import org.slf4j.LoggerFactory

/**
  * Created by jay on 15-12-5.
  */
class ScrockTest extends WordSpec with Matchers with OptionValues with Inside with Inspectors {
  val logger = LoggerFactory.getLogger(getClass)
  def dateTime(year: Int, month: Int, day: Int, hour: Int=0, minutes: Int=0, seconds: Int=0) = new DateTime(year, month, day, hour, minutes, seconds)
  "CronExpression" should {
    "return next qualify one no mater how current one is" in {
      val cron = Scrock.Cron("* 8 9 8 *").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 2))
      cron.next(dateTime(2015, 8, 9, 9, 1)).get should be(dateTime(2016, 8, 9, 8, 0))
      Scrock.Cron("5 * * * *").right.get.next(
        dateTime(2015, 11, 15, 23, 12, 46)).get should be (dateTime(2015, 11, 16, 0, 5))
    }
    "wrap out the seconds and milliseconds" in {
      val cron = Scrock.Cron("* 8 9 8 *").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1, 3)).get should be(dateTime(2015, 8, 9, 8, 2))
      cron.next(dateTime(2015, 8, 9, 8, 1, 3).withMillisOfSecond(20)).get should be(dateTime(2015, 8, 9, 8, 2))
    }
    "accept divide format" in {
      val con = Scrock.Cron("*/10 8 9 8 *").right.get
      con.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 10))
      con.next(dateTime(2015, 8, 9, 9, 1)).get should be(dateTime(2016, 8, 9, 8, 0))
    }
    "accept range format" in {
      val cron = Scrock.Cron("*/10 8-9 9 8 *").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 10))
      cron.next(dateTime(2015, 8, 9, 9, 1)).get should be(dateTime(2015, 8, 9, 9, 10))
      cron.next(dateTime(2015, 8, 9, 10, 1)).get should be(dateTime(2016, 8, 9, 8, 0))
      cron.next(dateTime(2015, 8, 9, 9, 0)).get should be(dateTime(2015, 8, 9, 9, 10))
    }
    "accept list format" in {
      val cron = Scrock.Cron("*/10 8,9 9 8 *").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 10))
      cron.next(dateTime(2015, 8, 9, 9, 1)).get should be(dateTime(2015, 8, 9, 9, 10))
    }
    "accept year extension" in {
      val cron = Scrock.Cron("*/10 8 9 8-9 * 2015").right.get
      logger.debug(s"$cron")
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 10))
      cron.next(dateTime(2015, 8, 9, 9, 1)).get should be(dateTime(2015, 9, 9, 8, 0))
      cron.next(dateTime(2015, 9, 9, 8, 59)) should be(None)
    }
    "validate weekday format" in {
      var cron = Scrock.Cron("*/10 8 9 8-9 7 2015").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 8, 9, 8, 10))
      cron.next(dateTime(2015, 8, 9, 9, 1)) should be(None)
      cron = Scrock.Cron("*/10 8 9 8-9 3 2015").right.get
      cron.next(dateTime(2015, 8, 9, 8, 1)).get should be(dateTime(2015, 9, 9, 8))
      cron.next(dateTime(2015, 9, 9, 9, 1)) should be(None)
      cron = Scrock.Cron("*/10 8 9 * 7 2015").right.get
      val con2 = Scrock.Cron("*/10 8 9 * 0 2015").right.get
      val t = dateTime(2015, 8, 9, 8, 1)
      cron.next(t) should be (con2.next(t))
    }
    "check boundary" in {
      Scrock.Cron("* 24 9 8 *") should be ('left)
      Scrock.Cron("* 22 29 2 *") should be ('right)
      Scrock.Cron("* 22 29 2-3 *") should be ('right)
      Scrock.Cron("* 22 30 2 *") should be ('left)
      Scrock.Cron("* 22 1-30 2 *") should be ('left)
      Scrock.Cron("* 22 1-32 3 *") should be ('left)
      Scrock.Cron("* 22 9 13 *") should be ('left)
      Scrock.Cron("* 22 9 12 0") should be ('right)
      Scrock.Cron("* 22 9 12 0 * *") should be ('left)
      Scrock.Cron("*/0 22 29 2 *") should be ('left)
      Scrock.Cron("* 22 29 2 */2") should be ('left)
      Scrock.Cron("s 22 29 2 *") should be ('left)
      var cron = Scrock.Cron("*/10 22 29 2 * 2000-2008").right.get
      cron.next(dateTime(2004, 2, 29, 22, 51)).get should be(dateTime(2008, 2, 29, 22))
      cron.next(dateTime(2008, 2, 29, 22, 51)) should be(None)
      cron = Scrock.Cron("*/10 22 * * * 2000-2008").right.get
      cron.next(dateTime(2004, 3, 31, 22, 51)).get should be(dateTime(2004, 4, 1, 22))
      cron.next(dateTime(2008, 12, 31, 22, 51)) should be(None)
    }
    "be able to validate existing datetime" in {
      Scrock.Cron("* 22 29 2 *").right.get.validate(dateTime(2012, 2, 29, 22, 1)) should be (right = true)
      Scrock.Cron("* 22 29 2 *").right.get.validate(dateTime(2012, 2, 29, 1, 1)) should be (right = false)
      Scrock.Cron("* 22 29 2 *").right.get.validate(dateTime(2012, 2, 29, 22)) should be (right = true)
      Scrock.Cron("* 22 29 2 *").right.get.validate(dateTime(2012, 2, 28, 22, 1)) should be (right = false)
      Scrock.Cron("* 22 29 2 *").right.get.validate(dateTime(2012, 3, 29, 21, 1)) should be (right = false)
    }
    "all generated value should be valid" in {
      val cron = Scrock.Cron("29 * 20-31 * 0").right.get
      var d = dateTime(2012, 1, 1)
      (1 to 10000).foreach(_ => {
        d = cron.next(d).get
        cron.validate(d) should be (right = true)
      })
    }
  }
}
