package com.stockai.repository;

import com.stockai.dto.*;
import com.stockai.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Transactional
public class HistoricalDataImpl implements HistoricalDataRepository, CustomersRespository, DashboardRepository, AuditLogRepository,
        InstrumentsRepository, DailyStockRepository, FiltersRepository, PredictionRepository, MarketRepository,
        PatternsRepository, PaymentRepository, ReportsRepository, PlansRepository, StatementRepository, HistoricalDashboardRepository {
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
    public List<HistoricalDataEntity> findAll(LocalDate startDate, LocalDate endDate) {
        return entityManager.createNativeQuery("SELECT * FROM dmastock.historical_data WHERE created_date BETWEEN :startDate AND :endDate ORDER BY symbol, created_date", HistoricalDataEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }

    @Override
    public List<HistoricalDataEntity> findAll(LocalDate startDate, LocalDate endDate, int offset, int limit) {
        return entityManager.createNativeQuery(
                        "SELECT * FROM dmastock.historical_data WHERE created_date BETWEEN :startDate AND :endDate ORDER BY symbol, created_date LIMIT :limit OFFSET :offset",
                        HistoricalDataEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
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
    public List<String> findInstrumentsSymbols() {
        return entityManager.createNativeQuery("SELECT symbol FROM dmastock.instruments").getResultList();
    }

    @Override
    public List<InstrumentsEntity> findInstrumentsByExchange(String exchange) {
        return entityManager.createQuery(
                        "SELECT i FROM InstrumentsEntity i WHERE UPPER(i.exchSeg) = :exchange", InstrumentsEntity.class)
                .setParameter("exchange", exchange.trim().toUpperCase(Locale.ROOT))
                .getResultList();
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
    public List<DashboardEntity> findAllDashboardData(DashboardFilterDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DashboardEntity> query = builder.createQuery(DashboardEntity.class);
        Root<DashboardEntity> root = query.from(DashboardEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getConfidenceScoreDiff() != null && !filters.getConfidenceScoreDiff().isEmpty()) {
            // map filter to actual DashboardEntity field
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidenceScore", filters.getConfidenceScoreDiff()));
        }
        if (filters.getNextDayHighDiff() != null && !filters.getNextDayHighDiff().isEmpty()) {
            // map filter to actual DashboardEntity field
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayHigh", filters.getNextDayHighDiff()));
        }
        if (filters.getNextDayLowDiff() != null && !filters.getNextDayLowDiff().isEmpty()) {
            // map filter to actual DashboardEntity field
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayLow", filters.getNextDayLowDiff()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    private static Predicate buildMultiValueRangePredicate(CriteriaBuilder builder, Root<?> root, String fieldName, List<String> values) {
        List<Predicate> valuePredicates = new ArrayList<>();
        for (String rawValue : values) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            Predicate predicate = buildSingleRangePredicate(builder, root, fieldName, rawValue.trim());
            if (predicate != null) {
                valuePredicates.add(predicate);
            }
        }
        if (valuePredicates.isEmpty()) {
            return builder.conjunction();
        }
        return valuePredicates.size() == 1 ? valuePredicates.get(0) : builder.or(valuePredicates.toArray(Predicate[]::new));
    }

    private static Predicate buildSingleRangePredicate(CriteriaBuilder builder, Root<?> root, String fieldName, String value) {
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("High")) {
            return builder.between(root.get(fieldName).as(Double.class), 80.0, 100.0);
        }
        if (normalized.equalsIgnoreCase("Medium")) {
            return builder.between(root.get(fieldName).as(Double.class), 60.0, 80.0);
        }
        if (normalized.equalsIgnoreCase("Low")) {
            return builder.between(root.get(fieldName).as(Double.class), 50.0, 70.0);
        }

        if (normalized.matches("(?i)^(highest|lowest)(\\d+)$")) {
            String kind = normalized.substring(0, normalized.length() - String.valueOf(Integer.parseInt(normalized.replaceAll("(?i)^(highest|lowest)", ""))).length());
            int upper = Integer.parseInt(normalized.replaceAll("(?i)^(highest|lowest)", ""));
            int lower = Math.max(0, upper - 10);
            if (upper <= 0) {
                return null;
            }
            return builder.between(root.get(fieldName).as(Double.class), (double) lower, (double) upper);
        }

        if (normalized.matches("^\\d+(?:\\.\\d+)?$")) {
            double numeric = Double.parseDouble(normalized);
            return builder.equal(root.get(fieldName).as(Double.class), numeric);
        }

        return null;
    }

    private static Predicate buildStringInPredicate(CriteriaBuilder builder, jakarta.persistence.criteria.Expression<String> field, List<String> values) {
        if (values == null || values.isEmpty()) {
            return builder.conjunction();
        }
        List<String> normalized = values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty()).toList();
        if (normalized.isEmpty()) {
            return builder.conjunction();
        }
        return field.in(normalized);
    }

    @Override
    public void replaceDashboardData(List<DashboardEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (DashboardEntity entity : rows) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public void deleteDashboardRecords() {
        entityManager.createQuery("DELETE FROM DashboardEntity").executeUpdate();
    }

    @Override
    public List<DashboardEntity> findAllDashboardSummary() {
        return entityManager.createQuery("SELECT d FROM DashboardEntity d", DashboardEntity.class).getResultList();
    }

    @Override
    public int getCount() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.markets").getSingleResult()).intValue();
    }

    @Override
    public List<MarketEntity> findMarketsByFilters(MarketFiltersDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MarketEntity> query = builder.createQuery(MarketEntity.class);
        Root<MarketEntity> root = query.from(MarketEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getConfidenceScore() != null && !filters.getConfidenceScore().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidenceScore", filters.getConfidenceScore()));
        }
        if (filters.getNextDayHigh() != null && !filters.getNextDayHigh().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayHigh", filters.getNextDayHigh()));
        }
        if(filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayLow", filters.getNextDayLow()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
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
    public List<CustomerEntity> findCustomers(CustomersFilterDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerEntity> query = builder.createQuery(CustomerEntity.class);
        Root<CustomerEntity> root = query.from(CustomerEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), filters.getStatus()));
        }
        if (filters.getCreatedDate() != null) {
            String createdDateStr = filters.getCreatedDate().trim();
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(createdDateStr);
                predicates.add(builder.equal(root.get("createdDate"), dt));
            } catch (java.time.format.DateTimeParseException ex) {
                try {
                    java.time.LocalDate d = java.time.LocalDate.parse(createdDateStr);
                    predicates.add(builder.between(root.get("createdDate"), d.atStartOfDay(), d.plusDays(1).atStartOfDay()));
                } catch (java.time.format.DateTimeParseException ex2) {
                }
            }
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
    public List<MarketEntity> findAllMarketData(int offset, int limit) {
        return entityManager.createQuery("SELECT m FROM MarketEntity m ORDER BY m.createdDate DESC, m.symbol", MarketEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<MarketEntity> findAllMarketData() {
        return entityManager.createQuery("SELECT m FROM MarketEntity m", MarketEntity.class)
                .getResultList();
    }

    @Override
    @Transactional
    public void upsertHistoricalMarkets(List<MarketEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        int count = 0;
        for (MarketEntity row : rows) {
            try {
                entityManager.persist(row);
                count++;
                if (count % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error persisting market row: " + (row.getSymbol() != null ? row.getSymbol() : "unknown") + " - " + e.getMessage(), e);
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public void upsertHistoricalMarketsEveryday(List<HistoricalMarketEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        int count = 0;
        for (HistoricalMarketEntity row : rows) {
            try {
                // Clear the ID since this is always a new historical snapshot
                row.setId(null);
                entityManager.persist(row);
                count++;
                if (count % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error persisting historical market row: " + (row.getSymbol() != null ? row.getSymbol() : "unknown") + " - " + e.getMessage(), e);
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public void deleteHistoricalMarkets() {
        entityManager.createQuery("DELETE FROM MarketEntity").executeUpdate();
    }

    @Override
    public List<PatternEntity> findAllPatternData(PatternEntity filters) {
        return findGenerated(PatternEntity.class,
                textFilters("name", filters.getName(), "description", filters.getDescription(),
                        "status", filters.getStatus()),
                null, filters.getFilterValue(), filters.getSymbol(), filters.getId(),
                filters.getFromDate(), filters.getToDate());
    }

    @Override
    @Transactional
    public int replacePatterns(List<PatternEntity> rows) {
        if(rows == null || rows.isEmpty()) {
            return 0;
        }
       for(PatternEntity row : rows) {
            entityManager.persist(row);
        }
        entityManager.flush();
        entityManager.clear();
        return rows.size();
    }

    @Override
    public void truncatePatternRecords() {
        entityManager.createQuery("DELETE FROM PatternEntity").executeUpdate();
    }

    @Override
    @Transactional
    public void saveAllHistoricalPatterns(List<HistoricalPatternEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        int count = 0;
        for (HistoricalPatternEntity row : rows) {
            try {
                // Clear the ID since this is always a new historical snapshot
                row.setId(null);
                entityManager.persist(row);
                count++;
                if (count % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error persisting historical pattern row: " + (row.getName() != null ? row.getName() : "unknown") + " - " + e.getMessage(), e);
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public List<PatternEntity> findPatternByFilters(PatternFilterDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PatternEntity> query = builder.createQuery(PatternEntity.class);
        Root<PatternEntity> root = query.from(PatternEntity.class);
        List<Predicate> predicates = new ArrayList<>();
      if(filters.getPatterns() != null) {
            predicates.add(builder.equal(root.get("name"), filters.getPatterns().stream().findFirst().orElse(null)));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<ReportEntity> findAllReports(ReportEntity filters) {
        return findGenerated(ReportEntity.class,
                textFilters("title", filters.getTitle(), "description", filters.getDescription(),
                        "status", filters.getStatus()),
                null, filters.getFilterValue(), filters.getSymbol(), filters.getId(),
                filters.getFromDate(), filters.getToDate());
    }

    @Override
    public int replaceReports(List<ReportEntity> rows) {
        return replaceGenerated("ReportEntity", rows, true);
    }

    private <T> List<T> findGenerated(Class<T> entityType, Map<String, String> likeFilters, String filterType,
                                      String filterValue, String symbol, Object id, LocalDate fromDate,
                                      LocalDate toDate) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityType);
        Root<T> root = query.from(entityType);
        List<Predicate> predicates = new ArrayList<>();
        likeFilters.forEach((field, value) -> addLikePredicate(builder, root, field, value, predicates));
        addExactIgnoreCasePredicate(builder, root, "filterType", filterType, predicates);
        addExactIgnoreCasePredicate(builder, root, "filterValue", filterValue, predicates);
        addLikePredicate(builder, root, "symbol", symbol, predicates);
        if (id != null) {
            predicates.add(builder.equal(root.get("id"), id));
        }
        addDateRange(builder, root, "createdDate", fromDate, toDate, predicates);
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.asc(root.get("filterValue")), builder.asc(root.get("rankNo")),
                builder.desc(root.get("createdDate")));
        return entityManager.createQuery(query).getResultList();
    }

    // Reports and patterns tables may hold other rows, so only generated rows (with a filter value) are replaced.
    private int replaceGenerated(String entityName, List<?> rows, boolean onlyGenerated) {
        entityManager.createQuery("DELETE FROM " + entityName + " e"
                + (onlyGenerated ? " WHERE e.filterValue IS NOT NULL" : "")).executeUpdate();
        int saved = 0;
        for (Object row : rows) {
            entityManager.persist(row);
            if (++saved % 500 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        return saved;
    }


    @Override
    public List<PaymentEntity> fetchAllPayments() {
        return entityManager.createQuery("SELECT P FROM PaymentEntity P", PaymentEntity.class).getResultList();
    }

    @Override
    @Transactional
    public void upsertPaymentPlans(List<PaymentEntity> plans) {
        if (plans == null || plans.isEmpty()) {
            return;
        }
        for (PaymentEntity plan : plans) {
            PaymentEntity existing = entityManager.find(PaymentEntity.class, plan.getPlanId());
            if (!Objects.nonNull(existing)) {
                entityManager.persist(plan);
            } else {
                entityManager.merge(plan);
            }
        }
        entityManager.flush();

    }

    @Override
    public List<PaymentEntity> findPaymentPlans(PaymentsFilterDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PaymentEntity> query = builder.createQuery(PaymentEntity.class);
        Root<PaymentEntity> root = query.from(PaymentEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getUserName() != null) {
            predicates.add(builder.equal(root.get("userName"), filters.getUserName()));
        }
        if (filters.getStatus() != null) {
            predicates.add(builder.equal(root.get("status"), filters.getStatus()));
        }
        if(filters.getUserName() != null && filters.getStatus() != null) {
            predicates.add(builder.and(builder.equal(root.get("userName"), filters.getUserName())));
        }
        query.where(predicates.toArray(Predicate[]::new));
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

    private static <T> void addExactIgnoreCasePredicate(CriteriaBuilder builder, Root<T> root, String field,
                                                        String value, List<Predicate> predicates) {
        if (value != null && !value.isBlank()) {
            predicates.add(builder.equal(builder.lower(root.get(field)), value.trim().toLowerCase(Locale.ROOT)));
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
    @Transactional
    public void savePrediction(List<PredictionEntity> predictionDto) {
        if (predictionDto == null || predictionDto.isEmpty()) {
            return;
        }
        for (PredictionEntity entity : predictionDto) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }

    /* Below all implemented Methods are Plan related Queries*/
    @Override
    @Transactional
    public void addPlan(PlanEntity planEntity) {
        if (Objects.nonNull(planEntity))
            entityManager.persist(planEntity);
        entityManager.flush();
    }

    @Override
    public PlanEntity findByUserName(String name) {
        // The PlanEntity stores the associated user's identifier in the userName column.
        // Query by userName (not by plan name) to locate the user's plan.
        return entityManager.createQuery("SELECT p FROM PlanEntity p WHERE p.userName = :name", PlanEntity.class)
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
        entityManager.createQuery("DELETE FROM AuditLogEntity").executeUpdate();
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
    public List<StatementEntity> findStatements(StatementFiltersDto filters) {
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
            predicates.add(builder.greaterThanOrEqualTo(root.get("tradeDate"), filters.getFromDate()));
        }
        if (filters.getToDate() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("tradeDate"), filters.getToDate()));
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

    // Historical Prediction Methods
    @Override
    @Transactional
    public void saveHistoricalPrediction(List<HistoricalPredictionEntity> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return;
        }
        for (HistoricalPredictionEntity prediction : predictions) {
            if (prediction.getId() != null) {
                prediction.setId(null);
            }
            entityManager.persist(prediction);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public List<PredictionEntity> findAllPredictions() {
        return entityManager.createQuery("SELECT p FROM PredictionEntity p", PredictionEntity.class).getResultList();
    }

    @Override
    @Transactional
    public void deletePredictionRecords() {
        entityManager.createQuery("DELETE FROM PredictionEntity").executeUpdate();
    }

    @Override
    public List<PredictionEntity> findPredictionsByFilters(PredictionFiltersDto filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PredictionEntity> query = builder.createQuery(PredictionEntity.class);
        Root<PredictionEntity> root = query.from(PredictionEntity.class);
        List<Predicate> predicates = new ArrayList<>();
        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getConfidenceScore() != null && !filters.getConfidenceScore().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidencePercent", filters.getConfidenceScore()));
        }
        if (filters.getNextDayHigh() != null && !filters.getNextDayHigh().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "predictedHigh", filters.getNextDayHigh()));
        }
        if(filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "predictedLow", filters.getNextDayLow()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    // Historical Dashboard Repository Methods

    @Override
    @Transactional
    public void saveAllHistoricalDashboards(List<HistoricalDashboardEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        int count = 0;
        for (HistoricalDashboardEntity entity : entities) {
            if (entity.getId() != null) {
                entity.setId(null);
            }
            entityManager.persist(entity);
            count++;
            if (count % 100 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }



}
