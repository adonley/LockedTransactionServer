package network.bitmesh.cloudserver.Bitcoin;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.SecureRandom;
import java.util.*;

public class LockedTransactionBroadcasterTimer extends Timer
{
    private static final Logger log = LoggerFactory.getLogger(LockedTransactionBroadcasterTimer.class);

    private static LockedTransactionBroadcasterTimer lockedTransactionBroadcasterTimer = null;

    private LockedTransactionBroadcasterTimer()
    {
        super();
    }

    public static LockedTransactionBroadcasterTimer getInstance()
    {
        if(lockedTransactionBroadcasterTimer == null)
        {
            lockedTransactionBroadcasterTimer = new LockedTransactionBroadcasterTimer();
        }
        return lockedTransactionBroadcasterTimer;
    }

    public synchronized void scheduleTransactionBroadcast(@Nonnull Transaction transaction)
    {
        long lockTime = transaction.getLockTime();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date date = cal.getTime();
        long now = date.getTime() / 1000L;
        log.info("Transaction TX: " + transaction.getHashAsString() + ": lock - now = " + (lockTime - now) + " seconds.");
        if(!transaction.isTimeLocked())
        {
            log.info("Broadcasting TX: " + transaction.getHashAsString() + " now.");
            broadcastTransaction(transaction);
        }
        else
        {
            log.info("Scheduling TX: " + transaction.getHashAsString() + " at " + new Date((lockTime+30)*1000L).toString());
            // Schedule the transaction in the future plus 30 seconds after lock (for potential variance?)
            // TODO: Add a function to write tx to disk / memory in case of failure
            schedule(new BroadcastTxTask(transaction), (lockTime - now + 30)*1000L);
        }
    }

    public static void broadcastTransaction(@Nonnull final Transaction transaction)
    {
        // Re-seed the peer selection
        TransactionBroadcast.random = new SecureRandom();

        TransactionBroadcast broadcast;

        // Broadcast on appropriate network
        if(transaction.getParams() == MainNetParams.get())
            broadcast = new TransactionBroadcast(WalletRunnable.getPeergroup(), transaction);
        else
            broadcast = new TransactionBroadcast(WalletRunnable.getTestPeergroup(), transaction);

        ListenableFuture<Transaction> listenable = broadcast.broadcast();

        Futures.addCallback(listenable, new FutureCallback<Transaction>()
        {
            // This doesn't mean that 
            public void onSuccess(Transaction result)
            {
                log.info("Broadcast successful");
            }

            public void onFailure(Throwable t)
            {
                log.info("Broadcast of transaction failed.\n" + t.toString());
            }

        });
    }

    private class BroadcastTxTask extends TimerTask
    {
        private Transaction transaction;

        public BroadcastTxTask(@Nonnull Transaction transaction)
        {
            this.transaction = transaction;
        }

        @Override
        public void run()
        {
            log.info("Broadcasting transaction from timertask: " + transaction.toString() + " at "
                    + Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().toString());
            broadcastTransaction(transaction);
        }
    }
}
