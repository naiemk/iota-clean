package com.iota.iri;

import com.iota.iri.controllers.BundleViewModel;
import com.iota.iri.hash.*;
import com.iota.iri.model.Hash;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Transaction;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.Converter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BundleValidator {

    public static List<List<TransactionViewModel>> validate(Tangle tangle, Hash tailHash) throws Exception {
        TransactionViewModel tail = TransactionViewModel.fromHash(tangle, tailHash);
        List<List<TransactionViewModel>> transactions = new LinkedList<>();
        if(tail.getCurrentIndex() != 0) return transactions;
        final Map<Hash, TransactionViewModel> bundleTransactions = loadTransactionsFromTangle(tangle, tail);

        // Understanding this part:
        // First for loop, only finds the tip of the bundle if it is valid.

        Optional<List<TransactionViewModel>> bundle = sortBunde(bundleTransactions.values());
        if (!bundle.isPresent()) {
            invalidateBundle(tangle, tail,  bundleTransactions.values());
            return transactions;
        } else {
            long sumNeg = bundle.get().stream().mapToLong(TransactionViewModel::value)
                    .filter(i -> i <= 0)
                    .reduce(0L, Math::addExact);
            long sumPos = bundle.get().stream().mapToLong(TransactionViewModel::value)
                    .filter(i -> i > 0)
                    .reduce(0L, Math::addExact);
            if (sumPos + sumNeg != 0 || sumPos > TransactionViewModel.SUPPLY) {
                invalidateBundle(tangle, tail, bundleTransactions.values());
            }

            // Squeeze the essence of all transactions in the bundle. If the hash is equal to the
            Hash actualBundleHash = calculateBundleHash(bundle.get());
            if (!actualBundleHash.equals(bundle.get().get(0).getHash())) {
                invalidateBundle(tangle, tail, bundleTransactions.values());
                return transactions;
            }

            // For each transaction that is signable validate signatures
            TransactionViewModel prev = null;
            boolean prevRequiresSignature = false;
            for (TransactionViewModel tvm: bundle.get()) {
                boolean requiresSig = requiresSignature(tvm, prev, prevRequiresSignature);
                if (requiresSig) {
                    validateSignature(tvm);
                }

                prevRequiresSignature = requiresSig;
            }

            // bundle hash,then for any transaction that requires a signature validate the signature
            // a transaction that has a value less than 0 requires a signature, any transaction
            // after this transaction that reflects the same address and has value of 0 is considered
            // part of the transaction and its message is used in the signature

        }

//        for (TransactionViewModel transactionViewModel : bundleTransactions.values()) {
//
//            if (transactionViewModel.getCurrentIndex() == 0 && transactionViewModel.getValidity() >= 0) {
//
//                final List<TransactionViewModel> instanceTransactionViewModels = new LinkedList<>();
//
//                final long lastIndex = transactionViewModel.lastIndex();
//                long bundleValue = 0;
//                int i = 0;
//                final Sponge curlInstance = SpongeFactory.create(SpongeFactory.Mode.KERL);
//                final Sponge addressInstance = SpongeFactory.create(SpongeFactory.Mode.KERL);
//
////                final int[] addressTrits = new int[TransactionViewModel.ADDRESS_TRINARY_SIZE];
////                final int[] bundleHashTrits = new int[TransactionViewModel.BUNDLE_TRINARY_SIZE];
////                final int[] normalizedBundle = new int[Curl.HASH_LENGTH / ISS.TRYTE_WIDTH];
////                final int[] digestTrits = new int[Curl.HASH_LENGTH];
//
//                MAIN_LOOP:
//                while (true) {
//
//                    instanceTransactionViewModels.add(transactionViewModel);
//
//                    // Validate bundle orders and ensure bundle is not transacting more than the supply
//                    if (
//                            transactionViewModel.getCurrentIndex() != i
//                            || transactionViewModel.lastIndex() != lastIndex
//                            || ((bundleValue = Math.addExact(bundleValue, transactionViewModel.value())) < -TransactionViewModel.SUPPLY
//                            || bundleValue > TransactionViewModel.SUPPLY)
//                            ) {
//                        instanceTransactionViewModels.get(0).setValidity(tangle, -1);
//                        break;
//                    }
//
//                    // TODO: This is not relevant to SHA3
////                    if (transactionViewModel.value() != 0 && transactionViewModel.getAddressHash().trits()[Curl.HASH_LENGTH - 1] != 0) {
////                        instanceTransactionViewModels.get(0).setValidity(tangle, -1);
////                        break;
////                    }
//
//                    // This loop sorts the bundle based on trunk link, ensures values sum to 0, and if the bundle is
//                    // valid does the bundle hash validation
//
//                    if (i++ == lastIndex) { // It's supposed to become -3812798742493 after 3812798742493 and to go "down" to -1 but we hope that noone will create such long bundles
//
//                        if (bundleValue == 0) {
//
//                            // Squeeze the essence of all transactions in the bundle. If the hash is equal to the
//                            // bundle hash,then for any transaction that requires a signature validate the signature
//                            if (instanceTransactionViewModels.get(0).getValidity() == 0) {
//                                curlInstance.reset();
//                                for (final TransactionViewModel transactionViewModel2 : instanceTransactionViewModels) {
//                                    curlInstance.absorb(transactionViewModel2.trits(), TransactionViewModel.ESSENCE_TRINARY_OFFSET, TransactionViewModel.ESSENCE_TRINARY_SIZE);
//                                }
//                                curlInstance.squeeze(bundleHashTrits, 0, bundleHashTrits.length);
//                                if (Arrays.equals(instanceTransactionViewModels.get(0).getBundleHash().trits(), bundleHashTrits)) {
//
//                                    ISSInPlace.normalizedBundle(bundleHashTrits, normalizedBundle);
//
//                                    for (int j = 0; j < instanceTransactionViewModels.size(); ) {
//
//                                        transactionViewModel = instanceTransactionViewModels.get(j);
//                                        if (transactionViewModel.value() < 0) { // let's recreate the address of the transactionViewModel.
//                                            addressInstance.reset();
//                                            int offset = 0, offsetNext = 0;
//                                            do {
//                                                offsetNext = (offset + ISS.NUMBER_OF_FRAGMENT_CHUNKS - 1) % (Curl.HASH_LENGTH / Converter.NUMBER_OF_TRITS_IN_A_TRYTE) + 1;
//                                                ISSInPlace.digest(SpongeFactory.Mode.KERL,
//                                                    normalizedBundle,
//                                                    offset % (Curl.HASH_LENGTH / Converter.NUMBER_OF_TRITS_IN_A_TRYTE),
//                                                    instanceTransactionViewModels.get(j).trits(),
//                                                    TransactionViewModel.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET,
//                                                    digestTrits);
//                                                addressInstance.absorb(digestTrits,0, Curl.HASH_LENGTH);
//                                                offset = offsetNext;
//                                            } while (++j < instanceTransactionViewModels.size()
//                                                    && instanceTransactionViewModels.get(j).getAddressHash().equals(transactionViewModel.getAddressHash())
//                                                    && instanceTransactionViewModels.get(j).value() == 0);
//
//                                            addressInstance.squeeze(addressTrits, 0, addressTrits.length);
//                                            //if (!Arrays.equals(Converter.bytes(addressTrits, 0, TransactionViewModel.ADDRESS_TRINARY_SIZE), transactionViewModel.getAddress().getHash().bytes())) {
//                                            if (! Arrays.equals(transactionViewModel.getAddressHash().trits(), addressTrits)) {
//                                                instanceTransactionViewModels.get(0).setValidity(tangle, -1);
//                                                break MAIN_LOOP;
//                                            }
//                                        } else {
//                                            j++;
//                                        }
//                                    }
//
//                                    instanceTransactionViewModels.get(0).setValidity(tangle, 1);
//                                    transactions.add(instanceTransactionViewModels);
//                                } else {
//                                    instanceTransactionViewModels.get(0).setValidity(tangle, -1);
//                                }
//                            } else {
//                                transactions.add(instanceTransactionViewModels);
//                            }
//                        } else {
//                            instanceTransactionViewModels.get(0).setValidity(tangle, -1);
//                        }
//                        break;
//
//                    } else {
//                        transactionViewModel = bundleTransactions.get(transactionViewModel.getTrunkTransactionHash());
//                        if (transactionViewModel == null) {
//                            break;
//                        }
//                    }
//                }
//            }
//        }
        return transactions;
    }

    private static void validateSignature(TransactionViewModel tvm) {
        throw new NotImplementedException();
    }

    private static boolean requiresSignature(TransactionViewModel tran,
                                             TransactionViewModel prev,
                                             boolean preRequiredSignature) {
        long tVal = tran.value();
        return tVal < 0 ||
                (tVal == 0 &&
                        prev != null &&
                        preRequiredSignature &&
                        prev.getAddressHash().equals(tran.getAddressHash()));

    }

    private static Hash calculateBundleHash(List<TransactionViewModel> transactionViewModels) {
        // bundle hash is the hash of th essence of all transactions
        throw new NotImplementedException();
    }

    private static void invalidateBundle(Tangle tangle,
                                         TransactionViewModel tail,
                                         Collection<TransactionViewModel> values) throws Exception {
        // Invalidate transaction
        // TODO: Can ve invalidate all the transactions?
        tail.setValidity(tangle, -1);
        for (TransactionViewModel t: values) {
            if (t.getCurrentIndex() == 0) {
                t.setValidity(tangle, -1);
            }
        }
    }

    private static Optional<List<TransactionViewModel>> sortBunde(Collection<TransactionViewModel> bundleTransactions) {
        List<TransactionViewModel> orderedTx = bundleTransactions.stream()
                .sorted(Comparator.comparingLong(TransactionViewModel::getCurrentIndex)).collect(Collectors.toList());
        if (orderedTx.size() == 0) {
            return Optional.empty();
        }

        if (orderedTx.size() == 1) {
            return Optional.of(orderedTx);
        }

        for(int i=0; i < orderedTx.size(); i++) {
            if ( orderedTx.get(i).getTrunkTransactionHash() == null ||
                    orderedTx.get(i + 1).getTrunkTransactionHash() == null ||
                    (!orderedTx.get(i).getTrunkTransactionHash().equals(orderedTx.get(i + 1).getHash()))
               ) {
                return Optional.empty();
            }
        }

        return Optional.of(orderedTx);
    }

    public static boolean isInconsistent(List<TransactionViewModel> transactionViewModels) {
        long value = 0;
        for (final TransactionViewModel bundleTransactionViewModel : transactionViewModels) {
            if (bundleTransactionViewModel.value() != 0) {
                value += bundleTransactionViewModel.value();
                /*
                if(!milestone && bundleTransactionViewModel.getAddressHash().equals(Hash.NULL_HASH) && bundleTransactionViewModel.snapshotIndex() == 0) {
                    return true;
                }
                */
            }
        }
        return (value != 0 || transactionViewModels.size() == 0);
    }

    private static Map<Hash, TransactionViewModel> loadTransactionsFromTangle(Tangle tangle, TransactionViewModel tail) {
        final Map<Hash, TransactionViewModel> bundleTransactions = new HashMap<>();
        final Hash bundleHash = tail.getBundleHash();
        try {
            TransactionViewModel tx = tail;
            long i = 0, end = tx.lastIndex();
            do {
                bundleTransactions.put(tx.getHash(), tx);
                tx = tx.getTrunkTransaction(tangle);
            } while (i++ < end && tx.getCurrentIndex() != 0 && tx.getBundleHash().equals(bundleHash));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bundleTransactions;
    }
}
