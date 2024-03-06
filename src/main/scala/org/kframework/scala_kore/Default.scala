// Copyright (c) Runtime Verification, Inc. All Rights Reserved.
package org.kframework.scala_kore

import org.kframework.scala_kore

object implementation {

  private object ConcreteClasses {

    case class Definition(att: scala_kore.Attributes, modules: Seq[scala_kore.Module])
        extends scala_kore.Definition

    case class Module(name: String, decls: Seq[Declaration], att: scala_kore.Attributes)
        extends scala_kore.Module

    case class Import(name: String, att: scala_kore.Attributes) extends scala_kore.Import

    case class SortDeclaration(
        params: Seq[scala_kore.SortVariable],
        sort: Sort,
        att: scala_kore.Attributes
    ) extends scala_kore.SortDeclaration

    case class HookSortDeclaration(
        params: Seq[scala_kore.SortVariable],
        sort: Sort,
        att: scala_kore.Attributes
    ) extends scala_kore.HookSortDeclaration

    case class SymbolDeclaration(
        symbol: scala_kore.Symbol,
        argSorts: Seq[Sort],
        returnSort: Sort,
        att: scala_kore.Attributes
    ) extends scala_kore.SymbolDeclaration

    case class HookSymbolDeclaration(
        symbol: scala_kore.Symbol,
        argSorts: Seq[Sort],
        returnSort: Sort,
        att: scala_kore.Attributes
    ) extends scala_kore.HookSymbolDeclaration

    case class AliasDeclaration(
        alias: scala_kore.Alias,
        argSorts: Seq[Sort],
        returnSort: Sort,
        leftPattern: Pattern,
        rightPattern: Pattern,
        att: scala_kore.Attributes
    ) extends scala_kore.AliasDeclaration

    case class AxiomDeclaration(
        params: Seq[scala_kore.SortVariable],
        pattern: Pattern,
        att: scala_kore.Attributes
    ) extends scala_kore.AxiomDeclaration

    case class ClaimDeclaration(
        params: Seq[scala_kore.SortVariable],
        pattern: Pattern,
        att: scala_kore.Attributes
    ) extends scala_kore.ClaimDeclaration

    case class Attributes(patterns: Seq[Pattern]) extends scala_kore.Attributes

    case class Variable(name: String, sort: Sort) extends scala_kore.Variable

    case class SetVariable(name: String, sort: Sort) extends scala_kore.SetVariable

    case class Application(head: scala_kore.SymbolOrAlias, args: Seq[Pattern])
        extends scala_kore.Application

    case class Top(s: Sort) extends scala_kore.Top

    case class Bottom(s: Sort) extends scala_kore.Bottom

    case class And(s: Sort, args: Seq[Pattern]) extends scala_kore.And

    case class Or(s: Sort, args: Seq[Pattern]) extends scala_kore.Or

    case class Not(s: Sort, _1: Pattern) extends scala_kore.Not

    case class Implies(s: Sort, _1: Pattern, _2: Pattern) extends scala_kore.Implies

    case class Iff(s: Sort, _1: Pattern, _2: Pattern) extends scala_kore.Iff

    case class Exists(s: Sort, v: scala_kore.Variable, p: Pattern) extends scala_kore.Exists

    case class Forall(s: Sort, v: scala_kore.Variable, p: Pattern) extends scala_kore.Forall

    // case class Next(s: i.Sort, _1: i.Pattern) extends i.Next

    case class Rewrites(s: Sort, _1: Pattern, _2: Pattern) extends scala_kore.Rewrites

    case class Ceil(s: Sort, rs: Sort, p: Pattern) extends scala_kore.Ceil

    case class Floor(s: Sort, rs: Sort, p: Pattern) extends scala_kore.Floor

    case class Equals(s: Sort, rs: Sort, _1: Pattern, _2: Pattern) extends scala_kore.Equals

    case class Mem(s: Sort, rs: Sort, p: Pattern, q: Pattern) extends scala_kore.Mem

    case class DomainValue(s: Sort, str: String) extends scala_kore.DomainValue

    // case class Subset(s: i.Sort, rs: i.Sort,_1: i.Pattern,_2: i.Pattern) extends i.Subset

    case class StringLiteral(str: String) extends scala_kore.StringLiteral

    case class SortVariable(name: String) extends scala_kore.SortVariable {
      override def toString           = name
      override lazy val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
    }

    case class CompoundSort(ctr: String, params: Seq[Sort]) extends scala_kore.CompoundSort {
      override lazy val toString      = ctr + "{" + params.map(_.toString).mkString(", ") + "}"
      override lazy val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
    }

    case class SymbolOrAlias(ctr: String, params: Seq[Sort]) extends scala_kore.SymbolOrAlias {
      override lazy val toString      = ctr + "{" + params.map(_.toString).mkString(", ") + "}"
      override lazy val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
    }

    case class Symbol(ctr: String, params: Seq[Sort]) extends scala_kore.Symbol

    case class Alias(ctr: String, params: Seq[Sort]) extends scala_kore.Alias
  }

  object DefaultBuilders extends Builders {

    import implementation.{ ConcreteClasses => d }

    def Definition(att: Attributes, modules: Seq[Module]): Definition =
      d.Definition(att, modules)

    def Module(name: String, decls: Seq[Declaration], att: Attributes): Module =
      d.Module(name, decls, att)

    def Import(name: String, att: Attributes): Declaration = d.Import(name, att)

