package org.ergoplatform.nodeView.history.storage.modifierprocessors.adproofs

import io.iohk.iodb.ByteArrayWrapper
import org.ergoplatform.modifiers.{ErgoFullBlock, ErgoPersistentModifier}
import org.ergoplatform.modifiers.history.{ADProof, BlockTransactions, Header, HistoryModifierSerializer}
import org.ergoplatform.nodeView.history.storage.modifierprocessors.FullBlockProcessor
import scorex.core.consensus.History.ProgressInfo
import scorex.crypto.encode.Base58

import scala.util.Try

/**
  * ADProof processor for regime that download ADPrrofs
  */
trait FullProofsProcessor extends ADProofsProcessor with FullBlockProcessor {

  protected val adState: Boolean

  /**
    *
    * @return
    */
  def process(m: ADProof): ProgressInfo[ErgoPersistentModifier] = {
    historyStorage.modifierById(m.headerId) match {
      case Some(header: Header) =>
        historyStorage.modifierById(header.transactionsId) match {
          case Some(txs: BlockTransactions) if adState =>
            processFullBlock(ErgoFullBlock(header, txs, Some(m), None), txsAreNew = false)
          case _ =>
            val modifierRow = Seq((ByteArrayWrapper(m.id), ByteArrayWrapper(HistoryModifierSerializer.toBytes(m))))
            historyStorage.insert(m.id, modifierRow)
            ProgressInfo(None, Seq(), Seq())
        }
      case _ =>
        throw new Error(s"Header for modifier $m is no defined")
    }
  }


  override def toDrop(modifier: ADProof): Seq[ByteArrayWrapper] = Seq(ByteArrayWrapper(modifier.id))

  override def validate(m: ADProof): Try[Unit] = Try {
    require(!historyStorage.contains(m.id), s"Modifier $m is already in history")
    historyStorage.modifierById(m.headerId) match {
      case Some(h: Header) =>
        require(h.ADProofsRoot sameElements m.digest,
          s"Header ADProofs root ${Base58.encode(h.ADProofsRoot)} differs from $m digest")
      case _ =>
        throw new Error(s"Header for modifier $m is no defined")
    }
  }
}
