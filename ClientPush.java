package ru.vibrotek.domain;

import ru.vibrotek.domain.database.ArchiveRecord;
import ru.vibrotek.domain.handbook.unittype.UnitType;
import ru.vibrotek.domain.nodes.ArchiveClientLoadingService;
import ru.vibrotek.domain.nodes.ClientTreeNode;
import ru.vibrotek.domain.nodes.machine.MachineNodeCopier;
import ru.vibrotek.domain.nodes.machine.level1.OrganizationMachineNode;
import ru.vibrotek.domain.nodes.machine.level2.MachineLogicGroupMachineNode;
import ru.vibrotek.domain.nodes.machine.level3.MachineNode;
import ru.vibrotek.domain.nodes.machine.level3.states.WorkState;
import ru.vibrotek.domain.nodes.machine.level4.MachineComponentMachineNode;
import ru.vibrotek.domain.nodes.machine.level5.MachineLogicComponentMachineNode;
import ru.vibrotek.domain.nodes.machine.level6.MachinePack;
import ru.vibrotek.domain.nodes.machine.level6.MachinePackList;
import ru.vibrotek.domain.nodes.machine.level6.MeasuringPointNode;
import ru.vibrotek.domain.nodes.machine.level7.Measurement;
import ru.vibrotek.domain.nodes.machine.level7.MeasurementPack;
import ru.vibrotek.model.*;
import ru.vibrotek.model.collection.list.ViLocalList;
import ru.vibrotek.model.collection.map.ViLocalMap;
import ru.vibrotek.param.description.ParamDescription;
import ru.vibrotek.param.value.ParamValue;
import ru.vibrotek.param.value.impl.SpecterParamValue;
import ru.vibrotek.service.PushParamValueService;
import ru.vibrotek.service.transaction.query.ClientCommitLogic;
import ru.vibrotek.ui.components.tree.ExtTree;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

import static ru.vibrotek.logger.ViLogger.NODE_COPIER;

public class ClientPush{
    Map<Measurement, ParamValue> m_map = new ViLocalMap<>();
    MeasurementPack m_measurementPack = null;

    public void copy(SharedObject sharedObject, ClientTreeNode sharedNode, ArchiveClientLoadingService archiveClientLoadingService, ExtTree<ClientTreeNode>extTree, PushParamValueService pushParamValueService) {
        NODE_COPIER.info("Начинаем копирование данных для нового узла, class id: {}", sharedNode.getTitle());

        new ClientCommitLogic("node copier logic", () -> {
                    hh( sharedObject, sharedNode, archiveClientLoadingService, extTree, pushParamValueService);
        });
        pushParamValueService.push(UUID.randomUUID(), m_measurementPack,  m_map, new TimeLabel(System.currentTimeMillis()));
    }

    private void hh(SharedObject sharedObject, ClientTreeNode sharedNode, ArchiveClientLoadingService archiveClientLoadingService, ExtTree<ClientTreeNode>extTree, PushParamValueService pushParamValueService){

            Iterator iterator1 = sharedNode.children().asIterator();
            //найдем доступные режимы
            List<ClientTreeNode> mnFrom = new ArrayList<>();
            List<ClientTreeNode> mnTo = new ArrayList<>();
            while (iterator1.hasNext()) {
                //mn.add((DefaultTreeNode)extTree.getSelectedNode().children().nextElement());
                mnFrom.add((ClientTreeNode) iterator1.next());
                //iterator.next();
            }
            Iterator iterator2 = extTree.getSelectedNode().children().asIterator();
            while (iterator2.hasNext()) {
                //mn.add((DefaultTreeNode)extTree.getSelectedNode().children().nextElement());
                mnTo.add((ClientTreeNode) iterator2.next());
                //iterator.next();
            }
            //берем измерения идля каждого режима для первой и второй машины
            for (int y = 0; y < mnFrom.size(); y++) {
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
                        for (Measurement key : map.keySet()) {
                            if (!measurementPackMap.keySet().contains(key.getMeasurementOwner())) {
                                measurementPackMap.put((MeasurementPack) key.getMeasurementOwner(), new ArrayList<>());
                            }
                            if (measurementPackMap.keySet().contains(key.getMeasurementOwner())) {
                                measurementPackMap.get(key.getMeasurementOwner()).add(key);
                            }

                        }
                        int yy = 0;
                        for (MeasurementPack mp : measurementPackMap.keySet()) {
                            TimeLabel timeLabel = new TimeLabel(System.currentTimeMillis());
                            Map<Measurement, ParamValue> mpv = new ViLocalMap<>();
                            measurementPackMap.get(mp).forEach(consumer -> {
                                mpv.put(consumer, map.get(consumer));
                            });
                            if (!mpv.isEmpty() && yy == 0) {
                                m_map = mpv ;
                                m_measurementPack = mp ;
                                //pushParamValueService.push(UUID.randomUUID(), mp, mpv, timeLabel);
                                return;
                            }
                        }

                    });

            }
        }
    }
    private List<Measurement> measList (ClientTreeNode selectedNode){
        //Enumeration<TreeNode> childVector = selectedNode.children();
        //List<TreeNode> childList = Collections.list(childVector);
        List<Measurement> result = new ArrayList<>();
        List<DefaultMutableTreeNode> mpn = findUserObject(selectedNode, "Measurement");
        for (DefaultMutableTreeNode dmtn : mpn) {
            result.add((Measurement) dmtn.getUserObject());
        }
        return result;
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


    private List<MeasuringPointNode> measPointList (ClientTreeNode selectedNode){
        //Enumeration<TreeNode> childVector = selectedNode.children();
        //List<TreeNode> childList = Collections.list(childVector);
        List<MeasuringPointNode> result = new ArrayList<>();
        List<DefaultMutableTreeNode> mpn = findUserObject(selectedNode, "MeasuringPointNode");
        for (DefaultMutableTreeNode dmtn : mpn) {
            result.add((MeasuringPointNode) dmtn.getUserObject());
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
}
