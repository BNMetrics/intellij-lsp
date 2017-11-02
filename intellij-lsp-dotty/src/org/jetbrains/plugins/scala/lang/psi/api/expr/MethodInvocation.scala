package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.{PsiElement, PsiMethod}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil._
import org.jetbrains.plugins.scala.lang.psi.api.InferUtil
import org.jetbrains.plugins.scala.lang.psi.api.InferUtil.SafeCheckException
import org.jetbrains.plugins.scala.lang.psi.api.expr.MethodInvocation.UpdateApplyData
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFun, ScFunction}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.usages.ImportUsed
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTrait
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.Compatibility.Expression
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api._
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.{Parameter, ScMethodType, ScTypePolymorphicType}
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.psi.{ScalaPsiElement, ScalaPsiUtil}
import org.jetbrains.plugins.scala.lang.resolve.processor.DynamicResolveProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ResolveUtils, ScalaResolveResult}
import org.jetbrains.plugins.scala.project._

/**
  * Pavel Fatin, Alexander Podkhalyuzin.
  */

// A common trait for Infix, Postfix and Prefix expressions
// and Method calls to handle them uniformly
trait MethodInvocation extends ScExpression with ScalaPsiElement {


  @volatile private var problemsVar: Seq[ApplicabilityProblem] = Seq.empty
  @volatile private var matchedParamsVar: Seq[(Parameter, ScExpression)] = Seq.empty
  @volatile private var importsUsedVar: collection.Set[ImportUsed] = collection.Set.empty
  @volatile private var implicitFunctionVar: Option[ScalaResolveResult] = None
  @volatile private var applyOrUpdateElemVar: Option[ScalaResolveResult] = None

  /**
    * For Infix, Postfix and Prefix expressions
    * it's refernce expression for operation
    *
    * @return method reference or invoked expression for calls
    */
  def getInvokedExpr: ScExpression

  /**
    * @return call arguments
    */
  def argumentExpressions: Seq[ScExpression]

  /**
    * Seq of application problems like type mismatch.
    *
    * @return seq of application problems
    */
  def applicationProblems: Seq[ApplicabilityProblem] = {
    `type`()
    problemsVar
  }

  /**
    * @return map of expressions and parameters
    */
  def matchedParameters: Seq[(ScExpression, Parameter)] = {
    matchedParametersInner.map(a => a.swap).filter(a => a._1 != null) //todo: catch when expression is null
  }

  private def matchedParametersInner: Seq[(Parameter, ScExpression)] = {
    `type`()
    matchedParamsVar
  }

  /**
    * @return map of expressions and parameters
    */
  def matchedParametersMap: Map[Parameter, Seq[ScExpression]] = {
    matchedParametersInner.groupBy(_._1).map(t => t.copy(_2 = t._2.map(_._2)))
  }

  /**
    * In case if invoked expression converted implicitly to invoke apply or update method
    *
    * @return imports used for implicit conversion
    */
  def getImportsUsed: collection.Set[ImportUsed] = {
    `type`()
    importsUsedVar
  }

  /**
    * In case if invoked expression converted implicitly to invoke apply or update method
    *
    * @return actual conversion element
    */
  def getImplicitFunction: Option[ScalaResolveResult] = {
    `type`()
    implicitFunctionVar
  }

  /**
    * true if this call is syntactic sugar for apply or update method.
    */
  def isApplyOrUpdateCall: Boolean = applyOrUpdateElement.isDefined

  /**
    * It's arguments for method and infix call.
    * For prefix and postfix call it's just operation.
    *
    * @return Element, which reflects arguments
    */
  def argsElement: PsiElement

  /**
    * @return Is this method invocation in 'update' syntax sugar position.
    */
  def isUpdateCall: Boolean = false

  //used in Play
  def setApplicabilityProblemsVar(seq: Seq[ApplicabilityProblem]): Unit = {
    problemsVar = seq
  }

