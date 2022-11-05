public class TransactionInput {
    public String transactionOutputId;
    public TransactionOutput UTXO; // Unspend Transaction Output

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}