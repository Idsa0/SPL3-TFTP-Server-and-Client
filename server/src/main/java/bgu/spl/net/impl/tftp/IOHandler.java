package bgu.spl.net.impl.tftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOHandler {
    private IOHelperMode mode;
    private String filename;
    private short blockNumber;
    FileOutputStream out = null;
    FileInputStream in = null;
    ByteArrayInputStream dirqIn = null;
    private boolean ioDone = false;

    private byte[] filenamesBytes;

    public IOHandler(IOHelperMode mode) {
        this.mode = mode;
        this.blockNumber = 1;
    }

    public IOHandler(String filename, IOHelperMode mode) {
        this(mode);
        this.filename = filename;
    }

    public short getBlockNumber() {
        return blockNumber;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] readNext() throws IOException {
        if (mode != IOHelperMode.READ && mode != IOHelperMode.DIR)
            throw new RuntimeException("reading in write/dirq mode");

        byte[] output = new byte[512];
        int readSize = 0;
        if (mode == IOHelperMode.DIR) {
            readSize = dirqIn.read(output);
            if (readSize == 0) {
                output[0] = 0;
                readSize = 1;
            }
            if (readSize < 512)
                ioDone = true;
        } else {
            readSize = in.read(output);
            if (readSize == 0) {
                output[0] = 0;
                readSize = 1;
            }
            if (readSize < 512)
                ioDone = true;
        }

        System.arraycopy(output, 0, output, 0, readSize);
        ++blockNumber;
        return output;
    }

    public boolean fileExists() {
        return new File(filename).isFile();
    }

    public void start() throws FileNotFoundException {
        if (mode == IOHelperMode.DIR) {
            String[] strArr = Stream.of(new File("").listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .map((String str) -> str.concat("\0"))
                    .collect(Collectors.toSet())
                    .toArray(new String[0]);
            StringBuilder builder = new StringBuilder();

            for (String str : strArr)
                builder.append(str);
            filenamesBytes = builder.toString().getBytes();

            dirqIn = new ByteArrayInputStream(filenamesBytes);
        } else if (mode == IOHelperMode.READ)
            in = new FileInputStream(filename);
        else
            out = new FileOutputStream(filename);

        ioDone = false;
    }

    public boolean isIODone() {
        return ioDone;
    }

    public void freeResources() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (dirqIn != null)
                dirqIn.close();
        } catch (Exception ignored) {
        }
    }

    public void writeNext(byte[] data) throws IOException {
        if (mode != IOHelperMode.WRITE)
            throw new RuntimeException("trying to write in read mode");

        if (out == null)
            throw new RuntimeException("outputstream did not open yet");

        if (data.length < 512)
            ioDone = true;

        out.write(data);
        ++blockNumber;
    }

    public boolean deleteFile() {
        return new File(filename).delete();
    }

    public enum IOHelperMode {
        READ, WRITE, DELETE, DIR
    }
}
