    package com.hytale.guild;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hytale.guild.claim.Claim;
import com.hytale.guild.claim.ClaimManager;
import com.hytale.guild.economy.EconomyManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {
private static GuildManager instance;
private final Map<UUID, Guild> guilds = new ConcurrentHashMap();
private final Map<UUID, UUID> playerGuilds = new ConcurrentHashMap();
private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap();
private final Path dataFile;
private final Gson gson;

private GuildManager(Path dataDirectory) {
this.dataFile = dataDirectory.resolve("guilds.json");
this.gson = (new GsonBuilder()).setPrettyPrinting().create();
this.loadGuilds();
}

public static void initialize(Path dataDirectory) {
if (instance == null) {
instance = new GuildManager(dataDirectory);
}

}

public static GuildManager getInstance() {
if (instance == null) {
throw new IllegalStateException("GuildManager not initialized");
} else {
return instance;
}
}

public Guild createGuild(String name, UUID leaderId, String leaderName) {
if (this.playerGuilds.containsKey(leaderId)) {
return null;
} else if (this.guilds.values().stream().anyMatch((g) -> {
return g.getName().equalsIgnoreCase(name);
})) {
return null;
} else {
Guild guild = new Guild(name, leaderId, leaderName);
this.guilds.put(guild.getId(), guild);
this.playerGuilds.put(leaderId, guild.getId());
this.saveGuilds();
return guild;
}
}

public boolean invitePlayer(UUID guildId, UUID playerId) {
if (this.playerGuilds.containsKey(playerId)) {
return false;
} else {
this.pendingInvites.put(playerId, guildId);
return true;
}
}

public boolean acceptInvite(UUID playerId, String playerName) {
UUID guildId = (UUID)this.pendingInvites.remove(playerId);
if (guildId == null) {
return false;
} else {
Guild guild = (Guild)this.guilds.get(guildId);
if (guild != null && !guild.isFull()) {
guild.addMember(playerId, playerName, GuildRank.RECRUIT);
this.playerGuilds.put(playerId, guildId);
this.saveGuilds();
return true;
} else {
return false;
}
}
}

public void cancelInvite(UUID playerId) {
this.pendingInvites.remove(playerId);
}

public boolean hasInvite(UUID playerId) {
return this.pendingInvites.containsKey(playerId);
}

public boolean leaveGuild(UUID playerId) {
UUID guildId = (UUID)this.playerGuilds.get(playerId);
if (guildId == null) {
return false;
} else {
Guild guild = (Guild)this.guilds.get(guildId);
if (guild == null) {
return false;
} else {
GuildMember member = guild.getMember(playerId);
if (member != null && member.getRank() == GuildRank.LEADER) {
return false;
} else {
guild.removeMember(playerId);
this.playerGuilds.remove(playerId);
this.saveGuilds();
return true;
}
}
}
}

public boolean kickPlayer(UUID guildId, UUID playerId) {
Guild guild = (Guild)this.guilds.get(guildId);
if (guild != null && guild.isMember(playerId)) {
GuildMember member = guild.getMember(playerId);
if (member != null && member.getRank() == GuildRank.LEADER) {
return false;
} else {
guild.removeMember(playerId);
this.playerGuilds.remove(playerId);
this.saveGuilds();
return true;
}
} else {
return false;
}
}

public boolean disbandGuild(UUID guildId) {
Guild guild = (Guild)this.guilds.remove(guildId);
if (guild == null) {
return false;
} else {
Iterator var3 = guild.getMembers().iterator();

         while(var3.hasNext()) {
            GuildMember member = (GuildMember)var3.next();
            this.playerGuilds.remove(member.getPlayerId());
         }

         try {
            ClaimManager claimManager = ClaimManager.getInstance();
            List<Claim> guildClaims = claimManager.getGuildClaims(guildId);
            Iterator var5 = guildClaims.iterator();

            while(var5.hasNext()) {
               Claim claim = (Claim)var5.next();
               claimManager.deleteClaim(claim.getId());
            }
         } catch (Exception var8) {
         }

         try {
            EconomyManager.getInstance().deleteGuildBalance(guildId);
         } catch (Exception var7) {
         }

         this.saveGuilds();
         return true;
      }

}

public boolean setPlayerRank(UUID guildId, UUID playerId, GuildRank newRank) {
Guild guild = (Guild)this.guilds.get(guildId);
if (guild != null && guild.isMember(playerId)) {
GuildMember member = guild.getMember(playerId);
if (member != null && member.getRank() == GuildRank.LEADER) {
return false;
} else {
guild.setMemberRank(playerId, newRank);
this.saveGuilds();
return true;
}
} else {
return false;
}
}

public Guild getPlayerGuild(UUID playerId) {
UUID guildId = (UUID)this.playerGuilds.get(playerId);
return guildId != null ? (Guild)this.guilds.get(guildId) : null;
}

public Guild getGuildById(UUID guildId) {
return (Guild)this.guilds.get(guildId);
}

public Collection<Guild> getAllGuilds() {
return new ArrayList(this.guilds.values());
}

public boolean isInGuild(UUID playerId) {
return this.playerGuilds.containsKey(playerId);
}

private void saveGuilds() {
try {
Files.createDirectories(this.dataFile.getParent());
JsonObject root = new JsonObject();
JsonArray guildsArray = new JsonArray();
Iterator var3 = this.guilds.values().iterator();

         while(var3.hasNext()) {
            Guild guild = (Guild)var3.next();
            JsonObject guildObj = new JsonObject();
            guildObj.addProperty("name", guild.getName());
            guildObj.addProperty("id", guild.getId().toString());
            guildObj.addProperty("createdAt", guild.getCreatedAt());
            JsonArray membersArray = new JsonArray();
            Iterator var7 = guild.getMembers().iterator();

            while(var7.hasNext()) {
               GuildMember member = (GuildMember)var7.next();
               JsonObject memberObj = new JsonObject();
               memberObj.addProperty("playerId", member.getPlayerId().toString());
               memberObj.addProperty("playerName", member.getPlayerName());
               memberObj.addProperty("rank", member.getRank().name());
               memberObj.addProperty("joinedAt", member.getJoinedAt());
               membersArray.add(memberObj);
            }

            guildObj.add("members", membersArray);
            guildsArray.add(guildObj);
         }

         root.add("guilds", guildsArray);
         BufferedWriter writer = Files.newBufferedWriter(this.dataFile);

         try {
            this.gson.toJson(root, writer);
         } catch (Throwable var11) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }
            }

            throw var11;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException var12) {
         System.err.println("Failed to save guilds: " + var12.getMessage());
         var12.printStackTrace();
      }

}

