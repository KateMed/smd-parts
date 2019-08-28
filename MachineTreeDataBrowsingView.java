package ru.vibrotek.gui.tabs.browsing;

import org.springframework.beans.factory.annotation.Autowired;
import ru.vibrotek.chart.prodivers.ArchiveValueProvider;
import ru.vibrotek.domain.ClientPush;
import ru.vibrotek.domain.nodes.ClientTreeNode;
import ru.vibrotek.domain.nodes.WorkStateNode;
import ru.vibrotek.service.copier.ClientPushTask;
import ru.vibrotek.components.ExtPanel;
import ru.vibrotek.components.ViPopupMenu;
import ru.vibrotek.domain.SharedServerModelHolder;
import ru.vibrotek.domain.handbook.unittype.UnitType;
import ru.vibrotek.domain.nodes.TreeHierarchyOwner;
import ru.vibrotek.domain.nodes.machine.danger.DangerStateOwner;
import ru.vibrotek.domain.nodes.machine.danger.TechnicalStateService;
import ru.vibrotek.domain.nodes.machine.level1.OrganizationMachineNode;
import ru.vibrotek.domain.nodes.machine.level3.MachineNode;
import ru.vibrotek.domain.nodes.machine.level3.states.WorkState;
import ru.vibrotek.domain.nodes.machine.level6.MachinePack;
import ru.vibrotek.domain.nodes.machine.level6.MachinePackList;
import ru.vibrotek.domain.nodes.machine.level6.MeasurementOwner;
import ru.vibrotek.domain.nodes.machine.level6.MeasuringPointNode;
import ru.vibrotek.domain.nodes.machine.level7.Measurement;
import ru.vibrotek.domain.nodes.machine.level7.MeasurementPack;
import ru.vibrotek.domain.nodes.machine.level8.Storage;
import ru.vibrotek.domain.service.ParamValuePushService;
import ru.vibrotek.gui.TitledNode;
import ru.vibrotek.gui.chart.v3.PanelWithChart;
import ru.vibrotek.gui.chart.v3.SpecterAnalysisPanel;
import ru.vibrotek.gui.components.InnerContainer;
import ru.vibrotek.gui.components.InnerPanel;
import ru.vibrotek.gui.components.InnerView;
import ru.vibrotek.domain.nodes.ArchiveClientLoadingService;
import ru.vibrotek.gui.tabs.browsing.archive.MachineArchiveLoadingView;
import ru.vibrotek.gui.tabs.state.DangerLevelTreeIcon;
import ru.vibrotek.gui.tabs.state.dependency.ShowNodeEvent;
import ru.vibrotek.gui.tabs.state.dependency.ShowNodeEventHandler;
import ru.vibrotek.gui.tabs.state.level6.MachineNodeStateView;
import ru.vibrotek.gui.tabs.state.level6.MeasurementNodeFixedStateView;
import ru.vibrotek.gui.tabs.state.level6.MeasuringPointNodeStateView;
import ru.vibrotek.gui.tabs.state.level6.StorageNodeStateView;
import ru.vibrotek.model.SharedObject;
import ru.vibrotek.model.TimeLabel;
import ru.vibrotek.model.collection.list.ViList;
import ru.vibrotek.model.collection.list.ViLocalList;
import ru.vibrotek.model.collection.map.ViLocalMap;
import ru.vibrotek.param.description.ParamDescription;
import ru.vibrotek.param.value.ParamValue;
import ru.vibrotek.param.value.impl.SpecterParamValue;
import ru.vibrotek.service.InjectorService;
import ru.vibrotek.service.PushParamValueService;
import ru.vibrotek.service.transaction.query.ClientCommitLogic;
import ru.vibrotek.service.transaction.query.ReadLogic;
import ru.vibrotek.service.transaction.query.ServerCommitLogic;
import ru.vibrotek.ui.RefreshableService;
import ru.vibrotek.ui.components.tree.ExtTree;
import ru.vibrotek.ui.components.tree.model.TwoCategoryTreeModel;
import ru.vibrotek.utils.ExtMessageDialogFactory;
import ru.vibrotek.utils.ExtMessageDialogPanel;
import ru.vibrotek.utils.ViResourceBundle;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

import static ru.vibrotek.logger.ViLogger.UI_LOGGER;

/**
 *
 */
public class MachineTreeDataBrowsingView extends ExtPanel implements InnerView {

    private SharedObject sharedObject;
    private ClientTreeNode sharedNode;
    private SharedObject selected;
    private static boolean deleteButtonIsEnabled;
    protected final PushParamValueService pushParamValueService = InjectorService.get(PushParamValueService.class);
    protected  final  ParamValuePushService paramValuePushService = InjectorService.get(ParamValuePushService.class);
    @Autowired
    private TechnicalStateService technicalStateService;

