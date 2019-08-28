package ru.vibrotek.service.registrator;

import ch.qos.logback.core.net.server.Client;
import ru.vibrotek.chart.*;
import ru.vibrotek.chart.prodivers.*;
import ru.vibrotek.collections.*;
import ru.vibrotek.controllers.*;
import ru.vibrotek.danger.*;
import ru.vibrotek.domain.*;
import ru.vibrotek.domain.controllers.siemens.*;
import ru.vibrotek.domain.controllers.siemens.rule.*;
import ru.vibrotek.domain.controllers.siemens.s1500.*;
import ru.vibrotek.domain.controllers.siemens.s1500record.*;
import ru.vibrotek.domain.controllers.simple.*;
import ru.vibrotek.domain.controllers.simple.auxillary.*;
import ru.vibrotek.domain.controllers.simple.binary.*;
import ru.vibrotek.domain.controllers.simple.modbus.*;
import ru.vibrotek.domain.controllers.simple.orangepi.*;
import ru.vibrotek.domain.controllers.simple.rules.*;
import ru.vibrotek.domain.controllers.simple.rules.danger.*;
import ru.vibrotek.domain.controllers.simple.rules.state.*;
import ru.vibrotek.domain.database.*;
import ru.vibrotek.domain.diagnostics.*;
import ru.vibrotek.domain.handbook.*;
import ru.vibrotek.domain.handbook.armindicators.*;
import ru.vibrotek.domain.handbook.changer.*;
import ru.vibrotek.domain.handbook.changer.tasks.*;
import ru.vibrotek.domain.handbook.dblevel.*;
import ru.vibrotek.domain.handbook.probation.*;
import ru.vibrotek.domain.handbook.transformation.*;
import ru.vibrotek.domain.handbook.unittype.*;
import ru.vibrotek.domain.metrology.*;
import ru.vibrotek.domain.metrology.data.*;
import ru.vibrotek.domain.metrology.task.*;
import ru.vibrotek.domain.metrology.task.type1.*;
import ru.vibrotek.domain.metrology.task.type10.*;
import ru.vibrotek.domain.metrology.task.type11.*;
import ru.vibrotek.domain.metrology.task.type12.*;
import ru.vibrotek.domain.metrology.task.type13.*;
import ru.vibrotek.domain.metrology.task.type14.*;
import ru.vibrotek.domain.metrology.task.type15.*;
import ru.vibrotek.domain.metrology.task.type16.*;
import ru.vibrotek.domain.metrology.task.type17.*;
import ru.vibrotek.domain.metrology.task.type2.*;
import ru.vibrotek.domain.metrology.task.type3.*;
import ru.vibrotek.domain.metrology.task.type5.*;
import ru.vibrotek.domain.metrology.task.type6.*;
import ru.vibrotek.domain.metrology.task.type7.*;
import ru.vibrotek.domain.metrology.task.type8.*;
import ru.vibrotek.domain.mnemonic.*;
import ru.vibrotek.domain.mnemonic.item.*;
import ru.vibrotek.domain.nodes.*;
import ru.vibrotek.domain.nodes.device.*;
import ru.vibrotek.domain.nodes.device.level1.*;
import ru.vibrotek.domain.nodes.device.level2.*;
import ru.vibrotek.domain.nodes.device.level3.generator.*;
import ru.vibrotek.domain.nodes.device.level3.mcp.*;
import ru.vibrotek.domain.nodes.device.level3.mcp.types.*;
import ru.vibrotek.domain.nodes.device.level4.*;
import ru.vibrotek.domain.nodes.device.level4.generator.*;
import ru.vibrotek.domain.nodes.device.level4.mcp.*;
import ru.vibrotek.domain.nodes.device.level4.mcp.types.*;
import ru.vibrotek.domain.nodes.machine.*;
import ru.vibrotek.domain.nodes.machine.decimation.*;
import ru.vibrotek.domain.nodes.machine.external.*;
import ru.vibrotek.domain.nodes.machine.level0.*;
import ru.vibrotek.domain.nodes.machine.level1.*;
import ru.vibrotek.domain.nodes.machine.level2.*;
import ru.vibrotek.domain.nodes.machine.level3.*;
import ru.vibrotek.domain.nodes.machine.level3.data.*;
import ru.vibrotek.domain.nodes.machine.level3.states.*;
import ru.vibrotek.domain.nodes.machine.level4.*;
import ru.vibrotek.domain.nodes.machine.level5.*;
import ru.vibrotek.domain.nodes.machine.level6.*;
import ru.vibrotek.domain.nodes.machine.level7.*;
import ru.vibrotek.domain.nodes.machine.level8.*;
import ru.vibrotek.domain.settings.*;
import ru.vibrotek.domain.settings.elements.*;
import ru.vibrotek.domain.threshold.*;
import ru.vibrotek.domain.threshold.state.*;
import ru.vibrotek.eventbus.event.*;
import ru.vibrotek.eventbus.events.*;
import ru.vibrotek.kryo.*;
import ru.vibrotek.kryo.service.registrator.*;
import ru.vibrotek.kryonet.*;
import ru.vibrotek.mcp.type.*;
import ru.vibrotek.model.*;
import ru.vibrotek.model.collection.*;
import ru.vibrotek.model.collection.bikeymap.*;
import ru.vibrotek.model.collection.category.*;
import ru.vibrotek.model.collection.list.*;
import ru.vibrotek.model.collection.list.wrapped.*;
import ru.vibrotek.model.collection.map.*;
import ru.vibrotek.model.collection.multiple.*;
import ru.vibrotek.model.collection.set.*;
import ru.vibrotek.model.math.trend.*;
import ru.vibrotek.model.math.window.common.type.*;
import ru.vibrotek.model.readtask.mcp.*;
import ru.vibrotek.param.description.*;
import ru.vibrotek.param.description.scalar.*;
import ru.vibrotek.param.description.signal.*;
import ru.vibrotek.param.description.specter.*;
import ru.vibrotek.param.types.*;
import ru.vibrotek.param.value.*;
import ru.vibrotek.param.value.impl.*;
import ru.vibrotek.query.*;
import ru.vibrotek.query.with.result.*;
import ru.vibrotek.query.with.result.LoadArchiveDataRequest;
import ru.vibrotek.service.channels.*;
import ru.vibrotek.service.history.*;
import ru.vibrotek.service.mcp.*;
import ru.vibrotek.service.params.*;
import ru.vibrotek.service.query.*;
import ru.vibrotek.service.query.dto.*;
import ru.vibrotek.gui.*;

