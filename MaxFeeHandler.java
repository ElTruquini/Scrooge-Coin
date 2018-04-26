import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;

public class MaxFeeHandler {

	public class TxFee implements Comparable<TxFee> {

		public int tx_num;
		public double fee;
		
		public TxFee(int t, double f){
			fee = f;
			tx_num = t;
		}

		@Override
		public int compareTo(final TxFee other){
			return Double.compare(this.fee, other.fee);
		}


		@Override
		public String toString(){
			return "Tx_Num:" + tx_num + " , fee:" + fee;
		}

	}

	public Transaction[] handleTxs(Transaction[] txs){
		ArrayList<TxFee> tx_fee_arr = new ArrayList<TxFee>();
		//Estimate fee on each transaction
		for(int i = 0 ; i < txs.length ; i++){
			double input_sum = 0;
			double output_sum = 0;
			ArrayList<Transaction.Input> inputs;
			ArrayList<Transaction.Output> outputs;
			
			System.out.println("\nhandleTxs - Manipulating tx hash:" + Hex.toHexString(txs[i].getHash()));
			inputs = txs[i].getInputs();
			//Getting sum of all inputs
			for(int j = 0 ; j < inputs.size() ; j++){
				UTXO utxo = new UTXO(inputs.get(j).prevTxHash, inputs.get(j).outputIndex);
				Transaction.Output utxo_output = TxHandler.spent_pool.getTxOutput(utxo);
				assert utxo_output != null;
				System.out.println("Prev tx.hash:" + Hex.toHexString(inputs.get(j).prevTxHash) + " : " + inputs.get(j).outputIndex);
				System.out.println("cointains?:" + TxHandler.spent_pool.contains(utxo));
				input_sum += utxo_output.value;
			}	
			outputs = txs[i].getOutputs();
			for(int j = 0 ; j < outputs.size() ; j++){
				output_sum += outputs.get(j).value;
			}
			System.out.println("Input_sum:" + input_sum);
			System.out.println("Output_sum:" + output_sum);


			TxFee tx = new TxFee(i, input_sum - output_sum);
			tx_fee_arr.add(tx);
		}
		System.out.println("Printing txs with fee arr...");
		for(TxFee tx : tx_fee_arr){
			System.out.println(tx);
		}

		return null;
	}


}