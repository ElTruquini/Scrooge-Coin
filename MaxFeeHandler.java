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
			System.out.println("Looping from txs inputs");
			inputs = txs[i].getInputs();

			for(int j = 0 ; j < inputs.size() ; j++){
				UTXO utxo = new UTXO(inputs.getInput(j).prevTxHash, inputs.getInput(j).outputIndex);
				
			}	

		}
		return null;
	}


}