    static {
        MachineViewStateSpec.register(MeasuringPointNode.class,
                                      (node, machineState) -> new MeasuringPointNodeStateView((MeasuringPointNode) node));
        MachineViewStateSpec.register(MachineNode.class, (node, machineState) -> new MachineNodeStateView((MachineNode) node));
        MachineViewStateSpec.register(MachineNode.class,
                                      (node, machineState) -> new MachineArchiveLoadingView((MachineNode) node, deleteButtonIsEnabled));
        MachineViewStateSpec.register(Measurement.class,
                                      (node, machineState) -> new MeasurementNodeFixedStateView((Measurement) node, machineState));
        MachineViewStateSpec.register(Storage.class, (node, machineState) -> new StorageNodeStateView((Storage) node));
    }

    private final ArchiveClientLoadingService archiveClientLoadingService;

    private MachineTreeModel machineTreeModel;
    private InnerContainer innerContainer;
    private ExtTree<ClientTreeNode> extTree;
    private NyViPopupMenu popUp;
    private ClientTreeNode mExtTreeNode;
    private List<ViLocalList<ParamValue>> valueList;

    private class NyViPopupMenu extends ViPopupMenu {

        private final JMenuItem copyMenuItem;
        private final JMenuItem pasteMenuItem;
        //private final JMenuItem createSchemeItem;
        //private SharedObject sharedObject;

        public NyViPopupMenu() {
            super(ViResourceBundle.getString("machine_view.menu.title1"));
            copyMenuItem = new JMenuItem(ViResourceBundle.getString("machine_view.menu.copy_item.name"));
            copyMenuItem.addActionListener(e -> copy());
            add(copyMenuItem);
            pasteMenuItem = new JMenuItem(ViResourceBundle.getString("machine_view.menu.insert_item_name"));
            pasteMenuItem.addActionListener(e -> paste2());
            add(pasteMenuItem);
        }
        @Override
        public void extShow(MouseEvent e) {
            selected = null;

            Optional<ClientTreeNode> closestNodeForLocation = extTree.getClosestNodeForLocation(e.getX(), e.getY());
            closestNodeForLocation.ifPresent(machineExtTreeNode -> {
                Object userObject = machineExtTreeNode.getUserObject();
                if (userObject instanceof SharedObject) {

                    selected = (SharedObject) userObject;
                    popUp.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    UI_LOGGER.warn("Узел - не SharedObject ({}), меню копирования не покажем", userObject.getClass()
                            .getSimpleName());
                }
            });
            if (!closestNodeForLocation.isPresent()) {
                UI_LOGGER.warn("Не найден узел по координатам {} {}, меню копирования не покажем", e.getX(), e.getY());
            }
            refreshState();
        }
        private void refreshState() {
            setLabel(ViResourceBundle.getString("machine_view.menu.title2"));
            pasteMenuItem.setEnabled(false);
            copyMenuItem.setEnabled(false);
            if (selected!=null) {
                new ReadLogic("sdfsdf", () -> {
                    setLabel(MessageFormat.format(
                            ViResourceBundle.getString("machine_view.menu.select_item_title"), selected.getTreeNodeTitle()));
                });
                if (selected instanceof TreeHierarchyOwner) {
                }

                copyMenuItem.setEnabled(true);
                if (sharedObject!=null) {
                    pasteMenuItem.setEnabled(true);
                }
            }
        }
    }

    private void getDataBasketFromTreeHA(ViList<TreeHierarchyOwner> thoList, MeasuringPointNode mpn, ParamValue pv, Measurement m) {
        thoList.forEach(treeHierarchyOwner -> {
               if(mpn.getKeyUuid().equals(((Measurement)treeHierarchyOwner).getMeasuringPointNode().getKeyUuid())){
                   //paramValuePushService.pushParamValueToMeasurement(m, pv);
               };
            });
    }

    private  void push(UUID id, MeasurementPack mp, Map<Measurement,ParamValue> mpv, TimeLabel timeLabel){
       /* new ServerCommitLogic("read_dsfsoidfsojvc", () -> {
            map.forEach((measurement, paramValue) -> {
                packCollector.pushDataToWaitingQueue(archiveRecord.getId(), measurement, paramValue);
            });
        });*/
        pushParamValueService.push(id, mp, mpv, timeLabel);
    }

    private  void paste2(){
        ClientPush clientPush = new ClientPush();
        clientPush.copy(sharedObject, sharedNode, archiveClientLoadingService, extTree, pushParamValueService);
    }

