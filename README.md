Scrock
======

Scrock is a simple implementation of cron rule parser and time generator written purely in scala.

This simple parser only support pure number definition, so text such like *Jan*, *Mon*, *@weekly* won't work.
 For detail definition of cron configuration, please refer [Cron](https://en.wikipedia.org/wiki/Cron).

Dependency
----------

This library has not been publish to sonatype yet. You can build a local copy or just reference github project in
build.sbt.

Usage
-----

    > Cron("* 24 9 8 *")
    res0: Either[String,com.busyjay.scrock.Scrock.Cron] = Left(24 is not allowed.)
    > Cron("* 23 9 8 *")
    res1: Either[String,com.busyjay.scrock.Scrock.Cron] = Right(Cron(*,23,9,8,*,None))
    > Cron("* 23 9 8 *").right.get.validate(new DateTime(2015, 8, 9, 0, 0, 0))
    res3: Boolean = false
    > Cron("* 23 9 8 *").right.get.validate(new DateTime(2015, 8, 9, 23, 0, 0))
    res4: Boolean = true
    > Cron("* 23 9 8 *").right.get.next(new DateTime(2015, 8, 9, 23, 0, 0))
    res5: Option[org.joda.time.DateTime] = Some(2015-08-09T23:01:00.000+00:00)
    > Cron("* 23 9 8 *").right.get.next(new DateTime(9999, 8, 9, 23, 59, 0)) // only allow years before 10000 by default.
    res6: Option[org.joda.time.DateTime] = None
    > Cron("* 23 9 8 * 0-10000").right.get.next(new DateTime(9999, 8, 9, 23, 59, 0))
    res7: Option[org.joda.time.DateTime] = Some(10000-08-09T23:00:00.000+00:00)
