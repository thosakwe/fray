package thosakwe.fray.lang.pipeline;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;

public class FrayAsset {
    private final String extension;
    private final String sourcePath;
    private final String name;
    private InputStream inputStream;
    private FrayPipeline pipeline;

    public FrayAsset(String extension, String sourcePath, String name, InputStream inputStream) {
        this.extension = extension;
        this.sourcePath = sourcePath;
        this.name = name;
        this.inputStream = inputStream;
    }

    public static FrayAsset forFile(File file) throws FileNotFoundException {
        return new FrayAsset(
                FilenameUtils.getExtension(file.getPath()),
                file.getAbsolutePath(),
                file.getName(),
                new FileInputStream(file)
        );
    }

    public static FrayAsset forUrl(URL url) throws IOException {
        return new FrayAsset(
                FilenameUtils.getExtension(url.getPath()),
                url.toString(),
                url.getPath(),
                url.openStream()
        );
    }

    public static FrayAsset forFile(String filename) throws FileNotFoundException {
        return forFile(new File(filename));
    }

    public FrayAsset changeExtension(String newExtension) {
        return new FrayAsset(newExtension, sourcePath, name, inputStream);
    }

    public FrayAsset changeInputStream(InputStream newStream) {
        return new FrayAsset(extension, sourcePath, name, newStream);
    }

    public FrayAsset changeName(String newName) {
        return new FrayAsset(extension, sourcePath, newName, inputStream);
    }

    public String getExtension() {
        return extension;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getName() {
        return name;
    }

    public FrayPipeline getPipeline() {
        return pipeline;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    FrayAsset setPipeline(FrayPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }
}
