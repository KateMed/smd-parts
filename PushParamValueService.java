package ru.vibrotek.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.vibrotek.domain.nodes.machine.level7.Measurement;
import ru.vibrotek.domain.nodes.machine.level7.MeasurementPack;
import ru.vibrotek.eventbus.EventBusHandlerContainer;
import ru.vibrotek.eventbus.events.PrintMcpStatsEvent;
import ru.vibrotek.exceptions.ViRuntimeException;
import ru.vibrotek.logger.ViLogger;
import ru.vibrotek.model.SharedKey;
import ru.vibrotek.model.TimeLabel;
import ru.vibrotek.model.collection.list.ViLocalList;
import ru.vibrotek.param.value.ParamValue;
import ru.vibrotek.query.PushAllParamValueQuery;
import ru.vibrotek.service.executor.ViTask;
import ru.vibrotek.service.executor.ViTaskExecutor;
import ru.vibrotek.service.history.PushParamValueQuery;
import ru.vibrotek.service.history.PushSyncPackValueQuery;
import ru.vibrotek.service.history.PushValueQuery;
import ru.vibrotek.service.query.LogicOnServerSender;
import ru.vibrotek.service.query.builder.SerialQuery;
import ru.vibrotek.service.query.builder.UniqueQuery;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ru.vibrotek.logger.ViLogger.MCP_DEBUG;

/**
 *
 */
public class PushParamValueService {

    @Autowired
    private ViTaskExecutor executor;
    @Autowired
    private LogicOnServerSender logicOnServerSender;
    @Value("${push.task.period:500}")
    private int pushTaskPeriod;


    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private Map<SharedKey, PushTask> tasks = new HashMap<>();


    @PostConstruct
    public void init() {

    }

    public void push(Measurement measurement, ParamValue paramValue) {
        lock.writeLock().lock();
        try {
            if (paramValue.getDescription() == null) {
                ViLogger.DEBUG.info("clone.getDescription() = null");
            }

            if (!paramValue.getDescription().equals(measurement.getParamDescription())) {
                ViLogger.DEBUG.error("дескрипшены разные!!! {} и {}", paramValue.getDescription(), measurement.getParamDescription());
                throw new ViRuntimeException("дескрипшены разные!");
            }

            // группировка минимум по машине - по измерениям нельзя!!!
            // приводит к параллельной модификации модели
            SharedKey key = measurement.getMachineNode().getKey();
//            SharedKey key = measurement.getKey();
            PushTask pushTask = getPushTask(key, measurement.getMachineNode().getTreeNodeTitle());
            pushTask.add(new PushParamValueQuery(measurement, paramValue));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void push(UUID recordId, MeasurementPack measurementPack, Map<Measurement, ParamValue> result, TimeLabel timeLabel) {
        lock.writeLock().lock();
        try {
            SharedKey key = measurementPack.getKey();
            PushTask pushTask = getPushTask(key, measurementPack.getInfo());
            pushTask.add(new PushSyncPackValueQuery(recordId, result, timeLabel));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private PushTask getPushTask(SharedKey key, String description) {
        PushTask pushTask = tasks.get(key);
        if (pushTask == null) {
            pushTask = new PushTask(description);
            tasks.put(key, pushTask);

            executor.scheduleWithFixedDelay(pushTask, pushTaskPeriod, pushTaskPeriod, TimeUnit.MILLISECONDS);
        }
        return pushTask;
    }


    private class PushTask extends ViTask implements SerialQuery, UniqueQuery {
        private final String description;
        private final ViLocalList<Long> executionTimes = new ViLocalList<>();
        private final ReentrantReadWriteLock execLocker = new ReentrantReadWriteLock();

        private ViLocalList<PushValueQuery> cache = new ViLocalList<>();

        private UUID previousQueryId = null;
        private UUID currentQueryId = getUuid();
        private UUID nextQueryId = getUuid();
        private ViLocalList<PushValueQuery> queries = new ViLocalList<>();

        private EventBusHandlerContainer eventBusHandlerContainer = new EventBusHandlerContainer();


        public PushTask(String description) {
            super("ru.vibrotek.service.PushParamValueService.PushTask" + " " + description);
            this.description = description;

            eventBusHandlerContainer.registerEventListener(PrintMcpStatsEvent.class, () -> {
                execLocker.writeLock().lock();
                int execCount = executionTimes.size();
                long totalTime = 0;
                try {
                    for (int i = 0; i < executionTimes.size(); i++) {
                        Long time = executionTimes.get(i);
                        totalTime += time;
                    }
                    executionTimes.clear();
                } finally {
                    execLocker.writeLock().unlock();
                }
                if (execCount > 0) {
                    long averageTime = totalTime / execCount;
                    MCP_DEBUG.info("PushAllParam: {} отправлено раз: {}, всего времени {} мс, среднее время: {} мс",
                                   description, execCount, totalTime, averageTime);
                } else {
                    MCP_DEBUG.info("PushAllParam: {} отправлено раз: {}, всего времени {} мс",
                                   description, execCount, totalTime);
                }
            });
        }

        @Override
        protected void execute() {
            lock.writeLock().lock();
            long start = System.currentTimeMillis();
            try {
                ViLocalList<PushValueQuery> tmp = queries;
                queries = cache;
                cache = tmp;
            } finally {
                lock.writeLock().unlock();
            }

            if (!queries.isEmpty()) {

                PushAllParamValueQuery pushAllParamValueQuery = new PushAllParamValueQuery();
                pushAllParamValueQuery.add(queries);
                queries.clear();

                logicOnServerSender.sendOnTransacionEnd(pushAllParamValueQuery, this, this);

                previousQueryId = currentQueryId;
                currentQueryId = nextQueryId;
                nextQueryId = getUuid();

            }
            long finish = System.currentTimeMillis();
            execLocker.writeLock().lock();
            try {
                executionTimes.add(finish - start);
            } finally {
                execLocker.writeLock().unlock();
            }

        }

        private UUID getUuid() {
            return UUID.randomUUID();
        }

        public void add(PushValueQuery pushParamValueQuery) {
            cache.add(pushParamValueQuery);
        }

        @Override
        public UUID getPreviousQueryId() {
            return previousQueryId;
        }

        @Override
        public UUID getNextQueryId() {
            return nextQueryId;
        }

        @Override
        public UUID getCurrentId() {
            return currentQueryId;
        }
    }
}
