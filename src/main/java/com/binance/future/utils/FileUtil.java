package com.binance.future.utils;

import java.io.*;

public class FileUtil {
    public static void write(String input) throws IOException {
        BufferedInputStream bufferIn = null;
        BufferedOutputStream bufferOut = null;

        try {
            InputStream inputStream = new FileInputStream("input.txt");
            OutputStream outputStream = new FileOutputStream("output.txt");

            bufferIn = new BufferedInputStream(inputStream);
            bufferOut = new BufferedOutputStream(outputStream);

            int c;
            while ((c = bufferIn.read()) != -1) {
                bufferOut.write(c);
            }
        } finally {
            if (bufferIn != null) {
                bufferIn.close();
            }
            if (bufferOut != null) {
                bufferOut.close();
            }
        }
    }
}
