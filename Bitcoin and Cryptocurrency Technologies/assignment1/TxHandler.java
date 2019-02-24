import java.util.HashSet;

public class TxHandler {
    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        pool = new UTXOPool(utxoPool);
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
        // IMPLEMENT THIS
        double totalInput = 0;
        HashSet<UTXO> used = new HashSet<>();

        for (int i = 0; i < tx.numInputs(); ++i) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (!pool.contains(utxo)) {
                return false; // check 1
            }

            Transaction.Output output = pool.getTxOutput(utxo);
            totalInput += output.value; // for check 5
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false; // check 2
            }

            if (!used.add(utxo)) {
                return false; // check 3
            }
        }

        // sum output in this tx
        double totalOutput = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0 ) {
                return false; // check 4
            }

            totalOutput += output.value;
        }

        if (totalInput < totalOutput) { return false; } // check 5

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        HashSet<Transaction> visited = new HashSet<>();

        // there must be one tx is valid in every iteration
        while(true) {
            boolean updated = false;
            for (Transaction tx : possibleTxs) {
                if (visited.contains(tx)) {
                    continue;
                }

                if (isValidTx(tx)) {
                    visited.add(tx);
                    updated = true;

                    // add unspent coin and delete used coin, update utxo pool
                    for (int i = 0; i < tx.numOutputs(); ++i) {
                        UTXO utxo = new UTXO(tx.getHash(), i);
                        pool.addUTXO(utxo, tx.getOutput(i));
                    }

                    for (int i = 0; i < tx.numInputs(); ++i) {
                        Transaction.Input input = tx.getInput(i);
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        pool.removeUTXO(utxo);
                    }
                }
            }

            if (!updated) {
                break;
            }
        }

        Transaction[] res = new Transaction[visited.size()];
        return visited.toArray(res);
    }

}
