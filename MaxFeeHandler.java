//Date: April, 2018

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

	/**
	* Takes an array of valid tranasctions, and returns a sorted ascending array according to their fee (from min to max)
	**/
	public Transaction[] handleTxs(Transaction[] txs){
		ArrayList<TxFee> tx_fee_arr = new ArrayList<TxFee>();
		//Calculate fee on each transaction
		for(int i = 0 ; i < txs.length ; i++){
			double input_sum = 0;
			double output_sum = 0;
			ArrayList<Transaction.Input> inputs;
			ArrayList<Transaction.Output> outputs;
			
			inputs = txs[i].getInputs();
			//Getting sum of all inputs
			for(int j = 0 ; j < inputs.size() ; j++){
				UTXO utxo = new UTXO(inputs.get(j).prevTxHash, inputs.get(j).outputIndex);
				Transaction.Output utxo_output = TxHandler.spent_pool.getTxOutput(utxo);
				assert utxo_output != null;
				input_sum += utxo_output.value;
			}	
			//Getting sum of all outputs
			outputs = txs[i].getOutputs();
			for(int j = 0 ; j < outputs.size() ; j++){
				output_sum += outputs.get(j).value;
			}
			TxFee tx = new TxFee(i, input_sum - output_sum);
			tx_fee_arr.add(tx);
		}
		Collections.sort(tx_fee_arr);

		assert tx_fee_arr.size() == txs.length; 
		Transaction[] ordered_tx = new Transaction[txs.length];
		for(int i = 0 ; i < txs.length ; i++){
			ordered_tx[i] = txs[tx_fee_arr.get(i).tx_num];
		}


		return ordered_tx;
	}


}