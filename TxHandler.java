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
		System.out.println("=====START UTXOPool=====");
		ArrayList<UTXO> curr_pool = pool.getAllUTXO();
		for(UTXO i : curr_pool){ 
			System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) + " | i:" + i.getIndex());
		}
		System.out.println("=====END UTXOPool=====\n");

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
	public boolean isValidTx(Transaction tx) {
		boolean isValid = true;
		Set<UTXO> set = new HashSet<UTXO>();
		printPool();

		double output_sum = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for(Transaction.Output i : outputs){
			// (4) - all of txs output values are non-negative
			if (i.value < 0){
				// System.out.println("[isValidTx - ERROR] Tx output is negative:" + i.value);
				return false;
			}
			output_sum += i.value;
		}
		int counter = 0;
		double input_sum = 0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		for(Transaction.Input i : inputs){
			// System.out.println("New input - Prev outputIndex:"+i.outputIndex+" | prevTxHash:"+Hex.toHexString(i.prevTxHash));
			UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
			Transaction.Output prev_output = pool.getTxOutput(utxo);
			//(1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				// System.out.println("[isValidTx - ERROR] Input is not part of UTXO set, " + Hex.toHexString(i.prevTxHash));
				return false;
			}
			//(2) - Verify the signatures on each input are valid
			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter), i.signature)){
				// System.out.println("[isValidTx - ERROR] Invalid input signature, " + Hex.toHexString(i.prevTxHash));
				return false;
			}
			// System.out.println("[isValidTx - OK] Signature is valid");
			//(3) - No UTXO is claimed multiple times
			if(set.contains(utxo)){
				// System.out.println("[isValidTx - ERROR] UTXO claimed multiple times " + Hex.toHexString(utxo.getTxHash()));
				return false;
			}else{
				set.add(utxo);
				// System.out.println("[isValidTx - OK] UTXO not been claimed before " + Hex.toHexString(utxo.getTxHash()));
			}
			input_sum += prev_output.value;
			counter++;
		}
		//(5) - Checking sum outputs is less than sum of input
		// System.out.printf("Final input_sum: %f\n", input_sum);
		if (input_sum < output_sum){
			// System.out.println("[isValidTx - ERROR] Output sum is greater than input sum");
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
		// IMPLEMENT THIS
		return null;
	}

}
