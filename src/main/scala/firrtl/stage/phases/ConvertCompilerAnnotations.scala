// See LICENSE for license details.

package firrtl.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Dependency, OptionsException, Phase}
import firrtl.stage.{CompilerAnnotation, RunFirrtlTransformAnnotation}

@deprecated(
  "This only exists to convert deprecated CompilerAnnotations to RunFirrtlTransformAnnotations.",
  "FIRRTL 1.5.0"
)
class ConvertCompilerAnnotations extends Phase {

  override def prerequisites = Seq.empty
  override def optionalPrerequisites = Seq.empty
  override def optionalPrerequisiteOf = Seq(Dependency[AddDefaults], Dependency[Checks])
  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq = {
    annotations.collect {
      case a: CompilerAnnotation => a
    } match {
      case a if a.size > 1 =>
        val (msg, suggest) = (s"""found '${a.mkString(", ")}'""", "use multiple of")
        throw new OptionsException(
          s"Zero or more deprecated CompilerAnnotation may be specified, but $msg.".stripMargin
        )
      case _ =>
    }
    annotations.map {
      case CompilerAnnotation(a) =>
        val suggestion = s"RunFirrtlTransformAnnotation(new ${a.emitter.getClass.getName})"
        logger.warn(s"CompilerAnnotation is deprecated since FIRRTL 1.4.0. Please use '$suggestion' instead.")
        RunFirrtlTransformAnnotation(a.emitter)
      case a => a
    }
  }

}