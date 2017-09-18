package org.coroutines.common



import scala.collection._
import scala.reflect.macros.whitebox



private[coroutines] class ByTreeUntyper[C <: whitebox.Context](val c: C)(val treeValue: Any) {
  import c.universe._
  private val tree = treeValue.asInstanceOf[Tree]
  private val untypedTree = c.untypecheck(tree)
  private val treeMapping = mutable.Map[Tree, Tree]()
  private val traverser = new TraverserUtil[c.type](c)
  traverser.traverseByShape(tree, untypedTree)((t, pt) => treeMapping(t) = pt)

  def untypecheck(t: Tree): c.universe.Tree = if (treeMapping.contains(t)) treeMapping(t) else t
}
