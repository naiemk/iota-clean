package com.iota.iri.controllers;

import com.google.gson.Gson;
import com.iota.iri.model.*;
import com.iota.iri.storage.Indexable;
import com.iota.iri.storage.Persistable;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.Pair;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class TransactionViewModel {

    private final com.iota.iri.model.Transaction transaction;

    public static final long SUPPLY = 2779530283277761L; // = (3^33 - 1) / 2


    private ApproveeViewModel approovers;
    private TransactionViewModel trunk;
    private TransactionViewModel branch;
    private final Hash hash;


    public final static int PREFILLED_SLOT = 1; // means that we know only hash of the tx, the rest is unknown yet: only another tx references that hash
    public final static int FILLED_SLOT = -1; //  knows the hash only coz another tx references that hash

    public int weightMagnitude;

    private static void fillMetadata(final Tangle tangle, TransactionViewModel transactionViewModel) throws Exception {
        if (transactionViewModel.getHash().equals(Hash.NULL_HASH)) { return; }
        if(transactionViewModel.getType() == FILLED_SLOT && !transactionViewModel.transaction.parsed) {
            tangle.saveBatch(transactionViewModel.getMetadataSaveBatch());
        }
    }

    public static TransactionViewModel find(final Tangle tangle, byte[] hash) throws Exception {
        TransactionViewModel transactionViewModel = new TransactionViewModel((Transaction) tangle.find(Transaction.class, hash), new Hash(hash));
        fillMetadata(tangle, transactionViewModel);
        return transactionViewModel;
    }

    public static TransactionViewModel fromHash(final Tangle tangle, final Hash hash) throws Exception {
        TransactionViewModel transactionViewModel = new TransactionViewModel((Transaction) tangle.load(Transaction.class, hash), hash);
        fillMetadata(tangle, transactionViewModel);
        return transactionViewModel;
    }

    public TransactionViewModel(final Transaction transaction, final Hash hash) {
        this.transaction = transaction == null || transaction.bytes == null ? new Transaction(): transaction;
        this.hash = hash == null? Hash.NULL_HASH: hash;

        // TODO: Figure out a different PoW mechanism that this
        weightMagnitude = 0;
    }

    public TransactionViewModel(final byte[] bytes, Hash hash) throws RuntimeException {
        // TODO: Temporarily use a string and JSON for serde
        String str = new String(bytes, StandardCharsets.UTF_8);
        transaction = new Gson().fromJson(str, Transaction.class);
        this.hash = hash;

        // TODO: Figure out a different PoW mechanism that this
        weightMagnitude = 0;
        transaction.type = FILLED_SLOT;
        transaction.bytes = bytes;
    }

    public static int getNumberOfStoredTransactions(final Tangle tangle) throws Exception {
        return tangle.getCount(Transaction.class).intValue();
    }

    public boolean update(final Tangle tangle, String item) throws Exception {
        getAddressHash();
        getTrunkTransactionHash();
        getBranchTransactionHash();
        getBundleHash();
        getTagValue();
        getObsoleteTagValue();
        setMetadata();
        if(hash.equals(Hash.NULL_HASH)) {
            return false;
        }
        return tangle.update(transaction, hash, item);
    }

    public TransactionViewModel getBranchTransaction(final Tangle tangle) throws Exception {
        if(branch == null) {
            branch = TransactionViewModel.fromHash(tangle, getBranchTransactionHash());
        }
        return branch;
    }

    public TransactionViewModel getTrunkTransaction(final Tangle tangle) throws Exception {
        if(trunk == null) {
            trunk = TransactionViewModel.fromHash(tangle, getTrunkTransactionHash());
        }
        return trunk;
    }

    public void delete(Tangle tangle) throws Exception {
        tangle.delete(Transaction.class, hash);
    }

    private List<Pair<Indexable, Persistable>> getMetadataSaveBatch() {
        List<Pair<Indexable, Persistable>> hashesList = new ArrayList<>();
        hashesList.add(new Pair<>(getAddressHash(), new Address(hash)));
        hashesList.add(new Pair<>(getBundleHash(), new Bundle(hash)));
        hashesList.add(new Pair<>(getBranchTransactionHash(), new Approvee(hash)));
        hashesList.add(new Pair<>(getTrunkTransactionHash(), new Approvee(hash)));
        hashesList.add(new Pair<>(getObsoleteTagValue(), new Tag(hash)));
        setMetadata();
        return hashesList;
    }

    public List<Pair<Indexable, Persistable>> getSaveBatch() {
        List<Pair<Indexable, Persistable>> hashesList = new ArrayList<>(getMetadataSaveBatch());
        getBytes();
        hashesList.add(new Pair<>(hash, transaction));
        return hashesList;
    }


    public static TransactionViewModel first(Tangle tangle) throws Exception {
        Pair<Indexable, Persistable> transactionPair = tangle.getFirst(Transaction.class, Hash.class);
        if(transactionPair != null && transactionPair.hi != null) {
            return new TransactionViewModel((Transaction) transactionPair.hi, (Hash) transactionPair.low);
        }
        return null;
    }

    public TransactionViewModel next(Tangle tangle) throws Exception {
        Pair<Indexable, Persistable> transactionPair = tangle.next(Transaction.class, hash);
        if(transactionPair != null && transactionPair.hi != null) {
            return new TransactionViewModel((Transaction) transactionPair.hi, (Hash) transactionPair.low);
        }
        return null;
    }

    public boolean store(Tangle tangle) throws Exception {
        if (hash.equals(Hash.NULL_HASH) || exists(tangle, hash)) {
            return false;
        }

        List<Pair<Indexable, Persistable>> batch = getSaveBatch();
        if (exists(tangle, hash)) {
            return false;
        }
        return tangle.saveBatch(batch);
    }

    public ApproveeViewModel getApprovers(Tangle tangle) throws Exception {
        if(approovers == null) {
            approovers = ApproveeViewModel.load(tangle, hash);
        }
        return approovers;
    }

    public final int getType() {
        return transaction.type;
    }

    public void setArrivalTime(long time) {
        transaction.arrivalTime = time;
    }

    public long getArrivalTime() {
        return transaction.arrivalTime;
    }

    public byte[] getBytes() {
        return transaction.bytes;
    }

    public Hash getHash() {
        return hash;
    }

    public Hash getAddressHash() {
        return transaction.address;
    }

    public Hash getObsoleteTagValue() {
        return transaction.obsoleteTag;
    }

    public Hash getBundleHash() {
        return transaction.bundle;
    }

    public Hash getTrunkTransactionHash() {
        return transaction.trunk;
    }

    public Hash getBranchTransactionHash() {
        return transaction.branch;
    }

    public Hash getTagValue() {
        return transaction.tag;
    }

    public long getAttachmentTimestamp() { return transaction.attachmentTimestamp; }


    public long value() {
        return transaction.value;
    }

    public void setValidity(final Tangle tangle, int validity) throws Exception {
        transaction.validity = validity;
        update(tangle, "validity");
    }

    public int getValidity() {
        return transaction.validity;
    }

    public long getCurrentIndex() {
        return transaction.currentIndex;
    }

    public long getTimestamp() {
        return transaction.timestamp;
    }

    public long lastIndex() {
        return transaction.lastIndex;
    }

    public void setMetadata() {
        transaction.type = transaction.bytes == null ? TransactionViewModel.PREFILLED_SLOT : TransactionViewModel.FILLED_SLOT;
    }

    public static boolean exists(Tangle tangle, Hash hash) throws Exception {
        return tangle.exists(Transaction.class, hash);
    }

    public static void updateSolidTransactions(final Tangle tangle, final Set<Hash> analyzedHashes) throws Exception {
        Iterator<Hash> hashIterator = analyzedHashes.iterator();
        TransactionViewModel transactionViewModel;
        while(hashIterator.hasNext()) {
            transactionViewModel = TransactionViewModel.fromHash(tangle, hashIterator.next());
            transactionViewModel.updateHeights(tangle);
            transactionViewModel.updateSolid(true);
            transactionViewModel.update(tangle, "solid|height");
        }
    }

    public void updateSolid(boolean solid) {
        if(solid != transaction.solid) {
            transaction.solid = solid;
        }
    }

    public boolean isSolid() {
        return transaction.solid;
    }

    public int snapshotIndex() {
        return transaction.snapshot;
    }

    public void setSnapshot(final Tangle tangle, final int index) throws Exception {
        if ( index != transaction.snapshot ) {
            transaction.snapshot = index;
            update(tangle, "snapshot");
        }
    }

    public long getHeight() {
        return transaction.height;
    }

    private void updateHeight(long height) {
        transaction.height = height;
    }

    public void updateHeights(final Tangle tangle) throws Exception {
        TransactionViewModel transactionVM = this, trunk = this.getTrunkTransaction(tangle);
        Stack<Hash> transactionViewModels = new Stack<>();
        transactionViewModels.push(transactionVM.getHash());
        while(trunk.getHeight() == 0 && trunk.getType() != PREFILLED_SLOT && !trunk.getHash().equals(Hash.NULL_HASH)) {
            transactionVM = trunk;
            trunk = transactionVM.getTrunkTransaction(tangle);
            transactionViewModels.push(transactionVM.getHash());
        }
        while(transactionViewModels.size() != 0) {
            transactionVM = TransactionViewModel.fromHash(tangle, transactionViewModels.pop());
            if(trunk.getHash().equals(Hash.NULL_HASH) && trunk.getHeight() == 0 && !transactionVM.getHash().equals(Hash.NULL_HASH)) {
                transactionVM.updateHeight(1L);
                transactionVM.update(tangle, "height");
            } else if ( trunk.getType() != PREFILLED_SLOT && transactionVM.getHeight() == 0){
                transactionVM.updateHeight(1 + trunk.getHeight());
                transactionVM.update(tangle, "height");
            } else {
                break;
            }
            trunk = transactionVM;
        }
    }

    public void updateSender(String sender) {
        transaction.sender = sender;
    }
}
