package j1.miniasm

import scala.collection.mutable.ListBuffer

import scala.reflect.runtime.{universe => ru}

/* Encoding support for the j1 instruction set. */

object InsnMask {
  val IMM: Int  = 0x8000
  val JMP: Int  = 0x0000
  val JPZ: Int  = 0x2000
  val CALL: Int = 0x4000
  val ALU: Int  = 0x6000
}

sealed abstract case class AluOp(enc: Int, ext: Boolean = false) {
  require(0x0 to 0xF contains enc)
  val mask: Int = ((if (ext) 0x10 else 0x0) | enc) << 8
}

object AluOp {
  object T        extends AluOp(0x0)
  object N        extends AluOp(0x1)
  object TplusN   extends AluOp(0x2)
  object TandN    extends AluOp(0x3)
  object TorN     extends AluOp(0x4)
  object TxorN    extends AluOp(0x5)
  object invT     extends AluOp(0x6)
  object NeqT     extends AluOp(0x7)
  object NleT     extends AluOp(0x8)
  object NrshiftT extends AluOp(0x9)
  object NlshiftT extends AluOp(0xA)
  object rT       extends AluOp(0xB)
  object fetchT   extends AluOp(0xC)
  object iofetchT extends AluOp(0xD)
  object status   extends AluOp(0xE)
  object NuleT    extends AluOp(0xF)
}

sealed abstract case class AluFunc(enc: Int, withExit: Boolean = false) {
  require(0x0 to 0x8 contains enc)
  val exits: Boolean = enc == 0x8 || withExit
  def mask: Int = ((if (withExit) 0x8 else 0x0) | enc) << 4
}

object AluFunc {
  object None      extends AluFunc(0x0, false)
  object T2N       extends AluFunc(0x1, false)
  object T2R       extends AluFunc(0x2, false)
  object NstoreT   extends AluFunc(0x3, false)
  object NiostoreT extends AluFunc(0x4, false)
  object BANK      extends AluFunc(0x5, false)
  object PROT      extends AluFunc(0x6, false)
  object HALT      extends AluFunc(0x7, false)
  object EXIT      extends AluFunc(0x8, false)
  final class withExit(func: AluFunc) extends AluFunc(func.enc, true)
}

sealed abstract case class Alu(op: AluOp, func: AluFunc, dspInc: Int,
                                                         rspInc: Int) {
  require(-2 to 1 contains dspInc)
  require(-2 to 1 contains rspInc)
  private val dsp_mask: Int = (dspInc & 0x3) << 0
  private val rsp_mask: Int = (rspInc & 0x3) << 2
  val mask: Int = op.mask | func.mask | dsp_mask | rsp_mask
  val encode: Short = (InsnMask.ALU | mask).toShort
  // TODO: Review whether the below is necessary and sufficient.
  def permitsExit = !func.exits && rspInc == 0 // REVIEW
}

final class withExit(val wrapped: Alu) extends
  Alu(wrapped.op, new AluFunc.withExit(wrapped.func), wrapped.dspInc,
                                                      wrapped.rspInc - 1) {
  /* NOTE: Not all Alu operations may perform a simultaneous EXIT. */
  require(wrapped.permitsExit)
}

object withExit {
  def apply(wrapped: Alu) = new withExit(wrapped)
}

object Basewords {
  // NOTE: STORE and IOSTORE need to be followed by a DROP.
  object NOOP    extends Alu(AluOp.T, AluFunc.None, 0, 0)
  object PLUS    extends Alu(AluOp.TplusN, AluFunc.None, -1, 0)
  object AND     extends Alu(AluOp.TandN, AluFunc.None, -1, 0)
  object OR      extends Alu(AluOp.TorN, AluFunc.None, -1, 0)
  object XOR     extends Alu(AluOp.TxorN, AluFunc.None, -1, 0)
  object INVERT  extends Alu(AluOp.invT, AluFunc.None, 0, 0)
  object EQUAL   extends Alu(AluOp.NeqT, AluFunc.None, -1, 0)
  object LESS    extends Alu(AluOp.NleT, AluFunc.None, -1, 0)
  object ULESS   extends Alu(AluOp.NuleT, AluFunc.None , -1, 0)
  object SWAP    extends Alu(AluOp.N, AluFunc.T2N, 0, 0)
  object DUP     extends Alu(AluOp.T, AluFunc.T2N, 1, 0)
  object DROP    extends Alu(AluOp.N, AluFunc.None, -1, 0)
  object OVER    extends Alu(AluOp.N, AluFunc.T2N, 1, 0)
  object NIP     extends Alu(AluOp.T, AluFunc.None, -1, 0)
  object TO_R    extends Alu(AluOp.N, AluFunc.T2R, -1, 1)
  object FROM_R  extends Alu(AluOp.rT, AluFunc.T2N, 1, -1)
  object R_FETCH extends Alu(AluOp.rT, AluFunc.T2N, 1, 0)
  object FETCH   extends Alu(AluOp.fetchT, AluFunc.None, 0, 0)
  object IOFETCH extends Alu(AluOp.iofetchT, AluFunc.None, 0, 0)
  object STORE   extends Alu(AluOp.T, AluFunc.NstoreT, -1, 0)
  object IOSTORE extends Alu(AluOp.T, AluFunc.NiostoreT, -1, 0)
  object RSHIFT  extends Alu(AluOp.NrshiftT, AluFunc.None, -1, 0)
  object LSHIFT  extends Alu(AluOp.NlshiftT, AluFunc.None, -1, 0)
  object DEPTHS  extends Alu(AluOp.status, AluFunc.T2N, 1, 0)
  object PROTECT extends Alu(AluOp.T, AluFunc.PROT, 0, 0)
  object HALT    extends Alu(AluOp.T, AluFunc.HALT, 0, 0)
  object EXIT    extends Alu(AluOp.T, AluFunc.EXIT, 0, -1)
}

