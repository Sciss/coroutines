package scala.coroutines



import scala.annotation.tailrec
import scala.collection._
import scala.coroutines.common._
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context



/** Synthesizes all coroutine-related functionality.
 */
private[coroutines] class Synthesizer[C <: Context](val c: C)
extends Analyzer[C]
with CfgGenerator[C]
with ThreeAddressFormTransformation[C] {
  import c.universe._

  val NUM_PREDEFINED_ENTRY_STUBS = 40

  private def genEntryPoint(
    subgraph: SubCfg, rettpt: Tree
  )(implicit table: Table): Tree = {
    val body = subgraph.emit()
    val defname = TermName(s"ep${subgraph.uid}")
    val defdef = if (subgraph.uid < NUM_PREDEFINED_ENTRY_STUBS) q"""
      override def $defname(${table.names.coroutineParam}: Coroutine[$rettpt]): Unit = {
        $body
      }
    """ else q"""
      def $defname(${table.names.coroutineParam}: Coroutine[$rettpt]): Unit = {
        $body
      }
    """
    defdef
  }

  private def genEntryPoints(
    cfg: Cfg, rettpt: Tree
  )(implicit table: Table): Map[Long, Tree] = {
    val entrypoints = for ((orignode, subgraph) <- cfg.subgraphs) yield {
      (subgraph.uid, genEntryPoint(subgraph, rettpt))
    }
    mutable.LinkedHashMap() ++= entrypoints.toSeq.sortBy(_._1)
  }

  private def genEnterMethod(
    entrypoints: Map[Long, Tree], tpt: Tree
  )(implicit table: Table): Tree = {
    if (entrypoints.size == 1) {
      val q"$_ def $ep0($_): Unit = $_" = entrypoints(0)

      q"""
        def enter(c: Coroutine[$tpt]): Unit = $ep0(c)
      """
    } else if (entrypoints.size == 2) {
      val q"$_ def $ep0($_): Unit = $_" = entrypoints(0)
      val q"$_ def $ep1($_): Unit = $_" = entrypoints(1)

      q"""
        def enter(c: Coroutine[$tpt]): Unit = {
          val pc = scala.coroutines.common.Stack.top(c.pcstack)
          if (pc == 0) $ep0(c) else $ep1(c)
        }
      """
    } else {
      val cases = for ((index, defdef) <- entrypoints) yield {
        val q"$_ def $ep($_): Unit = $rhs" = defdef
        cq"${index.toShort} => $ep(c)"
      }

      q"""
        def enter(c: Coroutine[$tpt]): Unit = {
          val pc: Short = scala.coroutines.common.Stack.top(c.pcstack)
          (pc: @scala.annotation.switch) match {
            case ..$cases
          }
        }
      """
    }
  }

  private def genReturnValueMethod(cfg: Cfg, tpt: Tree)(implicit table: Table): Tree = {
    def genReturnValueStore(n: Node) = {
      val sub = cfg.subgraphs(n.successors.head)
      val pcvalue = sub.uid
      val info = table(n.tree.symbol)
      val rvset = info.storeTree(q"c", q"v")
      (pcvalue, q"$rvset")
    }
    val returnstores = cfg.start.dfs.collect {
      case n @ Node.ApplyCoroutine(_, _, _) => genReturnValueStore(n)
    }

    val body = {
      if (returnstores.size == 0) {
        q"()"
      } else if (returnstores.size == 1) {
        returnstores(0)._2
      } else if (returnstores.size == 2) {
        q"""
          val pc = scala.coroutines.common.Stack.top(c.pcstack)
          if (pc == ${returnstores(0)._1.toShort}) {
            ${returnstores(0)._2}
          } else {
            ${returnstores(1)._2}
          }
        """
      } else {
        val cases = for ((pcvalue, rvset) <- returnstores) yield {
          cq"${pcvalue.toShort} => $rvset"
        }
        q"""
          val pc = scala.coroutines.common.Stack.top(c.pcstack)
          (pc: @scala.annotation.switch) match {
            case ..$cases
          }
        """
      }
    }

    q"""
      def returnvalue(c: scala.coroutines.Coroutine[$tpt], v: $tpt)(
        implicit cc: scala.coroutines.CanCallInternal
      ): Unit = {
        $body
      }
    """
  }

  def genVarPushesAndPops(cfg: Cfg)(implicit table: Table): (List[Tree], List[Tree]) = {
    val stackVars = cfg.stackVars
    val storedValVars = cfg.storedValVars
    val storedRefVars = cfg.storedRefVars
    def stackSize(vs: Map[Symbol, VarInfo]) = vs.map(_._2.stackpos._2).sum
    def genVarPushes(allvars: Map[Symbol, VarInfo], stack: Tree): List[Tree] = {
      val vars = allvars.filter(kv => stackVars.contains(kv._1))
      val varsize = stackSize(vars)
      val stacksize = math.max(table.initialStackSize, varsize)
      val bulkpushes = if (vars.size == 0) Nil else List(q"""
        scala.coroutines.common.Stack.bulkPush($stack, $varsize, $stacksize)
      """)
      val args = vars.values.filter(_.isArg).toList
      val argstores = for (a <- args) yield a.storeTree(q"c", q"${a.name}")
      bulkpushes ::: argstores
    }
    val varpushes = {
      genVarPushes(storedRefVars, q"c.refstack") ++
      genVarPushes(storedValVars, q"c.valstack")
    }
    val varpops = (for ((sym, info) <- storedRefVars.toList) yield {
      info.popTree
    }) ++ (if (storedValVars.size == 0) Nil else List(
      q"scala.coroutines.common.Stack.bulkPop(c.valstack, ${stackSize(storedValVars)})"
    ))
    (varpushes, varpops)
  }

  def synthesize(rawlambda: Tree): Tree = {
    // transform to two operand assignment form
    val typedtaflambda = transformToThreeAddressForm(rawlambda)
    println(typedtaflambda)
    println(typedtaflambda.tpe)

    implicit val table = new Table(typedtaflambda)
    
    // ensure that argument is a function literal
    val q"(..$args) => $body" = typedtaflambda
    val argidents = for (arg <- args) yield {
      val q"$_ val $argname: $_ = $_" = arg
      q"$argname"
    }

    // extract argument names and types
    val (argnames, argtpts) = (for (arg <- args) yield {
      val q"$_ val $name: $tpt = $_" = arg
      (name, tpt)
    }).unzip

    // infer coroutine return type
    val rettpt = inferReturnType(body)

    // generate control flow graph
    val cfg = genControlFlowGraph(args, body, rettpt)

    // generate entry points from yields and coroutine applications
    val entrypoints = genEntryPoints(cfg, rettpt)

    // generate entry method
    val entermethod = genEnterMethod(entrypoints, rettpt)

    // generate return value method
    val returnvaluemethod = genReturnValueMethod(cfg, rettpt)

    // generate variable pushes and pops for stack variables
    val (varpushes, varpops) = genVarPushesAndPops(cfg)

    // emit coroutine instantiation
    val coroutineTpe = TypeName(s"_${args.size}")
    val entrypointmethods = entrypoints.map(_._2)
    val valnme = TermName(c.freshName("c"))
    val co = q"""
      new scala.coroutines.Coroutine.$coroutineTpe[..$argtpts, $rettpt] {
        def call(..$args)(
          implicit cc: scala.coroutines.CanCallInternal
        ): scala.coroutines.Coroutine[$rettpt] = {
          val $valnme = new scala.coroutines.Coroutine[$rettpt]
          push($valnme, ..$argidents)
          $valnme
        }
        def apply(..$args): $rettpt = {
          sys.error(
            "Coroutines can only be invoked directly from within other coroutines. " +
            "Use `call(<coroutine>(<arg0>, ..., <argN>))` instead if you want to " +
            "start a new coroutine.")
        }
        def push(c: scala.coroutines.Coroutine[$rettpt], ..$args)(
          implicit cc: scala.coroutines.CanCallInternal
        ): Unit = {
          scala.coroutines.common.Stack.push(c.costack, this, -1)
          scala.coroutines.common.Stack.push(c.pcstack, 0.toShort, -1)
          ..$varpushes
        }
        def pop(c: scala.coroutines.Coroutine[$rettpt]): Unit = {
          scala.coroutines.common.Stack.pop(c.pcstack)
          scala.coroutines.common.Stack.pop(c.costack)
          ..$varpops
        }
        $entermethod
        ..$entrypointmethods
        $returnvaluemethod
      }
    """
    println(co)
    co
  }

  def call[T: WeakTypeTag](lambda: Tree): Tree = {
    val (receiver, args) = lambda match {
      case q"$r.apply(..$args)" =>
        if (!isCoroutineBlueprint(r.tpe))
          c.abort(r.pos,
            s"Receiver must be a coroutine.\n" +
            s"required: Coroutine.Definition[${implicitly[WeakTypeTag[T]]}]\n" +
            s"found:    ${r.tpe} (with underlying type ${r.tpe.widen})")
        (r, args)
      case _ =>
        c.abort(
          lambda.pos,
          "The call statement must take a coroutine invocation expression:\n" +
          "  call(<coroutine>.apply(<arg0>, ..., <argN>))")
    }

    val tpe = implicitly[WeakTypeTag[T]]
    val t = q"""
      import scala.coroutines.Permission.canCall
      $receiver.call(..$args)
    """
    t
  }
}
