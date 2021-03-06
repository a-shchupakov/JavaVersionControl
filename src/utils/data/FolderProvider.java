package utils.data;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class FolderProvider implements IDataProvider {
    private String origin;
    private String currentRoot;
    private Path path;

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(String origin) {
        this.origin = origin;
        setCurrentRoot(origin);
    }

    @Override
    public void setCurrentRoot(String currentRoot) {
        this.currentRoot = currentRoot;
        path = Paths.get(currentRoot);
    }

    @Override
    public String resolve(String root, String name) {
        return Paths.get(root).resolve(name).toString();
    }

    @Override
    public String getCurrentRoot() {
        return currentRoot;
    }

    @Override
    public byte[] read(String name) throws IOException {
        Path pathToFile = path.resolve(name);
        return Files.readAllBytes(pathToFile);
    }

    @Override
    public void write(String name, byte[] bytes) throws IOException {
        Path pathToFile = path.resolve(name);
        if (!pathToFile.toFile().exists()){
            new File(pathToFile.getParent().toString()).mkdirs();
        }
        Files.write(pathToFile, bytes);
    }

    @Override
    public void delete(String name) throws IOException {
        deleteFileOrFolder(path.resolve(name));
    }

    @Override
    public void createDirectory(String name) {
        new File(path.resolve(name).toString()).mkdirs();
    }

    public static void deleteFileOrFolder(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
            @Override public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return handleException(e);
            }

            private FileVisitResult handleException(final IOException e) {
                e.printStackTrace(); // replace with more robust error handling
                return TERMINATE;
            }

            @Override public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                    throws IOException {
                if(e!=null)return handleException(e);
                Files.delete(dir);
                return CONTINUE;
            }
        });
    }

    @Override
    public List<Pair<String, byte[]>> walkThrough(String dir) throws IOException{
        File folder = Paths.get(dir).toFile();
        List<Pair<String, byte[]>> listOfFiles = new ArrayList<>();
        addFilesToList(folder, listOfFiles, "");
        return listOfFiles;
    }

    @Override
    public void clearDirectory(String dir){
        File directoryFile = Paths.get(dir).toFile();
        if (!directoryFile.exists()) {
            try {
                Files.createDirectories(directoryFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        purgeDirectory(directoryFile);
    }

    @Override
    public void append(String name, byte[] bytes) throws IOException {
        Path pathToFile = path.resolve(name);
        if (!pathToFile.toFile().exists()){
            new File(pathToFile.getParent().toString()).mkdirs();
        }
        Files.write(pathToFile, bytes, Files.exists(pathToFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
    }

    private void purgeDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                purgeDirectory(file);
                try {
                    Files.delete(file.toPath());
                } catch (IOException e) { }
            }
            file.delete();
        }
    }

    private void addFilesToList(File dir, List<Pair<String, byte[]>> listOfFiles, String prefix) throws IOException{
        if(dir == null || dir.listFiles() == null){
            return;
        }
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                String name = entry.getName();
                if (!prefix.isEmpty())
                    name = prefix + "\\" + name;
                listOfFiles.add(new Pair<>(name, Files.readAllBytes(entry.toPath())));
            }
            else {
                String newPrefix = (prefix.isEmpty()) ? entry.getName() : prefix + "\\" + entry.getName();
                addFilesToList(entry, listOfFiles, newPrefix);
            }
        }
    }
}