object ElidedWords {
  /* Instructions that keep one operand on the stack. */
  // NOTE: KEEP1_STORE and KEEP1_IOSTORE need to be followed by a DROP.
  object KEEP1_PLUS    extends Alu(AluOp.TplusN, AluFunc.None, 0, 0);
  object KEEP1_AND     extends Alu(AluOp.TandN, AluFunc.None, 0, 0);
  object KEEP1_OR      extends Alu(AluOp.TorN, AluFunc.None, 0, 0);
  object KEEP1_XOR     extends Alu(AluOp.TxorN, AluFunc.None, 0, 0);
  object KEEP1_INVERT  extends Alu(AluOp.invT, AluFunc.T2N, 1, 0);
  object KEEP1_EQUAL   extends Alu(AluOp.NeqT, AluFunc.None, 0, 0);
  object KEEP1_LESS    extends Alu(AluOp.NleT, AluFunc.None, 0, 0);
  object KEEP1_ULESS   extends Alu(AluOp.NuleT, AluFunc.None, 0, 0);
  object KEEP1_RSHIFT  extends Alu(AluOp.NrshiftT, AluFunc.None, 0, 0);
  object KEEP1_LSHIFT  extends Alu(AluOp.NlshiftT, AluFunc.None, 0, 0);
  object KEEP1_TO_R    extends Alu(AluOp.T, AluFunc.T2R, 0, 1);
  object KEEP1_FETCH   extends Alu(AluOp.fetchT, AluFunc.T2N, 1, 0);
  object KEEP1_IOFETCH extends Alu(AluOp.iofetchT, AluFunc.T2N, 1, 0);
  object KEEP1_STORE   extends Alu(AluOp.T, AluFunc.NstoreT, 0, 0);
  object KEEP1_IOSTORE extends Alu(AluOp.T, AluFunc.NiostoreT, 0, 0);

  // Instructions that keep two operands on the stack. */
  object KEEP2_PLUS    extends Alu(AluOp.TplusN, AluFunc.T2N, 1, 0);
  object KEEP2_AND     extends Alu(AluOp.TandN, AluFunc.T2N, 1, 0);
  object KEEP2_OR      extends Alu(AluOp.TorN, AluFunc.T2N, 1, 0);
  object KEEP2_XOR     extends Alu(AluOp.TxorN, AluFunc.T2N, 1, 0);
  object KEEP2_EQUAL   extends Alu(AluOp.NeqT, AluFunc.T2N, 1, 0);
  object KEEP2_LESS    extends Alu(AluOp.NleT, AluFunc.T2N, 1, 0);
  object KEEP2_ULESS   extends Alu(AluOp.NuleT, AluFunc.T2N, 1, 0);
  object KEEP2_RSHIFT  extends Alu(AluOp.NrshiftT, AluFunc.T2N, 1, 0);
  object KEEP2_LSHIFT  extends Alu(AluOp.NlshiftT, AluFunc.T2N, 1, 0);
  object KEEP2_STORE   extends Alu(AluOp.T, AluFunc.NstoreT, 1, 0);
  object KEEP2_IOSTORE extends Alu(AluOp.T, AluFunc.NiostoreT, 1, 0);

  /* Miscellaneous */
  object RDROP         extends Alu(AluOp.T, AluFunc.None, 0, -1);
  object TUCK_STORE    extends Alu(AluOp.T, AluFunc.NstoreT, -1, 0);
}

/* We use a reflective approach to determine defined Alu insns. */
object Isa {
  /* List of all Alu baswords. */
  lazy val BASEWORDS = reflectAluObjects(Basewords)

  /* List of all Alu elided words. */
  lazy val ELIDEDWORDS = reflectAluObjects(ElidedWords)

  /* Complete list of all Alu words. */
  lazy val ALLWORDS = BASEWORDS ++ ELIDEDWORDS

  /* Run-time mirror used for reflective instantiation. */
  private val mirror = ru.runtimeMirror(getClass.getClassLoader)

  /* Method to obtain the relective type of an object. */
  private def getInstanceType[T: ru.TypeTag](obj: T) = ru.typeOf[T]

  /* We use reflection to iterate through all Alu objects. */
  private def reflectAluObjects[T: ru.TypeTag](obj: T): List[Alu] = {
    val result = ListBuffer[Alu]()
    val decls = getInstanceType(obj).decls
    decls.foreach {
      decl => {
        if (decl.isModule) {
          val obj = mirror.reflectModule(decl.asModule).instance
          if (obj.isInstanceOf[Alu]) {
            result += obj.asInstanceOf[Alu]
          }
        }
      }
    }
    result.toList
  }
}
