//Date: April, 2018

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;

public class Scrooge_main{


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

	//Uses bitcoin client API calls to generate private and public keys for transactions
	public static String bitcoinAPI(String cmd){
		try {
			// Execute command
			String command = "/opt/bitcoin-0.16.0/bin/bitcoin-cli ";
			command += cmd;
			System.out.println("Final command:"+ command);
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			String output = "";

			while ((line = buf.readLine()) != null) {
				output += line + "\n";
			}
			return output;
		} catch (InterruptedException e) {
			System.out.println(e);
		
		} catch (IOException e) {
			System.out.println(e);
		}
		return null;
	}

	public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		UTXOPool utxoPool = new UTXOPool();
		
		//BouncyCastle required, for installation details, https://justrocketscience.com/post/install-bouncy-castle
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyGen.initialize(1024, random);	

		// //Getting private and public keys from bitcoin API
		// String result = bitcoinAPI("getnewaddress");
		// System.out.println("Result getnew address: "+ result);
		// String waka = "dumpprivkey " + result;
		// String result2 = bitcoinAPI(waka);
		// System.out.println("dumpprivkey: "+result2);


		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey privk_scrooge = pair.getPrivate();
		PublicKey pubk_scrooge = pair.getPublic();
		// // System.out.printf("Scrooge private key: %s\n",printKey(priv_Scrooge.getEncoded()));
		// // System.out.printf("Scrooge public key: %s\n",printKey(pub_Scrooge.getEncoded()));
		pair = keyGen.generateKeyPair();
		PrivateKey privk_alice = pair.getPrivate();
		PublicKey pubk_alice = pair.getPublic();


		//START - tx, Generating Coinbase tx, included in UTXOPool, signed with Scrooge keys.
		Transaction tx = new Transaction();
		tx.addOutput(10, pubk_scrooge);
		tx.addOutput(1, pubk_scrooge);

		// using random place holder value.
		byte[] initial_hash = BigInteger.valueOf(1695609641).toByteArray();
		tx.addInput(initial_hash, 0);
		initial_hash = BigInteger.valueOf(1000000000).toByteArray();
		tx.addInput(initial_hash, 1);

		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privk_scrooge);
		signature.update(tx.getRawDataToSign(0));
		byte[] sig_bytes = signature.sign();
		
		tx.addSignature(sig_bytes, 0);
		tx.finalize();

		// adding coinbase tx to unspent outputs
		UTXO utxo = new UTXO(tx.getHash(),0);
		utxoPool.addUTXO(utxo, tx.getOutput(0));
		utxo = new UTXO(tx.getHash(),1);
		utxoPool.addUTXO(utxo, tx.getOutput(1));
		// //END - Coinbase tx


		// START - tx2 (valid), tx coinbase input, two valid outputs
		Transaction tx2 = new Transaction();
		tx2.addInput(tx.getHash(),1);
		tx2.addOutput(5, pubk_alice);
		tx2.addOutput(3, pubk_alice);
		tx2.addOutput(2, pubk_alice);

		// has only 1 input to sign
		signature.initSign(privk_scrooge); 
		signature.update(tx2.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx2.addSignature(sig_bytes, 0);
		tx2.finalize();
		// END - tx2


		// START - tx3 (invalid), coinbase input, output higher value than input
		Transaction tx3 = new Transaction();
		tx3.addInput(tx.getHash(),1);
		tx3.addOutput(0.5, pubk_alice);
		tx3.addOutput(0.5, pubk_alice);
		tx3.addOutput(0.5, pubk_alice);	// this output exceeds input sum

		signature.initSign(privk_scrooge);
		signature.update(tx3.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx3.addSignature(sig_bytes, 0);
		tx3.finalize();
		// END - tx3


		// START - tx33 (valid), coinbase input
		Transaction tx33 = new Transaction();
		tx33.addInput(tx.getHash(),0); //10.0
		tx33.addOutput(0.5, pubk_alice);
		tx33.addOutput(0.5, pubk_alice);

		signature.initSign(privk_scrooge);
		signature.update(tx33.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx33.addSignature(sig_bytes, 0);
		tx33.finalize();
		// END - tx33


//TODO: ADD ADDITIONAL TRANSACTIONS TO TEST COMPARE, SORT ARRAY METHODS.
		

		// START - tx4, Coinbase input, NOT part of UTXOpool
		Transaction tx4 = new Transaction();
		tx4.addInput(initial_hash, 0);
		tx4.addOutput(20, pubk_alice);

		signature.initSign(privk_scrooge);
		signature.update(tx4.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx4.addSignature(sig_bytes, 0);
		tx4.finalize();
		// END - tx4


		// START - tx5 (invalid), Tx4 input (not part of UTXOPool) 
		Transaction tx5 = new Transaction();
		tx5.addInput(tx4.getHash(),0);
		tx5.addOutput(5, pubk_alice);

		signature.initSign(privk_scrooge);
		signature.update(tx5.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx5.addSignature(sig_bytes, 0);
		tx5.finalize();
		// END - tx5


		// START - tx6 (invalid), two inputs, one is part of UTXOPool the other one is not
		Transaction tx6 = new Transaction();
		tx6.addInput(tx.getHash(),0); 	// in UTXO
		tx6.addInput(tx4.getHash(),0);	// NOT in UTXO
		tx6.addOutput(30, pubk_alice);

		signature.initSign(privk_scrooge);
		signature.update(tx6.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx6.addSignature(sig_bytes, 0);
		tx6.finalize();
		// END - tx6
		// System.out.println("[MAIN] Tx6 - sig valid:" + Crypto.verifySignature(pubk_scrooge, tx6.getRawDataToSign(0), tx6.getInput(0).signature));


		// START - tx7 (invalid), invalid signature
		Transaction tx7 = new Transaction();
		tx7.addInput(tx.getHash(),0);
		tx7.addOutput(10, pubk_alice); 

		signature.initSign(privk_alice);	// scrooge should be the signer instead
		signature.update(tx6.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx7.addSignature(sig_bytes, 0);
		tx7.finalize();
		// END - tx7


		// Start - tx8 (invalid), UTXO claimed multiple times
		Transaction tx8 = new Transaction();
		tx8.addInput(tx.getHash(),0);
		tx8.addInput(tx.getHash(),0);	// duplicate UTXO
		tx7.addOutput(10, pubk_alice); 

		signature.initSign(privk_scrooge);	// scrooge should be the signer instead
		signature.update(tx8.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx8.addSignature(sig_bytes, 0);
		tx8.addSignature(sig_bytes, 1);
		tx8.finalize();
		// END - tx8


		// START - tx9 (invalid), negative output
		Transaction tx9 = new Transaction();
		tx9.addInput(tx.getHash(),0);
		tx9.addOutput(5, pubk_alice);
		tx9.addOutput(-3, pubk_alice);
		tx9.addOutput(2, pubk_alice);

		// has only 1 input to sign
		signature.initSign(privk_scrooge); 
		signature.update(tx9.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx9.addSignature(sig_bytes, 0);
		tx9.finalize();
		// END - tx9


		TxHandler txHandler = new TxHandler(utxoPool);
		System.out.println("handleTxs:"+txHandler.handleTxs(new Transaction[]{tx33}).length);
		MaxFeeHandler maxFeeHandler = new MaxFeeHandler();
		maxFeeHandler.handleTxs(new Transaction[]{tx33});
	}
}

