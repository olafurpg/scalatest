/*
 * Copyright 2001-2012 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest

import org.scalactic._
import org.scalatest.words.{TypeCheckWord, CompileWord}
import org.scalatest.exceptions._

import scala.quoted._
import scala.tasty._

object CompileMacro {

  // parse and type check a code snippet, generate code to throw TestFailedException when type check passes or parse error
  def assertTypeErrorImpl(code: String, pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] = {
    import refl._
    import quoted.Toolbox.Default._

    if (!typing.typeChecks(code)) '{ Succeeded }
    else '{
      val messageExpr = Resources.expectedCompileErrorButGotNone(${ code.toExpr })
      throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
    }
  }

  def expectTypeErrorImpl(code: String, prettifier: Expr[Prettifier], pos: Expr[source.Position])(implicit refl: Reflection): Expr[Fact] = {
    import refl._

    if (typing.typeChecks(code))
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })
          Fact.No(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
    else
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })

          Fact.Yes(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
  }

  // parse and type check a code snippet, generate code to throw TestFailedException when both parse and type check succeeded
  def assertDoesNotCompileImpl(code: String, pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] = {
    import refl._

    if (!typing.typeChecks(code)) '{ Succeeded }
    else '{
      val messageExpr = Resources.expectedCompileErrorButGotNone(${ code.toExpr })
      throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
    }
  }

  // parse and type check a code snippet, generate code to return Fact (Yes or No).
  def expectDoesNotCompileImpl(code: String, prettifier: Expr[Prettifier], pos: Expr[source.Position])(implicit refl: Reflection): Expr[Fact] = {
    import refl._

    if (typing.typeChecks(code))
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })
          Fact.No(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
    else
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })

          Fact.Yes(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
  }

  // parse and type check a code snippet, generate code to throw TestFailedException when either parse or type check fails.
  def assertCompilesImpl(code: String, pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] = {
    import refl._

    if (typing.typeChecks(code)) '{ Succeeded }
    else '{
      val messageExpr = Resources.expectedCompileErrorButGotNone(${ code.toExpr })
      throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
    }
  }

  def expectCompilesImpl(code: String, prettifier: Expr[Prettifier], pos: Expr[source.Position])(implicit refl: Reflection): Expr[Fact] = {
    import refl._

    if (typing.typeChecks(code))
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })
          Fact.Yes(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
    else
      '{
          val messageExpr = Resources.expectedTypeErrorButGotNone(${ code.toExpr })

          Fact.No(
            messageExpr,
            messageExpr,
            messageExpr,
            messageExpr,
            Vector.empty,
            Vector.empty,
            Vector.empty,
            Vector.empty
          )($prettifier)
       }
  }

  // check that a code snippet does not compile
  def assertNotCompileImpl[T](self: Expr[T], compileWord: Expr[CompileWord], pos: Expr[source.Position])(shouldOrMust: String)(implicit refl: Reflection): Expr[Assertion] = {
    import refl._
    import Term._
    import Constant._

    // parse and type check a code snippet, generate code to throw TestFailedException if both parse and type check succeeded
    def checkNotCompile(code: String): Expr[Assertion] =
      if (!typing.typeChecks(code)) '{ Succeeded }
      else '{
        val messageExpr = Resources.expectedCompileErrorButGotNone(${ code.toExpr })
        throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
      }

    self.unseal.underlyingArgument match {
      
      case Apply(
             Apply(
               Select(_, shouldOrMustTerconvertToStringShouldOrMustWrapperTermName),
               List(
                 Literal(code)
               )
             ),
             _
           ) if shouldOrMustTerconvertToStringShouldOrMustWrapperTermName ==  "convertToString" + shouldOrMust.capitalize + "Wrapper" =>
        // LHS is a normal string literal, call checkCompile with the extracted code string to generate code
        checkNotCompile(code.toString)

      case Apply(
             Apply(
               Ident(shouldOrMustTerconvertToStringShouldOrMustWrapperTermName),
               List(
                 Literal(String(code))
               )
             ),
             _
           ) if shouldOrMustTerconvertToStringShouldOrMustWrapperTermName ==  "convertToString" + shouldOrMust.capitalize + "Wrapper" =>
        checkNotCompile(code.toString)     

      case other =>
        throw QuoteError("The '" + shouldOrMust + " compile' syntax only works with String literals.")
    }
  }

  // used by shouldNot compile syntax, delegate to assertNotCompileImpl to generate code
  def shouldNotCompileImpl(self: Expr[Matchers#AnyShouldWrapper[_]], compileWord: Expr[CompileWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertNotCompileImpl(self, compileWord, pos)("should")

  // used by mustNot compile syntax, delegate to assertNotCompileImpl to generate code
  def mustNotCompileImpl(self: Expr[MustMatchers#AnyMustWrapper[_]], compileWord: Expr[CompileWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertNotCompileImpl(self, compileWord, pos)("must")  

  // check that a code snippet does not compile
  def assertNotTypeCheckImpl(self: Expr[Matchers#AnyShouldWrapper[_]], typeCheckWord: Expr[TypeCheckWord], pos: Expr[source.Position])(shouldOrMust: String)(implicit refl: Reflection): Expr[Assertion] = {
    import refl._
    import Term._
    import Constant._

    // parse and type check a code snippet, generate code to throw TestFailedException if both parse and type check succeeded
    def checkNotTypeCheck(code: String): Expr[Assertion] =
      if (!typing.typeChecks(code)) '{ Succeeded }
      else '{
        val messageExpr = Resources.expectedCompileErrorButGotNone(${ code.toExpr })
        throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
      }

    val methodName = shouldOrMust + "Not"

    self.unseal.underlyingArgument match {
      case Apply(
             Apply(
               Select(
                 Apply(
                   Apply(
                     _,
                     List(
                      Literal(String(code))
                     )
                   ),
                   _
                 ),
                 methodNameTermName
               ),
               _
             ),
             _
           ) if methodNameTermName == methodName =>
        // LHS is a normal string literal, call checkNotTypeCheck with the extracted code string to generate code
        checkNotTypeCheck(code.toString)
      case _ =>
        throw QuoteError("The '" + shouldOrMust + "Not typeCheck' syntax only works with String literals.")
    }
  }

  // used by shouldNot typeCheck syntax, delegate to assertNotTypeCheckImpl to generate code
  def shouldNotTypeCheckImpl(self: Expr[Matchers#AnyShouldWrapper[_]], typeCheckWord: Expr[TypeCheckWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertNotTypeCheckImpl(self, typeCheckWord, pos)("should")

  // used by mustNot typeCheck syntax, delegate to assertNotTypeCheckImpl to generate code
  def mustNotTypeCheckImpl(self: Expr[Matchers#AnyShouldWrapper[_]], typeCheckWord: Expr[TypeCheckWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertNotTypeCheckImpl(self, typeCheckWord, pos)("must")

  // check that a code snippet compiles
  def assertCompileImpl[T](self: Expr[T], compileWord: Expr[CompileWord], pos: Expr[source.Position])(shouldOrMust: String)(implicit refl: Reflection): Expr[Assertion] = {
    import refl._
    import Term._
    import Constant._

    // parse and type check a code snippet, generate code to throw TestFailedException if both parse and type check succeeded
    def checkCompile(code: String): Expr[Assertion] =
      if (typing.typeChecks(code)) '{ Succeeded }
      else '{
        val messageExpr = Resources.expectedNoErrorButGotTypeError("", ${ code.toExpr })
        throw new TestFailedException((_: StackDepthException) => Some(messageExpr), None, $pos)
      }

    self.unseal.underlyingArgument match {

      case Apply(
             Apply(
               Select(_, shouldOrMustTerconvertToStringShouldOrMustWrapperTermName),
               List(
                 Literal(String(code))
               )
             ),
             _
           ) if shouldOrMustTerconvertToStringShouldOrMustWrapperTermName ==  "convertToString" + shouldOrMust.capitalize + "Wrapper" =>
        // LHS is a normal string literal, call checkCompile with the extracted code string to generate code
        checkCompile(code.toString)

      case Apply(
             Apply(
               Ident(shouldOrMustTerconvertToStringShouldOrMustWrapperTermName),
               List(
                 Literal(String(code))
               )
             ),
             _
           ) if shouldOrMustTerconvertToStringShouldOrMustWrapperTermName ==  "convertToString" + shouldOrMust.capitalize + "Wrapper" =>
        // LHS is a normal string literal, call checkCompile with the extracted code string to generate code
        checkCompile(code.toString)    

      case other =>
        println("###other: " + other)
        throw QuoteError("The '" + shouldOrMust + " compile' syntax only works with String literals.")
    }
  }

  // used by should compile syntax, delegate to assertCompileImpl to generate code
  def shouldCompileImpl(self: Expr[Matchers#AnyShouldWrapper[_]], compileWord: Expr[CompileWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertCompileImpl(self, compileWord, pos)("should")

    // used by should compile syntax, delegate to assertCompileImpl to generate code
  def mustCompileImpl(self: Expr[MustMatchers#AnyMustWrapper[_]], compileWord: Expr[CompileWord])(pos: Expr[source.Position])(implicit refl: Reflection): Expr[Assertion] =
    assertCompileImpl(self, compileWord, pos)("must")
}
