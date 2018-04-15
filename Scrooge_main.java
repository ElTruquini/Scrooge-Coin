//Date: April, 2018

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.Base64;
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

		//BouncyCastle required, for installation details, https://justrocketscience.com/post/install-bouncy-castle
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyGen.initialize(1024, random);	

		// Getting private and public keys from bitcoin API
		// String result = bitcoinAPI("getnewaddress");
		// System.out.println("Result getnew address: "+ result);
		// String waka = "dumpprivkey " + result;
		// String result2 = bitcoinAPI(waka);
		// System.out.println("dumpprivkey: "+result2);

		//Generating P. Wuille keys (coinbase transaction)
		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey priv_Wuille = pair.getPrivate();
		PublicKey pub_Wuille = pair.getPublic();
		// System.out.printf("Wuillie private key: %s\n",printKey(priv_Wuille.getEncoded()));
		// System.out.printf("Wuillie public key: %s\n",printKey(pub_Wuille.getEncoded()));

		//Generating Hodler1 keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Hodler1 = pair.getPrivate();
		PublicKey pub_Hodler1 = pair.getPublic();
		// System.out.printf("Hodler1 private key: %s\n",printKey(priv_Hodler1.getEncoded()));
		// System.out.printf("Hodler1 public key: %s\n",printKey(pub_Hodler1.getEncoded()));

		//Generating Hodler2 keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Hodler2 = pair.getPrivate();
		PublicKey pub_Hodler2 = pair.getPublic();
		// System.out.printf("Hodler2 private key: %s\n",printKey(priv_Hodler2.getEncoded()));
		// System.out.printf("Hodler2 public key: %s\n",printKey(pub_Hodler2.getEncoded()));


		UTXOPool utxoPool = new UTXOPool();

		//START - tx, Coinbase tx, included in UTXOPool
		Transaction tx = new Transaction();
		tx.addOutput(10, pub_Wuille);
		byte[] initial_hash = BigInteger.valueOf(1695609641).toByteArray();
		tx.addInput(initial_hash, 0);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(priv_Wuille);
		signature.update(tx.getRawDataToSign(0));
		byte[] sig_bytes = signature.sign();
		tx.addSignature(sig_bytes, 0);
		tx.finalize();
		UTXO utxo = new UTXO(tx.getHash(),0);
		utxoPool.addUTXO(utxo, tx.getOutput(0));
		//END - Coinbase tx


		//START - tx2 (valid), coinbase input, two valid outputs
		Transaction tx2 = new Transaction();
		tx2.addInput(tx.getHash(),0);
		signature.initSign(priv_Wuille);
		signature.update(tx2.getRawDataToSign(0));
		sig_bytes = signature.sign();
		tx2.addSignature(sig_bytes, 0);
		tx2.addOutput(5, pub_Hodler1);
		tx2.addOutput(5, pub_Hodler2);
		tx2.finalize();
		utxo = new UTXO(tx2.getHash(),0);
		utxoPool.addUTXO(utxo, tx2.getOutput(0));
		utxoPool.addUTXO(utxo, tx2.getOutput(1));

		//END - tx2

		//if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), in.signature))
		System.out.println("Pub P.Wuillie:" + pub_Wuille + "\n");
		System.out.println("pub key - tx.getOutput(0).address:" + tx.getOutput(0).address + "\n");
		System.out.println("message - tx2.getRawDataToSign(0):" + tx2.getRawDataToSign(0) + "\n");
		System.out.println("sig - tx2.getInput(0).signature:" + Hex.toHexString(tx2.getInput(0).signature) + "\n");
		System.out.println("sig_bytes:" + Hex.toHexString(sig_bytes) + "\n");
		System.out.println("Tx2 - sig valid:" + Crypto.verifySignature(tx.getOutput(0).address,tx2.getRawDataToSign(0), tx2.getInput(0).signature));
		
		TxHandler txHandler = new TxHandler(utxoPool);
		System.out.println("[FINAL] Tx2.isValidTx: "+ txHandler.isValidTx(tx2));
		System.exit(0);

		// //START - tx3 (invalid), coinbase input, output higher value than input
		// Transaction tx3 = new Transaction();
		// tx3.addInput(tx.getHash(),0);
		// signature.initSign(priv_Wuille);
		// signature.update(tx3.getRawDataToSign(0));
		// sig_bytes = signature.sign();
		// tx3.addSignature(sig_bytes, 0);
		// tx3.addOutput(10.1, pub_Hodler1);
		// tx3.finalize();
		// utxo = new UTXO(tx3.getHash(),0);
		// utxoPool.addUTXO(utxo, tx3.getOutput(0));
		// utxo = new UTXO(tx3.getHash(),1);
		// utxoPool.addUTXO(utxo, tx3.getOutput(1));
		// //END - tx3


		// //START - tx4, Coinbase tx, NOT part of UTXOpool
		// Transaction tx4 = new Transaction();
		// tx4.addInput(initial_hash, 0);
		// signature = Signature.getInstance("SHA256withRSA");
		// signature.initSign(priv_Wuille);
		// signature.update(tx4.getRawDataToSign(0));
		// sig_bytes = signature.sign();
		// tx4.addSignature(sig_bytes, 0);
		// tx4.addOutput(20, pub_Wuille);
		// tx4.finalize();
		// //END - tx4


		// //START - tx5 (invalid), one input (not part of UTXOPool) referencing tx4 
		// Transaction tx5 = new Transaction();
		// tx5.addInput(tx4.getHash(),0);
		// signature.initSign(priv_Wuille);
		// signature.update(tx5.getRawDataToSign(0));
		// sig_bytes = signature.sign();
		// tx5.addSignature(sig_bytes, 0);
		// tx5.addOutput(5, pub_Hodler1);
		// tx5.addOutput(5, pub_Hodler2);
		// tx5.finalize();
		// utxo = new UTXO(tx5.getHash(),0);
		// utxoPool.addUTXO(utxo, tx5.getOutput(0));
		// //END - tx5


		// //START - tx6 (invalid), two inputs, one is part of UTXOPool the other one is not
		// Transaction tx6 = new Transaction();
		// tx6.addInput(tx.getHash(),0); //in UTXO
		// tx6.addInput(tx4.getHash(),0);	//not in UTXO
		// signature.initSign(priv_Wuille);
		// signature.update(tx6.getRawDataToSign(0));
		// sig_bytes = signature.sign();
		// tx6.addSignature(sig_bytes, 0);
		// tx6.addOutput(30, pub_Hodler1);
		// tx6.finalize();
		// utxo = new UTXO(tx6.getHash(),0);
		// utxoPool.addUTXO(utxo, tx6.getOutput(0));
		// //END - tx6




		// TxHandler txHandler = new TxHandler(utxoPool);
		// System.out.println("[FINAL] Tx2.isValidTx: "+ txHandler.isValidTx(tx2));
		// System.out.println("[FINAL] Tx3.isValidTx: "+ txHandler.isValidTx(tx3));
		// System.out.println("[FINAL] Tx5.isValidTx: "+ txHandler.isValidTx(tx5));
		// System.out.println("[FINAL] Tx6.isValidTx: "+ txHandler.isValidTx(tx6));

		// System.out.print("handleTxs:"+txHandler.handleTxs(new Transaction[]{tx2}).length);







	}
}

