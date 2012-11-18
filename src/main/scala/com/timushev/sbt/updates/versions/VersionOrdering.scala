package com.timushev.sbt.updates.versions

import java.text.{ParsePosition, NumberFormat}

class VersionOrdering extends Ordering[Version] {

  private def parsePart(s: String): Either[Int, String] = {
    val nf = NumberFormat.getIntegerInstance
    val pp = new ParsePosition(0)
    val d = nf.parse(s, pp)
    if (pp.getErrorIndex == -1) Left(d.intValue()) else Right(s)
  }

  private def toOpt(x: Int): Option[Int] = if (x == 0) None else Some(x)

  private def comparePart(a: String, b: String) = {
    toOpt((parsePart(a), parsePart(b)) match {
      case (Left(x), Left(y)) => x compareTo y
      case (Left(_), Right(_)) => -1
      case (Right(_), Left(_)) => 1
      case (Right(x), Right(y)) => x compareTo y
    })
  }

  private def compareIntParts(a: List[Int], b: List[Int]): Option[Int] = (a, b) match {
    case (ah :: at, bh :: bt) => toOpt(ah compareTo bh) orElse compareIntParts(at, bt)
    case (ah :: at, Nil) => toOpt(ah compareTo 0) orElse compareIntParts(at, Nil)
    case (Nil, bh :: bt) => toOpt(0 compareTo bh) orElse compareIntParts(Nil, bt)
    case (Nil, Nil) => None
  }

  private def compareParts(a: List[String], b: List[String]): Option[Int] = (a, b) match {
    case (ah :: at, bh :: bt) => comparePart(ah, bh) orElse compareParts(at, bt)
    case (_ :: _, Nil) => Some(1)
    case (Nil, _ :: _) => Some(-1)
    case (Nil, Nil) => None
  }

  def compare(x: Version, y: Version) = (x, y) match {
    case (InvalidVersion(a), InvalidVersion(b)) => a compareTo b
    case (InvalidVersion(_), _) => -1
    case (_, InvalidVersion(_)) => 1
    case (ReleaseVersion(r1), ReleaseVersion(r2)) => compareIntParts(r1, r2) getOrElse 0
    case (ReleaseVersion(r1), PreReleaseVersion(r2, p2)) => compareIntParts(r1, r2) getOrElse 1
    case (ReleaseVersion(r1), PreReleaseBuildVersion(r2, p2, b2)) => compareIntParts(r1, r2) getOrElse 1
    case (ReleaseVersion(r1), BuildVersion(r2, b2)) => compareIntParts(r1, r2) getOrElse -1
    case (PreReleaseVersion(r1, p1), ReleaseVersion(r2)) => compareIntParts(r1, r2) getOrElse -1
    case (PreReleaseVersion(r1, p1), PreReleaseVersion(r2, p2)) => compareIntParts(r1, r2) orElse compareParts(p1, p2) getOrElse 0
    case (PreReleaseVersion(r1, p1), PreReleaseBuildVersion(r2, p2, b2)) => compareIntParts(r1, r2) orElse compareParts(p1, p2) getOrElse -1
    case (PreReleaseVersion(r1, p1), BuildVersion(r2, b2)) => compareIntParts(r1, r2) getOrElse -1
    case (PreReleaseBuildVersion(r1, p1, b1), ReleaseVersion(r2)) => compareIntParts(r1, r2) getOrElse -1
    case (PreReleaseBuildVersion(r1, p1, b1), PreReleaseVersion(r2, p2)) => compareIntParts(r1, r2) orElse compareParts(p1, p2) getOrElse 1
    case (PreReleaseBuildVersion(r1, p1, b1), PreReleaseBuildVersion(r2, p2, b2)) => compareIntParts(r1, r2) orElse compareParts(p1, p2) orElse compareParts(b1, b2) getOrElse 0
    case (PreReleaseBuildVersion(r1, p1, b1), BuildVersion(r2, b2)) => compareIntParts(r1, r2) getOrElse -1
    case (BuildVersion(r1, b1), ReleaseVersion(r2)) => compareIntParts(r1, r2) getOrElse 1
    case (BuildVersion(r1, b1), PreReleaseVersion(r2, p2)) => compareIntParts(r1, r2) getOrElse 1
    case (BuildVersion(r1, b1), PreReleaseBuildVersion(r2, p2, b2)) => compareIntParts(r1, r2) getOrElse 1
    case (BuildVersion(r1, b1), BuildVersion(r2, b2)) => compareIntParts(r1, r2) orElse compareParts(b1, b2) getOrElse 0
  }

}