private void loadGuilds() {
if (Files.exists(this.dataFile, new LinkOption[0])) {
try {
BufferedReader reader = Files.newBufferedReader(this.dataFile);

            label74: {
               try {
                  JsonObject root = (JsonObject)this.gson.fromJson(reader, JsonObject.class);
                  if (root != null && root.has("guilds")) {
                     JsonArray guildsArray = root.getAsJsonArray("guilds");
                     Iterator var4 = guildsArray.iterator();

                     while(true) {
                        if (!var4.hasNext()) {
                           break label74;
                        }

                        JsonElement guildElement = (JsonElement)var4.next();
                        JsonObject guildObj = guildElement.getAsJsonObject();
                        String name = guildObj.get("name").getAsString();
                        UUID id = UUID.fromString(guildObj.get("id").getAsString());
                        long createdAt = guildObj.get("createdAt").getAsLong();
                        Map<UUID, GuildMember> members = new HashMap();
                        JsonArray membersArray = guildObj.getAsJsonArray("members");
                        Iterator var13 = membersArray.iterator();

                        while(var13.hasNext()) {
                           JsonElement memberElement = (JsonElement)var13.next();
                           JsonObject memberObj = memberElement.getAsJsonObject();
                           UUID playerId = UUID.fromString(memberObj.get("playerId").getAsString());
                           String playerName = memberObj.get("playerName").getAsString();
                           GuildRank rank = GuildRank.valueOf(memberObj.get("rank").getAsString());
                           long joinedAt = memberObj.get("joinedAt").getAsLong();
                           GuildMember member = new GuildMember(playerId, playerName, rank, joinedAt);
                           members.put(playerId, member);
                        }

                        Guild guild = new Guild(name, id, members, createdAt);
                        this.guilds.put(id, guild);
                        Iterator var26 = members.keySet().iterator();

                        while(var26.hasNext()) {
                           UUID playerId = (UUID)var26.next();
                           this.playerGuilds.put(playerId, id);
                        }
                     }
                  }
               } catch (Throwable var23) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var22) {
                        var23.addSuppressed(var22);
                     }
                  }

                  throw var23;
               }

               if (reader != null) {
                  reader.close();
               }

               return;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (IOException var24) {
            System.err.println("Failed to load guilds: " + var24.getMessage());
            var24.printStackTrace();
         }

      }

}
}

    package com.hytale.guild.claim;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hytale.guild.Guild;
