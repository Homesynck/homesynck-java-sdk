package com.github.homesynck.file;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.github.openjson.JSONObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileManager {

    private List<JSONObject> objectsPush;

    private File baseDirectory;
    private File storageDirectory;
    private File saveDirectory;
    private File dataDirectory;

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
        objectsPush = new LinkedList<>();
    }

    public void editFile(String stringPath, String content) throws IOException{
        Path out = Paths.get(storageDirectory.getPath() + stringPath);
        List<String> contentList = new ArrayList<>(Arrays.asList(content.split(System.lineSeparator())));
        Files.write(out,contentList, Charset.defaultCharset());
    }

    public void deleteFile(String stringPath){
        File f = new File(storageDirectory, stringPath);
        boolean isDelete = f.delete();
        if (isDelete) {
            JSONObject del = new JSONObject();
            del.accumulate("delete", f.getPath());
            objectsPush.add(del);
        }
    }

    private List<Patch<String>> getDiffs(){
        ArrayList<File> newFiles = getAllFiles(storageDirectory);
        ArrayList<Patch<String>> patchs = new ArrayList<>();

        for (File f:newFiles){
            String abstractPath = f.getPath().substring(saveDirectory.getPath().length());
            File save = new File(saveDirectory, abstractPath);
            if (save.exists()){
                try{
                    Patch<String> patch = getDiff(save,f);
                    if (patch.getDeltas().size() != 0){
                        patchs.add(patch);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }else {
                try{
                    patchs.add(getDiff(null,f));
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return patchs;
    }

    private static Patch<String> getDiff(File original, File revised)throws IOException {
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
