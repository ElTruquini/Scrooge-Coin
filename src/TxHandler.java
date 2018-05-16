import java.nio.ByteBuffer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;


public class TxHandler {

	protected static UTXOPool pool;
	protected static UTXOPool spent_pool; // contains historic UTXOs
	protected static int tx_counter;

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		pool = new UTXOPool(utxoPool);
		spent_pool = new UTXOPool();
		tx_counter = 0;
	}

	// Prints current state of UTXOPool pool
	// args: 0 - current pool, 1 - historic pool
	public static void printPool(int mode){
		if(mode == 0){
			System.out.println("=======Curr UTXOPool==========");
			ArrayList<UTXO> curr = pool.getAllUTXO();
			for(UTXO i : curr){ 
				System.out.println("TxHash:" + trimHash(i.getTxHash()) + "\t|i:"
				+ i.getIndex() + "\t|value: " + pool.getTxOutput(i).value);
			}
		}else{
			System.out.println("=======Spent UTXOPool=========");
			ArrayList<UTXO> spents = spent_pool.getAllUTXO();
			for(UTXO i : spents){ 
				System.out.println("TxHash:" + trimHash(i.getTxHash()) + "\t|i:" 
				+ i.getIndex() + "\t|value: " + pool.getTxOutput(i).value);
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
	 * (5) the sum of {@code tx}s input values is greater than or equal to the sum of 
	 *		its output values; and false otherwise.
	 */
	public boolean isValid(Transaction tx) {
		System.out.println("[INFO] txHandler.isValid() Validating TxHash:" +  trimHash(tx.getHash()));
		printPool(0);	

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
			assert utxo != null;
			// (1) - Checking all outputs claimed are in the current UTXO pool
			if(prev_output == null){
				System.out.println("[INVALID] Prev_output not found - Tx#:" + tx_counter 
						+ "	|txHash: " + trimHash(tx.getHash()));
				return false;
			}
			//(2) - Verify the signatures on each input are valid
			if(!Crypto.verifySignature(prev_output.address, tx.getRawDataToSign(counter),
					 i.signature)){
				System.out.println("[INVALID] Invalid signature - Tx#:" + tx_counter 
					+ "	 |txHash: " + trimHash(tx.getHash()));
				return false;
			}
			//(3) - No UTXO is claimed multiple times
			if(set.contains(utxo)){
				System.out.println("[INVALID] UTXO claimed multiple times - Tx#:" 
						+ tx_counter 
				+ "	\t|txHash: " + trimHash(tx.getHash()));
				return false;
			}else{
				set.add(utxo);
			}
			input_sum += prev_output.value;
		}
		//(5) - Checking sum outputs is less than sum of input
		if (input_sum < output_sum){
			System.out.println("[INVALID] Output less than input - Tx#:" + tx_counter 
					+ " 	|txHash: " + trimHash(tx.getHash()));
			return false;
		}
		System.out.println("[VALID] - Tx#:" + tx_counter 
				+ "		|txHash:" + trimHash(tx.getHash()));
		return true;
	}

	public static String trimHash(byte[] bytes){
		String s = Hex.toHexString(bytes);
		return s.substring(0, Math.min(s.length(),6)); //avoids exception when string is less than 5
	}


	public static String printKey(byte[] key) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		String hash = "";
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(key);
		// byte[] digest = m.digest();

		return trimHash(m.digest());
	}



	/**
	 * Handles each epoch by receiving an unordered array of proposed transactions, 
	 *	checking each transaction for correctness, returning a mutually valid array
	 *	of accepted transactions, and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] candidateTxs) {
		ArrayList<Transaction> temp = new ArrayList<Transaction>();
		for(int i = 0 ; i < candidateTxs.length ; i++, tx_counter++){
			// System.out.println("[INFO] txHandler - Validating tx#:" + tx_counter +
				 // " |txHash: " +  trimHash(candidateTxs[i].getHash()));
			if(isValid(candidateTxs[i])){
				temp.add(candidateTxs[i]);
				// removing spent inputs from pool, adding them to spent (historic) pool
				ArrayList<Transaction.Input> inputs = candidateTxs[i].getInputs();
				for(Transaction.Input in : inputs){
					UTXO x = new UTXO(in.prevTxHash, in.outputIndex);
					Transaction.Output spent_out = pool.getTxOutput(x);
					spent_pool.addUTXO(x, spent_out);
					pool.removeUTXO(x);
				}
				// adding new utxo's to pool
				int index = 0;
				ArrayList<Transaction.Output> outputs = candidateTxs[i].getOutputs();
				for(Transaction.Output out : outputs){
					pool.addUTXO(new UTXO(candidateTxs[i].getHash(), index),out);
					index++;
				}
			}
		}

		return temp.toArray(new Transaction[temp.size()]);
	}

}
