package com.timushev.sbt.updates.versions

import scala.util.matching.Regex.Groups

class VersionOrdering extends Ordering[Version] {

  private val subParts = "(\\d+)?(\\D+)?".r

  private def parsePart(s: String): Seq[Either[BigInt, String]] =
    try {
      subParts
        .findAllIn(s)
        .matchData
        .flatMap {
          case Groups(num, str) => Seq(Option(num).map(BigInt.apply).map(Left.apply), Option(str).map(Right.apply))
        }
        .flatten
        .toList
    } catch {
      case _: NumberFormatException => List(Right(s))
    }

  private def toOpt(x: Int): Option[Int] = if (x == 0) None else Some(x)

  private def comparePart(a: String, b: String) = {
    if (a == b) None
    else
      parsePart(a)
        .zip(parsePart(b))
        .map {
          case (Left(x), Left(y))   => x.compare(y)
          case (Left(_), Right(_))  => -1
          case (Right(_), Left(_))  => 1
          case (Right(x), Right(y)) => compareIdentifiers(x, y)
        }
        .find(0 != _)
        .orElse(Some(a.compareTo(b)))
  }

  private def compareIdentifiers(a: String, b: String): Int = {
    def order(s: String): Int = s.toUpperCase match {
      case "SNAP" | "SNAPSHOT" => -5
      case "ALPHA"             => -4
      case "BETA"              => -3
      case "M"                 => -2
      case "RC"                => -1
      case _                   => 0
    }
    val (oa, ob) = (order(a), order(b))
    if (oa < 0 || ob < 0) oa.compareTo(ob) else a.compareTo(b)
  }

  private def compareNumericParts(a: List[Long], b: List[Long]): Option[Int] = (a, b) match {
    case (ah :: at, bh :: bt) => toOpt(ah.compareTo(bh)).orElse(compareNumericParts(at, bt))
    case (ah :: at, Nil)      => toOpt(ah.compareTo(0L)).orElse(compareNumericParts(at, Nil))
    case (Nil, bh :: bt)      => toOpt(0L.compareTo(bh)).orElse(compareNumericParts(Nil, bt))
    case (Nil, Nil)           => None
  }

  private def compareParts(a: List[String], b: List[String]): Option[Int] = (a, b) match {
    case (ah :: at, bh :: bt) => comparePart(ah, bh).orElse(compareParts(at, bt))
    case (_ :: _, Nil)        => Some(1)
    case (Nil, _ :: _)        => Some(-1)
    case (Nil, Nil)           => None
  }

  def compare(x: Version, y: Version): Int = (x, y) match {
    case (InvalidVersion(a), InvalidVersion(b))                 => a.compareTo(b)
    case (InvalidVersion(_), _)                                 => -1
    case (_, InvalidVersion(_))                                 => 1
    case (ReleaseVersion(r1), ReleaseVersion(r2))               => compareNumericParts(r1, r2).getOrElse(0)
    case (ReleaseVersion(r1), PreReleaseVersion(r2, _))         => compareNumericParts(r1, r2).getOrElse(1)
    case (ReleaseVersion(r1), PreReleaseBuildVersion(r2, _, _)) => compareNumericParts(r1, r2).getOrElse(1)
    case (ReleaseVersion(r1), BuildVersion(r2, _))              => compareNumericParts(r1, r2).getOrElse(-1)
    case (PreReleaseVersion(r1, _), ReleaseVersion(r2))         => compareNumericParts(r1, r2).getOrElse(-1)
    case (PreReleaseVersion(r1, p1), PreReleaseVersion(r2, p2)) =>
      compareNumericParts(r1, r2).orElse(compareParts(p1, p2)).getOrElse(0)
    case (PreReleaseVersion(r1, p1), PreReleaseBuildVersion(r2, p2, _)) =>
      compareNumericParts(r1, r2).orElse(compareParts(p1, p2)).getOrElse(-1)
    case (PreReleaseVersion(r1, _), BuildVersion(r2, _))        => compareNumericParts(r1, r2).getOrElse(-1)
    case (PreReleaseBuildVersion(r1, _, _), ReleaseVersion(r2)) => compareNumericParts(r1, r2).getOrElse(-1)
    case (PreReleaseBuildVersion(r1, p1, _), PreReleaseVersion(r2, p2)) =>
      compareNumericParts(r1, r2).orElse(compareParts(p1, p2)).getOrElse(1)
    case (PreReleaseBuildVersion(r1, p1, b1), PreReleaseBuildVersion(r2, p2, b2)) =>
      compareNumericParts(r1, r2).orElse(compareParts(p1, p2)).orElse(compareParts(b1, b2)).getOrElse(0)
    case (PreReleaseBuildVersion(r1, _, _), BuildVersion(r2, _)) => compareNumericParts(r1, r2).getOrElse(-1)
    case (BuildVersion(r1, _), ReleaseVersion(r2))               => compareNumericParts(r1, r2).getOrElse(1)
    case (BuildVersion(r1, _), PreReleaseVersion(r2, _))         => compareNumericParts(r1, r2).getOrElse(1)
    case (BuildVersion(r1, _), PreReleaseBuildVersion(r2, _, _)) => compareNumericParts(r1, r2).getOrElse(1)
    case (BuildVersion(r1, b1), BuildVersion(r2, b2)) =>
      compareNumericParts(r1, r2).orElse(compareParts(b1, b2)).getOrElse(0)
  }

}
