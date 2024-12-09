package me.gladrian.Kingdoms;
import org.bukkit.Bukkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

import org.checkerframework.checker.units.qual.A;
import org.json.*;

public class KingdomsDAL {
    private static int MAX_LOCK_WAIT_SECONDS = 30;
    private String _dataDirPath;
    private ArrayList<String> _lockedFilePaths = new ArrayList<>();

    public KingdomsDAL(String dataDirPath) {
        _dataDirPath = dataDirPath;
    }

    //region <LOAD DATA>
    public KingdomsDataCache loadData(){
        var cache = new KingdomsDataCache();
        cache.NextKingdomId = 0;
        cache.Kingdoms = new ArrayList<>();
        cache.ChunksIndexedByX = new HashMap<>();
        cache.MembersIndexedByUuid = new HashMap<>();
        cache.Invitations = new ArrayList<>();
        cache.Messages = new ArrayList<>();

        var kingdomsFile = new File(Paths.get(_dataDirPath, "kingdoms.json").toString());
        if (!kingdomsFile.exists()) {
            Bukkit.getLogger().info("No existing kingdoms data found, returning empty dataset.");
            return cache;
        }

        cache.Kingdoms = loadKingdoms(kingdomsFile.toPath());
        int maxKingdomId = -1;
        for (var kingdom : cache.Kingdoms) {
            for (var member : kingdom.Members) {
                cache.MembersIndexedByUuid.put(member.PlayerUuid, member);
            }
            for (var chunk : kingdom.Chunks) {
                if (!cache.ChunksIndexedByX.containsKey(chunk.X)) {
                    cache.ChunksIndexedByX.put(chunk.X, new ArrayList<>());
                }
                cache.ChunksIndexedByX.get(chunk.X).add(chunk);
            }
            if (kingdom.Id > maxKingdomId) maxKingdomId = kingdom.Id;
        }
        cache.NextKingdomId = maxKingdomId + 1;

        var invitationsFile = new File(Paths.get(_dataDirPath, "invitations.json").toString());
        if (invitationsFile.exists()) {
            cache.Invitations = loadInvitations(invitationsFile.toPath());
        }

        var messagesFile = new File(Paths.get(_dataDirPath, "messages.json").toString());
        if (messagesFile.exists()) {
            cache.Messages = loadMessages(messagesFile.toPath());
        }

        return cache;
    }

