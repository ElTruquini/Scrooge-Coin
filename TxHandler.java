import java.nio.ByteBuffer;
import org.bouncycastle.util.encoders.Hex;
import java.util.*;

public class TxHandler {

	private UTXOPool pool;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		pool = new UTXOPool(utxoPool);
	}

	// Prints current state of UTXOPool pool
	public void printPool(){
		System.out.println("===========UTXOPool=============");
		ArrayList<UTXO> curr_pool = pool.getAllUTXO();
		for(UTXO i : curr_pool){ 
			System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) + " | i:" + i.getIndex());
		}
		System.out.println("================================");
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
		boolean isValid = true;
		Set<UTXO> set = new HashSet<UTXO>();

		double output_sum = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for(Transaction.Output i : outputs){
			// (4) - all of txs output values are non-negative
			if (i.value < 0){
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
			// (1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				return false;
			}
			//(2) - Verify the signatures on each input are valid
			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter), i.signature)){
				return false;
			}
			//(3) - No UTXO is claimed multiple times
			if(set.contains(utxo)){
				return false;
			}else{
				set.add(utxo);
			}
			input_sum += prev_output.value;
			counter++;
		}
		//(5) - Checking sum outputs is less than sum of input
		if (input_sum < output_sum){
			return false;
		}
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
				// removing spent inputs from pool
				ArrayList<Transaction.Input> inputs = possibleTxs[i].getInputs();
				for(Transaction.Input in : inputs){
					pool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
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
		Transaction[] valid_txs = temp.toArray(new Transaction[temp.size()]);
		return valid_txs;
	}

}
