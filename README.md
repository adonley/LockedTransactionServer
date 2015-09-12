LockedTransactionServer
---------------
A Bitcoin transaction server that broadcast serialzed transactions after nLocktime expires. Works for both testnet and mainnet (and also for transactions that do not contain a locktime). Powered by bitcoinj.

Server Requirements:
bitcoinj needs a place to write wallet files. You have to enable Tomcat (or your servlet container) to write to a directory in order to do so. The following stackoverflow link contains information on how to do so for Tomcat: 
https://stackoverflow.com/questions/5115339/tomcat-opts-environment-variable-and-system-getenv
