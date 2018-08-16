package com.iota.iri.model;

import com.iota.iri.hash.Curl;
import com.iota.iri.hash.Sponge;
import com.iota.iri.storage.Indexable;
import com.iota.iri.utils.Converter;
import org.bouncycastle.jcajce.provider.digest.SHA3;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Hash implements Serializable, Indexable {

   public static final int SIZE_IN_BYTES = 32;

    public static final Hash NULL_HASH = new Hash(new byte[SIZE_IN_BYTES]);

    private byte[] bytes;
    private int[] trits;
    private int hashCode;
    
    // constructors' bill

    public Hash(final byte[] bytes, final int offset, final int size) {
        fullRead(bytes, offset, size);
    }

    public Hash(){}

    public Hash(final byte[] bytes) {
        this(bytes, 0, SIZE_IN_BYTES);
    }

    public Hash(final String hash) {
        this(hash.getBytes(StandardCharsets.UTF_8));
    }

    public static Hash calculate(byte[] bytes) {
        return calculateSHA3(bytes);
    }

    public static Hash calculate(byte[] bytes, final Sponge curl) {
        return calculate(bytes);
    }

    public static byte[] Sha3Digest(byte[] bytes) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
        return digestSHA3.digest(bytes);
    }

    private static Hash calculateSHA3(byte[] bytes) {
        return new Hash(Sha3Digest(bytes));
    }

    public int[] trits() {
        if(trits == null) {
            trits = new int[Curl.HASH_LENGTH];
            Converter.getTrits(bytes, trits);
        }
        return trits;
    }

    @Override
    public boolean equals(final Object obj) {
        assert obj instanceof Hash;
        if (obj == null) return false;
        return Arrays.equals(bytes(), ((Hash) obj).bytes());
    }

    @Override
    public int hashCode() {
        if(bytes == null) {
            bytes();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return Converter.trytes(trits());
    }
    
    public byte[] bytes() {
        if(bytes == null) {
            bytes = new byte[SIZE_IN_BYTES];
            Converter.bytes(trits, 0, bytes, 0, trits.length);
            hashCode = Arrays.hashCode(this.bytes);
        }
        return bytes;
    }

    private void fullRead(byte[] bytes, int offset, int size) {
        this.bytes = new byte[SIZE_IN_BYTES];
        System.arraycopy(bytes, offset, this.bytes, 0, size - offset > bytes.length ? bytes.length-offset: size);
        hashCode = Arrays.hashCode(this.bytes);
    }

    @Override
    public void read(byte[] bytes) {
        fullRead(bytes, 0, SIZE_IN_BYTES);
    }

    @Override
    public Indexable incremented() {
        return null;
    }

    @Override
    public Indexable decremented() {
        return null;
    }

    @Override
    public int compareTo(Indexable indexable) {
        int idx = 0;
        for(byte b: indexable.bytes()) {
            if (bytes.length == idx) {
                return -1;
            }
            if (b != bytes[idx]) {
                return Byte.compare(bytes[idx], b);
            }
            idx ++;
        }

        return 0;
    }
}

