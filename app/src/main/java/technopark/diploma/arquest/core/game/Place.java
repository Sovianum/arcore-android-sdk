package technopark.diploma.arquest.core.game;

import android.location.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import technopark.diploma.arquest.core.ar.SceneObject;

public class Place {
    private int id;
    private String name;
    private String description;
    private Map<Integer, InteractiveObject> interactiveObjects;
    private Location location;

    public Place() {
        this(0, "", "");
    }

    public Place(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = new Location("");
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        interactiveObjects = new HashMap<>();
    }

    public InteractiveObject getInteractiveObject(int id) {
        return interactiveObjects.get(id);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (location == null) {
            return;
        }
        this.location = location;
    }

    public List<SceneObject> getAll() {
        List<SceneObject> result = new ArrayList<>(interactiveObjects.size());
        result.addAll(interactiveObjects.values());

        for (InteractiveObject obj : interactiveObjects.values()) {
            result.addAll(obj.getItems());
        }
        return result;
    }

    public Map<Integer, InteractiveObject> getInteractiveObjects() {
        return interactiveObjects;
    }

    public Map<Integer, InteractiveObject> getAccessibleInteractiveObjects() {
        Map<Integer, InteractiveObject> result = new HashMap<>();
        for (Map.Entry<Integer, InteractiveObject> entry : interactiveObjects.entrySet()) {
            if (entry.getValue().isEnabled()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void setInteractiveObjects(Map<Integer, InteractiveObject> interactiveObjects) {
        this.interactiveObjects = interactiveObjects;
    }

    public void loadInteractiveObjects(Iterable<InteractiveObject> interactiveObjects) {
        for (InteractiveObject interactiveObject : interactiveObjects) {
            this.interactiveObjects.put(interactiveObject.getIdentifiable().getId(), interactiveObject);
        }
    }
}
