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

		//BouncyCastly required, for installation details, https://justrocketscience.com/post/install-bouncy-castle
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
		
		//Generating Sodler keys
		pair = keyGen.generateKeyPair();
		PrivateKey priv_Sodler = pair.getPrivate();
		PublicKey pub_Sodler = pair.getPublic();
		System.out.printf("Sodler private key: %s\n",printKey(priv_Sodler.getEncoded()));
		System.out.printf("Sodler public key: %s\n",printKey(pub_Sodler.getEncoded()));
	}	

}