import com.hytale.guild.GuildManager;
import com.hytale.guild.GuildMember;
import com.hytale.guild.GuildPlugin;
import com.hytale.guild.GuildRank;
import com.hytale.guild.util.GuildMessages;
import com.hytale.guild.util.Lang;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;

public class ClaimCommand extends AbstractPlayerCommand {
private final GuildPlugin plugin;

public ClaimCommand(GuildPlugin plugin) {
super("claim", "Comandi territorio gilda", false);
this.plugin = plugin;
this.setAllowsExtraArguments(true);
}

protected boolean canGeneratePermission() {
return false;
}

protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore>
ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
String inputString = context.getInputString();
String[] args = inputString.trim().split("\\s+");
UUID playerId = playerRef.getUuid();
if (args.length < 2) {
this.sendHelp(playerRef);
} else {
String subCommand = args[1].toLowerCase();
byte var11 = -1;
switch(subCommand.hashCode()) {
case -1352294148:
if (subCommand.equals("create")) {
var11 = 0;
}
break;
case -1335458389:
if (subCommand.equals("delete")) {
var11 = 1;
}
break;
case 3237038:
if (subCommand.equals("info")) {
var11 = 2;
}
break;
case 3322014:
if (subCommand.equals("list")) {
var11 = 3;
}
break;
case 94627080:
if (subCommand.equals("check")) {
var11 = 4;
}
}

         switch(var11) {
         case 0:
            if (!this.hasPermission(context, "hyguild.claim.create")) {
               this.sendNoPermission(playerRef);
               return;
            }

            this.handleCreate(playerRef, playerId, world);
            break;
         case 1:
            if (!this.hasPermission(context, "hyguild.claim.delete")) {
               this.sendNoPermission(playerRef);
               return;
            }

            this.handleDelete(playerRef, playerId, world);
            break;
         case 2:
            if (!this.hasPermission(context, "hyguild.claim.info")) {
               this.sendNoPermission(playerRef);
               return;
            }

            this.handleInfo(playerRef, playerId, world);
            break;
         case 3:
            if (!this.hasPermission(context, "hyguild.claim.list")) {
               this.sendNoPermission(playerRef);
               return;
            }

            this.handleList(playerRef, playerId);
            break;
         case 4:
            if (!this.hasPermission(context, "hyguild.claim.info")) {
               this.sendNoPermission(playerRef);
               return;
            }

            this.handleCheck(playerRef, world);
            break;
         default:
            this.sendHelp(playerRef);
         }

      }

}

private boolean hasPermission(CommandContext context, String permission) {
return context.sender().hasPermission("hyguild.admin") ? true : context.sender().hasPermission(permission);
}

private void sendNoPermission(PlayerRef playerRef) {
GuildMessages.sendError(playerRef, Lang.get("error.no.permission"));
}

private void sendHelp(PlayerRef playerRef) {
playerRef.sendMessage(Message.raw("=== " + Lang.get("claim.help.title") + " ===").color(new Color(255, 215, 0)));
this.sendHelpLine(playerRef, "/claim create", Lang.get("claim.help.create"));
this.sendHelpLine(playerRef, "/claim delete", Lang.get("claim.help.delete"));
this.sendHelpLine(playerRef, "/claim info", Lang.get("claim.help.info"));
this.sendHelpLine(playerRef, "/claim list", Lang.get("claim.help.list"));
}

private void sendHelpLine(PlayerRef playerRef, String command, String description) {
playerRef.sendMessage(Message.join(new Message[]{Message.raw(command).color(new Color(255, 165, 0)), Message.raw(" - ")
.color(Color.GRAY), Message.raw(description).color(new Color(135, 206, 250))}));
}

