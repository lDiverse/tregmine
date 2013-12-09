package info.tregmine.api;

import java.util.Random;

/**
 * Represents a bank account holding information as to
 * how much a player currently has in a certain bank.
 * @author Robert Catron
 * @since 12/7/2013
 */
public class Account
{
    
    private Bank bank;
    private int id;
    private String player;
    private long balance;
    
    private int account_number;

    public Account()
    {
        Random r = new Random(9);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 12; i++){
            sb.append(r.nextInt());
        }
        try{
            account_number = Integer.parseInt(sb.toString().trim());
        }catch(NumberFormatException e){
            
        }
    }
    
    public Bank getBank(){ return bank; }
    public void setBank(Bank bank){ this.bank = bank; }
    
    public int getId(){ return id; }
    public void setId(int id){ this.id = id; }
    
    public String getPlayer(){ return player; }
    public void setPlayer(String player){ this.player = player; }
    
    public long getBalance(){ return balance; }
    public void setBalance(long balance){ this.balance = balance; }

    public int getAccountNumber(){ return account_number; }
    public void setAccountNumber(int account_number) { this.account_number = account_number; }
}
