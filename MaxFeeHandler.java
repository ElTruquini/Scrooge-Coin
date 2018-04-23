import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;

public class MaxFeeHandler{

	public class TxFee{

		public int tx_num;
		public int fee;
		
		public TxFee(int tx_num, int fee){
			this.fee = fee;
			this.tx_num = tx_num;
		}


	}

	public static Transaction[] handleTxs(Transaction[] txs){
		//Estimate fee on each transaction
		ArrayList<Transaction.Input> inputs;
		for(int i = 0 ; i < txs.length ; i++){
			System.out.println("\nhandleTxs - Manipulating tx hash:" + Hex.toHexString(txs[i].getHash()));
			inputs = txs[i].getInputs();
			//Getting sum of all inputs
			for(int j = 0 ; j < inputs.size() ; j++){
				System.out.println("Prev tx.hash:" + Hex.toHexString(inputs.get(j).prevTxHash) + " : " + inputs.get(j).outputIndex);
				UTXO utxo = new UTXO(inputs.get(j).prevTxHash, inputs.get(j).outputIndex);
				System.out.println("cointains?:" + TxHandler.spent_pool.contains(utxo));
				
			}	
			// ArrayList<UTXO> set_pool = TxHandler.pool.getAllUTXO();
			// System.out.println("======UTXOPool======");
			// for(int x = 0 ; x < set_pool.size() ; x++){
			// 	System.out.println(Hex.toHexString(set_pool.get(x).getTxHash()) + " : " + set_pool.get(x).getIndex());
			// }
			TxHandler.printPool(1);

		}
		return null;
	}


}