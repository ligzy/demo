package com.realtime.demo;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class TranslateCharset {
    static public void main(String args[]) throws Exception {
        String inFilename = "testTransferGBK.txt";
        String inFileCharsetName = "GBK";
        String outFilename = "outputFileName.txt";
        String outFileCharsetName = "UTF-8";

        File infile = new File(inFilename);
        File outfile = new File(outFilename);

        RandomAccessFile inraf = new RandomAccessFile(infile, "r");
        RandomAccessFile outraf = new RandomAccessFile(outfile, "rw");

        FileChannel finc = inraf.getChannel();
        FileChannel foutc = outraf.getChannel();

        String hi = "你好，测试";
        byte[] bufin = hi.getBytes();
        ByteBuffer butin = java.nio.ByteBuffer.allocateDirect(bufin.length);
        butin.wrap(bufin);
        //ByteBuffer butout = java.nio.ByteBuffer.allocateDirect(1024);

        //        MappedByteBuffer inmbb = finc.map(FileChannel.MapMode.READ_ONLY, 0, (int) infile.length());

        Charset inCharset = Charset.forName(inFileCharsetName);
        Charset outCharset = Charset.forName(outFileCharsetName);

        CharsetDecoder inDecoder = inCharset.newDecoder();
        CharsetEncoder outEncoder = outCharset.newEncoder();

        CharBuffer cb = inDecoder.decode(butin);
        ByteBuffer outbb = outEncoder.encode(cb);

        foutc.write(outbb);
        System.out.println(new String(outbb.array(),outFileCharsetName));

        inraf.close();
        outraf.close();
    }
}