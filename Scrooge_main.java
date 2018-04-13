//Date: April, 2018

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.math.BigInteger;
import java.security.*;
import java.util.Base64;

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

		//Generating Hodler keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Sodler = pair.getPrivate();
		PublicKey pub_Sodler = pair.getPublic();
		System.out.printf("Hodler private key: %s\n",printKey(priv_Sodler.getEncoded()));
		System.out.printf("Hodler public key: %s\n",printKey(pub_Sodler.getEncoded()));

		//Creating genesis tx
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

		//Adding genesis output to the UTXPool
		UTXOPool utxoPool = new UTXOPool();
		UTXO utxo = new UTXO(tx.getHash(),0);
		utxoPool.addUTXO(utxo, tx.getOutput(0));

		
	}		

}