  protected override def innerType: TypeResult = {
    try {
      tryToGetInnerType(useExpectedType = true)
    } catch {
      case _: SafeCheckException =>
        tryToGetInnerType(useExpectedType = false)
    }
  }

  private def tryToGetInnerType(useExpectedType: Boolean): TypeResult = {
    var problemsLocal: Seq[ApplicabilityProblem] = Seq.empty
    var matchedParamsLocal: Seq[(Parameter, ScExpression)] = Seq.empty
    var importsUsedLocal: collection.Set[ImportUsed] = collection.Set.empty
    var implicitFunctionLocal: Option[ScalaResolveResult] = None
    var applyOrUpdateElemLocal: Option[ScalaResolveResult] = None

    def updateCacheFields(): Unit = {
      problemsVar = problemsLocal
      matchedParamsVar = matchedParamsLocal
      importsUsedVar = importsUsedLocal
      implicitFunctionVar = implicitFunctionLocal
      applyOrUpdateElemVar = applyOrUpdateElemLocal
    }

    var nonValueType: TypeResult = getEffectiveInvokedExpr.getNonValueType()
    this match {
      case _: ScPrefixExpr => return nonValueType //no arg exprs, just reference expression type
      case _: ScPostfixExpr => return nonValueType //no arg exprs, just reference expression type
      case _ =>
    }

    val withExpectedType = useExpectedType && this.expectedType().isDefined //optimization to avoid except

    if (useExpectedType) nonValueType = this.updateAccordingToExpectedType(nonValueType, canThrowSCE = true)

    def checkConformance(retType: ScType, psiExprs: Seq[Expression], parameters: Seq[Parameter]) = {
      tuplizyCase(psiExprs) { t =>
        val result = Compatibility.checkConformanceExt(checkNames = true, parameters = parameters, exprs = t,
          checkWithImplicits = true, isShapesResolve = false)
        (retType, result.problems, result.matchedArgs, result.matchedTypes)
      }
    }

    def checkConformanceWithInference(retType: ScType, psiExprs: Seq[Expression],
                                      typeParams: Seq[TypeParameter], parameters: Seq[Parameter]) = {
      tuplizyCase(psiExprs) { t =>
        InferUtil.localTypeInferenceWithApplicabilityExt(retType, parameters, t, typeParams, canThrowSCE = withExpectedType)
      }
    }

    def tuplizyCase(exprs: Seq[Expression])
                   (fun: (Seq[Expression]) => (ScType, scala.Seq[ApplicabilityProblem],
                     Seq[(Parameter, ScExpression)], Seq[(Parameter, ScType)])): ScType = {
      val c = fun(exprs)

      def tail: ScType = {
        problemsLocal = c._2
        matchedParamsLocal = c._3
        val dependentSubst = ScSubstitutor(() => {
          c._4.toMap
        })
        dependentSubst.subst(c._1)
      }

      if (c._2.nonEmpty) {
        ScalaPsiUtil.tuplizy(exprs, this.resolveScope, getManager, ScalaPsiUtil.firstLeaf(this)).map { e =>
          val cd = fun(e)
          if (cd._2.nonEmpty) tail
          else {
            problemsLocal = cd._2
            matchedParamsLocal = cd._3
            val dependentSubst = ScSubstitutor(() => {

              cd._4.toMap

            })
            dependentSubst.subst(cd._1)
          }
        }.getOrElse(tail)
      } else tail
    }

    def functionParams(params: Seq[ScType]): Seq[Parameter] = {
      val functionName = s"scala.Function${params.length}"
      val functionClass = elementScope.getCachedClass(functionName)
        .collect {
          case t: ScTrait => t
        }
      val applyFunction = functionClass.flatMap(_.functions.find(_.name == "apply"))
      params.mapWithIndex {
        case (tp, i) =>
          new Parameter("v" + (i + 1), None, tp, tp, false, false, false, i, applyFunction.map(_.parameters.apply(i)))
      }
    }

    def checkApplication(tpe: ScType, args: Seq[Expression]): Option[ScType] = tpe match {
      case ScMethodType(retType, params, _) =>
        Some(checkConformance(retType, args, params))
      case ScTypePolymorphicType(ScMethodType(retType, params, _), typeParams) =>
        Some(checkConformanceWithInference(retType, args, typeParams, params))
      case ScTypePolymorphicType(FunctionType(retType, params), typeParams) =>
        Some(checkConformanceWithInference(retType, args, typeParams, functionParams(params)))
      case any if ScalaPsiUtil.isSAMEnabled(this) =>
        ScalaPsiUtil.toSAMType(any, this) match {
          case Some(FunctionType(retType: ScType, params: Seq[ScType])) =>
            Some(checkConformance(retType, args, functionParams(params)))
          case _ => None
        }
      case _ => None
    }

    val invokedType: ScType = nonValueType.getOrElse(return nonValueType)

    def args(includeUpdateCall: Boolean = false, isNamedDynamic: Boolean = false): Seq[Expression] = {
      def default: Seq[ScExpression] =
        if (includeUpdateCall) argumentExpressionsIncludeUpdateCall
        else argumentExpressions

      if (isNamedDynamic) {
        default.map {
          expr =>
            val actualExpr = expr match {
              case a: ScAssignStmt =>
                a.getLExpression match {
                  case ref: ScReferenceExpression if ref.qualifier.isEmpty => a.getRExpression.getOrElse(expr)
                  case _ => expr
                }
              case _ => expr
            }
            new Expression(actualExpr) {
              override def getTypeAfterImplicitConversion(checkImplicits: Boolean, isShape: Boolean,
                                                          _expectedOption: Option[ScType]): (TypeResult, collection.Set[ImportUsed]) = {
                val expectedOption = _expectedOption.map {
                  case TupleType(comps) if comps.length == 2 => comps(1)
                  case t => t
                }
                val (res, imports) = super.getTypeAfterImplicitConversion(checkImplicits, isShape, expectedOption)
                implicit val project = getProject
                implicit val scope = MethodInvocation.this.resolveScope

                val str = ScalaPsiManager.instance(project).getCachedClass(scope, "java.lang.String")
                val stringType = str.map(ScalaType.designator(_)).getOrElse(Any)
                (res.map(tp => TupleType(Seq(stringType, tp))), imports)
              }
            }
        }
      } else default
    }

    def isApplyDynamicNamed: Boolean = {
      getEffectiveInvokedExpr match {
        case ref: ScReferenceExpression =>
          ref.bind().exists(result => result.isDynamic && result.name == DynamicResolveProcessor.APPLY_DYNAMIC_NAMED)
        case _ => false
      }
    }

    val res: ScType = checkApplication(invokedType, args(isNamedDynamic = isApplyDynamicNamed)) match {
      case Some(s) => s
      case None =>
        var UpdateApplyData(processedType, importsUsed, implicitFunction, applyOrUpdateResult) =
          MethodInvocation.processTypeForUpdateOrApply(invokedType, this, isShape = false).getOrElse {
            UpdateApplyData(Nothing, Set.empty[ImportUsed], None, this.applyOrUpdateElement)
          }
        if (useExpectedType) {
          this.updateAccordingToExpectedType(Right(processedType)).foreach(x => processedType = x)
        }
        applyOrUpdateElemLocal = applyOrUpdateResult
        importsUsedLocal = importsUsed
        implicitFunctionLocal = implicitFunction
        val isNamedDynamic: Boolean =
          applyOrUpdateResult.exists(result => result.isDynamic &&
            result.name == DynamicResolveProcessor.APPLY_DYNAMIC_NAMED)
        checkApplication(processedType, args(includeUpdateCall = true, isNamedDynamic)).getOrElse {
          applyOrUpdateElemLocal = None
          problemsLocal = Seq(new DoesNotTakeParameters)
          matchedParamsLocal = Seq()
          processedType
        }
    }

    val (newType, params) = this.updatedWithImplicitParameters(res, useExpectedType)
    setImplicitParameters(params)

    updateCacheFields()

    Right(newType)
  }