private void handleCreate(PlayerRef playerRef, UUID playerId, World world) {
GuildManager guildManager = GuildManager.getInstance();
ClaimManager claimManager = ClaimManager.getInstance();
Guild guild = guildManager.getPlayerGuild(playerId);
if (guild == null) {
GuildMessages.sendError(playerRef, Lang.get("error.not.in.guild"));
} else {
GuildMember member = guild.getMember(playerId);
if (member.getRank() != GuildRank.LEADER) {
GuildMessages.sendError(playerRef, Lang.get("claim.create.only.leader"));
} else {
Transform transform = playerRef.getTransform();
int blockX = (int)transform.getPosition().x;
int blockZ = (int)transform.getPosition().z;
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
String worldName = world.getName();
Claim existing = claimManager.getClaimAtChunk(worldName, chunkX, chunkZ);
if (existing != null) {
GuildMessages.sendError(playerRef, Lang.get("claim.create.already.claimed"));
} else {
Claim claim = claimManager.createClaim(guild.getId(), worldName, chunkX, chunkZ);
if (claim == null) {
GuildMessages.sendError(playerRef, Lang.get("claim.create.failed"));
} else {
GuildMessages.sendSuccess(playerRef, Lang.get("claim.create.success", chunkX, chunkZ));
}
}
}
}
}

private void handleDelete(PlayerRef playerRef, UUID playerId, World world) {
GuildManager guildManager = GuildManager.getInstance();
ClaimManager claimManager = ClaimManager.getInstance();
Guild guild = guildManager.getPlayerGuild(playerId);
if (guild == null) {
GuildMessages.sendError(playerRef, Lang.get("error.not.in.guild"));
} else {
GuildMember member = guild.getMember(playerId);
if (member.getRank() != GuildRank.LEADER) {
GuildMessages.sendError(playerRef, Lang.get("claim.delete.only.leader"));
} else {
Transform transform = playerRef.getTransform();
int blockX = (int)transform.getPosition().x;
int blockZ = (int)transform.getPosition().z;
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
String worldName = world.getName();
Claim claim = claimManager.getClaimAtChunk(worldName, chunkX, chunkZ);
if (claim == null) {
GuildMessages.sendError(playerRef, Lang.get("claim.delete.not.claimed"));
} else if (!claim.getGuildId().equals(guild.getId())) {
GuildMessages.sendError(playerRef, Lang.get("claim.delete.not.yours"));
} else {
if (claimManager.deleteClaim(claim.getId())) {
GuildMessages.sendSuccess(playerRef, Lang.get("claim.delete.success", chunkX, chunkZ));
} else {
GuildMessages.sendError(playerRef, Lang.get("claim.delete.failed"));
}

            }
         }
      }

}

private void handleInfo(PlayerRef playerRef, UUID playerId, World world) {
ClaimManager claimManager = ClaimManager.getInstance();
GuildManager guildManager = GuildManager.getInstance();
Transform transform = playerRef.getTransform();
int blockX = (int)transform.getPosition().x;
int blockZ = (int)transform.getPosition().z;
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
String worldName = world.getName();
Claim claim = claimManager.getClaimAtChunk(worldName, chunkX, chunkZ);
if (claim == null) {
GuildMessages.sendInfo(playerRef, Lang.get("claim.info.not.claimed"));
} else {
Guild ownerGuild = guildManager.getGuildById(claim.getGuildId());
String guildName = ownerGuild != null ? ownerGuild.getName() : "Unknown";
playerRef.sendMessage(Message.raw("=== " + Lang.get("claim.info.title") + " ===").color(new Color(255, 215, 0)));
playerRef.sendMessage(Message.join(new Message[]{Message.raw(Lang.get("claim.info.guild") + ": ").color(new Color(255,
165, 0)), Message.raw(guildName).color(Color.WHITE)}));
playerRef.sendMessage(Message.join(new Message[]{Message.raw(Lang.get("claim.info.chunk") + ": ").color(new Color(255,
165, 0)), Message.raw(chunkX + ", " + chunkZ).color(Color.WHITE)}));
}
}

