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
		System.out.println("\n\nValidating new transaction...");

		printPool();

		double output_sum = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for(Transaction.Output i : outputs){
			// (4) - all of txs output values are non-negative
			if (i.value < 0){
				System.out.println("[isValid - ERROR] Tx output is negative:" + i.value);
				return false;
			}
			output_sum += i.value;
		}
		int counter = 0;
		double input_sum = 0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		for(Transaction.Input i : inputs){
			System.out.println("[New input] curr.hash:" + Hex.toHexString(tx.getHash()) + " \n 		| prevTxHash:"+Hex.toHexString(i.prevTxHash) + " | i:" + i.outputIndex);
			UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
			Transaction.Output prev_output = pool.getTxOutput(utxo);
			// (1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				System.out.println("[isValid - ERROR] Input is not part of UTXO set, " + Hex.toHexString(i.prevTxHash) + " | i:" + i.outputIndex);
				return false;
			}
			//(2) - Verify the signatures on each input are valid
			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter), i.signature)){
				System.out.println("[isValid - ERROR] Invalid input signature, " + Hex.toHexString(i.prevTxHash));
				return false;
			}
			// System.out.println("[isValid - OK] Signature is valid");
			//(3) - No UTXO is claimed multiple times
			if(set.contains(utxo)){
				System.out.println("[isValid - ERROR] UTXO claimed multiple times " + Hex.toHexString(utxo.getTxHash()));
				return false;
			}else{
				set.add(utxo);
				// System.out.println("[isValid - OK] UTXO not been claimed before " + Hex.toHexString(utxo.getTxHash()));
			}
			input_sum += prev_output.value;
			counter++;
		}
		//(5) - Checking sum outputs is less than sum of input
		// System.out.printf("Final input_sum: %f\n", input_sum);
		if (input_sum < output_sum){
			System.out.println("[isValid - ERROR] Output sum is greater than input sum");
			return false;
		}
		System.out.println("[isValid] Tx is valid");
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
					System.out.println("[Removing input]: prevTxHash: " + Hex.toHexString(in.prevTxHash) + " | i:" + in.outputIndex);
					pool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
				}
				System.out.println("After removal of UTXO....");
				printPool();
				// adding new utxo's to pool
				int index = 0;
				ArrayList<Transaction.Output> outputs = possibleTxs[i].getOutputs();
				for(Transaction.Output out : outputs){
					pool.addUTXO(new UTXO(possibleTxs[i].getHash(), index),out);
					index++;
				}
				System.out.println("After adding new UTXO...");
				printPool();
			}
		}
		System.out.println("\n\nFinal Pool:");
		printPool();

		System.out.println("Final Tx array:");
		Transaction[] valid_txs = temp.toArray(new Transaction[temp.size()]);
		for(Transaction i : valid_txs)
			System.out.println("[handleTxs] Final arr, hash:" + Hex.toHexString(i.getHash()));
		return valid_txs;
	}

}