    private void paste() {
        ClientPushTask clientPushTask = new ClientPushTask();
        //PushParamValueService pushParamValueService = InjectorService.get(PushParamValueService.class);
        new ClientCommitLogic("commit_3t89yghoilbgoi3", () -> {
            //sharedObject = (SharedObject) extTree.getSelectedNode().getUserObject();
            //new ReadLogic("read_alksdjkas", () -> {
                //// Object userObject = extTree.getSelectedNode().getUserObject();
                //загружаем все измерения для выбранного узла(возможно это не нужно)
                ////getMeasurement();
                ///создеем копируемый объект
                //extTree.reloadDataUnderTransactionInner();
                //MachinePackList machinePackList = ((MachineNode)extTree.getSelectedNode().getUserObject()).getMachinePackList();
                //sharedObject = (SharedObject) extTree.getSelectedNode().getUserObject();
                //int i = 1;
                extTree.reloadDataUnderTransactionInner();

                //SharedObject selected1  = (SharedObject) extTree.getSelectedNode().getUserObject();
                //MachineNodeCopier.copy(selected1, sharedObject);

                //!!!! Попытка номер 1!!

                //PushParamValueService basePushTask = new PushParamValueService();
                Iterator iterator1 = sharedNode.children().asIterator();
                //найдем доступные режимы
                List<ClientTreeNode> mnFrom = new ArrayList<>();
                List<ClientTreeNode> mnTo = new ArrayList<>();
                while(iterator1.hasNext()){
                    //mn.add((DefaultTreeNode)extTree.getSelectedNode().children().nextElement());
                    mnFrom.add((ClientTreeNode)iterator1.next());
                    //iterator.next();
                }
                Iterator iterator2 = extTree.getSelectedNode().children().asIterator();
                while(iterator2.hasNext()){
                    //mn.add((DefaultTreeNode)extTree.getSelectedNode().children().nextElement());
                    mnTo.add((ClientTreeNode)iterator2.next());
                    //iterator.next();
                }
                //берем измерения идля каждого режима для первой и второй машины
                for(int y=0; y<mnFrom.size(); y++) {
                    ClientTreeNode workStateFrom = mnFrom.get(y);
                    ClientTreeNode workStateTo = mnTo.get(y);

                    if (workStateFrom.getTitle().equals(workStateTo.getTitle())) {
                        //PackCollector packCollector = InjectorService.get(PackCollector.class);
                        ClientTreeNode workStateNode = workStateTo;
                        WorkState workState = (WorkState) workStateNode.getUserObject();
                        List<Measurement> measurementsFROM = measList(mnFrom.get(y));
                        List<Measurement> measurementsTO = measList(mnTo.get(y));
                        //комплекты
                        MachinePackList machinePackListTo = ((MachineNode) workStateTo.getParent().getUserObject()).getMachinePackList();
                        MachinePackList machinePackListFrom = ((MachineNode) workStateFrom.getParent().getUserObject()).getMachinePackList();


                        //archiveRecord.setTechnicalState(technicalStateService.getDangerState(mTo.getMeasuringPointNode().getMachineNode())
                        //.getTechnicalState());

                        //Создадим списки измерений (спектров) для всех точек для каждого комплекта
                        //по количеству комплектов MachinePackList.size()
                        Map<MachinePack, List<Measurement>> mapOfMeashuringForMachinePackFrom = new HashMap<>();
                        machinePackListFrom.forEach(machinePack -> {
                            List<Measurement> measurementsList = new ArrayList<>();
                            machinePack.forEachNode(measuringPointNode -> {
                                for (int num2 = 0; num2 < machinePack.getMeasurementPack(measuringPointNode).getMeasurements().size(); num2++) {
                                    Measurement m = machinePack.getMeasurementPack(measuringPointNode).getMeasurements().get(num2);
                                    measurementsList.add(m);
                                }
                            });
                            mapOfMeashuringForMachinePackFrom.put(machinePack, measurementsList);
                        });

                        //свяжем значения уидов измерений для результатов до копирования и после для данного режима!!!
                        Map<UUID, Measurement> uuidMeasuringMap = new TreeMap<>();
                        for (int i = 0; i < measurementsTO.size(); i++) {
                            Measurement mFrom = measurementsFROM.get(i);
                            Measurement mTo = measurementsTO.get(i);
                            uuidMeasuringMap.put(mFrom.getKeyUuid(), mTo);
                        }
                        Map<UUID, MachinePack> uuidMachinePackMap = new TreeMap<>();
                        for (int i = 0; i < machinePackListTo.size(); i++) {
                            MachinePack mpTo = machinePackListTo.get(i);
                            MachinePack mpFrom = machinePackListFrom.get(i);
                            uuidMachinePackMap.put(mpFrom.getKeyUuid(), mpTo);
                        }
                        //Map<UUID, WorkState> uuidWorkStateMap = new TreeMap<>();
                        //uuidWorkStateMap.put(((WorkState)workStateFrom.getUserObject()).getKeyUuid(), (WorkState) workStateTo.getUserObject());

                        //теперь пробежимся для данного режима по всем комплектам по очереди и запишем их на сервер
                        mapOfMeashuringForMachinePackFrom.forEach((machinePackFrom, measurementsListFrom) -> {
                            MachinePack machinePackTo = uuidMachinePackMap.get(machinePackFrom.getKeyUuid());
                            HashMap<Measurement, ParamValue> map = new HashMap<>();
                            /*ArchiveRecord archiveRecord = new ArchiveRecord();
                            archiveRecord.setId(UUID.randomUUID());
                            archiveRecord.setCreationTimestamp(System.currentTimeMillis());
                            archiveRecord.setWorkState((WorkState)workStateTo.getUserObject());
                            //archiveRecord.setTechnicalState();
                            archiveRecord.setMachinePack(machinePackTo);*/
                            Boolean spectraExist = false;
                            for (Measurement measurementFrom : measurementsListFrom) {
                                Measurement measurementTo = uuidMeasuringMap.get(measurementFrom.getKeyUuid());
                                ViLocalList<ParamValue> paramValues = archiveClientLoadingService.createList(measurementFrom, ((WorkState) workStateFrom.getUserObject()).getKeyStr());
                                for (ParamValue pv :
                                        paramValues) {
                                    if (pv instanceof SpecterParamValue) {
                                        spectraExist = true;
                                        ParamValue pv2 = new ParamValue() {
                                            @Override
                                            public ParamDescription getDescription() {
                                                return pv.getDescription();
                                            }
                                        };
                                        UnitType ut = new UnitType();
                                        ut.setFieldValues(pv.getUnitType());
                                        pv2.setUnitType(ut);
                                        map.put(measurementTo, pv2);
                                    }
                                }
                            }
                            //TimeLabel timeLabel = new TimeLabel(System.currentTimeMillis());
                            Map<MeasurementPack, List<Measurement>> measurementPackMap = new HashMap<>();
                            for(Measurement key : map.keySet()){
                                if(!measurementPackMap.keySet().contains(key.getMeasurementOwner())){
                                    measurementPackMap.put((MeasurementPack)key.getMeasurementOwner(), new ArrayList<>());
                                }
                                if(measurementPackMap.keySet().contains(key.getMeasurementOwner())){
                                    measurementPackMap.get(key.getMeasurementOwner()).add(key);                                }

                            }

                            for(MeasurementPack mp : measurementPackMap.keySet()){
                                TimeLabel timeLabel = new TimeLabel(System.currentTimeMillis());
                                Map<Measurement, ParamValue> mpv= new ViLocalMap<>();
                                measurementPackMap.get(mp).forEach(consumer -> {
                                    mpv.put(consumer, map.get(consumer));
                                });
                                    if(!mpv.isEmpty()){
                                        push(UUID.randomUUID(), mp, mpv, timeLabel);
                                        return;
                                    }
                            }
                            //pushParamValueService.push(archiveRecord.getId(), measurementPack, map, timeLabel);
                            /*if (spectraExist && packCollector.tryAddNewRecordFor(archiveRecord)) {
                                new ServerCommitLogic("read_dsfsoidfsojvc", () -> {
                                    map.forEach((measurementy, paramValue) -> {
                                        packCollector.pushDataToWaitingQueue(archiveRecord.getId(), measurementy, paramValue);
                                    });
                                });
                            }*/
                        });


                        /*for (int i = 0; i < measurementsTO.size(); i++) {
                            Measurement mFrom = measurementsFROM.get(i);
                            Measurement mTo = measurementsTO.get(i);

                            ViLocalList<ParamValue> paramValues = archiveClientLoadingService.createList(mFrom,((WorkState)workStateFrom.getUserObject()).getKeyStr());
                            for (ParamValue pv :
                                    paramValues) {
                                //pushParamValueService.push( UUID.randomUUID(),measurementPack, );
                                if (pv instanceof SpecterParamValue) {
                                    //UnitType ut =  pv.getUnitType();
                                    //UnitType value = new UnitType();
                                    //value.setFieldValues(ut);
                                    ParamValue pv2 = new ParamValue() {
                                        @Override
                                        public ParamDescription getDescription() {
                                            return pv.getDescription();
                                        }
                                    };
                                    UnitType ut = new UnitType();
                                    ut.setFieldValues(pv.getUnitType());
                                    //ut.setKeyUuid(UUID.randomUUID());
                                    pv2.setUnitType(ut);
                                    //pv2.getUnitType().setKeyUuid(UUID.randomUUID());
                                    //f@BViChain viChain = InjectorService.get(ViChain .class, pv.getDescription());

                                    //getDataBasketFromTreeHA(thhoToList, mTo.getMeasuringPointNode(), pv);
                                    //PushSpecterConsumer pushSpecterConsumer = new PushSpecterConsumer(viChain, mTo);
                                    //paramValuePushService = new ParamValuePushService();
                                    //mTo.getDataBasket();
                                    //mTo.getDataBasketForCurrentState();
                                    //int j = 0;

                                    //mTo.getMeasuringPointNode().getTreeHierarchyAdapter().getChildNodes().forEach(treeHierarchyOwner -> {
                                    //Measurement m = (Measurement) treeHierarchyOwner;
                                    //paramValuePushService.pushParamValueToMeasurement(m, pv);
                                    //DefaultTreeNode node = findNode(extTree.getSelectedNode(), mTo);
                                    //WorkStateNode workStateNode = (WorkStateNode) node;
                                    //WorkStateNode workStateNode = (WorkStateNode) mTo.getMeasurementOwner();


                                    HashMap<Measurement, ParamValue> map = new HashMap<>();
                                    map.put(mTo, pv2);
                                    MachinePack machinePack = null;
                                    for (int k = 0; k < machinePackList.size(); k++) {
                                        if (machinePackList.get(k).getMeasurementPack(mTo.getMeasuringPointNode()).getMeasurements().contains(mTo)) {
                                            machinePack = machinePackList.get(k);
                                        }
                                    }
                                    archiveRecord.setMachinePack(machinePack);
                                    if (packCollector.tryAddNewRecordFor(archiveRecord)) {
                                        new ServerCommitLogic("read_dsfsoidfsojvc", () -> {
                                            map.forEach((measurement, paramValue) -> {
                                                packCollector.pushDataToWaitingQueue(archiveRecord.getId(), measurement, paramValue);
                                            });
                                        });
                                    }
                                    //ArrayList<MachineState> tStates = new ArrayList<>();
                                    //mFrom.getMachineNode().getMachineStatesManager().getWorkStates().forEach( tStates::add);
                                    //DataBasket sourceBasket = mFrom.getMachineNode().getSingleDataBasket(mFrom, tStates.get(0));
                                    //paramValuePushService.pushParamValueToMeasurement(mTo, pv);
                                    //pushSpecterConsumer.produceParams2(pv);
                                    //pushSpecterConsumer.pushToServer(pv2);
                                    //pushParamValueService.push(mTo,pv2);
                                    //pushParamValueService.push(UUID.randomUUID(),);
                                } else {
                                    //pushParamValueService.push(mTo,pv);
                                }
                                //reloadDataUnderTransaction();
                                //basePushTask.push(mTo, pv);
                            }
                        }
                    }
                    List<MeasuringPointNode> mpnListFrom = measPointList(sharedNode);
                    List<MeasuringPointNode> mpnListTo = measPointList(extTree.getSelectedNode());
                    ViList<TreeHierarchyOwner> thhoFromList = new ViList<>();
                    ViList<TreeHierarchyOwner> thhoToList = new ViList<>();
                    int n = 0;


            /*for (MeasuringPointNode mpnTo:
                    mpnListTo) {
                   MeasuringPointNode mpnFrom = mpnListFrom.get(n);
                   for (int i = 0; i < mpnFrom.getTreeHierarchyAdapter().getChildNodes().size(); i++){
                       thhoFromList.add(mpnFrom.getTreeHierarchyAdapter().getChildNodes().get(i));
                       thhoToList.add(mpnTo.getTreeHierarchyAdapter().getChildNodes().get(i));
                       //ViLocalList<ParamValue> paramValues = archiveClientLoadingService.my_createList((Measurement)mFrom);
                       //Measurement m = (Measurement) mTo;
                }
                   //mpnTo.getTreeHierarchyAdapter().getChildNodes().forEach( treeHierarchyOwner -> {}
                n++;
            }*/
                /*MachineNode machineNode = (MachineNode) sharedNode.getUserObject();
                MachinePackList machinePackList = machineNode.getMachinePackList();
                MeasurementPack measurementPack = machinePackList.get(0).getMeasurementPack(mpnList.get(0));*/
                    }
                }
        }).run();
    }

