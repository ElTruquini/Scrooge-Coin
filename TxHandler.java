import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.bouncycastle.util.encoders.Hex;


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
		System.out.println("=====Printing UTXO pool=====");
		ArrayList<UTXO> curr_pool = pool.getAllUTXO();
		for(UTXO i : curr_pool){ 
			System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) + " | i:" + i.getIndex());
		}
		System.out.println("=====END UTXO pool=====");

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
		printPool();

		double output_sum = 0;
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for(Transaction.Output i : outputs){
			// System.out.printf("outputs:%f\n",i.value);
			output_sum += i.value;
		}
		// assert(output_sum > 0);
		// System.out.printf("Final output_sum: %f\n", output_sum);

		int counter = 0;
		double input_sum = 0;
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		for(Transaction.Input i : inputs){
			System.out.println("i.outputIndex:"+i.outputIndex+" | i.prevTxHash:"+Hex.toHexString(i.prevTxHash));
			UTXO utxo = new UTXO(i.prevTxHash, i.outputIndex);
			Transaction.Output prev_output = pool.getTxOutput(utxo);
			//(1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				System.out.println("[ERROR] Input is not part of UTXO set, " + Hex.toHexString(i.prevTxHash));
				return false;
			}
			//(2) - Verify the signatures on each input are valid

			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter), i.signature)){
				System.out.println("[ERROR] Invalid input signature, " + Hex.toHexString(i.prevTxHash));
				return false;
			}
			System.out.println("[OK] Signature is valid");

			input_sum += prev_output.value;
			counter++;
		}

		for(int i = 0 ; i < tx.numInputs() ; i++){
			Transaction.Input in = tx.getInput(i);
			UTXO prev_utxo = new UTXO(in.prevTxHash, in.outputIndex);

			//TODO: What message to use for verification?
			// public static boolean verifySignature(PublicKey pubKey, byte[] message, byte[] signature) {
			// if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(i), in.signature)){
			// 	System.out.println("[ERROR] Invalid input signature, " + Hex.toHexString(in.prevTxHash));
			// 	isValid = false;
			// 	break;
			// }
			// System.out.println("[OK] Signature is valid");
		}

		// System.out.printf("Final input_sum: %f\n", input_sum);
		//(5) - Checking sum outputs is less than sum of input
		if (input_sum < output_sum){
			System.out.println("[ERROR] Output sum is greater than input sum");
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
