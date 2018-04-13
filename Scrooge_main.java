//Date: April, 2018

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

		//Generating P. Wuille keys
		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey priv_Wuille = pair.getPrivate();
		PublicKey pub_Wuille = pair.getPublic();
		System.out.printf("Wuillie private key: %s\n",printKey(priv_Wuille.getEncoded()));
		System.out.printf("Wuillie public key: %s\n",printKey(pub_Wuille.getEncoded()));
		System.out.println();

		//Generating Hodler1 keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Hodler1 = pair.getPrivate();
		PublicKey pub_Hodler1 = pair.getPublic();
		System.out.printf("Hodler1 private key: %s\n",printKey(priv_Hodler1.getEncoded()));
		System.out.printf("Hodler1 public key: %s\n",printKey(pub_Hodler1.getEncoded()));

		//Generating Hodler2 keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Hodler2 = pair.getPrivate();
		PublicKey pub_Hodler2 = pair.getPublic();
		System.out.printf("Hodler2 private key: %s\n",printKey(priv_Hodler2.getEncoded()));
		System.out.printf("Hodler2 public key: %s\n",printKey(pub_Hodler2.getEncoded()));

		//START - Genesis tx
		Transaction tx = new Transaction();
		tx.addOutput(10, pub_Wuille);
		byte[] initial_hash = BigInteger.valueOf(1695609641).toByteArray();
		tx.addInput(initial_hash, 0);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(priv_Wuille);
		signature.update(tx.getRawDataToSign(0));
		byte[] sig = signature.sign();
		tx.addSignature(sig, 0);
		tx.finalize();
		//END - Genesis tx

		//Adding genesis output to the UTXPool
		UTXOPool utxoPool = new UTXOPool();
		UTXO utxo = new UTXO(tx.getHash(),0);
		utxoPool.addUTXO(utxo, tx.getOutput(0));


		//START - Testing txs
		Transaction tx2 = new Transaction();
		tx.addInput(tx.getHash(),0);

		tx2.addOutput(5, pub_Hodler1);
		tx2.addOutput(5, pub_Hodler2);

		String result = bitcoinAPI("getnewaddress");
		System.out.println("Result getnew address: "+ result);


		String waka = "dumpprivkey " + result;
		String result2 = bitcoinAPI(waka);
		System.out.println("dumpprivkey: "+result2);



	}
}

