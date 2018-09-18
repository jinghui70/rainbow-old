package rainbow.core.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import rainbow.core.bundle.Resource;

public class FileResource implements Resource {

    private String name;

    private File file;

    public FileResource(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public File getFile() {
        return file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getSize() {
        return file.length();
    }
}
