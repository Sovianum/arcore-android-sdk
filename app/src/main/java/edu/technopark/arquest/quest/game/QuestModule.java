package edu.technopark.arquest.quest.game;

import android.content.Context;

import com.viro.core.Object3D;
import com.viro.core.PhysicsBody;
import com.viro.core.PhysicsShapeSphere;
import com.viro.core.Vector;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import edu.technopark.arquest.App;
import edu.technopark.arquest.GameModule;
import edu.technopark.arquest.R;
import edu.technopark.arquest.common.CollectionUtils;
import edu.technopark.arquest.game.InteractionResult;
import edu.technopark.arquest.game.InteractiveObject;
import edu.technopark.arquest.game.Item;
import edu.technopark.arquest.game.Place;
import edu.technopark.arquest.game.script.ActionCondition;
import edu.technopark.arquest.game.script.ObjectState;
import edu.technopark.arquest.game.script.ScriptAction;
import edu.technopark.arquest.game.slot.Slot;
import edu.technopark.arquest.model.Quest;
import edu.technopark.arquest.model.VisualResource;

@Module
public class QuestModule {
    @Inject
    GameModule gameModule;

    @Inject
    Context context;

    @Provides
    @Singleton
    public QuestModule provideQuestModule() {
        App.getAppComponent().inject(this);
        return this;
    }

    public List<Quest> getQuests() {
        //Quest q1 = new Quest(
        //        2,
        //        "Демо-квест взаимодействие с персонажами",
        //        "Это демонстрационный квест из одного места." +
        //                "Здесь вы можете опробовать взаимодействие с виртуальным объектами", 3
        //);
        Quest q1 = new Quest(
                2,
                "Раздобудьте карту",
                "Вы попадаете в очень странное место с загадочными персонажами. Ваша цель " +
                        "покинуть это место. Распросив разных существ, вы понимаете, что вам нужна таинственная карта," +
                        "которую можно найти у местного свина-торговца. Удастся ли вам договориться и выбраться?",
                3
        );
        q1.setCurrPurpose("Подойдите к андроиду неподалеку");

        if (gameModule.isWithAR()) {
//            q1.addPlace(getNewStyleInteractionDemoPlace());
//            q1.addPlace(getSingleObjectPlace());
            q1.addPlace(getSkullPlace());
        } else {
            q1.addPlace(new Place());
        }

        return Collections.singletonList(q1);
    }

    public Place getSingleObjectPlace() {
        InteractiveObject andy = new InteractiveObject(
                1, "obj", "obj"
        );
        andy.setScale(new Vector(0.0003, 0.0003, 0.0003));
        andy.setVisualResource(new VisualResource(Object3D.Type.FBX).setModelUri("file:///android_asset/helmet_skull.vrx"));
        andy.setPosition(new Vector(0, 0, -0.5f));
        andy.initPhysicsBody(PhysicsBody.RigidBodyType.KINEMATIC, 0, new PhysicsShapeSphere(0.3f));

        Place place = new Place();
        place.loadInteractiveObjects(CollectionUtils.listOf(andy));

        return place;
    }

    public Place getSkullPlace() {
        final float mainScale = 0.0005f;
        final float smallScale = mainScale / 3;
        final String assetPrefix = "file:///android_asset/scene/";

        SkullPlaceConstructor constructor = new SkullPlaceConstructor(mainScale, smallScale, assetPrefix);
        return constructor.getPlace();
    }

