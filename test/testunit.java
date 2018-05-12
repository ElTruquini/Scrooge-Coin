import junit.framework.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.io.*;


public class testunit extends TestCase {
	KeyPair[] kpair_arr;
	Transaction[] txs_arr;
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



	public void createKeys(int signers) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		System.out.println("[INFO] testunit - Creating kpairs, signers: " + signers);

		//BouncyCastle required, for installation details, https://justrocketscience.com/post/install-bouncy-castle
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		kpair_arr = new KeyPair[(signers*2)+1]; 

		//Generates keys, even numbers for i are for destination, odds for progon
		for(int i = 0, j = 1 ; i < signers*2 ; i++, j++){
			keyGen.initialize(1024, random); 
			kpair_arr[i] = keyGen.generateKeyPair();
			priv_orig = kpair_arr[i].getPrivate();
			pub_orig = kpair_arr[i].getPublic();
			// System.out.printf("%d: Orig private key: %s\n",i ,printKey(priv_orig.getEncoded()));
			// System.out.printf("%d: Orig public key: %s\n",i ,printKey(pub_orig.getEncoded()));

			kpair_arr[i+1] = keyGen.generateKeyPair();
			priv_dest = kpair_arr[j].getPrivate();
			pub_dest = kpair_arr[j].getPublic(); 
			// System.out.printf("%d: Dest private key: %s\n",i ,printKey(priv_dest.getEncoded()));
			// System.out.printf("%d: Dest public key: %s\n",i ,printKey(pub_dest.getEncoded()));
		}
	}

	public void createTxs(int tx_num){
		txs_arr = new Transaction[tx_num];
		Transaction tx;
		for(int i = 0; i < tx_num ; i++){
			tx = new Transaction();
			System.out.println("[INFO] testunit.createTxs - Creating txs#: " + i);
			
			//Need to add tx.addOutput next...



		}
	}


	//args[0] = Test number
	//args[1] = Number of trasactions 
	public static void main(String[] args) {
		testunit t = new testunit();

		if (args.length < 2){
			System.out.println("Usage: [Program_name] [test_num] [number_of_transactions]");
			System.exit(0);
		}
		int test_case = Integer.parseInt(args[0]);
		int txs_num = Integer.parseInt(args[1]);
		
		//Test1 - Simple transaction - 1 kpair, multiple txs
		try{
			if(test_case == 1){
				t.createKeys(1);
				t.createTxs(txs_num);
			}

			Transaction tx0 = new Transaction();
			UTXOPool utxoPool = new UTXOPool();
			TxHandler txHandler = new TxHandler(utxoPool);

			System.out.println("handleTxs:"+txHandler.handleTxs(new Transaction[]{tx0}).length);

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