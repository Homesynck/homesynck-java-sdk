package com.github.homesynck.file;

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

    public FileManager(String storageFile){
        this.baseDirectory = new File(storageFile);
        this.storageDirectory = new File(storageFile, "files/");
        if (!storageDirectory.exists()){
            storageDirectory.mkdirs();
        }
        this.saveDirectory = new File(storageFile, ".homsynck/");
        if (!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
        this.dataDirectory = new File(storageFile, ".data/");
        if(!saveDirectory.exists()){
            saveDirectory.mkdirs();
        }
    }

    public void editFile(String stringPath, String content) throws IOException{
        Path out = Paths.get(storageDirectory.getPath() + stringPath);
        List<String> contentList = new ArrayList<>(Arrays.asList(content.split(System.lineSeparator())));
        Files.write(out,contentList, Charset.defaultCharset());
    }

    public String getFile(String stringPath) throws IOException{
        List<String> stringList = Files.readAllLines(Paths.get(stringPath));
        StringBuilder sb = new StringBuilder();
        for (String s:stringList){
            sb.append(s).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public void deleteFile(String stringPath)throws FileException {
        File f = new File(storageDirectory, stringPath);
        boolean isDelete = f.delete();

        if (!isDelete){
            throw new FileException("The file can't be deleted");
        }

        File del = new File(dataDirectory, "/delete.hs");

        List<String> deletedFiles;
        if(!del.exists()){
            try {
                del.createNewFile();
            }catch(IOException e){
                throw new FileException("File can't be created.");
            }
            deletedFiles = new ArrayList<>();
        }else {
            try{
            deletedFiles = Files.readAllLines(del.toPath());
            }catch (IOException e){
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

    public void applyPatch(String patchId, String path, String unifiedPatch) throws IOException, PatchFailedException {
        List<String> unifiedPatchList = Arrays.asList(unifiedPatch.split(System.lineSeparator()));
        Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(unifiedPatchList);

        File saveFile = new File(saveDirectory, path);
        File storedFile = new File(storageDirectory, path);

        List<String> file = Files.readAllLines(saveFile.toPath());

        List<String> result = DiffUtils.patch(file, patch);

        Files.write(saveFile.toPath(), result, Charset.defaultCharset());
        Files.write(storedFile.toPath(), result, Charset.defaultCharset());

        addUpdate(patchId);
    }

    public void addUpdate(String patchId){
        File patchFile = new File(dataDirectory, "patchList.hs");
        try {
            List<String> updateList = Files.readAllLines(patchFile.toPath());

            if (!updateList.contains(patchId)){
                updateList.add(patchId);
            }

            Files.write(patchFile.toPath(), updateList, Charset.defaultCharset());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String getListUpdate() throws IOException {
        StringBuilder sb = new StringBuilder();
        File f = new File(dataDirectory, "patchlist.hs");
        if (!f.exists()){
            f.createNewFile();
        }
        List<String> list = Files.readAllLines(f.toPath());
        sb.append("[");
        for (String s: list){
            sb.append(s).append(",");
        }
        sb.substring(0, sb.length()-2);
        sb.append("]");
        return sb.toString();
    }

    private List<String> getDiffs(){
        ArrayList<File> newFiles = getAllFiles(storageDirectory);
        ArrayList<String> patches = new ArrayList<>();

        for (File f:newFiles){
            String abstractPath = f.getPath().substring(saveDirectory.getPath().length());
            File save = new File(saveDirectory, abstractPath);
            if (save.exists()){
                try{
                    Patch<String> patch = getFileDiff(save,f);
                    if (patch.getDeltas().size() != 0){
                        List<String> saveList = Files.readAllLines(save.toPath());
                        List<String> list = UnifiedDiffUtils.generateUnifiedDiff(save.getPath(), f.getPath(), saveList, patch, 0);
                        StringBuilder sb = new StringBuilder();
                        for (String s:list){
                            sb.append(s).append(System.lineSeparator());
                        }
                        patches.add(sb.toString());
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }else {
                try{
                    Patch<String> patch = getFileDiff(null,f);
                    List<String> saveList = new ArrayList<>();
                    List<String> list = UnifiedDiffUtils.generateUnifiedDiff("", f.getPath(), saveList, patch, 0);
                    StringBuilder sb = new StringBuilder();
                    for (String s:list){
                        sb.append(s).append(System.lineSeparator());
                    }
                    patches.add(sb.toString());
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return patches;
    }

    private static Patch<String> getFileDiff(File original, File revised)throws IOException {
        List<String> list1;
        if (original==null){
            list1 = Collections.emptyList();
        }else {
            list1 = Files.readAllLines(original.toPath());
        }
        List<String> list2 = Files.readAllLines(revised.toPath());

        return DiffUtils.diff(list1, list2);
    }

    private static ArrayList<File> getAllFiles(@NotNull File originalFile){
        ArrayList<File> allFiles = new ArrayList<>();
        if (originalFile.listFiles() == null || originalFile.isFile()){
            return new ArrayList<>();
        }
        for (File f:originalFile.listFiles()){
            if(f.isDirectory()){
                allFiles.addAll(getAllFiles(f));
            }else {
                allFiles.add(f);
            }
        }
        return allFiles;
    }
}