    public Place getNewStyleInteractionDemoPlace() {
        final float scale = 0.001f;

        final Item rose = new Item(
                20, "rose", "rose",
                new VisualResource(Object3D.Type.OBJ).setModelUri("file:///android_asset/rose.obj").setTextureUri("rose.jpg")
        );
        rose.setScale(new Vector(scale, scale, scale));

        final Item banana = new Item(
                10, "banana", "banana",
                new VisualResource(Object3D.Type.FBX).setModelUri("file:///android_asset/banana.obj").setTextureUri("banana.lpg")
        );
        banana.setScale(new Vector(scale, scale, scale));

        InteractiveObject andy = new InteractiveObject(
                1, "andy", "andy",
                CollectionUtils.singleItemList(rose)
        );
        final InteractiveObject whiteGuy = new InteractiveObject(
                2, "white", "white",
                CollectionUtils.singleItemList(banana)
        );

        andy.setVisualResource(new VisualResource(Object3D.Type.OBJ).setModelUri("file:///android_asset/andy.obj").setTextureUri("andy.png"));
        andy.setPosition(new Vector(0, 0, -0.5f));
        andy.initPhysicsBody(PhysicsBody.RigidBodyType.KINEMATIC, 0, new PhysicsShapeSphere(0.3f));

        ObjectState andyState1 = new ObjectState(1, true);
        andyState1.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.journalRecordResult("Андроид сказал: Возьми поесть у белого человека"),
                                InteractionResult.nextPurposeResult("Найдите неподалеку белого человека и попросите поесть"),
                                InteractionResult.transitionsResult(
                                        CollectionUtils.listOf(
                                                new ScriptAction.StateTransition(andy.getName(), 2),
                                                new ScriptAction.StateTransition(whiteGuy.getName(), 1)
                                        )
                                ),
                                InteractionResult.hintResult(R.id.journal_btn_hint)
                        )
                )
        ));
        andyState1.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.singleItemList(1),
                CollectionUtils.singleItemList(
                        new ActionCondition(1)
                )
        ));

        ObjectState andyState2 = new ObjectState(2, false);
        andyState2.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.journalRecordResult("Андроид сказал: Отблагодари белого человека"),
                                InteractionResult.nextPurposeResult("Передайте розу белому человеку"),
                                InteractionResult.newItemsResult(new Slot.RepeatedItem(rose)),
                                InteractionResult.takeItemsResult(new Slot.RepeatedItem(banana)),
                                InteractionResult.transitionsResult(
                                        CollectionUtils.listOf(
                                                new ScriptAction.StateTransition(andy.getName(), 3)
                                        )
                                )
                        )
                )
        ));
        andyState2.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.singleItemList(1),
                CollectionUtils.singleItemList(
                        new ActionCondition(
                                CollectionUtils.singleItemList(
                                        new ActionCondition.ItemInfo(banana.getId(), 1)
                                ),
                                2
                        )
                )
        ));

        ObjectState andyState3 = new ObjectState(3, false);
        andyState3.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.messageResult("Мне нечего тебе сказать")
                        )
                )
        ));
        andyState3.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.singleItemList(1),
                CollectionUtils.singleItemList(new ActionCondition(3))
        ));

        andy.setStates(CollectionUtils.listOf(andyState1, andyState2, andyState3));


        whiteGuy.setVisualResource(new VisualResource(Object3D.Type.OBJ).setModelUri("file:///android_asset/bigmax.obj").setTextureUri("bigmax.jpg"));
        whiteGuy.setPosition(new Vector(0.25f, 0, 0));
        whiteGuy.setScale(new Vector(0.003f, 0.003f, 0.003f));
        whiteGuy.initPhysicsBody(PhysicsBody.RigidBodyType.KINEMATIC, 0, new PhysicsShapeSphere(0.3f));
        whiteGuy.setStates(CollectionUtils.listOf(ObjectState.enableObjectState(0, true, false)));

        ObjectState guyState0 = new ObjectState(0, true);
        guyState0.setEnabled(false);

        ObjectState guyState1 = new ObjectState(1, false);
        guyState1.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.newItemsResult(new Slot.RepeatedItem(banana)),
                                InteractionResult.journalRecordResult("Белый человек сказал: Дай ему поесть"),
                                InteractionResult.nextPurposeResult("Передайте банан андроиду"),
                                InteractionResult.hintResult(R.id.inventory_btn_hint),
                                InteractionResult.transitionsResult(CollectionUtils.listOf(
                                        new ScriptAction.StateTransition(whiteGuy.getName(), 2)
                                ))
                        )
                )
        ));
        guyState1.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.listOf(1),
                CollectionUtils.listOf(new ActionCondition(1))
        ));

        ObjectState guyState2= new ObjectState(2, false);
        guyState2.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.journalRecordResult("Белый человек сказал: Да за кого он меня принимает?!"),
                                InteractionResult.nextPurposeResult(""),
                                InteractionResult.questEndResult(),
                                InteractionResult.transitionsResult(CollectionUtils.listOf(
                                        new ScriptAction.StateTransition(whiteGuy.getName(), 3)
                                ))
                        )
                )
        ));
        guyState2.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.listOf(1),
                CollectionUtils.listOf(new ActionCondition(
                        CollectionUtils.listOf(
                                new ActionCondition.ItemInfo(rose.getId(), 1)
                        ),
                        2
                ))
        ));

        ObjectState guyState3 = new ObjectState(3, false);
        guyState3.setActions(CollectionUtils.listOf(
                new ScriptAction(
                        1,
                        CollectionUtils.listOf(
                                InteractionResult.messageResult("Ммм?")
                        )
                )
        ));
        guyState3.setConditions(ActionCondition.makeConditionMap(
                CollectionUtils.listOf(1),
                CollectionUtils.listOf(new ActionCondition(3))
        ));

        whiteGuy.setStates(CollectionUtils.listOf(guyState0, guyState1, guyState2, guyState3));

        andy.setAction(andy.getActionFromStates());
        whiteGuy.setAction(whiteGuy.getActionFromStates());
        Place place = new Place();
        place.loadInteractiveObjects(CollectionUtils.listOf(andy, whiteGuy));

        return place;
    }
}
