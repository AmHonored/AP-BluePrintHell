package server.domain;

import server.persistence.RunRepository;

import java.sql.SQLException;
import java.util.List;

public class LeaderboardService {
    public List<RunRepository.LeaderboardEntry> topTimes(String levelCode, int limit) throws SQLException {
        return RunRepository.topTimes(levelCode, limit);
    }

    public List<RunRepository.LeaderboardEntry> topXpAllTime(int limit) throws SQLException {
        return RunRepository.topXpAllTime(limit);
    }

    public List<RunRepository.LeaderboardEntry> topCampaignTimeAllTime(int limit) throws SQLException {
        return RunRepository.topCampaignTimeAllTime(limit);
    }
}