import javax.swing.*;
import java.awt.Point;
import java.awt.*;
import java.awt.color.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import static ru.vibrotek.kryo.service.registrator.RegisterRecordType.*;

public class NetCoreRegistrator {
    private static Set<Class> defaultClasses = new HashSet<>();

    static {
        defaultClasses.add(int.class);
        defaultClasses.add(String.class);
        defaultClasses.add(float.class);
        defaultClasses.add(boolean.class);
        defaultClasses.add(byte.class);
        defaultClasses.add(char.class);
        defaultClasses.add(short.class);
        defaultClasses.add(long.class);
        defaultClasses.add(double.class);
        defaultClasses.add(void.class);
        defaultClasses.add(String[].class);
        defaultClasses.add(Class.class);
        defaultClasses.add(float[].class);
        defaultClasses.add(double[].class);
        defaultClasses.add(int[].class);
        defaultClasses.add(long[].class);
        defaultClasses.add(ViLocalList.class);
        defaultClasses.add(ViLocalListWithBaseEquals.class);
        defaultClasses.add(ViLocalMap.class);
        defaultClasses.add(ViLocalTreeMap.class);
        defaultClasses.add(ViLocalSet.class);
        defaultClasses.add(Date.class);
        defaultClasses.add(CopyOnWriteArrayList.class);
        defaultClasses.add(ConcurrentHashMap.class);
        defaultClasses.add(java.sql.Timestamp.class);
        defaultClasses.add(InvocationHandler.class);
        defaultClasses.add(Object.class);
        defaultClasses.add(Object[].class);
        defaultClasses.add(StringBuilder.class);
        defaultClasses.add(ViLocalLinkedMap.class);
        defaultClasses.add(ViLocalTreeMap.class);
        defaultClasses.add(Color.class);
        defaultClasses.add(ColorSpace.class);
        defaultClasses.add(Point.class);
        defaultClasses.add(ScalarParamValue[].class);
        defaultClasses.add(short[].class);
        defaultClasses.add(ClientPush.class);
    }

    private static void put(Class<?> clazz, int id) {
        if (clazz.getSimpleName().contains("ClientPush")){
            int hhh = 0;
        }
        if (SharedObject.class.isAssignableFrom(clazz)) {
            ClassRegistrator.getInstance().put(clazz, id, SHARED_OBJECT, true);
        } else if (Enum.class.isAssignableFrom(clazz)) {
            ClassRegistrator.getInstance().put(clazz, id, ENUM_TYPE, true);
        } else if (KryoConfigurationService.map.containsKey(clazz)) {
            ClassRegistrator.getInstance().put(clazz, id, CUSTOM, true);
        } else if (defaultClasses.contains(clazz)) {
            if (haveDefaultConstructor(clazz)) {
                ClassRegistrator.getInstance().put(clazz, id, DEFAULT_OBJECT, true);
            } else {
                ClassRegistrator.getInstance().put(clazz, id, DEFAULT_OBJECT, false);
            }
        } else {
            ClassRegistrator.getInstance().put(clazz, id, OBJECT_WITH_TAG, false);
        }

    }

