import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;


public class testunit{

	KeyPair[] kpair_org, kpair_dest;
	ArrayList<Transaction> txs_list = new ArrayList<Transaction>();
	UTXO utxo;
	UTXOPool pool;
	PrivateKey priv_orig, priv_dest;
	PublicKey pub_orig, pub_dest;
	
	public static String printKey(byte[] key){
		String hash = "";
		try{
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(key);
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1, digest);
			hash = bigInt.toString(16);	
		}catch (NoSuchAlgorithmException e){
			e.printStackTrace(System.out);
		}
		return hash;
	}

	//Generates 2 lists (origin and dest) with key pairs which will be used to sign transactions
	public void createKeys(int signers) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		System.out.println("[INFO] testunit - Creating kpairs, signers: " + signers);

		//BouncyCastle required, for installation details, https://justrocketscience.com/post/install-bouncy-castle
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		kpair_org = new KeyPair[signers];
		kpair_dest = new KeyPair[signers];
		
		for(int i = 0 ; i < signers ; i++){
			keyGen.initialize(1024, random); 
			kpair_org[i] = keyGen.generateKeyPair();
			priv_orig = kpair_org[i].getPrivate();
			pub_orig = kpair_org[i].getPublic();
			// System.out.printf("%d: Orig private key: %s\n",i ,printKey(priv_orig.getEncoded()));
			// System.out.printf("%d: Orig public key: %s\n",i ,printKey(pub_orig.getEncoded()));

			keyGen.initialize(1024, random); 
			kpair_dest[i] = keyGen.generateKeyPair();
			priv_dest = kpair_dest[i].getPrivate();
			pub_dest = kpair_dest[i].getPublic();
			// System.out.printf("%d: Dest private key: %s\n",i ,printKey(priv_dest.getEncoded()));
			// System.out.printf("%d: Dest public key: %s\n",i ,printKey(pub_dest.getEncoded()));
		}
	}

	//Prints current state of UTXOpool
	public static void printPool(UTXOPool pool){
		System.out.println("=======Test UTXOPool==========");
		ArrayList<UTXO> curr = pool.getAllUTXO();
		for(UTXO i : curr){ 
			System.out.println("TxHash:" + Hex.toHexString(i.getTxHash()) 
				+ " | out_ind:"	+ i.getIndex() + " | value:"
				+ pool.getTxOutput(i).value);
		}

		System.out.println("==============================");
	}

	//Creates a digital signature based of privk and raw_data
	public static byte[] sign(PrivateKey privk, byte[] raw_data) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privk);
		signature.update(raw_data);
		return signature.sign();
	}

	//Printsf a list of all candidate txs to pass to txHandler
	public void printTxList(){
		System.out.println("+++++Printing TxList+++++");

		for(int i = 0 ; i < txs_list.size() ; i++){
			ArrayList<Transaction.Input> inps = txs_list.get(i).getInputs();
			ArrayList<Transaction.Output> outs = txs_list.get(i).getOutputs();

			System.out.println(txs_list.get(i));
			for(int j = 0 ; j < inps.size() ; j++) System.out.println(inps.get(j));
			for(int z = 0 ; z<outs.size() ; z++) System.out.println(outs.get(z));		
		}
		System.out.println("+++++++++++++++++++++++++++");
	}

	//Generates a random double between min and max
	public static double randDouble(int min, int max){
		double r = new Random().nextDouble();
		double result = min + (r * (max - min));
		return result;
	}

	//Generates random int between min and max
	public static int randInt(int min, int max){
		assert(min < max);
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

	// Mode = 1, Simple tx, one input, one output
	public Transaction createCoinbase(int tcase, int outputs) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Random random = new Random();
		int max = 20, min = 0, range = ((max-min)+1)+min;
		Transaction tx = new Transaction();

		System.out.println("[INFO] testunit.createCoinbase - tCase#: " + tcase);
		
		// Simple transaction, 1 input, 1 output
		if (tcase == 1 || tcase == 2){
			for(int i = 0; i<outputs ; i++){
				tx.addOutput(10, kpair_dest[0].getPublic());
			}
			// using random place holder value for hash
			byte[] initial_hash = BigInteger.valueOf(1695609641).toByteArray();
			tx.addInput(initial_hash, 0);
			byte[] signature = sign(kpair_org[0].getPrivate(), tx.getRawDataToSign(0));
			tx.addSignature(signature, 0);
			tx.finalize();
		}
		//Adding outputs to UTXOpool
		for(int i = 0; i < tx.numOutputs() ; i++){
			TxHandler.pool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
			pool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
			
		}
		System.out.println("++++++COINBASE UTXOPool++++++");
		TxHandler.printPool(0);
		return tx;
	}

	// Returns a sorted list of random indexes 
	public ArrayList<Integer> randIndexes(int tx_num, int num){
		ArrayList<Integer> invalid_ind = new ArrayList<Integer>();
		for(int i = 0, r = -1 ; i < num ; i++){
			while(r == -1 || invalid_ind.contains(r)){
				r = randInt(0, tx_num-1);
			}
			invalid_ind.add(r);
		}
		Collections.sort(invalid_ind);
		return invalid_ind;
	}

	public void t1createTxs(int tx_num, UTXOPool p) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {		
		Transaction tx;
		ArrayList<UTXO> unused_utxo = pool.getAllUTXO();
		Set<UTXO> used_utxo = new HashSet<UTXO>();
		utxo = null;
		int rand = -1;

		System.out.println("Printing POOL within createTxs");
		for (UTXO u : unused_utxo) System.out.println(u);

		//All transactions have UTXO, are valid and simple
		for(int i = 0; i < tx_num ; i++){
			tx = new Transaction ();
			//Get an existing UTXO
			while(utxo == null || used_utxo.contains(utxo)){
				rand = randInt(0, tx_num-1);
				utxo = unused_utxo.get(rand);
			}
			assert(utxo != null && rand != -1) : "UTXO or Rand are null";
			// System.out.println("Adding - random:" + rand + " utxo: " + utxo);
			tx.addInput(utxo.getTxHash(), utxo.getIndex());
			used_utxo.add(utxo);				
			tx.addOutput(randDouble(0, 10), kpair_dest[1].getPublic());
			
			byte[] signature = sign(kpair_dest[0].getPrivate(), tx.getRawDataToSign(0));
			tx.addSignature(signature,0);
			tx.finalize();
			txs_list.add(tx);
		}
	}

	public int t1 (int txs_num)throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {	
		Transaction tx = null;
		pool = new UTXOPool();
		TxHandler txHandler = new TxHandler(pool); 

		createKeys(2); //[0], used for coinbase - [1], for other txs
		tx = createCoinbase(1, txs_num);
		t1createTxs(txs_num, pool);
		printTxList();
		return txHandler.handleTxs(txs_list.toArray(new Transaction[txs_list.size()])).length;
	}

	public void t2createTxs(int tx_num, int tx_invalid, UTXOPool p) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {		
		Transaction tx;
		ArrayList<UTXO> unused_utxo = pool.getAllUTXO();
		Set<UTXO> used_utxo = new HashSet<UTXO>();
		utxo = null;
		int rand = -1;

		// System.out.println("Printing POOL within createTxs");
		// for (UTXO u : unused_utxo) System.out.println(u);

		//Create list of invalid indexes
		ArrayList<Integer> invalid_ind = randIndexes(tx_num, tx_invalid);

		//Simple transactions with [tx_invalid] txs with invalid signature
		for(int i = 0 ; i < tx_num ; i++){
			tx = new Transaction();
			while(utxo == null || used_utxo.contains(utxo)){
				rand = randInt(0, tx_num-1);
				utxo = unused_utxo.get(rand);
			}
			assert(utxo != null && rand != -1): "UTXO or Rand are NULL";
			tx.addInput(utxo.getTxHash(), utxo.getIndex());
			used_utxo.add(utxo);
			tx.addOutput(randDouble(0, 10), kpair_dest[1].getPublic());

			byte[] signature;
			if((invalid_ind.size() > 0) && (invalid_ind.get(0) == i)){
				System.out.println("[X] Turn for invalid TX! -------w00t ind:"+i );
				signature = sign(kpair_org[1].getPrivate(), tx.getRawDataToSign(0));
				invalid_ind.remove(0);
			}else{
				signature = sign(kpair_dest[0].getPrivate(), tx.getRawDataToSign(0));
			}
			tx.addSignature(signature,0);
			tx.finalize();
			txs_list.add(tx);
		}

	}

	public int t2 (int txs_num, int txs_invalid)throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {	
		Transaction tx = null;
		pool = new UTXOPool();
		TxHandler txHandler = new TxHandler(pool); 
		createKeys(2); //[0], used for coinbase - [1], for other txs
		tx = createCoinbase(2,txs_num);
		t2createTxs(txs_num, txs_invalid, pool);
		printTxList();
		return txHandler.handleTxs(txs_list.toArray(new Transaction[txs_list.size()])).length;
	}

	//args[0] = Test number
	//args[1] = Number of transactions 
	public static void main(String[] args) {
		testunit t = new testunit();
		int test_case = Integer.parseInt(args[0]);
		if (test_case == 1 && args.length < 2){
			System.out.println("Usage: [test_num] [txs_num]");
			System.exit(0);
		} else if(test_case == 2 && args.length < 3){
			System.out.println("Usage: [test_num] [txs_num] [txs_invalid]");
			System.exit(0);
		}
		int txs_num = Integer.parseInt(args[1]);
		//Test1 - Simple transaction - 1 kpair, multiple txs
		try{
			if(test_case == 1){
				assert(t.t1(txs_num) == txs_num): "Test1 failed, valid txs not equal to txs_num";
				System.out.println("\n[RESULT] T1 successful with " + txs_num + " valid txs.");
			}
			if(test_case == 2){
				int txs_invalid = Integer.parseInt(args[2]);
				assert(t.t2(txs_num, txs_invalid) == txs_num - txs_invalid): "Test2 failed, valid txs not equal to txs_num";
				System.out.println("\n[RESULT] T2 successful with " + (txs_num - txs_invalid) + " valid txs.");
			}
		}catch(NoSuchAlgorithmException e){
			System.out.println("[Error] - " + e);
			System.exit(0);
		}catch(NoSuchProviderException e){
			System.out.println("[Error] - " + e);
			System.exit(0);
		}catch(InvalidKeyException e){
			System.out.println("[Error] - " + e);
			System.exit(0);
		}catch(SignatureException e){
			System.out.println("[Error] - " + e);
			System.exit(0);
		}
	}
}