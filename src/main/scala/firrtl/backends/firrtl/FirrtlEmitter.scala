package firrtl.backends.firrtl

import java.io.{File, Writer}

import firrtl._
import firrtl.Utils._
import firrtl.ir._
import firrtl.stage.FirrtlFileAnnotation
import firrtl.traversals.Foreachers._

import scala.collection.mutable
// Datastructures

case class EmittedFirrtlModuleAnnotation(value: EmittedFirrtlModule)
    extends EmittedModuleAnnotation[EmittedFirrtlModule]

case class EmittedFirrtlCircuitAnnotation(value: EmittedFirrtlCircuit)
    extends EmittedCircuitAnnotation[EmittedFirrtlCircuit] {

  override def replacements(file: File): AnnotationSeq = Seq(FirrtlFileAnnotation(file.toString))
}
final case class EmittedFirrtlCircuit(name: String, value: String, outputSuffix: String) extends EmittedCircuit
final case class EmittedFirrtlModule(name: String, value: String, outputSuffix: String) extends EmittedModule

// ***** Start actual Emitters *****
class ChirrtlEmitter extends FirrtlEmitter(ChirrtlForm)
class HighFirrtlEmitter extends FirrtlEmitter(HighForm)
class MiddleFirrtlEmitter extends FirrtlEmitter(MidForm)
class LowFirrtlEmitter extends FirrtlEmitter(LowForm)

sealed abstract class FirrtlEmitter(form: CircuitForm) extends Transform with Emitter {
  def inputForm = form
  def outputForm = form

  val outputSuffix: String = form.outputSuffix

  private def emitAllModules(circuit: Circuit): Seq[EmittedFirrtlModule] = {
    // For a given module, returns a Seq of all modules instantited inside of it
    def collectInstantiatedModules(mod: Module, map: Map[String, DefModule]): Seq[DefModule] = {
      // Use list instead of set to maintain order
      val modules = mutable.ArrayBuffer.empty[DefModule]
      def onStmt(stmt: Statement): Unit = stmt match {
        case DefInstance(_, _, name, _)  => modules += map(name)
        case WDefInstance(_, _, name, _) => modules += map(name)
        case _: WDefInstanceConnector => throwInternalError(s"unrecognized statement: $stmt")
        case other => other.foreach(onStmt)
      }
      onStmt(mod.body)
      modules.distinct.toSeq
    }
    val modMap = circuit.modules.map(m => m.name -> m).toMap
    // Turn each module into it's own circuit with it as the top and all instantied modules as ExtModules
    circuit.modules.collect {
      case m: Module =>
        val instModules = collectInstantiatedModules(m, modMap)
        val extModules = instModules.map {
          case Module(info, name, ports, _) => ExtModule(info, name, ports, name, Seq.empty)
          case ext: ExtModule => ext
        }
        val newCircuit = Circuit(m.info, extModules :+ m, m.name)
        EmittedFirrtlModule(m.name, newCircuit.serialize, outputSuffix)
    }
  }

  override def execute(state: CircuitState): CircuitState = {
    val newAnnos = state.annotations.flatMap {
      case EmitCircuitAnnotation(a) if this.getClass == a =>
        Seq(
          EmittedFirrtlCircuitAnnotation(
            EmittedFirrtlCircuit(state.circuit.main, state.circuit.serialize, outputSuffix)
          )
        )
      case EmitAllModulesAnnotation(a) if this.getClass == a =>
        emitAllModules(state.circuit).map(EmittedFirrtlModuleAnnotation(_))
      case _ => Seq()
    }
    state.copy(annotations = newAnnos ++ state.annotations)
  }

  // Old style, deprecated
  def emit(state: CircuitState, writer: Writer): Unit = writer.write(state.circuit.serialize)
}
