package com.google.ar.core.examples.java.helloar.scene.record;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SceneRecordTest {
    static private final String ANDY = "andy";

    private Gson gson;
    private SceneRecord sceneRecord;
    private static final float DELTA = 1e-4f;

    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder().setPrettyPrinting().create();

        ObjectRecord root = new ObjectRecord();
        root.setId(1);
        root.setParentId(0);
        root.setName(ANDY);
        root.setPoseRecord(new PoseRecord(
                new Translation(0, 0, -2),
                Rotation.Identity()
        ));

        ObjectRecord child1 = new ObjectRecord();
        child1.setId(2);
        child1.setParentId(1);
        child1.setName(ANDY);
        child1.setPoseRecord(new PoseRecord(
                new Translation(1, 0, 0),
                Rotation.Identity()
        ));

        ObjectRecord child2 = new ObjectRecord();
        child2.setId(3);
        child2.setParentId(1);
        child2.setName(ANDY);
        child2.setPoseRecord(new PoseRecord(
                new Translation(0, 0, 1),
                Rotation.Identity()
        ));

        sceneRecord = new SceneRecord(new ObjectRecord[]{root, child1, child2});
    }

    @Test
    public void testSmoke() {
        String s = gson.toJson(sceneRecord);
        SceneRecord reverse = gson.fromJson(s, SceneRecord.class);

        assertEquals(
                sceneRecord.getObjectRecords().length,
                reverse.getObjectRecords().length
        );

        for (int i = 0; i != sceneRecord.getObjectRecords().length; ++i) {
            ObjectRecord first = sceneRecord.getObjectRecords()[i];
            ObjectRecord second = reverse.getObjectRecords()[i];

            assertEquals(first.getModelName(), second.getModelName());
            assertEquals(first.getId(), second.getId());
            assertEquals(first.getParentId(), second.getParentId());

            PoseRecord firstPose = first.getPoseRecord();
            Rotation firstRotation = firstPose.getRotation();
            Translation firstTranslation = firstPose.getTranslation();

            PoseRecord secondPose = second.getPoseRecord();
            Rotation secondRotation = secondPose.getRotation();
            Translation secondTranslation = secondPose.getTranslation();

            assertEquals(firstRotation.getX(), secondRotation.getX(), DELTA);
            assertEquals(firstRotation.getY(), secondRotation.getY(), DELTA);
            assertEquals(firstRotation.getZ(), secondRotation.getZ(), DELTA);
            assertEquals(firstRotation.getAngle(), secondRotation.getAngle(), DELTA);

            assertEquals(firstTranslation.getX(), secondTranslation.getX(), DELTA);
            assertEquals(firstTranslation.getY(), secondTranslation.getY(), DELTA);
            assertEquals(firstTranslation.getZ(), secondTranslation.getZ(), DELTA);
        }
    }
}