  /**
    * Unwraps parenthesised expression for method calls
    *
    * @return unwrapped invoked expression
    */
  def getEffectiveInvokedExpr: ScExpression = getInvokedExpr

  /**
    * Important method for method calls like: foo(expr) = assign.
    * Usually this is same as argumentExpressions
    *
    * @return arguments with additional argument if call in update position
    */
  def argumentExpressionsIncludeUpdateCall: Seq[ScExpression] = argumentExpressions

  def applyOrUpdateElement: Option[ScalaResolveResult] = {
    `type`()
    applyOrUpdateElemVar
  }

}

object MethodInvocation {

  def unapply(methodInvocation: MethodInvocation): Option[(ScExpression, Seq[ScExpression])] =
    for {
      invocation <- Option(methodInvocation)
      expression = invocation.getInvokedExpr
      if expression != null
    } yield (expression, invocation.argumentExpressions)

  implicit class MethodInvocationExt(val invocation: MethodInvocation) extends AnyVal {
    private implicit def elementScope = invocation.elementScope

    /**
      * This method useful in case if you want to update some polymorphic type
      * according to method call expected type
      */
    def updateAccordingToExpectedType(nonValueType: TypeResult,
                                      canThrowSCE: Boolean = false): TypeResult = {
      InferUtil.updateAccordingToExpectedType(nonValueType, fromImplicitParameters = false, filterTypeParams = false,
        expectedType = invocation.expectedType(), expr = invocation, canThrowSCE)
    }

  }

