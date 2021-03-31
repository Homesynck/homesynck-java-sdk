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
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
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
        File f = new File(storageDirectory, stringPath);
        f = f.getParentFile();
        f = new File(f, "/");
        f.mkdirs();
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
     */
    public void deleteFile(@NotNull String stringPath) {

        File deletedFiles = new File(dataDirectory, "deletedFiles.hs");

        File file = new File(storageDirectory, stringPath);
        if (!deletedFiles.exists()){
            try {
                deletedFiles.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            List<String> deletedPaths = Files.readAllLines(deletedFiles.toPath());

            deletedPaths.add(file.getPath());

            Files.write(deletedFiles.toPath(),deletedPaths, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.delete()){
            file.delete();
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

    void applyPatch(@NotNull int patchId, @NotNull String path, @NotNull String unifiedPatch) throws IOException, PatchFailedException {
        addUpdate(patchId);

        List<String> unifiedPatchList = Arrays.asList(unifiedPatch.split(System.lineSeparator()));

        String firstLine = unifiedPatchList.get(0);

        File storedFile = new File(path);

        path = path.substring(storageDirectory.getPath().length());
        File saveFile = new File(saveDirectory, path);

        if (firstLine.trim().startsWith("del")){
            storedFile.delete();
            saveFile.delete();
            return;
        }

        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedPatchList);

        File saveParent = saveFile.getParentFile();
        if (!saveParent.exists()){
            saveParent.mkdirs();
        }
        if (!saveFile.exists()){
            saveFile.createNewFile();
        }

        File storedParent = storedFile.getParentFile();
        if (!storedParent.exists()){
            storedParent.mkdirs();
        }

        List<String> file = Files.readAllLines(saveFile.toPath());

        List<String> result = DiffUtils.patch(file, patch);

        Files.write(saveFile.toPath(), result, Charset.defaultCharset());
        Files.write(storedFile.toPath(), result, Charset.defaultCharset());
    }


    private void addUpdate(int patchId) {
        File patchFile = new File(dataDirectory, "patchList.hs");
        try {
            List<String> updateList = Files.readAllLines(patchFile.toPath());

            String patchString = String.valueOf(patchId);
            if (!updateList.contains(patchString)) {
                updateList.add(patchString);
            }

            Files.write(patchFile.toPath(), updateList, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<Integer> getListUpdate() {
        StringBuilder sb = new StringBuilder();
        File f = new File(dataDirectory, "patchlist.hs");
        List<String> list;
        try {
            if (!f.exists()) {
                f.createNewFile();
                return new ArrayList<>();
            }
            list = Files.readAllLines(f.toPath());
            List<Integer> intList = new ArrayList<>();
            for(String s:list){
                intList.add(Integer.parseInt(s));
            }

            return intList;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    List<String> getPatch() {
        ArrayList<File> newFiles = getAllFiles(storageDirectory);
        ArrayList<String> patches = new ArrayList<>();

        File deletedFile = new File(dataDirectory, "deletedFiles.hs");
        try {
            if (deletedFile.exists()) {
                List<String> deletedList = Files.readAllLines(deletedFile.toPath());
                for (String s : deletedList) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("del").append(System.lineSeparator());
                    sb.append("+++").append(Optional.ofNullable(s).orElse("/dev/null"));
                    patches.add(sb.toString());
                }
                deletedFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (File f : newFiles) {
            String abstractPath = f.getPath().substring(storageDirectory.getPath().length());
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
}