    def SortDeclaration(
        params: Seq[SortVariable],
        sort: Sort,
        att: Attributes
    ): Declaration = d.SortDeclaration(params, sort, att)

    def HookSortDeclaration(
        params: Seq[SortVariable],
        sort: Sort,
        att: Attributes
    ): Declaration = d.HookSortDeclaration(params, sort, att)

    def SymbolDeclaration(
        symbol: Symbol,
        argSorts: Seq[Sort],
        returnSort: Sort,
        att: Attributes
    ): Declaration = d.SymbolDeclaration(symbol, argSorts, returnSort, att)

    def HookSymbolDeclaration(
        symbol: Symbol,
        argSorts: Seq[Sort],
        returnSort: Sort,
        att: Attributes
    ): Declaration = d.HookSymbolDeclaration(symbol, argSorts, returnSort, att)

    def AliasDeclaration(
        alias: Alias,
        argSorts: Seq[Sort],
        returnSort: Sort,
        leftPattern: Pattern,
        rightPattern: Pattern,
        att: Attributes
    ): Declaration =
      d.AliasDeclaration(alias, argSorts, returnSort, leftPattern, rightPattern, att)

    def AxiomDeclaration(
        params: Seq[SortVariable],
        _1: Pattern,
        att: Attributes
    ): Declaration = d.AxiomDeclaration(params, _1, att)

    def ClaimDeclaration(
        params: Seq[SortVariable],
        _1: Pattern,
        att: Attributes
    ): Declaration = d.ClaimDeclaration(params, _1, att)

    def Attributes(patterns: Seq[Pattern]): Attributes = d.Attributes(patterns)

    def Variable(name: String, sort: Sort): Variable = d.Variable(name, sort)

    def SetVariable(name: String, sort: Sort): SetVariable = d.SetVariable(name, sort)

    def Application(head: SymbolOrAlias, args: Seq[Pattern]): Pattern =
      d.Application(head, args)

    def Top(s: Sort): Pattern = d.Top(s)

    def Bottom(s: Sort): Pattern = d.Bottom(s)

    def And(s: Sort, _1: Pattern, _2: Pattern): Pattern = d.And(s, Seq(_1, _2))

    def And(s: Sort, args: Seq[Pattern]): Pattern =
      args.size match {
        case 0 => Top(s)
        case 1 => args(0)
        case _ => d.And(s, args)
      }

    def Or(s: Sort, _1: Pattern, _2: Pattern): Pattern = d.Or(s, Seq(_1, _2))

    def Or(s: Sort, args: Seq[Pattern]): Pattern =
      args.size match {
        case 0 => Bottom(s)
        case 1 => args(0)
        case _ => d.Or(s, args)
      }

    def Not(s: Sort, _1: Pattern): Pattern = d.Not(s, _1)

    def Implies(s: Sort, _1: Pattern, _2: Pattern): Pattern = d.Implies(s, _1, _2)

    def Iff(s: Sort, _1: Pattern, _2: Pattern): Pattern = d.Iff(s, _1, _2)

    def Exists(s: Sort, v: Variable, p: Pattern): Pattern = d.Exists(s, v, p)

    def Forall(s: Sort, v: Variable, p: Pattern): Pattern = d.Forall(s, v, p)

    // def Next(s: i.Sort, _1: i.Pattern): i.Pattern = d.Next(s, _1)

    def Rewrites(s: Sort, _1: Pattern, _2: Pattern): Rewrites = d.Rewrites(s, _1, _2)

    def Ceil(s: Sort, rs: Sort, p: Pattern): Pattern = d.Ceil(s, rs, p)

    def Floor(s: Sort, rs: Sort, p: Pattern): Pattern = d.Floor(s, rs, p)

    def Equals(s: Sort, rs: Sort, _1: Pattern, _2: Pattern): Equals =
      d.Equals(s, rs, _1, _2)

    def Mem(s: Sort, rs: Sort, p: Pattern, q: Pattern): Pattern = d.Mem(s, rs, p, q)

    def DomainValue(s: Sort, str: String): Pattern = d.DomainValue(s, str)

    // def Subset(s: i.Sort, rs: i.Sort, _1: Pattern, _2: Pattern): i.Pattern = d.Subset(s, rs, _1,
    // _2)

    def StringLiteral(str: String): Pattern = d.StringLiteral(str)

    // def DomainValue(sortStr: String, valueStr: String): Pattern = d.DomainValue(sortStr,
    // valueStr)

    def SortVariable(name: String): SortVariable = d.SortVariable(name)

    def CompoundSort(ctr: String, params: Seq[Sort]): CompoundSort = d.CompoundSort(ctr, params)

    def SymbolOrAlias(ctr: String, params: Seq[Sort]): SymbolOrAlias =
      d.SymbolOrAlias(ctr, params)

    def Symbol(ctr: String, params: Seq[Sort]): Symbol = d.Symbol(ctr, params)

    def Alias(ctr: String, params: Seq[Sort]): Alias = d.Alias(ctr, params)

    def LeftAssoc(ctr: (Pattern, Pattern) => Pattern, args: Seq[Pattern]): Pattern =
      args.reduceLeft((accum, p) => ctr(accum, p))

    def RightAssoc(ctr: (Pattern, Pattern) => Pattern, args: Seq[Pattern]): Pattern =
      args.reduceRight((p, accum) => ctr(p, accum))
  }
}