  private def processTypeForUpdateOrApply(tp: ScType, call: MethodInvocation, isShape: Boolean): Option[UpdateApplyData] = {
    implicit val ctx: ProjectContext = call

    def checkCandidates(withDynamic: Boolean = false): Option[UpdateApplyData] = {
      val candidates: Array[ScalaResolveResult] = processTypeForUpdateOrApplyCandidates(call, tp, isShape, isDynamic = withDynamic)
      PartialFunction.condOpt(candidates) {
        case Array(r@ScalaResolveResult(fun: PsiMethod, s: ScSubstitutor)) =>
          def update(tp: ScType): ScType = {
            if (r.isDynamic) DynamicResolveProcessor.getDynamicReturn(tp)
            else tp
          }

          val res = fun match {
            case fun: ScFun => UpdateApplyData(update(s.subst(fun.polymorphicType)), r.importsUsed, r.implicitConversion, Some(r))
            case fun: ScFunction => UpdateApplyData(update(s.subst(fun.polymorphicType())), r.importsUsed, r.implicitConversion, Some(r))
            case meth: PsiMethod => UpdateApplyData(update(ResolveUtils.javaPolymorphicType(meth, s, call.resolveScope)),
              r.importsUsed, r.implicitConversion, Some(r))
          }
          call.getInvokedExpr.getNonValueType() match {
            case Right(ScTypePolymorphicType(_, typeParams)) =>
              val fixedType = res.processedType match {
                case ScTypePolymorphicType(internal, typeParams2) =>
                  ScalaPsiUtil.removeBadBounds(ScTypePolymorphicType(internal, typeParams ++ typeParams2))
                case _ => ScTypePolymorphicType(res.processedType, typeParams)
              }
              res.copy(processedType = fixedType)
            case _ => res
          }
      }
    }

    checkCandidates().orElse(checkCandidates(withDynamic = true))
  }

  private case class UpdateApplyData(processedType: ScType,
                                     importsUsed: collection.Set[ImportUsed],
                                     implicitFunction: Option[ScalaResolveResult],
                                     applyOrUpdateResult: Option[ScalaResolveResult])


}
