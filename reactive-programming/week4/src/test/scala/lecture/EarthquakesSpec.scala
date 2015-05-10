package lecture

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner
import rx.lang.scala._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class EarthquakesSpec extends FunSuite with Matchers {

  import Magnitude._
  import EarthQuake._

  ignore ("earthquake example") {
    val quakes = usgs()

    val major: Observable[(GeoCoordinate, Magnitude)] = quakes.map(
      q => (q.location, Magnitude(q.magnitude))
    ).filter {
      case (loc, mag) => mag >= Major
    }

    major.subscribe(
      { case(loc, mag) => println(s"Magnitude ${mag} quake at ${loc}") },
      e => Unit,
      () => Unit
    )
  }
}

case class GeoCoordinate()
case class Country()

class EarthQuake() {
  def magnitude: Double = ???
  def location: GeoCoordinate = ???
}

object EarthQuake {
  def usgs(): Observable[EarthQuake] = ???
  def reverseGeocode(c: GeoCoordinate): Future[Country] = ???

  val withCountry: Observable[Observable[(EarthQuake, Country)]] = {
    usgs().map(quake => {
      val country: Future[Country] = reverseGeocode(quake.location)
      Observable.from(country.map(c => (quake, c)))
    })
  }

  val m: Observable[(EarthQuake, Country)] = withCountry.flatten
  val c: Observable[(EarthQuake, Country)] = withCountry.concat // ordered. but slow.

  val byCountry: Observable[(Country, Observable[(EarthQuake, Country)])] =
    m.groupBy({case (quake, country) => country })
}

object Magnitude extends Enumeration {
  def apply(magnitude: Double): Magnitude = ???
  type Magnitude = Value

  val Micro, Minor, Light, Moderate, Strong, Major, Great = Value
}