    private static boolean haveDefaultConstructor(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            if (constructor == null) {
                return false;
            }
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    public static void registerNetKryoClasses() {
        put(int.class, 0);
        put(String.class, 1);
        put(float.class, 2);
        put(boolean.class, 3);
        put(byte.class, 4);
        put(char.class, 5);
        put(short.class, 6);
        put(long.class, 7);
        put(double.class, 8);
        put(void.class, 9);
        put(String[].class, 10);
        put(Class.class, 11);
        put(float[].class, 12);
        put(double[].class, 13);
        put(int[].class, 14);
        put(long[].class, 15);
        put(ViLocalList.class, 16);
        put(ViLocalMap.class, 17);
        put(ViLocalSet.class, 18);
        put(Date.class, 19);
        put(CopyOnWriteArrayList.class, 20);
        put(ConcurrentHashMap.class, 21);
        put(java.sql.Timestamp.class, 22);
        put(InvocationHandler.class, 23);
        put(Object.class, 24);
        put(Object[].class, 25);
        put(StringBuilder.class, 26);
        put(ViLocalLinkedMap.class, 27);
        put(ViLocalTreeMap.class, 28);
        put(Color.class, 29);
        put(ColorSpace.class, 30);
        put(Point.class, 31);
        put(ImageIcon.class, 32);
        put(UUID.class, 33);
        put(Font.class, 34);
        put(byte[].class, 35);
        put(EndPointGarmonicSignalGenerator.class, 36);
        put(SharedServerModel.class, 37);
        put(GlobalHandbook.class, 38);
        put(DbZeroRift.class, 39);
        put(DbZeroRiftHandbook.class, 40);
        put(TransformationRule.class, 41);
        put(TransformationHandbook.class, 42);
        put(UnitType.class, 43);
        put(UnitTypeHandbook.class, 44);
        put(MnemoImage.class, 45);
        put(MnemoIndicator.class, 46);
        put(MnemoItem.class, 47);
        put(MnemoLabel.class, 48);
        put(MnemoTaho.class, 49);
        put(MnemoThermometer.class, 50);
        put(MnemoSchema.class, 51);
        put(MnemoSchemaSet.class, 52);
        put(TreeHierarchyAdapter.class, 53);
        put(DeviceTreeNode.class, 54);
        put(ServerSettings.class, 55);
        put(StationSettings.class, 56);
        put(GeneratorNode.class, 57);
        put(McpSettings.class, 58);
        put(ConnectableNode.class, 59);
        put(AmplModularGamonicSignalGenerator.class, 60);
        put(GarmonicSignalGenerator.class, 61);
        put(SignalGenerator.class, 62);
        put(TahoSignalGenerator.class, 63);
        put(VibroChannelControlSettings.class, 64);
        put(MCPVibroChannelOption.class, 65);
        put(LogicMachineNode.class, 66);
        put(PhysMachineNode.class, 67);
        //put(MachineTreeDataBrowsing.class, 67);
//        put(DangerStateAdapter.class, 68);
//        put(StorageDangerStateAdapter.class, 69);
//        put(DangerStateByIntervalAdapter.class, 70);
//        put(StorageDangerStateByIntervalAdapter.class, 71);
        put(DecimationSchema.class, 72);
        put(DefaultDecimationSchemaAdapter.class, 73);
        put(ParamValueSelectionRule.class, 74);
        put(ApplicationRootNode.class, 75);
        put(OrganizationMachineNode.class, 76);
        put(MachineLogicGroupMachineNode.class, 77);
        put(MachineNode.class, 78);
        put(DataBasket.class, 79);
        put(StateRule.class, 80);
        put(AccelerationState.class, 81);
//        put(AccelerationStateImpl.class, 82);
//        put(ExclusiveWorkState.class, 83);
        put(MachineState.class, 84);
        put(MachineStatesManager.class, 85);
        put(StoppedState.class, 86);
        put(UnknownState.class, 87);
//        put(UnknownStateImpl.class, 88);
        put(WorkState.class, 89);
        put(LogicWithIntervalsMachineNode.class, 90);
        put(MachineComponentMachineNode.class, 91);
        put(MachineLogicComponentMachineNode.class, 92);
        put(MeasuringPointNode.class, 93);
        put(Measurement.class, 94);
        put(Storage.class, 95);
        put(DatabaseSettings.class, 96);
        put(SystemSettings.class, 97);
//        put(Rift.class, 98);
        put(ScalarThreshold.class, 99);
        put(Zone.class, 100);
//        put(BaseDangerState.class, 101);
        put(DangerStatePrediction.class, 102);
        put(NodeDangerState.class, 103);
        put(NodeChannelPipe.class, 104);
//        put(MachineAccident.class, 105);
//        put(Accident.class, 106);
//        put(AccidentService.class, 107);
        put(ParamValueSelectionRuleType.class, 108);
        put(SharedObject.class, 109);
        put(DemoSharedObject.class, 110);
        put(SharedKey.class, 111);
        put(ChangerQueryHandbook.class, 112);
        put(ModifySharedObjectOnServerQuery.class, 113);
        put(SetDefaultRiftTask.class, 114);
        put(RemoveParamValueTask.class, 115);
        put(ChangerTask.class, 116);
        put(MnemoMaсhineMode.class, 117);
        put(MultipleValuesMap.class, 118);
        put(ViMap.class, 119);
        put(ViList.class, 120);
        put(ViSet.class, 121);
        put(ViCategoryMap.class, 122);
        put(ViBiKeyMap.class, 123);
        put(TransformationMap.class, 124);
        put(ComplexSpecterParamValue.class, 125);
        put(Oct3SpecterMeasurement.class, 126);
        put(Oct3SpecterThreshold.class, 127);
        put(Oct3SpecterStorage.class, 128);
        put(ScalarStorage.class, 129);
        put(IndicatorsList.class, 130);
        put(SharedObjectWrap.class, 131);
        put(ViWrappedList.class, 132);
        put(SpecterStorage.class, 133);
        put(LoadDataRequest.class, 134);
        put(LocalMultipleValuesMap.class, 135);
        put(RemoveStorageFromLoading.class, 136);
        put(AddStorageToLoading.class, 137);
        put(ViLocalBiKeyMap.class, 138);
        put(Request.class, 139);
        put(TotalTrendResult.class, 140);
        put(TrendOrderType.class, 141);
        put(DTO.class, 142);
        put(TimeLabel.class, 143);
        put(ViLocalCategoryMap.class, 144);
        put(DefaultUnitType.class, 145);
        put(DefaultDbZeroRift.class, 146);
        put(TechnicalState.class, 147);
        put(StationState.class, 148);
        put(Contact.class, 149);
        put(McpMode.class, 150);
        put(SoftwareInfo.class, 151);
        put(McpChannelType.class, 152);
        put(MCPSamplingMode.class, 153);
        put(McpChannelICPType.class, 154);
        put(MCPChannelACDCType.class, 155);
        put(MCPChannelPower.class, 156);
        put(MCPKuType.class, 157);
//        put(ControlGeneratorWorkType.class, 158);
        put(ChannelControlType.class, 159);
        put(InputSignalType.class, 160);
        put(PhysicalQuantityType.class, 161);
        put(AxisType.class, 162);
        put(FilterType.class, 163);
        put(ResizeStrategyType.class, 164);
        put(ScalarChartOptions.class, 165);
        put(SignalChartOptions.class, 166);
        put(SpecterChartOptions.class, 168);
        put(Oct3SpecterChartOptions.class, 169);
        put(TimeInterval.class, 170);
        put(InputAverageDescription.class, 171);
        put(AmplDescription.class, 172);
        put(AverageBySignalDescription.class, 173);
        put(CommonLevelDescription.class, 174);
        put(DFreqDescription.class, 175);
        put(FreqDescription.class, 176);
        put(MaxBySignalDescription.class, 177);
        put(PhaseDescription.class, 178);
        put(ScalarDescription.class, 179);
        put(SubbandLevelDescription.class, 180);
        put(TahoIntervalsDescription.class, 181);
        put(TahoIntervalsDreamFormatDescription.class, 182);
        put(DigitalSignalDescription.class, 183);
        put(EnvelopeDecDescription.class, 184);
        put(EnvelopeDescription.class, 185);
        put(Oct1EnvelopeDescription.class, 188);
        put(Oct1EnvelopeDec.class, 189);
        put(Oct2EnvelopeDescription.class, 190);
        put(Oct2EnvelopeDec.class, 191);
        put(Oct3EnvelopeDescription.class, 192);
        put(Oct3EnvelopeDec.class, 193);
        put(SignalDescription.class, 194);
        put(Oct3LineDescription.class, 195);
        put(Oct3LineType.class, 196);
        put(Oct3SpecterDescription.class, 197);
        put(ProgrammSpecterDescription.class, 198);
        put(BaseOctEnvelopeSpecterOrder.class, 199);
        put(Oct1EnvelopeSpecterOrder.class, 202);
        put(Oct2EnvelopeSpecterOrder.class, 203);
        put(Oct3EnvelopeSpecterOrder.class, 204);
        put(ParamDescription.class, 206);
        put(ParamType.class, 207);
        put(Titles.class, 208);
//        put(UserOrderType.class, 209);
        put(Oct3SpecterParamValue.class, 210);
        put(ScalarParamValue.class, 211);
        put(SignalParamValue.class, 212);
        put(SpecterParamValue.class, 213);
        put(TahoIntervalsDreamFormatParamValue.class, 214);
        put(TahoIntervalsParamValue.class, 215);
        put(ParamValue.class, 216);
        put(AvtoSpecterLimitFrequency.class, 217);
        put(DecType.class, 218);
        put(Oct1EnvelopeLineType.class, 219);
        put(Oct2EnvelopeLineType.class, 220);
        put(Oct3EnvelopeLineType.class, 221);
        put(Oct3RangeType.class, 222);
        put(SamplingRate.class, 223);
        put(SpecterLineCount.class, 224);
        put(StepType.class, 225);
        put(IntegrationLevelType.class, 226);
        put(CommonLevelType.class, 227);
        put(SubbandLevelType.class, 228);
        put(IntervalBuffer.class, 229);
        put(LabelBuffer.class, 230);
        put(ComplexBuffer.class, 231);
        put(FloatBuffer.class, 232);
        put(IntBuffer.class, 233);
        put(LogicOnServer.class, 234);
        put(PojoUpdateSet.class, 235);
        put(PackedPojo.class, 236);
        put(ChartOptions.class, 238);
        put(McpSettingsPojo.class, 239);
        put(MeasurementPojo.class, 240);
        put(ConnectableNodePojo.class, 241);
        put(UnitTypeHandbookPojo.class, 242);
        put(MachineComponentMachineNodePojo.class, 243);
        put(TransformationRulePojo.class, 244);
        put(GeneratorNodePojo.class, 245);
//        put(DangerStateAdapterPojo.class, 246);
        put(TransformationHandbookPojo.class, 247);
        put(ThresholdPojo.class, 248);
        put(AmplModularGamonicSignalGeneratorPojo.class, 249);
        put(SharedServerModelPojo.class, 250);
        put(AccelerationStatePojo.class, 251);
        put(MnemoTahoPojo.class, 252);
        put(ViMapPojo.class, 253);
        put(LogicWithIntervalsMachineNodePojo.class, 254);
        put(PhysMachineNodePojo.class, 255);
        put(ViCategoryMapPojo.class, 256);
        put(StationSettingsPojo.class, 257);
//        put(AccidentPojo.class, 258);
        put(DataBasketPojo.class, 259);
        put(DecimationSchemaPojo.class, 260);
        put(DangerStatePredictionPojo.class, 261);
        put(TreeHierarchyAdapterPojo.class, 262);
        put(StoppedStatePojo.class, 263);
        put(DemoSharedObject.DemoSharedObjectPojo.class, 264);
        put(MnemoIndicatorPojo.class, 265);
        put(DefaultDecimationSchemaAdapterPojo.class, 266);
        put(WorkStatePojo.class, 267);
        put(TahoSignalGeneratorPojo.class, 268);
        put(ChangerQueryHandbookPojo.class, 269);
        put(ApplicationRootNodePojo.class, 270);
        put(MachineStatePojo.class, 271);
        put(SystemSettingsPojo.class, 272);
        put(ViBiKeyMapPojo.class, 273);
        put(MnemoSchemaPojo.class, 274);
        put(MachineStatesManagerPojo.class, 275);
        put(MachineNodePojo.class, 276);
        put(MnemoThermometerPojo.class, 277);
        put(NodeDangerStatePojo.class, 278);
        put(OrganizationMachineNodePojo.class, 279);
//        put(BaseDangerStatePojo.class, 280);
        put(MnemoImagePojo.class, 281);
        put(MachineLogicGroupMachineNodePojo.class, 282);
        put(DbZeroRiftHandbookPojo.class, 283);
//        put(UnknownStateImplPojo.class, 284);
        put(TrendAverageResult.class, 285);
        put(BaseStoragePojo.class, 286);
        put(MCPVibroChannelOptionPojo.class, 287);
        put(ChangerTaskPojo.class, 288);
        put(UnitTypePojo.class, 289);
        put(MeasuringPointNodePojo.class, 290);
        put(NodeChannelPipePojo.class, 291);
//        put(RiftPojo.class, 292);
        put(ViSetPojo.class, 293);
        put(ViListPojo.class, 294);
        put(MnemoSchemaSetPojo.class, 295);
        put(MnemoLabelPojo.class, 296);
        put(StateRulePojo.class, 297);
        put(MnemoMaсhineModePojo.class, 298);
//        put(DangerStateByIntervalAdapterPojo.class, 299);
//        put(AccelerationStateImplPojo.class, 300);
        put(ParamValueSelectionRulePojo.class, 301);
        put(ChangerQueryPojo.class, 302);
//        put(TotalTrendResultPojo.class, 303);
        put(UnknownStatePojo.class, 304);
        put(SignalGeneratorPojo.class, 305);
//        put(MachineAccidentPojo.class, 306);
        put(GarmonicSignalGeneratorPojo.class, 307);
        put(VibroChannelControlSettingsPojo.class, 309);
        put(LogicMachineNodePojo.class, 310);
//        put(ExclusiveWorkStatePojo.class, 311);
//        put(ZonePojo.class, 312);
        put(ServerSettingsPojo.class, 313);
        put(MultipleValuesMapPojo.class, 314);
        put(TrendResult.class, 315);
        put(GlobalHandBookPojo.class, 316);
        put(DbZeroRiftPojo.class, 317);
        put(DeviceTreeNodePojo.class, 318);
        put(DatabaseSettingsPojo.class, 319);
        put(LogicMeasuringPointMachineNodePojo.class, 320);
//        put(AccidentServicePojo.class, 321);
        put(TransformationMapPojo.class, 322);
        put(Oct3SpecterMeasurementPojo.class, 323);
        put(Oct3SpecterThresholdPojo.class, 324);
        put(Oct3SpecterStoragePojo.class, 325);
        put(ScalarStoragePojo.class, 326);
        put(IndicatorsListPojo.class, 327);
        put(SharedObjectWrapPojo.class, 328);
        put(RequestRegisterAsWorkClientQuery.class, 329);
        put(PushAllParamValueQuery.class, 330);
        put(PushParamValueQuery.class, 331);
        put(ServerAPI.class, 332);
        put(FindNewMcpRequestEvent.class, 333);
        put(RegisterAsConfigClientQuery.class, 334);
        put(RegisterAsArmOperatorQuery.class, 335);
        put(SpecterPowerType.class, 336);
        put(PikBySignalDescription.class, 337);
        put(MCPTahoChannelOption.class, 338);
        put(MCPTahoChannelObjectPojo.class, 339);
        put(RuleThreshold.class, 340);
        put(Oct3LineThreshold.class, 341);
        put(Oct3LineThresholdPojo.class, 342);
        put(SpecterLineStoryOptions.class, 343);
        put(OctSpecterLineStoryOptions.class, 344);
        put(ModifyQueryExecutorQuery.class, 345);
        put(SpecterStoragePojo.class, 346);
//        put(NeedUpdateTrendInOctStorage.class, 349);
//        put(NeedUpdateTrendInOctStoragePojo.class, 350);
        put(ThresholdValues.class, 351);
        put(ThresholdValuesPojo.class, 352);
//        put(StorageFlags.class, 353);
//        put(StorageFlagsPojo.class, 354);
        put(SetNeedUpdateCurrentZoneThresholdQuery.class, 355);
        put(MetrologyModel.class, 356);
        put(MetrologyModelPojo.class, 357);
        put(ProbationMethod.class, 358);
        put(ProbationMethodPojo.class, 359);
        put(VerificationFestival.class, 360);
        put(VerificationFestivalPojo.class, 361);
        put(TahoChannelControlSettings.class, 362);
        put(TahoChannelControlSettingsPojo.class, 363);
        put(GeneralData.class, 364);
        put(GeneralDataPojo.class, 365);
        put(TahoOption.class, 366);
        put(TahoOptionPojo.class, 367);
        put(VibroOption.class, 368);
        put(VibroOptionPojo.class, 369);
        put(ProbationMethodType.class, 371);
        put(TaskType3.class, 372);
        put(TaskType3Pojo.class, 373);
        put(TaskType3Result.class, 374);
        put(TaskType3ResultPojo.class, 375);
        put(ProbationMethodHandbook.class, 376);
        put(ProbationMethodHandbookPojo.class, 377);
        put(MetrologyMeasuringPointNode.class, 378);
        put(MetrologyMeasuringPointNodePojo.class, 379);
        put(TaskSchema.class, 380);
        put(TaskSchemaPojo.class, 381);
        put(Sensor.class, 382);
        put(SensorPojo.class, 383);
        put(TaskType1.class, 386);
        put(TaskType1Pojo.class, 387);
        put(TaskType1Result.class, 388);
        put(TaskType1ResultPojo.class, 389);
        put(TaskType2.class, 390);
        put(TaskType2Pojo.class, 391);
        put(TaskType2Result.class, 392);
        put(TaskType2ResultPojo.class, 393);
        put(TaskType6.class, 398);
        put(TaskType6Pojo.class, 399);
        put(TaskType6Result.class, 400);
        put(TaskType6ResultPojo.class, 401);
        put(TaskType8.class, 402);
        put(TaskType8Pojo.class, 403);
        put(TaskType8Result.class, 404);
        put(TaskType8ResultPojo.class, 405);
        put(TaskType5.class, 410);
        put(TaskType5Pojo.class, 411);
        put(TaskType5Result.class, 412);
        put(TaskType5ResultPojo.class, 413);
        put(TaskType7Vibro.class, 414);
        put(TaskType7VibroPojo.class, 415);
        put(TaskType7VibroResult.class, 416);
        put(TaskType7VibroResultPojo.class, 417);
        put(TaskType7Taho.class, 418);
        put(TaskType7TahoPojo.class, 419);
        put(TaskType7TahoResult.class, 420);
        put(TaskType7TahoResultPojo.class, 421);
        put(TaskType10.class, 422);
        put(TaskType10Pojo.class, 423);
        put(TaskType10Result.class, 424);
        put(TaskType10ResultPojo.class, 425);
        put(TaskType11.class, 426);
        put(TaskType11Pojo.class, 427);
        put(TaskType11Result.class, 428);
        put(TaskType11ResultPojo.class, 429);
        put(TaskType11ResultsPerMeasurementNumber.class, 430);
        put(TaskType11ResultsPerMeasurementNumberPojo.class, 431);
        put(TaskType12.class, 432);
        put(TaskType12Pojo.class, 433);
        put(TaskType12Result.class, 434);
        put(TaskType12ResultPojo.class, 435);
        put(SiemensControllerRule.class, 436);
        put(SiemensControllerRulePojo.class, 437);
        put(RuleType.class, 438);
        put(S1500Controller.class, 439);
        put(S1500ControllerPojo.class, 440);
        put(ControllerSettings.class, 443);
        put(ControllerSettingsPojo.class, 444);
        put(MeasurementPack.class, 445);
        put(MeasurementPackPojo.class, 446);
        put(PushSyncPackValueQuery.class, 447);
        put(MachinePackList.class, 448);
//        put(MeasurementPackListPojo.class, 449);
        put(LogicOnServerWithResult.class, 450);
        put(UpdateModelRequest.class, 451);
        put(LoadDataResponse.class, 452);
        put(StorageValueProvider.class, 453);
        put(ArchiveValueProvider.class, 454);
//        put(ChartValueProviderFactory.class, 455);
//        put(OctSpecterValueProvider.class, 456);
//        put(ScalarValueProvider.class, 457);
//        put(SpecterValueProvider.class, 458);
//        put(LoadFromArchiveByCount.class, 459);
//        put(LoadFromArchiveByTimestamp.class, 460);
//        put(LoadFromArchiveRequest.class, 461);
        put(McpType.class, 462);
        put(MultiScalarChartOptions.class, 463);
        put(MultiStorageValueProvider.class, 464);
        put(TemplateMeasurement.class, 465);
        put(TemplateMeasurementPojo.class, 466);
        put(TemplateOct3SpecterMeasurement.class, 467);
        put(TemplateOct3SpecterMeasurementPojo.class, 468);
        put(TaskType13.class, 469);
        put(TaskType13Pojo.class, 470);
        put(TaskType13Result.class, 471);
        put(TaskType13ResultPojo.class, 472);
        put(TaskType13ResultsPerSensors.class, 473);
        put(TaskType13ResultsPerSensorsPojo.class, 474);
        put(TaskType14.class, 475);
        put(TaskType14Pojo.class, 476);
        put(TaskType14Result.class, 477);
        put(TaskType14ResultPojo.class, 478);
        put(TaskType14ResultsPerSensors.class, 479);
        put(TaskType14ResultsPerSensorsPojo.class, 480);
        put(TaskType15.class, 481);
        put(TaskType15Pojo.class, 482);
        put(TaskType15Result.class, 483);
        put(TaskType15ResultPojo.class, 484);
        put(TaskType15ResultsPerSensors.class, 485);
        put(TaskType15ResultsPerSensorsPojo.class, 486);
        put(TaskType16.class, 487);
        put(TaskType16Pojo.class, 488);
        put(TaskType16Result.class, 489);
        put(TaskType16ResultPojo.class, 490);
        put(TaskType16ResultsPerSensors.class, 491);
        put(TaskType16ResultsPerSensorsPojo.class, 492);
        put(TaskType17.class, 493);
        put(TaskType17Pojo.class, 494);
        put(TaskType17Result.class, 495);
        put(TaskType17ResultPojo.class, 496);
        put(TaskType17ResultsPerSensors.class, 497);
        put(TaskType17ResultsPerSensorsPojo.class, 498);
        put(StorageCountersHolder.class, 499);
        put(StorageCountersHolderPojo.class, 500);
        put(LoadLastStoragesValues.class, 501);
        put(StateRuleGroup.class, 502);
        put(StateRuleGroupPojo.class, 503);
        put(RunUp.class, 504);
        put(RunUpPojo.class, 505);
        put(LoadRunUpDataRequest.class, 506);
        put(BaseMnemoItemWithText.class, 507);
        put(BaseMnemoItemWithTextPojo.class, 508);
        put(MnemoNodeName.class, 509);
        put(MnemoNodeNamePojo.class, 510);
        put(MnemoSerialNumber.class, 511);
        put(MnemoSerialNumberPojo.class, 512);
        put(S1500ControllerRecord.class, 513);
        put(S1500ControllerRecordPojo.class, 514);
//        put(ET7000Controller.class, 515);
//        put(ET7000ControllerPojo.class, 516);
//        put(EtControllerRecord.class, 517);
//        put(EtControllerRecordPojo.class, 518);
        put(ControllerRecord.class, 519);
        put(ScalarBySignalDescription.class, 520);
        put(AverageByInputSignalDescription.class, 521);
        put(MaxByInputSignalDescription.class, 522);
        put(PikByInputSignalDescription.class, 523);
//        put(SendSerialsRecord.class, 524);
//        put(SendSerialsRecordState.class, 525);
//        put(SiemensSendSerialsRule.class, 526);
//        put(SiemensSendSerialsRulePojo.class, 527);
//        put(SetNeedSendSerialsFlag.class, 528);
        put(SharedObjectPojo.class, 529);
        put(ViLocalListWithBaseEquals.class, 530);
        put(TaskType10ResultsPerSensors.class, 531);
        put(TaskType10ResultsPerSensorsPojo.class, 532);
        put(ArchiveRecord.class, 533);
        put(ArchiveData.class, 534);
        put(LoadAllArchiveRecordRequest.class, 535);
        put(LoadArchiveRecordRequest.class, 536);
        put(LoadArchiveDataRequest.class, 537);
        put(RunUpRecord.class, 538);
        put(RunUpData.class, 539);
        put(LoadAllRunUpRecordRequest.class, 540);
        put(ArchiveFiltrationTimestamp.class, 541);
        put(ArchiveFiltrationTimestampPojo.class, 542);
        put(MachinePack.class, 543);
        put(MachinePackPojo.class, 544);
        put(MachinePackListPojo.class, 545);
        put(CaclPackRequestEvent.class, 546);
        put(ScalarParamValue[].class, 547);
        put(DeleteFromArchiveRequest.class, 548);
        put(DeleteFromRunUpRequest.class, 549);
        put(CalcTempNodeRequest.class, 550);
        put(SetChannelWorkRequest.class, 551);
        put(SetStatefulOnlineRequest.class, 552);
        put(Oct3UnstableDescription.class, 553);
        put(Oct3UnstableWithModuleDescription.class, 554);
        put(MnemoParamValuePojo.class, 555);
        put(MnemoParamValue.class, 556);
        put(ChannelWorkState.class, 557);
        put(Representation.class, 558);
        put(ChannelWorkStateWithDescription.class, 559);
        put(McpFailureDescription.class, 560);
        put(CommitToModelRequest.class, 561);
        put(ClearDangerStateTask.class, 562);
        put(SimpleController.class, 563);
        put(SimpleControllerPojo.class, 564);
        put(ModbusController.class, 565);
        put(ModbusControllerPojo.class, 566);
        put(ControllerRule.class, 567);
        put(ControllerRulePojo.class, 568);
        put(LogicNodeBreakRule.class, 569);
        put(HighDangerStateRule.class, 570);
        put(NormalDangerStateRule.class, 571);
        put(OrangeDangerStateRule.class, 572);
        put(RedDangerStateRule.class, 573);
        put(WarnDangerStateRule.class, 574);
        put(OrangePiGpioController.class, 575);
        put(OrangePiGpioControllerPojo.class, 576);
        put(OrangePiGpioEvent.class, 577);
        put(BaseDangerStateRule.class, 578);
        put(SpecterLineStoryOptionsFreqByHann.class, 579);
        put(DiagnosticsModule.class, 580);
        put(DiagnosticsModulePojo.class, 581);
        put(Defect.class, 582);
        put(DefectPojo.class, 583);
        put(DefectValues.class, 584);
        put(DefectValuesPojo.class, 585);
        put(DefectRecommendations.class, 586);
        put(DefectRecommendationsPojo.class, 587);
        put(ViLinkedMap.class, 588);
        put(ViLinkedMapPojo.class, 589);
        put(ExternalSystemKeys.class, 590);
        put(ExternalSystemKeysPojo.class, 591);
        put(LimanDeviceId.class, 592);
        put(ExternalKey.class, 593);
        put(ReadSerialsRecord.class, 594);
        put(ReadSerialsRecordState.class, 595);
        put(SiemensReadSerialsRule.class, 596);
        put(SiemensReadSerialsRulePojo.class, 597);
        put(LoadS1500ControllerDataRequest.class, 598);
        put(S1500ExchangeData.class, 599);
        put(S1500ExchangeData.PlatformRecord.class, 600);
        put(PlatformState.class, 610);
        put(CannotLoadDbByNameEvent.class, 611);
        put(LoadDbByNameEvent.class, 612);
        put(BinaryProtoController.class, 613);
        put(BinaryProtoControllerPojo.class, 614);
        put(NetProtocol.class, 615);
        put(ControllerRuleWithAreas.class, 616);
        put(ControllerRuleWithAreasPojo.class, 617);
        put(SimpleControllerWithAreas.class, 618);
        put(SimpleControllerWithAreasPojo.class, 619);
        put(MachineStateRule.class, 620);
        put(MachineStateRulePojo.class, 621);
        put(short[].class, 622);
        put(SaveMcpCurrentRawDataEvent.class, 623);
        put(SaveMcpCurrentRawDataEventHandler.class, 624);
        put(ClientPush.class, 700);
    }
}
