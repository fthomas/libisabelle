package edu.tum.cs.isabelle.impl

import java.nio.file.Path

import scala.concurrent.ExecutionContext

import edu.tum.cs.isabelle.api

@api.Implementation(version = "2015")
class Environment(home: Path) extends api.Environment(home) {

  isabelle.Isabelle_System.init(isabelle_home = home.toAbsolutePath.toString)

  type XMLTree = isabelle.XML.Tree

  def fromYXML(source: String) = isabelle.YXML.parse(source)
  def toYXML(tree: XMLTree) = isabelle.YXML.string_of_tree(tree)

  def text(content: String) = isabelle.XML.Text(content)
  def elem(markup: api.Markup, body: XMLBody) = isabelle.XML.Elem(isabelle.Markup(markup._1, markup._2), body)

  private def destMarkup(markup: isabelle.Markup) =
    (markup.name, markup.properties)

  def destTree(tree: XMLTree) = tree match {
    case isabelle.XML.Text(content) => Left(content)
    case isabelle.XML.Elem(markup, body) => Right((destMarkup(markup), body))
  }

  val exitMarkup = isabelle.Markup.EXIT
  val functionMarkup = isabelle.Markup.FUNCTION
  val initMarkup = isabelle.Markup.INIT
  val protocolMarkup = isabelle.Markup.PROTOCOL

  lazy val executionContext =
    isabelle.Future.execution_context

  type Session = isabelle.Session

  lazy val options = isabelle.Options.init()

  private def mkPaths(path: Option[Path]) =
    path.map(p => isabelle.Path.explode(p.toAbsolutePath.toString)).toList


  def build(config: Configuration) = 
    isabelle.Build.build(
      options = options,
      progress = new isabelle.Build.Console_Progress(verbose = true),
      build_heap = true,
      dirs = mkPaths(config.path),
      verbose = true,
      sessions = List(config.session)
    )

  def create(config: Configuration, consumer: (api.Markup, XMLBody) => Unit) = {
    val content = isabelle.Build.session_content(options, false, mkPaths(config.path), config.session)
    val resources = new isabelle.Resources(content.loaded_theories, content.known_theories, content.syntax)
    val session = new isabelle.Session(resources)

    session.all_messages += isabelle.Session.Consumer[isabelle.Prover.Message]("firehose") {
      case msg: isabelle.Prover.Protocol_Output =>
        consumer(destMarkup(msg.message.markup), isabelle.YXML.parse_body(msg.text))
      case msg: isabelle.Prover.Output =>
        consumer(destMarkup(msg.message.markup), msg.message.body)
      case _ =>
    }

    session.start("Isabelle" /* name is ignored anyway */, List("-r", "-q", config.session))
    session
  }

  def sendCommand(session: Session, name: String, args: List[String]) =
    session.protocol_command(name, args: _*)

  def sendOptions(session: Session) =
    session.protocol_command("Prover.options", isabelle.YXML.string_of_body(options.encode))

  def dispose(session: Session) = session.stop()

}
