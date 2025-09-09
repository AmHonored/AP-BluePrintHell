package server.persistence;

import server.domain.UserProfile;

public interface ProfileRepository {
    UserProfile getByDeviceId(String deviceId);
    UserProfile upsert(UserProfile profile);
}



