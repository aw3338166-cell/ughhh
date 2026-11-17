package com.austinwing.mcaibuild.schematic;
public final class LitematicBitArray {
    private LitematicBitArray() {}
    public static int bitsFor(int paletteSize) {
        if (paletteSize <= 1) return 2;
        return Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteSize - 1));
    }
    public static int get(long[] data, int index, int bits) {
        if (data == null || index < 0 || bits < 1 || bits > 32) return -1;
        long startOffset=(long)index*bits,endOffset=startOffset+bits-1;
        int startWord=(int)(startOffset>>>6),endWord=(int)(endOffset>>>6);
        if(endWord>=data.length)return -1;
        int startBit=(int)(startOffset&63L);
        long mask=(1L<<bits)-1L;
        if(startWord==endWord)return (int)((data[startWord]>>>startBit)&mask);
        int firstPartBits=64-startBit;
        return (int)(((data[startWord]>>>startBit)|(data[endWord]<<firstPartBits))&mask);
    }
    public static long[] pack(int[] values,int bits) {
        if(bits<1||bits>32)throw new IllegalArgumentException("bits must be 1..32");
        long totalBits=(long)values.length*bits;
        long[] data=new long[(int)((totalBits+63)>>>6)];
        long mask=(1L<<bits)-1L;
        for(int i=0;i<values.length;i++){
            long value=values[i]&mask,offset=(long)i*bits;
            int word=(int)(offset>>>6),bit=(int)(offset&63L);
            data[word]|=value<<bit;
            if(bit+bits>64)data[word+1]|=value>>>(64-bit);
        }
        return data;
    }
}
