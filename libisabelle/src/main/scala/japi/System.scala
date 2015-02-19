package edu.tum.cs.isabelle.japi

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import edu.tum.cs.isabelle._
import edu.tum.cs.isabelle.defaults._

import isabelle._

object JSystem {

  def instance(sessionPath: java.io.File, sessionName: String, timeout: Duration): JSystem =
    new JSystem(Await.result(System.instance(Option(sessionPath), sessionName), timeout), timeout)

  def instance(sessionPath: java.io.File, sessionName: String): JSystem =
    instance(sessionPath, sessionName, Duration.Inf)

}

class JSystem private(system: System, timeout: Duration) {

  def getSystem(): System = system
  def getTimeout(): Duration = timeout

  def withTimeout(newTimeout: Duration): JSystem = new JSystem(system, newTimeout)

  private def await[A](future: Future[A]) =
    Await.result(future, timeout)

  def dispose(): Unit =
    await(system.dispose)

  def sendCommand(command: String, args: java.util.List[String]): String =
    XML.Decode.string(await(system.sendCommand(command, args.asScala.map(XML.Encode.string): _*)))

  def sendCommandXML(command: String, args: java.util.List[XML.Body]): XML.Body =
    await(system.sendCommand(command, args.asScala: _*))

}