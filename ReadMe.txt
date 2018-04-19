
Program implements transaction validation of inputs and outputs (similar structure to Bitcoin transactions), managing UTXO pool sets and simulating validation that nodes complete when they recieve a new transaction.

Required libraries:

	Apache commons codec (commons-codec-1.11.jar)
	Bouncy Castle (bcprov-jdk15on-156.jar)

To compile:

	javac -cp bcprov-jdk15on-156 -cp commons-codec-1.11 *.java

To run:

	java Scrooge_main