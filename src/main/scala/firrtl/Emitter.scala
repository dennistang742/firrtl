// See LICENSE for license details.

package firrtl

import firrtl.annotations.NoTargetAnnotation
import firrtl.stage.{FirrtlOptions, RunFirrtlTransformAnnotation}
import firrtl.passes.PassException
import firrtl.options.{CustomFileEmission, HasShellOptions, PhaseException, ShellOption}
import firrtl.options.Viewer.view
import firrtl.backends.experimental.smt.{Btor2Emitter, SMTLibEmitter}
import firrtl.backends.firrtl.{ChirrtlEmitter, HighFirrtlEmitter, LowFirrtlEmitter, MiddleFirrtlEmitter}
import firrtl.backends.verilog.{SystemVerilogEmitter, VerilogEmitter}

case class EmitterException(message: String) extends PassException(message)

// ***** Annotations for telling the Emitters what to emit *****
trait EmitAnnotation extends NoTargetAnnotation {
  val emitter: Class[_ <: Emitter]
}

case class EmitCircuitAnnotation(emitter: Class[_ <: Emitter]) extends EmitAnnotation

case class EmitAllModulesAnnotation(emitter: Class[_ <: Emitter]) extends EmitAnnotation

object EmitCircuitAnnotation extends HasShellOptions {
  val options = Seq(
    new ShellOption[String](
      longOption = "emit-circuit",
      toAnnotationSeq = (a: String) =>
        a match {
          case "chirrtl" =>
            Seq(RunFirrtlTransformAnnotation(new ChirrtlEmitter), EmitCircuitAnnotation(classOf[ChirrtlEmitter]))
          case "high" =>
            Seq(RunFirrtlTransformAnnotation(new HighFirrtlEmitter), EmitCircuitAnnotation(classOf[HighFirrtlEmitter]))
          case "middle" =>
            Seq(
              RunFirrtlTransformAnnotation(new MiddleFirrtlEmitter),
              EmitCircuitAnnotation(classOf[MiddleFirrtlEmitter])
            )
          case "low" =>
            Seq(RunFirrtlTransformAnnotation(new LowFirrtlEmitter), EmitCircuitAnnotation(classOf[LowFirrtlEmitter]))
          case "verilog" | "mverilog" =>
            Seq(RunFirrtlTransformAnnotation(new VerilogEmitter), EmitCircuitAnnotation(classOf[VerilogEmitter]))
          case "sverilog" =>
            Seq(
              RunFirrtlTransformAnnotation(new SystemVerilogEmitter),
              EmitCircuitAnnotation(classOf[SystemVerilogEmitter])
            )
          case "experimental-btor2" =>
            Seq(RunFirrtlTransformAnnotation(new Btor2Emitter), EmitCircuitAnnotation(classOf[Btor2Emitter]))
          case "experimental-smt2" =>
            Seq(RunFirrtlTransformAnnotation(new SMTLibEmitter), EmitCircuitAnnotation(classOf[SMTLibEmitter]))
          case _ => throw new PhaseException(s"Unknown emitter '$a'! (Did you misspell it?)")
        },
      helpText = "Run the specified circuit emitter (all modules in one file)",
      shortOption = Some("E"),
      // the experimental options are intentionally excluded from the help message
      helpValueName = Some("<chirrtl|high|middle|low|verilog|mverilog|sverilog>")
    )
  )
}

object EmitAllModulesAnnotation extends HasShellOptions {

  val options = Seq(
    new ShellOption[String](
      longOption = "emit-modules",
      toAnnotationSeq = (a: String) =>
        a match {
          case "chirrtl" =>
            Seq(RunFirrtlTransformAnnotation(new ChirrtlEmitter), EmitAllModulesAnnotation(classOf[ChirrtlEmitter]))
          case "high" =>
            Seq(
              RunFirrtlTransformAnnotation(new HighFirrtlEmitter),
              EmitAllModulesAnnotation(classOf[HighFirrtlEmitter])
            )
          case "middle" =>
            Seq(
              RunFirrtlTransformAnnotation(new MiddleFirrtlEmitter),
              EmitAllModulesAnnotation(classOf[MiddleFirrtlEmitter])
            )
          case "low" =>
            Seq(RunFirrtlTransformAnnotation(new LowFirrtlEmitter), EmitAllModulesAnnotation(classOf[LowFirrtlEmitter]))
          case "verilog" | "mverilog" =>
            Seq(RunFirrtlTransformAnnotation(new VerilogEmitter), EmitAllModulesAnnotation(classOf[VerilogEmitter]))
          case "sverilog" =>
            Seq(
              RunFirrtlTransformAnnotation(new SystemVerilogEmitter),
              EmitAllModulesAnnotation(classOf[SystemVerilogEmitter])
            )
          case _ => throw new PhaseException(s"Unknown emitter '$a'! (Did you misspell it?)")
        },
      helpText = "Run the specified module emitter (one file per module)",
      shortOption = Some("e"),
      helpValueName = Some("<chirrtl|high|middle|low|verilog|mverilog|sverilog>")
    )
  )

}

// ***** Annotations for results of emission *****
abstract class EmittedComponent {
  def name: String

  def value: String

  def outputSuffix: String
}

abstract class EmittedCircuit extends EmittedComponent

abstract class EmittedModule extends EmittedComponent

/** Traits for Annotations containing emitted components */
trait EmittedAnnotation[T <: EmittedComponent] extends NoTargetAnnotation with CustomFileEmission {
  val value: T

  override protected def baseFileName(annotations: AnnotationSeq): String = {
    view[FirrtlOptions](annotations).outputFileName.getOrElse(value.name)
  }

  override protected val suffix: Option[String] = Some(value.outputSuffix)
}

trait EmittedCircuitAnnotation[T <: EmittedCircuit] extends EmittedAnnotation[T] {
  override def getBytes = value.value.getBytes
}

trait EmittedModuleAnnotation[T <: EmittedModule] extends EmittedAnnotation[T] {
  override def getBytes = value.value.getBytes
}
