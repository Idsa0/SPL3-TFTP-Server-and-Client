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
    private final String PATH;
    private IOHandlerMode mode;
    private String filename;
    private short blockNumber;
    FileOutputStream out = null;
    FileInputStream in = null;
    ByteArrayInputStream dirqIn = null;
    private boolean ioDone = false;

    private byte[] filenamesBytes;

    public IOHandler(IOHandlerMode mode) {
        this.mode = mode;
        this.blockNumber = 1;
        String path = System.getProperty("user.dir");
        if (path.contains("server"))
            PATH = path + "/Files/";
        else
            PATH = path + "/server/Files/";
    }

    public IOHandler(String filename, IOHandlerMode mode) {
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
        if (mode != IOHandlerMode.READ && mode != IOHandlerMode.DIR)
            throw new RuntimeException("reading in write/dirq mode");

        byte[] readData = new byte[512];
        int readSize = 0;
        if (mode == IOHandlerMode.DIR) {
            readSize = dirqIn.read(readData);
            if (readSize == 0) {
                readData[0] = 0;
                readSize = 1;
            }
            if (readSize < 512)
                ioDone = true;
        } else {
            readSize = in.read(readData);
            if (readSize == 0) {
                readData[0] = 0;
                readSize = 1;
            }
            if (readSize < 512)
                ioDone = true;
        }

        byte[] result = new byte[readSize];
        System.arraycopy(readData, 0, result, 0, readSize);
        ++blockNumber;
        return result;
    }

    public boolean fileExists() {
        return new File(PATH + filename).isFile();
    }

    public void start() throws FileNotFoundException {
        if (mode == IOHandlerMode.DIR) {
            String[] strArr = Stream.of(new File(PATH).listFiles())
                    .filter(file -> !file.isDirectory())
                    .filter(file -> !file.isHidden())
                    .map(File::getName)
                    .map((String str) -> str.concat("\0"))
                    .collect(Collectors.toSet())
                    .toArray(new String[0]);

            if (strArr.length == 0) {
                strArr = new String[] { "Directory is empty" };
            }
            StringBuilder builder = new StringBuilder();

            for (String str : strArr)
                builder.append(str);
            filenamesBytes = builder.toString().getBytes();

            dirqIn = new ByteArrayInputStream(filenamesBytes);
        } else if (mode == IOHandlerMode.READ)
            in = new FileInputStream(PATH + filename);
        else
            out = new FileOutputStream(PATH + filename);

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
        if (mode != IOHandlerMode.WRITE)
            throw new RuntimeException("trying to write in read mode");

        if (out == null)
            throw new RuntimeException("outputstream did not open yet");

        if (data.length < 512)
            ioDone = true;

        out.write(data);
        ++blockNumber;
    }

    public boolean deleteFile() {
        return new File(PATH + filename).delete();
    }

    public enum IOHandlerMode {
        READ, WRITE, DELETE, DIR
    }
}
