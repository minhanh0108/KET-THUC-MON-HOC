import java.security.Security;
import java.util.ArrayList;
//import java.util.Base64;
import java.util.HashMap;
import com.google.gson.GsonBuilder;
import java.util.Map;
import java.util.Scanner;

public class CHM_BlockChain {

    public static ArrayList<VNPT_Minh> blockchain = new ArrayList<VNPT_Minh>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Store storeA; //Ví A
    public static Store storeB; //Ví B
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider

        //Create Stores:
        storeA = new Store();
        storeB = new Store();
        Store storebase = new Store();

        Scanner stock = new Scanner(System.in);
        System.out.println("Nhập số lượng điện thoại trong kho A: ");  
        int stockA = stock.nextInt();

        System.out.println("Nhập số lượng điện thoại trong kho B: ");  
        int stockB = stock.nextInt();

        System.out.println("Nhập số lượng chuyển từ kho A sang kho B: ");  
        int sl = stock.nextInt();
        
        
        
        while (sl > stockA)
        {   System.out.println("Kho A không đủ số lượng điện thoại để chuyển: ");
            System.out.println("Nhập số lượng chuyển từ kho A sang kho B: ");  
            sl = stock.nextInt();
        }
        stock.close();
      
        genesisTransaction = new Transaction(storebase.publicKey, storeA.publicKey, stockA, null);
        genesisTransaction.generateSignature(storebase.privateKey);	 //Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        
        
         Transaction genesisTransaction2 = new Transaction(storebase.publicKey, storeB.publicKey, stockB, null);
         genesisTransaction2.generateSignature(storebase.privateKey);	 //Gán private key (ký thủ công) vào giao dịch gốc
         genesisTransaction2.transactionId = "0"; //Gán ID cho giao dịch gốc
         genesisTransaction2.outputs.add(new TransactionOutput(genesisTransaction2.reciepient, genesisTransaction2.value, genesisTransaction2.transactionId)); //Thêm Transactions Output
         UTXOs.put(genesisTransaction2.outputs.get(0).id, genesisTransaction2.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Đang tạo và đào khối gốc .... ");
        VNPT_Minh genesis = new VNPT_Minh("0");
        genesis.addTransaction(genesisTransaction);
        genesis.addTransaction(genesisTransaction2);
        addBlock(genesis);
  
        VNPT_Minh block1 = new VNPT_Minh(genesis.hash);
        System.out.println("\nSố lượng điện thoại trong kho A là : " + storeA.getBalance());
        System.out.println("\nSố lượng điện thoại trong kho B là : " + storeB.getBalance());
        System.out.println("\nGiao dịch chuyển " + sl + " điện thoại từ kho A sang kho B...");
        block1.addTransaction(storeA.sendFunds(storeB.publicKey, sl));
        addBlock(block1);
        System.out.println("\nSố dư mới của ví A là : " + storeA.getBalance());
        System.out.println("Số dư của ví B là : " + storeB.getBalance());
        isChainValid();

    }

    public static Boolean isChainValid() {
        VNPT_Minh currentBlock;
        VNPT_Minh previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }

            }

        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_Minh newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}
