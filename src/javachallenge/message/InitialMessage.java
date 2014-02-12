package javachallenge.message;

import javachallenge.util.Map;

import java.io.Serializable;

/**
 * Created by mohammad on 2/6/14.
 */
public class InitialMessage implements Serializable {
    private Map map;
    private int teamId;
    private int resource;

    public InitialMessage(Map map, int teamId, int resource) {
        this.map = map;
        this.teamId = teamId;
        this.resource = resource;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }
}