private void handleList(PlayerRef playerRef, UUID playerId) {
GuildManager guildManager = GuildManager.getInstance();
ClaimManager claimManager = ClaimManager.getInstance();
Guild guild = guildManager.getPlayerGuild(playerId);
if (guild == null) {
GuildMessages.sendError(playerRef, Lang.get("error.not.in.guild"));
} else {
List<Claim> claims = claimManager.getGuildClaims(guild.getId());
if (claims.isEmpty()) {
GuildMessages.sendInfo(playerRef, Lang.get("claim.list.empty"));
} else {
Object[] var10002 = new Object[]{claims.size()};
playerRef.sendMessage(Message.raw("=== " + Lang.get("claim.list.title", var10002) + " ===").color(new Color(255, 215,
0)));
Iterator var7 = claims.iterator();

            while(var7.hasNext()) {
               Claim claim = (Claim)var7.next();
               playerRef.sendMessage(Message.join(new Message[]{Message.raw("- ").color(Color.GRAY), Message.raw(Lang.get("claim.list.entry", claim.getChunkX(), claim.getChunkZ())).color(Color.WHITE)}));
            }

         }
      }

}

private void handleCheck(PlayerRef playerRef, World world) {
Transform transform = playerRef.getTransform();
int blockX = (int)transform.getPosition().x;
int blockZ = (int)transform.getPosition().z;
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
String worldName = world.getName();
Claim claim = ClaimManager.getInstance().getClaimAtChunk(worldName, chunkX, chunkZ);
String message;
Color color;
if (claim == null) {
message = Lang.get("territory.wilderness");
color = new Color(144, 238, 144);
} else {
Guild guild = GuildManager.getInstance().getGuildById(claim.getGuildId());
String guildName = guild != null ? guild.getName() : "???";
String var10000 = Lang.get("territory.guild");
message = var10000 + " " + guildName;
color = new Color(255, 215, 0);
}

      EventTitleUtil.showEventTitleToPlayer(playerRef, Message.raw(message).color(color), Message.raw(""), false, "", 0.2F, 0.5F, 3.0F);
      GuildMessages.sendInfo(playerRef, message);

}
}
package com.hytale.guild.claim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClaimManager {
private static ClaimManager instance;
private final Map<String, Map<Long, Claim>> claimsByWorld = new ConcurrentHashMap();
private final Map<UUID, Claim> claimsById = new ConcurrentHashMap();
private final Path dataFile;
private final Gson gson;

private ClaimManager(Path dataDirectory) {
this.dataFile = dataDirectory.resolve("claims.json");
this.gson = (new GsonBuilder()).setPrettyPrinting().create();
this.loadClaims();
}

public static void initialize(Path dataDirectory) {
if (instance == null) {
instance = new ClaimManager(dataDirectory);
}

}

public static ClaimManager getInstance() {
if (instance == null) {
throw new IllegalStateException("ClaimManager not initialized");
} else {
return instance;
}
}

private long chunkKey(int chunkX, int chunkZ) {
return (long)chunkX << 32 | (long)chunkZ & 4294967295L;
}

public Claim createClaim(UUID guildId, String worldName, int chunkX, int chunkZ) {
Claim existing = this.getClaimAtChunk(worldName, chunkX, chunkZ);
if (existing != null) {
return null;
} else {
Claim claim = new Claim(guildId, worldName, chunkX, chunkZ);
((Map)this.claimsByWorld.computeIfAbsent(worldName, (k) -> {
return new ConcurrentHashMap();
})).put(this.chunkKey(chunkX, chunkZ), claim);
this.claimsById.put(claim.getId(), claim);
this.saveClaims();
return claim;
}
}

public boolean deleteClaim(UUID claimId) {
Claim claim = (Claim)this.claimsById.remove(claimId);
if (claim == null) {
return false;
} else {
Map<Long, Claim> worldClaims = (Map)this.claimsByWorld.get(claim.getWorldName());
if (worldClaims != null) {
worldClaims.remove(this.chunkKey(claim.getChunkX(), claim.getChunkZ()));
}

         this.saveClaims();
         return true;
      }

}

public Claim getClaimAt(String worldName, int blockX, int blockZ) {
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
return this.getClaimAtChunk(worldName, chunkX, chunkZ);
}

public Claim getClaimAtChunk(String worldName, int chunkX, int chunkZ) {
Map<Long, Claim> worldClaims = (Map)this.claimsByWorld.get(worldName);
return worldClaims == null ? null : (Claim)worldClaims.get(this.chunkKey(chunkX, chunkZ));
}

public List<Claim> getGuildClaims(UUID guildId) {
return (List)this.claimsById.values().stream().filter((c) -> {
return c.getGuildId().equals(guildId);
}).collect(Collectors.toList());
}

public Claim getClaimById(UUID claimId) {
return (Claim)this.claimsById.get(claimId);
}

public boolean isGuildClaim(UUID guildId, String worldName, int blockX, int blockZ) {
Claim claim = this.getClaimAt(worldName, blockX, blockZ);
return claim != null && claim.getGuildId().equals(guildId);
}

public Claim getClaimAtChunkAnyWorld(int chunkX, int chunkZ) {
long key = this.chunkKey(chunkX, chunkZ);
Iterator var5 = this.claimsByWorld.values().iterator();

      Claim claim;
      do {
         if (!var5.hasNext()) {
            return null;
         }

         Map<Long, Claim> worldClaims = (Map)var5.next();
         claim = (Claim)worldClaims.get(key);
      } while(claim == null);

      return claim;

}

private void saveClaims() {
try {
Files.createDirectories(this.dataFile.getParent());
JsonObject root = new JsonObject();
JsonArray claimsArray = new JsonArray();
Iterator var3 = this.claimsById.values().iterator();

         while(var3.hasNext()) {
            Claim claim = (Claim)var3.next();
            JsonObject claimObj = new JsonObject();
            claimObj.addProperty("id", claim.getId().toString());
            claimObj.addProperty("guildId", claim.getGuildId().toString());
            claimObj.addProperty("worldName", claim.getWorldName());
            claimObj.addProperty("chunkX", claim.getChunkX());
            claimObj.addProperty("chunkZ", claim.getChunkZ());
            claimObj.addProperty("createdAt", claim.getCreatedAt());
            claimsArray.add(claimObj);
         }

         root.add("claims", claimsArray);
         BufferedWriter writer = Files.newBufferedWriter(this.dataFile);

         try {
            this.gson.toJson(root, writer);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException var8) {
         System.err.println("Failed to save claims: " + var8.getMessage());
         var8.printStackTrace();
      }

}

private void loadClaims() {
if (Files.exists(this.dataFile, new LinkOption[0])) {
try {
BufferedReader reader = Files.newBufferedReader(this.dataFile);

            label59: {
               try {
                  JsonObject root = (JsonObject)this.gson.fromJson(reader, JsonObject.class);
                  if (root != null && root.has("claims")) {
                     JsonArray claimsArray = root.getAsJsonArray("claims");
                     Iterator var4 = claimsArray.iterator();

                     while(true) {
                        if (!var4.hasNext()) {
                           break label59;
                        }

                        JsonElement claimElement = (JsonElement)var4.next();
                        JsonObject claimObj = claimElement.getAsJsonObject();
                        UUID id = UUID.fromString(claimObj.get("id").getAsString());
                        UUID guildId = UUID.fromString(claimObj.get("guildId").getAsString());
                        String worldName = claimObj.get("worldName").getAsString();
                        int chunkX = claimObj.get("chunkX").getAsInt();
                        int chunkZ = claimObj.get("chunkZ").getAsInt();
                        long createdAt = claimObj.get("createdAt").getAsLong();
                        Claim claim = new Claim(id, guildId, worldName, chunkX, chunkZ, createdAt);
                        ((Map)this.claimsByWorld.computeIfAbsent(worldName, (k) -> {
                           return new ConcurrentHashMap();
                        })).put(this.chunkKey(chunkX, chunkZ), claim);
                        this.claimsById.put(id, claim);
                     }
                  }
               } catch (Throwable var16) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (reader != null) {
                  reader.close();
               }

               return;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (IOException var17) {
            System.err.println("Failed to load claims: " + var17.getMessage());
            var17.printStackTrace();
         }

      }

}
}
package com.hytale.guild.claim;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hytale.guild.Guild;
import com.hytale.guild.GuildManager;
import com.hytale.guild.util.Lang;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryTickingSystem extends EntityTickingSystem<EntityStore> {
private static final Message SUBTITLE_MESSAGE = Message.raw("HyGuild");
private final Map<UUID, String> playerLastTitle = new ConcurrentHashMap();

public Query<EntityStore> getQuery() {
return PlayerRef.getComponentType();
}

public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
CommandBuffer<EntityStore> buf) {
Ref<EntityStore> ref = chunk.getReferenceTo(index);
PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
Player player = (Player)store.getComponent(ref, Player.getComponentType());
if (playerRef != null && player != null) {
String wildernessText = Lang.get("territory.wilderness");
Message titleMessage = Message.raw(wildernessText).color(Color.GREEN);
String titleText = wildernessText;
int blockX = (int)playerRef.getTransform().getPosition().getX();
int blockZ = (int)playerRef.getTransform().getPosition().getZ();
int chunkX = blockX >> 4;
int chunkZ = blockZ >> 4;
String worldName = player.getWorld().getName();
Claim claim = ClaimManager.getInstance().getClaimAtChunk(worldName, chunkX, chunkZ);
if (claim != null) {
Guild guild = GuildManager.getInstance().getGuildById(claim.getGuildId());
if (guild != null) {
String var10000 = Lang.get("territory.guild");
titleText = var10000 + " " + guild.getName();
titleMessage = Message.raw(titleText).color(new Color(255, 215, 0));
}
}

         String previousTitle = (String)this.playerLastTitle.get(playerRef.getUuid());
         if (!titleText.equals(previousTitle)) {
            this.playerLastTitle.put(playerRef.getUuid(), titleText);
            EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, SUBTITLE_MESSAGE, false, (String)null, 2.0F, 0.5F, 0.5F);
         }

      }

}

public void removePlayer(UUID playerId) {
this.playerLastTitle.remove(playerId);
}
}

    package com.hytale.guild.claim;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent.Pre;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytale.guild.Guild;
import com.hytale.guild.GuildManager;
import com.hytale.guild.util.Lang;
import java.awt.Color;
import java.util.UUID;

public class ClaimProtectionSystems {
private static boolean isProtected(Store<EntityStore> store, Ref<EntityStore> ref, Vector3i pos) {
PlayerRef player = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
if (player == null) {
return false;
} else {
World world = ((EntityStore)store.getExternalData()).getWorld();
String worldName = world.getName();
Claim claim = ClaimManager.getInstance().getClaimAt(worldName, pos.x, pos.z);
if (claim == null) {
return false;
} else {
UUID playerId = player.getUuid();
Guild playerGuild = GuildManager.getInstance().getPlayerGuild(playerId);
if (playerGuild != null && playerGuild.getId().equals(claim.getGuildId())) {
return false;
} else {
player.sendMessage(Message.raw(Lang.get("claim.protected")).color(Color.RED));
return true;
}
}
}
}

public static class UseBlockProtectionSystem extends EntityEventSystem<EntityStore, Pre> {
public UseBlockProtectionSystem() {
super(Pre.class);
}

      public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buf, Pre event) {
         Ref<EntityStore> ref = chunk.getReferenceTo(index);
         Vector3i pos = event.getTargetBlock();
         if (ClaimProtectionSystems.isProtected(store, ref, pos)) {
            event.setCancelled(true);
         }

      }

      public Query<EntityStore> getQuery() {
         return Archetype.empty();
      }

}

public static class BreakBlockProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
public BreakBlockProtectionSystem() {
super(BreakBlockEvent.class);
}

      public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buf, BreakBlockEvent event) {
         Ref<EntityStore> ref = chunk.getReferenceTo(index);
         Vector3i pos = event.getTargetBlock();
         if (ClaimProtectionSystems.isProtected(store, ref, pos)) {
            event.setCancelled(true);
         }

      }

      public Query<EntityStore> getQuery() {
         return Archetype.empty();
      }

}

public static class PlaceBlockProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
public PlaceBlockProtectionSystem() {
super(PlaceBlockEvent.class);
}

      public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buf, PlaceBlockEvent event) {
         Ref<EntityStore> ref = chunk.getReferenceTo(index);
         Vector3i pos = event.getTargetBlock();
         if (ClaimProtectionSystems.isProtected(store, ref, pos)) {
            event.setCancelled(true);
         }

      }

      public Query<EntityStore> getQuery() {
         return Archetype.empty();
      }

}
}
    