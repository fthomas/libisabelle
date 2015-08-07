package edu.tum.cs.isabelle

import java.net.{URL, URLClassLoader}
import java.nio.file.Path

import scala.util.control.Exception._

import edu.tum.cs.isabelle.api._

object Implementations {
  def empty: Implementations = new Implementations(Map.empty)

  case class Entry(urls: List[URL], name: String)

  def makeEnvironment(home: Path, clazz: Class[_ <: Environment]): Environment =
    clazz.getConstructor(classOf[Path]).newInstance(home)
}

class Implementations private(entries: Map[Version, Implementations.Entry]) {

  private def loadClass(entry: Implementations.Entry): Option[Class[_ <: Environment]] = {
    val classLoader = new URLClassLoader(entry.urls.toArray, Thread.currentThread.getContextClassLoader)
    catching(classOf[ClassCastException]) opt classLoader.loadClass(entry.name).asSubclass(classOf[Environment])
  }

  def add(entry: Implementations.Entry): Option[Implementations] =
    loadClass(entry).flatMap(Environment.getVersion) map { version =>
      new Implementations(entries + (version -> entry))
    }

  def addAll(entries: List[Implementations.Entry]): Option[Implementations] =
    entries.foldLeft(Some(this): Option[Implementations]) { (impls, entry) =>
      impls.flatMap(_.add(entry))
    }

  def versions: Set[Version] = entries.keySet

  def makeEnvironment(home: Path, version: Version) =
    entries get version flatMap loadClass map { Implementations.makeEnvironment(home, _) }

}
