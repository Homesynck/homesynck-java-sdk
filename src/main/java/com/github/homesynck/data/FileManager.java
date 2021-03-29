package com.github.homesynck.data;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileManager {

    private final File baseDirectory; //client directory
    private final File storageDirectory; //this directory contains all data
    private final File saveDirectory; // this directory save push of the server / have the latest version of the server sync
    private final File dataDirectory; // contains all data updated / deleted

    public FileManager(String storageFile) {
        this.baseDirectory = new File(storageFile);
        this.storageDirectory = new File(storageFile, "files/");
        if (!storageDirectory.exists()) {
            storageDirectory.mkdirs();
        }
        this.saveDirectory = new File(storageFile, ".homsynck/");
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        this.dataDirectory = new File(storageFile, ".data/");
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
    }

    private static Patch<String> getFileDiff(File original, File revised) throws IOException {
        List<String> list1;
        if (original == null) {
            list1 = Collections.emptyList();
        } else {
            list1 = Files.readAllLines(original.toPath());
        }
        List<String> list2 = Files.readAllLines(revised.toPath());

        return DiffUtils.diff(list1, list2);
    }

    private static ArrayList<File> getAllFiles(@NotNull File originalFile) {
        ArrayList<File> allFiles = new ArrayList<>();
        if (originalFile.listFiles() == null || originalFile.isFile()) {
            return new ArrayList<>();
        }
        for (File f : originalFile.listFiles()) {
            if (f.isDirectory()) {
                allFiles.addAll(getAllFiles(f));
            } else {
                allFiles.add(f);
            }
        }
        return allFiles;
    }

    /**
     * Edit the file specified by path
     *
     * @param stringPath    The path of the file
     * @param content       The content that will override the file
     * @throws IOException  if an I/O error occurs writing to or creating the file,
     *                      or the text cannot be encoded using the specified charset
     */
    public void editFile(@NotNull String stringPath, @NotNull String content) throws IOException {
        Path out = Paths.get(storageDirectory.getPath() + stringPath);
        List<String> contentList = new ArrayList<>(Arrays.asList(content.split(System.lineSeparator())));
        Files.write(out, contentList, Charset.defaultCharset());
    }

    /**
     * Get all the files stored by the api and give it to the user. It gives the more recent version found
     * on the device for all files.
     *
     * @return  the {@link HashMap} with path on key and content on value
     */
    public HashMap<String, String> getFiles() {
        HashMap<String, String> hashMap = new HashMap<>();
        for (File f : getAllFiles(storageDirectory)) {
            try {
                List<String> list = Files.readAllLines(f.toPath());
                StringBuilder sb = new StringBuilder();
                for (String s : list) {
                    sb.append(s).append(System.lineSeparator());
                }
                hashMap.put(f.getPath(), sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hashMap;
    }

    /**
     * Get a file with a specified.
     *
     * @param stringPath
     * @return
     * @throws IOException
     */
    public String getFile(@NotNull String stringPath) throws IOException {
        List<String> stringList = Files.readAllLines(Paths.get(stringPath));
        StringBuilder sb = new StringBuilder();
        for (String s : stringList) {
            sb.append(s).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * Delete the file specified with the path
     *
     * @param stringPath        path of the file that will be deleted
     * @throws FileException    if here is an error with the file deletion
     */
    public void deleteFile(@NotNull String stringPath) throws FileException {
        File f = new File(storageDirectory, stringPath);
        boolean isDelete = f.delete();

        if (!isDelete) {
            throw new FileException("The file can't be deleted");
        }

        File del = new File(dataDirectory, "/delete.hs");

        List<String> deletedFiles;
        if (!del.exists()) {
            try {
                del.createNewFile();
            } catch (IOException e) {
                throw new FileException("File can't be created.");
            }
            deletedFiles = new ArrayList<>();
        } else {
            try {
                deletedFiles = Files.readAllLines(del.toPath());
            } catch (IOException e) {
                throw new FileException("File can't be read");
            }
        }
        deletedFiles.add(f.getPath());
        try {
            Files.write(f.toPath(), deletedFiles, Charset.defaultCharset());
        } catch (IOException e) {
            throw new FileException("File can't be write");
        }
    }

    void applyPatch(@NotNull String patchId, @NotNull String path, @NotNull String unifiedPatch) throws IOException, PatchFailedException {
        addUpdate(patchId);
        List<String> unifiedPatchList = Arrays.asList(unifiedPatch.split(System.lineSeparator()));
        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedPatchList);

        File saveFile = new File(saveDirectory, path);
        File storedFile = new File(storageDirectory, path);

        List<String> file = Files.readAllLines(saveFile.toPath());

        List<String> result = DiffUtils.patch(file, patch);

        Files.write(saveFile.toPath(), result, Charset.defaultCharset());
        Files.write(storedFile.toPath(), result, Charset.defaultCharset());
    }

    private void addUpdate(String patchId) {
        File patchFile = new File(dataDirectory, "patchList.hs");
        try {
            List<String> updateList = Files.readAllLines(patchFile.toPath());

            if (!updateList.contains(patchId)) {
                updateList.add(patchId);
            }

            Files.write(patchFile.toPath(), updateList, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get the last past id pushed on the server
     *
     * @return the patch id
     */
    public int getPatchId() {
        try {
            List<String> list = Files.readAllLines(new File(dataDirectory, "patchlist.hs").toPath());
            return list.size();
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    String getListUpdate() {
        StringBuilder sb = new StringBuilder();
        File f = new File(dataDirectory, "patchlist.hs");
        List<String> list;
        try {
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    throw new IOException();
                }
            }
            list = Files.readAllLines(f.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return "[]";
        }
        sb.append("[");
        for (String s : list) {
            sb.append(s).append(",");
        }
        sb.substring(0, sb.length() - 2);
        sb.append("]");
        return sb.toString();

    }

    void accept(@NotNull String PatchId, @NotNull String unifiedPatch) throws IOException, PatchFailedException {
        addUpdate(PatchId);

        String path = FileSynck.getFilesPath(unifiedPatch);
        File f = new File(saveDirectory, path);

        List<String> original = Files.readAllLines(f.toPath());

        List<String> patchList = Arrays.asList(unifiedPatch.split(System.lineSeparator()));

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchList);

        List<String> result = DiffUtils.patch(original, patch);

        Files.write(new File(path).toPath(), result, Charset.defaultCharset());
    }

    public List<String> getPatch() {
        ArrayList<File> newFiles = getAllFiles(storageDirectory);
        ArrayList<String> patches = new ArrayList<>();

        for (File f : newFiles) {
            String abstractPath = f.getPath().substring(saveDirectory.getPath().length());
            File save = new File(saveDirectory, abstractPath);
            if (save.exists()) {
                try {
                    Patch<String> patch = getFileDiff(save, f);
                    if (patch.getDeltas().size() != 0) {
                        List<String> saveList = Files.readAllLines(save.toPath());
                        List<String> list = UnifiedDiffUtils.generateUnifiedDiff(save.getPath(), f.getPath(), saveList, patch, 0);
                        StringBuilder sb = new StringBuilder();
                        for (String s : list) {
                            sb.append(s).append(System.lineSeparator());
                        }
                        patches.add(sb.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Patch<String> patch = getFileDiff(null, f);
                    List<String> saveList = new ArrayList<>();
                    List<String> list = UnifiedDiffUtils.generateUnifiedDiff("", f.getPath(), saveList, patch, 0);
                    StringBuilder sb = new StringBuilder();
                    for (String s : list) {
                        sb.append(s).append(System.lineSeparator());
                    }
                    patches.add(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return patches;
    }
}
