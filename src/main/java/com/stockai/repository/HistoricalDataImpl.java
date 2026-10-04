package com.stockai.repository;

import com.stockai.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Transactional
public class HistoricalDataImpl implements HistoricalDataRepository, CustomersRespository, DashboardRepository, AuditLogRepository,
        InstrumentsRepository, DailyStockRepository, FiltersRepository, PredictionRepository, MarketRepository,
        PatternsRepository, PaymentRepository, ReportsRepository, PlansRepository, StatementRepository, StockAnalyzerRepository {
    @PersistenceContext
    EntityManager entityManager = null;

    @Override
    @Transactional
    public void saveAllHistoricalData(List<HistoricalDataEntity> historicalDataEntities) {
        for (HistoricalDataEntity entity : historicalDataEntities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public List<HistoricalDataEntity> findAll(String startDate, String endDate) {
        return entityManager.createNativeQuery("SELECT * FROM dmastock.historical_data WHERE created_date BETWEEN :startDate AND :endDate ORDER BY symbol, created_date", HistoricalDataEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }


    @Override
    @Transactional
    public void saveAllInstrumetns(List<InstrumentsEntity> instrumentsEntities) {
        for (InstrumentsEntity entity : instrumentsEntities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public List<String> findAllInstruments() {
        return entityManager.createNativeQuery("SELECT name FROM dmastock.instruments").getResultList();
    }

    @Override
    @Transactional
    public void truncateInstrumentsTable() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.instruments").executeUpdate();
    }

    @Override
    @Transactional
    public void saveAllDailyStockData(List<DailyStockModel> dailyStockModels) {
        for (DailyStockModel entity : dailyStockModels) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public int countAll() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.daily_stock").getSingleResult()).intValue();
    }

    @Override
    public List<DashboardEntity> findAllDashboardData(DashboardEntity filters) {
        return findTextFiltered(DashboardEntity.class, "createdDate",
                textFilters("title", filters.getTitle(), "description", filters.getDescription(),
                        "status", filters.getStatus()), filters.getId(), filters.getFromDate(), filters.getToDate());
    }

    @Override
    @Transactional
    public void truncateDailyStockData() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.daily_stock").executeUpdate();
    }

    @Override
    public List<DailyStockModel> fullData() {
        return entityManager.createQuery("SELECT d FROM DailyStockModel d", DailyStockModel.class).getResultList();
    }

    /*Below are the all implemented Methods are customer Repository implementation methods Quires.*/

    @Override
    public List<CustomerEntity> fetchAllCustomers() {
        return entityManager.createQuery("SELECT c FROM CustomerEntity c ORDER BY c.createdDate DESC", CustomerEntity.class).getResultList();
    }

    @Override
    public List<CustomerEntity> findCustomers(CustomerEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerEntity> query = builder.createQuery(CustomerEntity.class);
        Root<CustomerEntity> root = query.from(CustomerEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), filters.getStatus()));
        }
        if (filters.getCreatedDate() != null) {
            predicates.add(builder.equal(root.get("createdDate"), filters.getCreatedDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.select(root);
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public int saveAllCustomers(List<CustomerEntity> entities) {
        int count = 0;
        for (CustomerEntity entity : entities) {
            CustomerEntity existing = entityManager.find(CustomerEntity.class, entity.getUserId());
            if (!Objects.nonNull(existing)) {
                entityManager.persist(entity);
                count++;
            } else {
                entityManager.merge(entity);
                count++;
            }
        }
        return count;
    }


    @Override
    public List<MarketEntity> findAllMarketData(MarketEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MarketEntity> query = builder.createQuery(MarketEntity.class);
        Root<MarketEntity> root = query.from(MarketEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        addLikePredicate(builder, root, "name", filters.getName(), predicates);
        addLikePredicate(builder, root, "description", filters.getDescription(), predicates);
        addLikePredicate(builder, root, "status", filters.getStatus(), predicates);
        addLikePredicate(builder, root, "symbol", filters.getSymbol(), predicates);
        if (filters.getId() != null) {
            predicates.add(builder.equal(root.get("id"), filters.getId()));
        }
        if (filters.getHistoricalDataId() != null) {
            predicates.add(builder.equal(root.get("historicalDataId"), filters.getHistoricalDataId()));
        }
        if (filters.getFromDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.<LocalDate>get("marketDate"), filters.getFromDate()));
        }
        if (filters.getToDate() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.<LocalDate>get("marketDate"), filters.getToDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("marketDate")), builder.asc(root.get("symbol")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public void upsertHistoricalMarkets(List<MarketEntity> rows) {
        for (MarketEntity row : rows) {
            MarketEntity existing = entityManager.createQuery(
                            "SELECT m FROM MarketEntity m WHERE m.historicalDataId = :historicalDataId",
                            MarketEntity.class)
                    .setParameter("historicalDataId", row.getHistoricalDataId())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                entityManager.persist(row);
            } else {
                row.setId(existing.getId());
                entityManager.merge(row);
            }
        }
        entityManager.flush();
    }

    @Override
    public int deleteSyncedMarketHistoryBefore(LocalDate cutoffDate) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<MarketEntity> delete = builder.createCriteriaDelete(MarketEntity.class);
        Root<MarketEntity> root = delete.from(MarketEntity.class);
        delete.where(
                builder.isNotNull(root.get("historicalDataId")),
                builder.lessThan(root.<LocalDate>get("marketDate"), cutoffDate));
        return entityManager.createQuery(delete).executeUpdate();
    }

    @Override
    public List<PatternEntity> findAllPatternData(PatternEntity filters) {
        return findTextFiltered(PatternEntity.class, "createdDate",
                textFilters("name", filters.getName(), "description", filters.getDescription(),
                        "status", filters.getStatus()), filters.getId(), filters.getFromDate(), filters.getToDate());
    }

    @Override
    public List<ReportEntity> findAllReports(ReportEntity filters) {
        return findTextFiltered(ReportEntity.class, "createdDate",
                textFilters("title", filters.getTitle(), "description", filters.getDescription(),
                        "status", filters.getStatus()), filters.getId(), filters.getFromDate(), filters.getToDate());
    }

    @Override
    public List<StockAnalyzerEntity> findAllStockAnalyses(StockAnalyzerEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StockAnalyzerEntity> query = builder.createQuery(StockAnalyzerEntity.class);
        Root<StockAnalyzerEntity> root = query.from(StockAnalyzerEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        addLikePredicate(builder, root, "symbol", filters.getSymbol(), predicates);
        addLikePredicate(builder, root, "analysisResult", filters.getAnalysisResult(), predicates);
        addLikePredicate(builder, root, "status", filters.getStatus(), predicates);
        if (filters.getId() != null) {
            predicates.add(builder.equal(root.get("id"), filters.getId()));
        }
        if (filters.getPredictionId() != null) {
            predicates.add(builder.equal(root.get("predictionId"), filters.getPredictionId()));
        }
        if (filters.getPredictionRunId() != null) {
            predicates.add(builder.equal(root.get("predictionRunId"), filters.getPredictionRunId()));
        }
        if (filters.getPredictionDirectionFilter() != null && !filters.getPredictionDirectionFilter().isBlank()) {
            predicates.add(builder.equal(builder.lower(root.get("predictionDirection")),
                    filters.getPredictionDirectionFilter().trim().toLowerCase(Locale.ROOT)));
        }
        if (filters.getMinimumConfidence() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.<Double>get("confidencePercent"), filters.getMinimumConfidence()));
        }
        if (filters.getMaximumConfidence() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.<Double>get("confidencePercent"), filters.getMaximumConfidence()));
        }
        if (filters.getFromDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdDate"),
                    filters.getFromDate().atStartOfDay()));
        }
        if (filters.getToDate() != null) {
            predicates.add(builder.lessThan(root.<LocalDateTime>get("createdDate"),
                    filters.getToDate().plusDays(1).atStartOfDay()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("createdDate")), builder.desc(root.get("id")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public void upsertStockAnalyzerRows(List<StockAnalyzerEntity> rows) {
        for (StockAnalyzerEntity row : rows) {
            StockAnalyzerEntity existing = entityManager.createQuery(
                            "SELECT s FROM StockAnalyzerEntity s WHERE s.predictionId = :predictionId",
                            StockAnalyzerEntity.class)
                    .setParameter("predictionId", row.getPredictionId())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                entityManager.persist(row);
            } else {
                row.setId(existing.getId());
                entityManager.merge(row);
            }
        }
        entityManager.flush();
    }

    @Override
    public List<PaymentEntity> fetchAllPayments() {
        return entityManager.createQuery("SELECT P FROM PaymentEntity P", PaymentEntity.class).getResultList();
    }

    @Override
    public List<PaymentEntity> findPaymentPlans(PaymentEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PaymentEntity> query = builder.createQuery(PaymentEntity.class);
        Root<PaymentEntity> root = query.from(PaymentEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getPaymentType() != null) {
            predicates.add(builder.equal(root.get("status"), filters.getStatus()));
        }
        if (filters.getPaymentType() != null) {
            predicates.add(builder.equal(root.get("paymentType"), filters.getPaymentType()));
        }
        if (filters.getCreatedDate() != null) {
            predicates.add(builder.equal(root.get("createdDate"), filters.getCreatedDate()));
        }
        if (filters.getUserName() != null) {
            predicates.add(builder.equal(root.get("userName"), filters.getUserName()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public void upsertPaymentPlans(List<PaymentEntity> plans) {
        for (PaymentEntity plan : plans) {
            PaymentEntity existing = entityManager.createQuery(
                            "SELECT p FROM PaymentEntity p WHERE p.recordType = :recordType AND p.planId = :planId",
                            PaymentEntity.class)
                    .setParameter("recordType", "SUBSCRIPTION")
                    .setParameter("planId", plan.getPlanId())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                entityManager.persist(plan);
            } else {
                plan.setId(existing.getId());
                entityManager.merge(plan);
            }
        }
        entityManager.flush();
    }

    private <T> List<T> findTextFiltered(Class<T> entityType, String sortField, Map<String, String> filters,
                                         Object id, LocalDate fromDate, LocalDate toDate) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        Root<T> root = query.from(entityType);
        List<Predicate> predicates = new ArrayList<>();
        filters.forEach((field, value) -> addLikePredicate(builder, root, field, value, predicates));
        if (id != null) {
            predicates.add(builder.equal(root.get("id"), id));
        }
        addDateRange(builder, root, sortField, fromDate, toDate, predicates);
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get(sortField)));
        return entityManager.createQuery(query).getResultList();
    }

    private static Map<String, String> textFilters(String... fieldValues) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (int index = 0; index < fieldValues.length; index += 2) {
            filters.put(fieldValues[index], fieldValues[index + 1]);
        }
        return filters;
    }

    private static <T> void addDateRange(CriteriaBuilder builder, Root<T> root, String dateField,
                                         LocalDate fromDate, LocalDate toDate, List<Predicate> predicates) {
        if (fromDate != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    root.<LocalDateTime>get(dateField), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            predicates.add(builder.lessThan(
                    root.<LocalDateTime>get(dateField), toDate.plusDays(1).atStartOfDay()));
        }
    }

    private static <T> void addLikePredicate(CriteriaBuilder builder, Root<T> root, String field,
                                             String value, List<Predicate> predicates) {
        if (value != null && !value.isBlank()) {
            String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(builder.like(builder.lower(root.get(field).as(String.class)), pattern));
        }
    }

    @Override
    public void savePrediction(List<PredictionEntity> predictionDto) {
        for (PredictionEntity entity : predictionDto) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public List<PredictionEntity> findAllPredictions() {
        return entityManager.createQuery("SELECT p FROM PredictionEntity p ORDER BY p.id", PredictionEntity.class).getResultList();
    }

    @Override
    public void savePredictionRun(PredictionRunEntity predictionRun) {
        entityManager.persist(predictionRun);
        entityManager.flush();
    }

    @Override
    public PredictionRunEntity findLatestPredictionRun() {
        return entityManager.createQuery(
                        "SELECT r FROM PredictionRunEntity r ORDER BY r.createdAt DESC, r.id DESC",
                        PredictionRunEntity.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<PredictionRunEntity> findPredictionRuns(PredictionRunEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PredictionRunEntity> query = builder.createQuery(PredictionRunEntity.class);
        Root<PredictionRunEntity> root = query.from(PredictionRunEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters != null && filters.getStartDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("startDate"), filters.getStartDate()));
        }
        if (filters != null && filters.getEndDate() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("endDate"), filters.getEndDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("createdAt")), builder.desc(root.get("id")));
        int limit = filters == null || filters.getLimit() == null ? 20 : filters.getLimit();
        return entityManager.createQuery(query).setMaxResults(Math.min(100, Math.max(1, limit))).getResultList();
    }

    @Override
    public List<PredictionEntity> findPredictions(UUID runId, PredictionEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PredictionEntity> query = builder.createQuery(PredictionEntity.class);
        Root<PredictionEntity> root = query.from(PredictionEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("runId"), runId));
        addLikePredicate(builder, root, "symbol", filters.getSymbol(), predicates);
        if (filters.getId() != null) {
            predicates.add(builder.equal(root.get("id"), filters.getId()));
        }
        if (filters.getPredictionDirectionFilter() != null && !filters.getPredictionDirectionFilter().isBlank()) {
            predicates.add(builder.equal(builder.lower(root.get("prediction_direction")),
                    filters.getPredictionDirectionFilter().trim().toLowerCase(Locale.ROOT)));
        }
        if (filters.getMinimumConfidence() != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    root.<Double>get("confidence_percent"), filters.getMinimumConfidence()));
        }
        if (filters.getMaximumConfidence() != null) {
            predicates.add(builder.lessThanOrEqualTo(
                    root.<Double>get("confidence_percent"), filters.getMaximumConfidence()));
        }
        if (filters.getFromDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    root.<LocalDate>get("created_date"), filters.getFromDate()));
        }
        if (filters.getToDate() != null) {
            predicates.add(builder.lessThanOrEqualTo(
                    root.<LocalDate>get("created_date"), filters.getToDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("confidence_percent")), builder.asc(root.get("symbol")));
        return entityManager.createQuery(query).getResultList();
    }

    /* Below all implemented Methods are Plan related Queries*/
    @Override
    @Transactional
    public void addPlan(PlanEntity planEntity) {
        if (Objects.nonNull(planEntity))
            entityManager.persist(planEntity);
        entityManager.flush();
    }

    public PlanEntity findByUserName(String name) {
        return entityManager.createQuery("SELECT p FROM PlanEntity p WHERE p.name = :name", PlanEntity.class)
                .setParameter("name", name)
                .getResultList().stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public PlanEntity updatePlan(PlanEntity planEntity) {
        PlanEntity merged = entityManager.merge(planEntity);
        entityManager.flush();
        return merged;
    }

    @Override
    public List<PlanEntity> findAllPlans() {
        return entityManager.createQuery("SELECT p FROM PlanEntity p", PlanEntity.class).getResultList();
    }

    /* Below all implemented methos are Audit logs  implementation Quires*/
    @Override
    public List<AuditLogEntity> fetchByFilterLogs(AuditLogEntity auditLogEntity) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLogEntity> query = builder.createQuery(AuditLogEntity.class);
        Root<AuditLogEntity> root = query.from(AuditLogEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (auditLogEntity.getEmail() != null) {
            predicates.add(builder.equal(root.get("email"), auditLogEntity.getEmail()));
        }
        if (auditLogEntity.getEvent_type() != null) {
            predicates.add(builder.equal(root.get("event_type"), auditLogEntity.getEvent_type()));
        }
        if (auditLogEntity.getEventAt() != null) {
            predicates.add(builder.equal(root.get("createdDate"), auditLogEntity.getCreatedDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<AuditLogEntity> fetchAllAuditLogs() {
        return entityManager.createQuery("SELECT a FROM AuditLogEntity a", AuditLogEntity.class).getResultList();
    }

    @Override
    public AuditLogEntity saveAuditLogEntity(AuditLogEntity auditLogEntity) {
        if (Objects.nonNull(auditLogEntity)) {
            entityManager.persist(auditLogEntity);
        }
        entityManager.flush();
        return auditLogEntity;
    }

    @Override
    public void truncateAudiLogs() {
        entityManager.createQuery("TRUNCATE TABLE AuditLogEntity").executeUpdate();
    }

    /* Below written queries all for statement related quires*/
    @Override
    public List<StatementEntity> findAllStatements() {
        return entityManager.createQuery("SELECT S FROM StatementEntity S", StatementEntity.class).getResultList();
    }

    @Override
    public StatementEntity saveStatement(StatementEntity statement) {
        entityManager.persist(statement);
        entityManager.flush();
        return statement;
    }

    @Override
    public List<StatementEntity> findStatements(StatementEntity filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<StatementEntity> query = builder.createQuery(StatementEntity.class);
        Root<StatementEntity> root = query.from(StatementEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), filters.getStatus()));
        }
        if (filters.getSymbol() != null) {
            predicates.add(builder.equal(root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getStockType() != null) {
            predicates.add(builder.equal(root.get("stockType"), filters.getStockType()));
        }
        if (filters.getTradeType() != null) {
            predicates.add(builder.equal(root.get("tradeType"), filters.getTradeType()));
        }
        if (filters.getFromDate() != null) {
            predicates.add(builder.equal(root.get("fromDate"), filters.getFromDate()));
        }
        if (filters.getToDate() != null) {
            predicates.add(builder.equal(root.get("toDate"), filters.getToDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    /* Below are the all filter related quaries implemented*/
    @Override
    public List<String> getReportFilters() {
        return entityManager.createQuery(
                        "SELECT DISTINCT r.reports FROM ReportFiltersEntity r ORDER BY r.reports", String.class)
                .getResultList();
    }

    @Override
    public List<String> getPatternFilters() {
        return entityManager.createQuery(
                        "SELECT DISTINCT p.patternName FROM PatternFiltersEntity p ORDER BY p.patternName", String.class)
                .getResultList();
    }

    @Override
    public List<StockAnalyzerFiltersEntity> getStockAnalyzerFilters() {
        return entityManager.createQuery(
                        "SELECT s FROM StockAnalyzerFiltersEntity s ORDER BY s.id", StockAnalyzerFiltersEntity.class)
                .getResultList();
    }

    @Override
    public List<MarketFiltersEntity> getStockMarketFilters() {
        return entityManager.createQuery(
                        "SELECT m FROM MarketFiltersEntity m ORDER BY m.id", MarketFiltersEntity.class)
                .getResultList();
    }

    @Override
    public List<MarketOverviewFiltersEntity> getDashBoardFilters() {
        return entityManager.createQuery(
                        "SELECT m FROM MarketOverviewFiltersEntity m ORDER BY m.id", MarketOverviewFiltersEntity.class)
                .getResultList();
    }

}
