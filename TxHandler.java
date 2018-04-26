import java.nio.ByteBuffer;
import org.bouncycastle.util.encoders.Hex;
import java.util.*;

public class TxHandler {

	protected static UTXOPool pool;
	protected static UTXOPool spent_pool; // contains historic UTXOs

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		pool = new UTXOPool(utxoPool);
		spent_pool = new UTXOPool();
	}

	// Prints current state of UTXOPool pool
	// args: 0 - current pool, 1 - historic pool
	public static void printPool(int mode){
		if(mode == 0){
			System.out.println("=======Curr UTXOPool==========");
			ArrayList<UTXO> curr = pool.getAllUTXO();
			for(UTXO i : curr){ 
				System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) + " | i:" + i.getIndex());
			}
		}else{
			System.out.println("=======Spent UTXOPool=========");
			ArrayList<UTXO> spents = spent_pool.getAllUTXO();
			for(UTXO i : spents){ 
				System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) + " | i:" + i.getIndex());
			}
		}
		System.out.println("==============================");
	}

	/**
	 * @return true if:
	 * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
	 * (2) the signatures on each input of {@code tx} are valid, 
	 * (3) no UTXO is claimed multiple times by {@code tx},
	 * (4) all of {@code tx}s output values are non-negative, and
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
	 *     values; and false otherwise.
	 */
	public boolean isValid(Transaction tx) {
		System.out.println("Validating new tx.......");
		boolean isValid = true;
		Set<UTXO> set = new HashSet<UTXO>();

		double output_sum = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for(Transaction.Output i : outputs){
			// (4) - all of txs output values are non-negative
			if (i.value < 0){
				System.out.println("[handleTx] invalid tx - negative inputs");
				return false;
			}
			output_sum += i.value;
		}
		int counter = 0;
		double input_sum = 0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		for(Transaction.Input i : inputs){
			UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
			Transaction.Output prev_output = pool.getTxOutput(utxo);
			assert utxo != null;
			assert prev_output != null;
			// (1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				System.out.println("[handleTx] invalid tx - Prev_output == null");

				return false;
			}
			//(2) - Verify the signatures on each input are valid
			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter), i.signature)){
				System.out.println("[handleTx] invalid tx - Signature failed");
				return false;
			}
			//(3) - No UTXO is claimed multiple times
			if(set.contains(utxo)){
				System.out.println("[handleTx] invalid tx - UTXO Claimed multiple times");
				return false;
			}else{
				set.add(utxo);
			}
			System.out.println("Input_sum:" + prev_output.value );
			input_sum += prev_output.value;
			counter++;
			System.out.println("[Info] - new input_sum:" + input_sum);
		}
		//(5) - Checking sum outputs is less than sum of input
		if (input_sum < output_sum){
			System.out.println("[handleTx] invalid tx - Sum outputs is less than sum of input | inputs:" + input_sum + " | outputs:" + output_sum);
			return false;
		}
		System.out.println("[handleTx] Tx is valid");

		return true;
	}

	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions, checking each
	 * transaction for correctness, returning a mutually valid array of accepted transactions, and
	 * updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		ArrayList<Transaction> temp = new ArrayList<Transaction>();
		for(int i = 0 ; i < possibleTxs.length ; i++){
			if(isValid(possibleTxs[i])){
				temp.add(possibleTxs[i]);
				// removing spent inputs from pool, adding them to spent (historic) pool
				ArrayList<Transaction.Input> inputs = possibleTxs[i].getInputs();
				for(Transaction.Input in : inputs){
					UTXO x = new UTXO(in.prevTxHash, in.outputIndex);
					Transaction.Output spent_out = pool.getTxOutput(x);
					spent_pool.addUTXO(x, spent_out);
					pool.removeUTXO(x);
				}
				// adding new utxo's to pool
				int index = 0;
				ArrayList<Transaction.Output> outputs = possibleTxs[i].getOutputs();
				for(Transaction.Output out : outputs){
					pool.addUTXO(new UTXO(possibleTxs[i].getHash(), index),out);
					index++;
				}
			}
		}
		printPool(0);
		printPool(1);

		Transaction[] valid_txs = temp.toArray(new Transaction[temp.size()]);
		for(int i = 0 ; i < valid_txs.length ; i++){
			System.out.println("valid_txs[" + i + "]:" + Hex.toHexString(valid_txs[i].getHash()));
		}


		return valid_txs;
	}

}