    private void copy() {
        new ClientCommitLogic("commit_3t89yghoilbgoi3", () -> {
            sharedObject = (SharedObject) extTree.getSelectedNode().getUserObject();
            sharedNode =  extTree.getSelectedNode();
        }).run();
    }

    public List<DefaultMutableTreeNode> findUserObject(DefaultMutableTreeNode root, String search ) {
        List<DefaultMutableTreeNode> result = new ArrayList(){};
        Enumeration nodeEnumeration =  root.breadthFirstEnumeration();
        while( nodeEnumeration.hasMoreElements() ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)nodeEnumeration.nextElement();
            String found = node.getUserObject().getClass().getSimpleName();
            if( search.equals(found) ) {
                result.add(node);
            }
        }
        return result;
    }

    public ClientTreeNode findNode(DefaultMutableTreeNode root, Object search ) {
        //List<DefaultMutableTreeNode> result = new ArrayList(){};
        ClientTreeNode result = null;
        Enumeration nodeEnumeration =  root.breadthFirstEnumeration();
        while( nodeEnumeration.hasMoreElements() ) {
            ClientTreeNode node = (ClientTreeNode)nodeEnumeration.nextElement();
            Object found = node.getUserObject();
            if( search.equals(found) ) {
                result = node;
            }
        }
        return result;
    }

    private void getMeasurement(){
        List<Measurement> measurements = measList(extTree.getSelectedNode());
        int i = 0;
        for (Measurement measurement:
                measurements) {
            //extTree.selectNodeAsProgram((DefaultTreeNode)measurement.);
            ClientTreeNode node = findNode(extTree.getSelectedNode(), measurement);
            WorkStateNode workStateNode =  (WorkStateNode) node;
            PanelWithChart panelWithChart;
            WorkState workState = workStateNode.getWorkState();
            Optional<Storage> firstStorage = Optional.empty();

            if (measurement.getParamDescription().type().isSpecter()) {
                panelWithChart = new SpecterAnalysisPanel(measurement);
                ((SpecterNode) node).setSpecterAnalysisPanel((SpecterAnalysisPanel) panelWithChart);
            } else {
                firstStorage = measurement.getStorages(workState).getFirstElement();
                panelWithChart = new PanelWithChart(measurement);
            }

            ViLocalList<ParamValue> paramValues = archiveClientLoadingService.my_createList(measurement);
            ArchiveValueProvider archiveValueProvider = new ArchiveValueProvider(measurement, paramValues, firstStorage, workState.getKeyStr());
            //panelWithChart.showArchive(archiveValueProvider, measurement);
            //innerContainer.openView(panelWithChart);
            //measurement.getParamDescription();
            i++;
        }
    }

    private List<Measurement> measList(ClientTreeNode selectedNode) {
        //Enumeration<TreeNode> childVector = selectedNode.children();
        //List<TreeNode> childList = Collections.list(childVector);
        List<Measurement> result = new ArrayList<>();
        List<DefaultMutableTreeNode> mpn = findUserObject(selectedNode, "Measurement");
        for (DefaultMutableTreeNode dmtn : mpn){
            result.add((Measurement) dmtn.getUserObject());
        }
        return result;
    }

    private List<MeasuringPointNode> measPointList(ClientTreeNode selectedNode) {
        //Enumeration<TreeNode> childVector = selectedNode.children();
        //List<TreeNode> childList = Collections.list(childVector);
        List<MeasuringPointNode> result = new ArrayList<>();
        List<DefaultMutableTreeNode> mpn = findUserObject(selectedNode, "MeasuringPointNode");
        for (DefaultMutableTreeNode dmtn : mpn){
            result.add((MeasuringPointNode) dmtn.getUserObject());
        }
        return result;
    }

    public MachineTreeDataBrowsingView(boolean deleteButtonIsEnabled) {
        super();
        popUp = new NyViPopupMenu();
        this.deleteButtonIsEnabled = deleteButtonIsEnabled;
        archiveClientLoadingService = InjectorService.get(ArchiveClientLoadingService.class);
        new ReadLogic("MachineTreeDataBrowsingView constructor readLogic", () -> {
            machineTreeModel = new MachineTreeModel();
            extTree = new ExtTree<>(machineTreeModel);
            extTree.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            extTree.addSelectionListener((currentNode, previousNode) -> specterAnalysisUpdate(previousNode));

            extTree.setPopupMenu(new MachineTreeDataExportPopupMenu(extTree));
            extTree.setPopupMenu(popUp);

            innerContainer = new InnerContainer();
            innerContainer.setFillMode(true);

            setLayout(new BorderLayout());

            ExtPanel extPanel = new ExtPanel(new BorderLayout());
            extPanel.add(extTree, BorderLayout.CENTER);
            add(createSplitPane(extPanel, innerContainer), BorderLayout.CENTER);

            extTree.selectRootNodeAsUser();

            registerEventListener(ShowNodeEvent.class, (ShowNodeEventHandler) nodeDangerState -> {
                new ReadLogic("read_dfskljdsfhkjsdhfk", () -> {
                    TitledNode owner = nodeDangerState;
                    ClientTreeNode defaultTreeNode = extTree.searchNode(owner);
                    if (defaultTreeNode != null) {
                        extTree.selectNodeAsUser(defaultTreeNode);
                    }
                }).run();
            });

            reloadDataUnderTransaction();
        });
        RefreshableService.add(this);
    }

    @Override
    public void reloadDataUnderTransaction() {
        extTree.reloadDataUnderTransactionInner();
    }

    private void specterAnalysisUpdate(ClientTreeNode previousNode) {
        if (previousNode instanceof SpecterNode) {
            SpecterAnalysisPanel specterAnalysisPanel = ((SpecterNode) previousNode).getSpecterAnalysisPanel();
            if (!specterAnalysisPanel.hasOpenedAnalysisDialogs()) {
                updateInnerViewPanel();
            } else {
                ExtMessageDialogPanel specterAnalysisPanelCloseDialog = ExtMessageDialogFactory.getOKCancelMessage(specterAnalysisPanel,
                                                                                                                   "Закрытие панели анализа спектра",
                                                                                                                   "Закрыть все окна анализа и переключиться на другое измерение?",
                                                                                                                   "Продолжить", "Отмена");
                if (specterAnalysisPanelCloseDialog.getResult() == ExtMessageDialogPanel.ResultType.YES_RESULT) {
                    specterAnalysisPanel.closeSpecterAnalysisPanel();
                    updateInnerViewPanel();
                } else {
                    extTree.selectNodeAsProgram(previousNode);
                }
            }
        } else {
            updateInnerViewPanel();
        }
    }

    private void updateInnerViewPanel() {
        extTree.getSelected().ifPresent(node -> {
            Object userObject = node.getUserObject();
            if (userObject instanceof Measurement && node instanceof WorkStateNode) {
                WorkStateNode workStateNode = (WorkStateNode) node;
                Measurement measurement = (Measurement) userObject;
                new ReadLogic("read_alksdjkas", () -> {
                    PanelWithChart panelWithChart;
                    WorkState workState = workStateNode.getWorkState();
                    Optional<Storage> firstStorage = Optional.empty();

                    if (measurement.getParamDescription().type().isSpecter()) {
                        panelWithChart = new SpecterAnalysisPanel(measurement);
                        ((SpecterNode) node).setSpecterAnalysisPanel((SpecterAnalysisPanel) panelWithChart);
                    } else {
                        firstStorage = measurement.getStorages(workState).getFirstElement();
                        panelWithChart = new PanelWithChart(measurement);
                    }

                    ViLocalList<ParamValue> paramValues = archiveClientLoadingService.createList(measurement, workState.getKeyStr());
                    ArchiveValueProvider archiveValueProvider = new ArchiveValueProvider(measurement, paramValues, firstStorage,
                                                                                         workState.getKeyStr());
                    panelWithChart.showArchive(archiveValueProvider, measurement);
                    innerContainer.openView(panelWithChart);
                });
            } else if (node instanceof DangerOwnerNode) {
                List<InnerPanel> panelFor = MachineViewStateSpec.getPanelFor(node.getUserObject(), ((DangerOwnerNode) node).getWorkState());
                if (!panelFor.isEmpty() && panelFor.get(0) != null) {
                    innerContainer.openView(panelFor.get(0));
                } else {
                    innerContainer.openView(MachineViewStateSpec.getPanelFor(node.getUserObject()));
                }
            } else {
                innerContainer.openView(MachineViewStateSpec.getPanelFor(node.getUserObject()));
            }
        });
    }


    @Override
    public JComponent getComponent() {
        return this;
    }

    private class MachineTreeModel extends TwoCategoryTreeModel<ClientTreeNode> {

        @Override
        protected ClientTreeNode build() {
            long startTime = System.currentTimeMillis();

            UI_LOGGER.info("tree data reload time = {}", System.currentTimeMillis() - startTime);
            startTime = System.currentTimeMillis();

            SharedServerModelHolder statSystemModel = InjectorService.get(SharedServerModelHolder.class);
            OrganizationMachineNode organizationNode = statSystemModel.getModel().getOrganizationNode();

            ClientTreeNode rootNode = createOrGetByBuildTree(() -> new ClientTreeNode(organizationNode), organizationNode, "structure");
            organizationNode.getTreeHierarchyAdapter().getChildNodes().forEach(treeHierarchyOwner -> {
                addLogicNodes(rootNode, treeHierarchyOwner);
            });
            UI_LOGGER.info("tree building time = {}", System.currentTimeMillis() - startTime);
            return rootNode;
        }

        public void addLogicNodes(ClientTreeNode rootNode, TreeHierarchyOwner treeHierarchyOwner) {
            ClientTreeNode currentNode = createOrGetByBuildTree(() -> new ClientTreeNode(treeHierarchyOwner), treeHierarchyOwner,
                                                                 "logic group nodes");
            rootNode.add(currentNode);

            if (treeHierarchyOwner instanceof MachineNode) {
                MachineNode machineNode = (MachineNode) treeHierarchyOwner;
                machineNode.getMachineStatesManager().getWorkStates().forEach(workState -> {
                    ClientTreeNode workStateNode = createOrGetByBuildTree(() -> new ClientTreeNode(workState), workState,
                                                                           "work state key");
                    currentNode.add(workStateNode);
                    machineNode.getTreeHierarchyAdapter().getChildNodes().forEach(childOwner -> {
                        addModes(workStateNode, childOwner, workState);
                    });
                });
            } else {
                treeHierarchyOwner.getTreeHierarchyAdapter().getChildNodes().forEach(childOwner -> {
                    addLogicNodes(currentNode, childOwner);
                });
            }
        }

        private void addModes(ClientTreeNode root, TreeHierarchyOwner treeHierarchyOwner, WorkState workState) {
            ClientTreeNode currentNode = createOrGetByBuildTree(() -> new DangerOwnerNode(treeHierarchyOwner, workState),
                                                                 treeHierarchyOwner, workState);
            root.add(currentNode);

            if (treeHierarchyOwner instanceof MeasuringPointNode) {
                MeasuringPointNode measuringPointNode = (MeasuringPointNode) treeHierarchyOwner;
                addSyncPackNodes(workState, currentNode, measuringPointNode);
            }
            if (treeHierarchyOwner instanceof Measurement) {
                Measurement measurement = (Measurement) treeHierarchyOwner;
                measurement.getStorages(workState).forEach(storage -> {
                    ClientTreeNode storageNode = createOrGetByBuildTree(() -> new StorageNode(storage), storage, "storage key const");
                    currentNode.add(storageNode);
                });
            } else {
                treeHierarchyOwner.getTreeHierarchyAdapter().getChildNodes().forEach(childOwner -> {
                    addModes(currentNode, childOwner, workState);
                });
            }

        }

        private void addSyncPackNodes(WorkState workState, ClientTreeNode parentNode, MeasuringPointNode measuringPointNode) {

            if (measuringPointNode.getInputSignalType().canHavePack()) {
                MachinePackList machinePackList = measuringPointNode.getMachineNode().getMachinePackList();
                ClientTreeNode packsNode = createOrGetByBuildTree(() -> new ClientTreeNode(machinePackList),
                                                                   measuringPointNode.getKeyStr() + "sync", workState);
                packsNode.setTitle(ViResourceBundle.getString("machine_tree_data_browsing_view.sync_pack"));

                parentNode.add(packsNode);

                measuringPointNode.getMachineNode().getMachinePackList().forEach(machinePack -> {
                    MeasurementOwner measurementPack = machinePack.getMeasurementPack(measuringPointNode);
                    if (measurementPack != null) {
                        ClientTreeNode measurementPackNode = createOrGetByBuildTree(() -> new ClientTreeNode(measurementPack),
                                                                                     measurementPack, workState);
                        packsNode.add(measurementPackNode);
                        measurementPack.getMeasurements().forEach(measurement -> {
                            ClientTreeNode measurementNode = createOrGetByBuildTree(() -> {
                                if (measurement.getParamDescription().type().isSpecter()) {
                                    return new SpecterNode(measurement, workState);
                                } else {
                                    return new WorkStateNode(measurement, workState);
                                }
                            }, measurement, workState);
                            measurementPackNode.add(measurementNode);
                        });
                    }
                });
            }

        }
    }

    private class SpecterNode extends WorkStateNode {
        private SpecterAnalysisPanel specterAnalysisPanel;

        public SpecterNode(TreeHierarchyOwner value, WorkState workState) {
            super(value, workState);
        }

        private void setSpecterAnalysisPanel(SpecterAnalysisPanel panel) {
            specterAnalysisPanel = panel;
        }

        public SpecterAnalysisPanel getSpecterAnalysisPanel() {
            return specterAnalysisPanel;
        }
    }
    public class DangerOwnerNode extends WorkStateNode {

        public DangerOwnerNode(TreeHierarchyOwner value, WorkState workState) {
            super(value, workState);
            if (value instanceof DangerStateOwner) {
                getLeftIcons().add(new DangerLevelTreeIcon((DangerStateOwner) value, workState));
            }
        }

    }
    private class StorageNode extends ClientTreeNode {

        public StorageNode(Storage storage) {
            super(storage);
            getLeftIcons().add(new DangerLevelTreeIcon(storage));
        }

    }
}
