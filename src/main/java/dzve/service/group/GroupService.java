package dzve.service.group;

import com.hypixel.hytale.logger.HytaleLogger;
import dzve.model.Group;
import dzve.model.GroupMember;
import dzve.service.JsonStorage;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dzve.config.BetterGroupSystemPluginConfig.DATA_FOLDER;
import static dzve.config.BetterGroupSystemPluginConfig.FILE_NAME;

public class GroupService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final GroupService instance = new GroupService();
    private final JsonStorage<GroupData> storage;
    private final Map<UUID, Group> groups;
    private final Map<UUID, GroupMember> onlinePlayerCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> invitations = new ConcurrentHashMap<>();

    private GroupService() {
        File dataFolder = new File(DATA_FOLDER);
        File groupsFile = new File(dataFolder, FILE_NAME);
        this.storage = new JsonStorage<>(groupsFile, GroupData.class);
        this.groups = new HashMap<>();
        loadGroups();
    }

    public static synchronized GroupService getInstance() {
        return instance;
    }

    private void loadGroups() {
        GroupData data = storage.load();
        if (data != null && data.getGroups() != null) {
            this.groups.putAll(data.getGroups());
            LOGGER.atInfo().log("Loaded " + groups.size() + " groups from storage.");
        } else {
            LOGGER.atInfo().log("No existing group data found. Starting fresh.");
        }
    }

    public void saveGroups() {
        GroupData data = new GroupData(new HashMap<>(groups));
        storage.saveAsync(data);
    }

    public void saveGroupsSync() {
        GroupData data = new GroupData(new HashMap<>(groups));
        storage.saveSync(data);
    }

    public void shutdown() {
        saveGroupsSync();
        storage.shutdown();
    }

    @Getter
    private static class GroupData {
        private Map<UUID, Group> groups;

        public GroupData(Map<UUID, Group> groups) {
            this.groups = groups;
        }

    }
}
