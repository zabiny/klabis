package com.dpolach.inmemoryrepository;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

    private final Map<String, InMemoryEntityStore> transactionBackups = new ConcurrentHashMap<>();
    private final InMemoryEntityStore store;

    public InMemoryTransactionManager(InMemoryEntityStore entityStore) {
        this.store = entityStore;
        setNestedTransactionAllowed(false);
    }

    @Override
    protected Object doGetTransaction() throws TransactionException {
        InMemoryTransactionObject txObject = new InMemoryTransactionObject();
        txObject.setEntityStore(store);
        return txObject;
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        InMemoryTransactionObject txObject = (InMemoryTransactionObject) transaction;

        // Vygenerujeme unikátní ID pro transakci
        String txId = "tx-" + System.nanoTime();
        txObject.setTransactionId(txId);

        // Vytvoříme zálohu současného stavu před zahájením transakce
        transactionBackups.put(txId, store.backupClone());

        logger.debug("Started transaction " + txId);
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        InMemoryTransactionObject txObject = (InMemoryTransactionObject) status.getTransaction();
        String txId = txObject.getTransactionId();

        // Při commitu můžeme odstranit zálohu
        transactionBackups.remove(txId);

        logger.debug("Committed transaction " + txId);
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        InMemoryTransactionObject txObject = (InMemoryTransactionObject) status.getTransaction();
        String txId = txObject.getTransactionId();

        // Obnovíme data ze zálohy
        InMemoryEntityStore backupStore = transactionBackups.get(txId);
        if (backupStore != null) {
            store.restoreFromClone(backupStore);
        }
        transactionBackups.remove(txId);

        logger.debug("Rolled back transaction " + txId);
    }

    // Třída reprezentující transakci
    private static class InMemoryTransactionObject implements SmartTransactionObject {
        private String transactionId;
        private InMemoryEntityStore entityStore;

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public InMemoryEntityStore getEntityStore() {
            return entityStore;
        }

        public void setEntityStore(InMemoryEntityStore entityStore) {
            this.entityStore = entityStore;
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public void flush() {
            // Implementujte, pokud váš repositář podporuje flush operaci
        }
    }
}