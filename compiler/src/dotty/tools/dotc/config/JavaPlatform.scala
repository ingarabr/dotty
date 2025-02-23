package dotty.tools
package dotc
package config

import io._
import classpath.AggregateClassPath
import core._
import Symbols._, Types._, Contexts._, Denotations._, SymDenotations._, StdNames._, Names._
import Flags._, Scopes._, Decorators._, NameOps._, util.Positions._
import transform.ExplicitOuter, transform.SymUtils._

class JavaPlatform extends Platform {

  private[this] var currentClassPath: Option[ClassPath] = None

  def classPath(implicit ctx: Context): ClassPath = {
    if (currentClassPath.isEmpty)
      currentClassPath = Some(new PathResolver().result)
    val cp = currentClassPath.get
    cp
  }

  // The given symbol is a method with the right name and signature to be a runnable java program.
  def isMainMethod(sym: SymDenotation)(implicit ctx: Context) =
    (sym.name == nme.main) && (sym.info match {
      case MethodTpe(_, defn.ArrayOf(el) :: Nil, restpe) => el =:= defn.StringType && (restpe isRef defn.UnitClass)
      case _ => false
    })

  /** Update classpath with a substituted subentry */
  def updateClassPath(subst: Map[ClassPath, ClassPath]): Unit = currentClassPath.get match {
    case AggregateClassPath(entries) =>
      currentClassPath = Some(AggregateClassPath(entries map (e => subst.getOrElse(e, e))))
    case cp: ClassPath =>
      currentClassPath = Some(subst.getOrElse(cp, cp))
  }

  def rootLoader(root: TermSymbol)(implicit ctx: Context): SymbolLoader = new ctx.base.loaders.PackageLoader(root, classPath)

  /** Is the SAMType `cls` also a SAM under the rules of the JVM? */
  def isSam(cls: ClassSymbol)(implicit ctx: Context): Boolean =
    cls.is(NoInitsTrait) &&
    cls.superClass == defn.ObjectClass &&
    cls.directlyInheritedTraits.forall(_.is(NoInits)) &&
    !ExplicitOuter.needsOuterIfReferenced(cls) &&
    cls.typeRef.fields.isEmpty // Superaccessors already show up as abstract methods here, so no test necessary

  /** We could get away with excluding BoxedBooleanClass for the
   *  purpose of equality testing since it need not compare equal
   *  to anything but other booleans, but it should be present in
   *  case this is put to other uses.
   */
  def isMaybeBoxed(sym: ClassSymbol)(implicit ctx: Context) = {
    val d = defn
    import d._
    (sym == ObjectClass) ||
    (sym == JavaSerializableClass) ||
    (sym == ComparableClass) ||
    (sym derivesFrom BoxedNumberClass) ||
    (sym derivesFrom BoxedCharClass) ||
    (sym derivesFrom BoxedBooleanClass)
  }

  def newClassLoader(bin: AbstractFile)(implicit ctx: Context): SymbolLoader =
    new ClassfileLoader(bin)
}