    private ArrayList<Kingdom> loadKingdoms(Path kingdomsFilePath) {
        var kingdoms = new ArrayList<Kingdom>();
        JSONArray kingdomsArray;
        try {
            kingdomsArray = new JSONArray(Files.readString(kingdomsFilePath));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE,"Failed to load data from file " + kingdomsFilePath + "\nException: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for(int n = 0; n < kingdomsArray.length(); n++)
        {
            var kingdomObj = kingdomsArray.getJSONObject(n);
            var kingdom = Kingdom.fromJson(kingdomObj);
            kingdom.Members = loadMembersForKingdom(kingdom.Id);
            kingdom.Chunks = loadChunksForKingdom(kingdom.Id);
            kingdoms.add(kingdom);
        }
        return kingdoms;
    }

    private ArrayList<KingdomMember> loadMembersForKingdom(int kingdomId) {
        var members = new ArrayList<KingdomMember>();
        var membersFilePath = Paths.get(_dataDirPath, "members", kingdomId + ".json");
        JSONArray membersArray;
        try {
            membersArray = new JSONArray(Files.readString(membersFilePath));
        }
        catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE,"Failed to load data from file " + membersFilePath + "\nException: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        for(int n = 0; n < membersArray.length(); n++)
        {
            var memberObj = membersArray.getJSONObject(n);
            members.add(KingdomMember.fromJson(memberObj, kingdomId));
        }

        return members;
    }

    private ArrayList<ClaimedChunk> loadChunksForKingdom(int kingdomId) {
        var chunks = new ArrayList<ClaimedChunk>();
        var chunksFilePath = Paths.get(_dataDirPath, "chunks", kingdomId + ".tsv");
        try {
            var scanner = new Scanner(chunksFilePath);
            int lineNum = 0;
            while (scanner.hasNext()) {
                var line = scanner.nextLine();
                chunks.add(parseChunk(line, kingdomId, lineNum++));
            }
        }
        catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE,"Failed to load data from file " + chunksFilePath + "\nException: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return chunks;
    }

    private ClaimedChunk parseChunk(String strCoordinates, int kingdomId, int lineNumberForError) {
        var chunk = new ClaimedChunk();
        try {
            var coordinates = strCoordinates.split("\\t", 2);
            chunk.X = Integer.parseInt(coordinates[0]);
            chunk.Z = Integer.parseInt(coordinates[1]);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Chunk coordinates are invalid at line " + lineNumberForError);
        }
        chunk.KingdomId = kingdomId;
        return chunk;
    }

    private ArrayList<Invitation> loadInvitations(Path invitationsFilePath) {
        var invitations = new ArrayList<Invitation>();
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(Files.readString(invitationsFilePath));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE,"Failed to load data from file " + invitationsFilePath + "\nException: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for(int n = 0; n < jsonArray.length(); n++)
        {
            var invitationJson = jsonArray.getJSONObject(n);
            var invitation = Invitation.fromJson(invitationJson);
            invitations.add(invitation);
        }
        return invitations;
    }

    private ArrayList<Message> loadMessages(Path messagesFilePath) {
        var messages = new ArrayList<Message>();
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(Files.readString(messagesFilePath));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE,"Failed to load data from file " + messagesFilePath + "\nException: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for(int n = 0; n < jsonArray.length(); n++)
        {
            var messageJson = jsonArray.getJSONObject(n);
            var message = Message.fromJson(messageJson);
            messages.add(message);
        }
        return messages;
    }
    //endregion

    //region <SAVE DATA>
    private boolean lockFile(String filePathToLock) {
        var waitMs = 100;
        var totalWaitedSeconds = 0;
        while (_lockedFilePaths.contains(filePathToLock)) {
            if (totalWaitedSeconds > MAX_LOCK_WAIT_SECONDS) {
                Bukkit.getLogger().log(Level.WARNING, "Timed out while waiting on lock for " + filePathToLock);
                return false;
            }
            try {
                Thread.sleep(waitMs);
            }
            catch (InterruptedException e) {
                Bukkit.getLogger().log(Level.WARNING, "Interrupted while waiting on lock");
                e.printStackTrace();
            }
            totalWaitedSeconds += waitMs / 1000;
        }
        _lockedFilePaths.add(filePathToLock);
        return true;
    }

    private void unlockFile(String filePathToUnlock) {
        _lockedFilePaths.remove(filePathToUnlock);
    }

    /* Saves all cached data synchronously */
    public void saveAllData(KingdomsDataCache cache) {
        for (var kingdom : cache.Kingdoms) {
            saveMembers(kingdom);
            saveChunks(kingdom);
        }
        saveAllKingdomInfo(cache.Kingdoms);
        saveInvitations(cache.Invitations);
        saveMessages(cache.Messages);
    }

    /* Saves all kingdoms to kingdoms.json asynchronously */
    public void saveAllKingdomInfoAsync(ArrayList<Kingdom> kingdoms) {
        Thread newThread = new Thread(() -> {
            saveAllKingdomInfo(kingdoms);
        });
        newThread.start();
    }

    private void saveAllKingdomInfo(ArrayList<Kingdom> kingdoms) {
        var jsonArray = new JSONArray();
        for (var kingdom : kingdoms) {
            var json = kingdom.toJson();
            jsonArray.put(kingdom.toJson());
        }
        var kingdomsJson = jsonArray.toString();
        saveDataToFile(kingdomsJson, _dataDirPath, "kingdoms.json");
    }

    /* Saves all members of a kingdom to members/[kingdom-id].json asynchronously */
    public void saveMembersAsync(Kingdom kingdom) {
        Thread newThread = new Thread(() -> {
            saveMembers(kingdom);
        });
        newThread.start();
    }

    private void saveMembers(Kingdom kingdom) {
        var jsonArray = new JSONArray();
        for (var member : kingdom.Members) {
            jsonArray.put(member.toJson());
        }
        var membersJson = jsonArray.toString();
        var membersDirPath = Paths.get(_dataDirPath, "members").toString();
        var fileName = kingdom.Id + ".json";
        saveDataToFile(membersJson, membersDirPath, fileName);
    }

    public void deleteMembers(Kingdom kingdom) {
        var membersFilePath = Paths.get(_dataDirPath, "members", kingdom.Id + ".json").toString();
        var file = new File(membersFilePath);
        if (!file.delete())
            throw new RuntimeException("Failed to delete file " + membersFilePath);
    }

    /* Saves all claimed chunks of a kingdom to chunks/[kingdom-id].json asynchronously */
    public void saveChunksAsync(Kingdom kingdom) {
        Thread newThread = new Thread(() -> {
            saveChunks(kingdom);
        });
        newThread.start();
    }

    private void saveChunks(Kingdom kingdom) {
        var strChunksTsv = "";
        for (var chunk : kingdom.Chunks) {
            strChunksTsv += chunk.X + "\t" + chunk.Z + "\n";
        }
        var chunksDirPath = Paths.get(_dataDirPath, "chunks").toString();
        var fileName = kingdom.Id + ".tsv";
        saveDataToFile(strChunksTsv, chunksDirPath, fileName);
    }

    public void deleteChunks(Kingdom kingdom) {
        var chunksFilePath = Paths.get(_dataDirPath, "chunks", kingdom.Id + ".tsv").toString();
        var file = new File(chunksFilePath);
        if (!file.delete())
            throw new RuntimeException("Failed to delete file " + chunksFilePath);
    }

    /* Saves all invitations to invitations.json asynchronously */
    public void saveInvitationsAsync(ArrayList<Invitation> invitations) {
        Thread newThread = new Thread(() -> {
            saveInvitations(invitations);
        });
        newThread.start();
    }

    private void saveInvitations(ArrayList<Invitation> invitations) {
        var jsonArray = new JSONArray();
        for (var invitation : invitations) {
            jsonArray.put(invitation.toJson());
        }
        var invitationsJson = jsonArray.toString();
        saveDataToFile(invitationsJson, _dataDirPath, "invitations.json");
    }

    /* Saves all messages to messages.json asynchronously */
    public void saveMessagesAsync(ArrayList<Message> messages) {
        Thread newThread = new Thread(() -> {
            saveMessages(messages);
        });
        newThread.start();
    }

    private void saveMessages(ArrayList<Message> messages) {
        var jsonArray = new JSONArray();
        for (var message : messages) {
            jsonArray.put(message.toJson());
        }
        var messagesJson = jsonArray.toString();
        saveDataToFile(messagesJson, _dataDirPath, "messages.json");
    }

    private void saveDataToFile(String strData, String dirPath, String fileName) {
        var fullPath = Paths.get(dirPath, fileName).toString();
        if (!lockFile(fullPath)) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save data to " + fullPath + " due to lock on file.");
            throw new RuntimeException();
        }
        try {
            var dir = new File(dirPath);
            dir.mkdirs();
            var file =  new File(fullPath);
            file.createNewFile();
            var out = new PrintWriter(fullPath);
            out.print(strData);
            out.close();
        }
        catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save data to " + fullPath + " due to exception: " + e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally {
            unlockFile(fullPath);
        }
    }

    /* Makes a backup of the currently saved data */
    public void backupData() {
        var backupFolderName = Long.toString(getCurrentTicks());
        var backupDirPath = Paths.get(_dataDirPath, "backups", backupFolderName).toString();
        var backupDir = new File(backupDirPath);

        try {
            backupDir.mkdirs();
            copyDirectory(_dataDirPath, backupDirPath, "members");
            copyDirectory(_dataDirPath, backupDirPath, "chunks");
            copyFile(_dataDirPath, backupDirPath, "kingdoms.json");
            copyFile(_dataDirPath, backupDirPath, "messages.json");
            copyFile(_dataDirPath, backupDirPath, "invitations.json");
            copyFile(_dataDirPath, backupDirPath, "config.yml");
        }
        catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to backup data to " + backupDirPath + " due to exception: " + e);
            e.printStackTrace();
        }
    }

    /* Deletes the specified backup directory */
    public void deleteBackup(String backupFolderName) {
        var backupDirPath = Paths.get(_dataDirPath, "backups", backupFolderName).toString();
        var backupDir = new File(backupDirPath);

        try {
            backupDir.delete();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed delete unretained backup " + backupDirPath + " due to exception: " + e);
            e.printStackTrace();
        }
    }

    /* Returns the folder names of all existing backups */
    public ArrayList<String> getBackupNames() {
        var backupRootDir = new File(Paths.get(_dataDirPath, "backups").toString());
        var directoryNames = new ArrayList<String>();
        var directories = backupRootDir.listFiles();
        if (directories != null) {
            for (var file : directories) {
                if (file.isDirectory()) {
                    directoryNames.add(file.getName());
                }
            }
        }
        return directoryNames;
    }

    private void copyDirectory(String sourceRootDir, String destRootDir, String directoryName) {
        File source = new File(Paths.get(sourceRootDir, directoryName).toString());
        File destination = new File(Paths.get(destRootDir, directoryName).toString());
        try {
            FileUtils.copyDirectory(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void copyFile(String sourceRootDir, String destRootDir, String fileName) {
        File source = new File(Paths.get(sourceRootDir, fileName).toString());
        File destination = new File(Paths.get(destRootDir, fileName).toString());
        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private long getCurrentTicks() {
        return Instant.now().toEpochMilli();
    }
    //endregion
}
