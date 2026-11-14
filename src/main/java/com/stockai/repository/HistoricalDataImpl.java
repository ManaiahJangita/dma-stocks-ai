package com.stockai.repository;

import com.stockai.dto.*;
import com.stockai.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@Transactional
@Slf4j
public class HistoricalDataImpl implements HistoricalDataRepository, CustomersRespository, DashboardRepository, AuditLogRepository,
        InstrumentsRepository, DailyStockRepository, FiltersRepository, PredictionRepository, MarketRepository,
        PatternsRepository, PaymentRepository, ReportsRepository, PlansRepository, StatementRepository, HistoricalDashboardRepository, PredictionComparisonRepository,
        NotificationRepository {
    @PersistenceContext
    EntityManager entityManager = null;

    @Override
    @Transactional
    public void saveAllHistoricalData(List<HistoricalDataEntity> historicalDataEntities) {
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM dmastock.historical_data WHERE created_date = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.historical_data WHERE created_date = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
        }
        log.info("Saving {} historical rows", historicalDataEntities == null ? 0 : historicalDataEntities.size());
        for (HistoricalDataEntity entity : historicalDataEntities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
    }

    @Override
    public List<HistoricalDataEntity> findAll(LocalDate startDate, LocalDate endDate) {
        log.info("Repository query: historical_data between {} and {}", startDate, endDate);
        try {
            List<HistoricalDataEntity> rows = entityManager.createNativeQuery(
                            "SELECT * FROM dmastock.historical_data WHERE created_date BETWEEN :startDate AND :endDate ORDER BY symbol, created_date",
                            HistoricalDataEntity.class)
                    .setParameter("startDate", startDate)
                    .setParameter("endDate", endDate)
                    .getResultList();
            log.info("Repository query returned {} rows for historical_data between {} and {}", rows.size(), startDate, endDate);
            return rows;
        } catch (Exception e) {
            log.error("Repository query failed for historical_data between {} and {}", startDate, endDate, e);
            throw e;
        }
    }


    @Override
    public LocalDate findLatestCreatedDate() {
        try {
            Object result = entityManager.createQuery("SELECT MAX(h.createdDate) FROM HistoricalDataEntity h").getSingleResult();
            if (result == null) {
                return null;
            }
            if (result instanceof LocalDate localDate) {
                return localDate;
            }
            if (result instanceof java.sql.Date sqlDate) {
                return sqlDate.toLocalDate();
            }
            if (result instanceof java.util.Date utilDate) {
                return utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            return LocalDate.parse(result.toString());
        } catch (Exception e) {
            log.error("Failed to fetch latest historical created date", e);
            return null;
        }
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

    private static Predicate buildStringInPredicate(CriteriaBuilder builder, jakarta.persistence.criteria.Expression<String> field, String value) {
        if (value == null || value.isBlank()) {
            return builder.conjunction();
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return builder.conjunction();
        }
        return field.in(normalized);
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
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.dashboard").executeUpdate();
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

        if (filters == null) {
            return entityManager.createQuery(query.select(root)).getResultList();
        }

        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getConfidenceScore() != null && !filters.getConfidenceScore().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidenceScore", filters.getConfidenceScore()));
        }
        if (filters.getNextDayHigh() != null && !filters.getNextDayHigh().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayHigh", filters.getNextDayHigh()));
        }
        if (filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
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
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (CustomerEntity entity : entities) {
            if (entity == null || entity.getUserId() == null) {
                continue;
            }

            CustomerEntity existing = entityManager.createQuery(
                            "SELECT c FROM CustomerEntity c WHERE c.userId = :userId", CustomerEntity.class)
                    .setParameter("userId", entity.getUserId())
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                entity.setId(existing.getId());
                entityManager.merge(entity);
            } else {
                entityManager.persist(entity);
            }
            count++;
        }
        entityManager.flush();
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
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.historical_markets where created_date = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.historical_markets WHERE created_date = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
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
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.markets").executeUpdate();
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
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        for (PatternEntity row : rows) {
            entityManager.persist(row);
        }
        entityManager.flush();
        entityManager.clear();
        return rows.size();
    }

    @Override
    public void truncatePatternRecords() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.patterns").executeUpdate();
    }

    @Override
    @Transactional
    public void saveAllHistoricalPatterns(List<HistoricalPatternEntity> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.historical_patterns WHERE created_date = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.historical_patterns WHERE created_date = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
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
        if (filters.getPatterns() != null) {
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
            if (plan == null) {
                continue;
            }

            PaymentEntity existing = null;
            if (plan.getPlanId() != null) {
                existing = entityManager.createQuery(
                                "SELECT p FROM PaymentEntity p WHERE p.planId = :planId", PaymentEntity.class)
                        .setParameter("planId", plan.getPlanId())
                        .setMaxResults(1)
                        .getResultStream()
                        .findFirst()
                        .orElse(null);
            }

            if (existing != null) {
                plan.setId(existing.getId());
                entityManager.merge(plan);
            } else {
                entityManager.persist(plan);
            }
        }
        entityManager.flush();
        entityManager.clear();
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
        if (filters.getUserName() != null && filters.getStatus() != null) {
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
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.audit_logs").executeUpdate();
        entityManager.flush();
        entityManager.clear();
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
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.historical_prediction WHERE created_date = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.historical_prediction WHERE created_date = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
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
    public void truncatePredictionData() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.prediction").executeUpdate();
    }

    @Override
    public void truncatePredictionComparisonData() {
        entityManager.createNativeQuery("TRUNCATE TABLE dmastock.prediction_comparison").executeUpdate();
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
        if (filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "predictedLow", filters.getNextDayLow()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public LocalDate findLatestPredictionDate() {
        try {
            Object result = entityManager.createQuery("SELECT MAX(p.predictionDate) FROM PredictionEntity p").getSingleResult();
            if (result == null) {
                return null;
            }
            if (result instanceof LocalDate localDate) {
                return localDate;
            }
            if (result instanceof java.sql.Date sqlDate) {
                return sqlDate.toLocalDate();
            }
            if (result instanceof java.util.Date utilDate) {
                return utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
            return LocalDate.parse(result.toString());
        } catch (Exception e) {
            log.error("Failed to fetch latest prediction date", e);
            return null;
        }
    }

    @Override
    public List<PredictionEntity> findByCreatedDateBetween(LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery(
                        "SELECT p FROM PredictionEntity p WHERE p.predictionDate BETWEEN :startDate AND :endDate ORDER BY p.symbol, p.predictionDate",
                        PredictionEntity.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }

    @Override
    public int countPredictionRecords() {
        return ((Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.prediction").getSingleResult()).intValue();
    }

    // Historical Dashboard Repository Methods

    @Override
    @Transactional
    public void saveAllHistoricalDashboards(List<HistoricalDashboardEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.historical_dashboard WHERE created_date = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.historical_dashboard WHERE created_date = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
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

    @Override
    @Transactional
    public void saveComparison(List<PredictionComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        for (PredictionComparisonEntity entity : entities) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public void archiveComparison(List<PredictionComparisonEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        LocalDate createdDate = LocalDate.now();
        Number countResult = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM dmastock.prediction_comparison_history WHERE created_at = :createdDate")
                .setParameter("createdDate", createdDate)
                .getSingleResult();
        int currentHistoryCount = countResult != null ? countResult.intValue() : 0;
        if (currentHistoryCount != 0) {
            entityManager.createNativeQuery("DELETE FROM dmastock.prediction_comparison_history WHERE created_at = :createdDate")
                    .setParameter("createdDate", createdDate)
                    .executeUpdate();
        }
        for (PredictionComparisonEntity entity : entities) {
            PredictionComparisonHistoryEntity history = new PredictionComparisonHistoryEntity();
            history.setSymbol(entity.getSymbol());
            history.setPredictionRunId(entity.getPredictionRunId());
            history.setPredictionDate(entity.getPredictionDate());
            history.setActualDate(entity.getActualDate());
            history.setPredictedOpen(entity.getPredictedOpen());
            history.setPredictedHigh(entity.getPredictedHigh());
            history.setPredictedLow(entity.getPredictedLow());
            history.setPredictedClose(entity.getPredictedClose());
            history.setPredictedDirection(entity.getPredictedDirection());
            history.setActualOpen(entity.getActualOpen());
            history.setActualHigh(entity.getActualHigh());
            history.setActualLow(entity.getActualLow());
            history.setActualClose(entity.getActualClose());
            history.setActualDirection(entity.getActualDirection());
            history.setCloseDiff(entity.getCloseDiff());
            history.setHighDiff(entity.getHighDiff());
            history.setLowDiff(entity.getLowDiff());
            history.setCloseDiffPercent(entity.getCloseDiffPercent());
            history.setHighDiffPercent(entity.getHighDiffPercent());
            history.setLowDiffPercent(entity.getLowDiffPercent());
            history.setDirectionMatched(entity.getDirectionMatched());
            history.setStatus(entity.getStatus());
            history.setReason(entity.getReason());
            history.setCreatedAt(entity.getCreatedAt());
            history.setPredictedProbabilityUp(entity.getPredictedProbabilityUp());
            history.setPredictedProbabilityDown(entity.getPredictedProbabilityDown());
            history.setConfidencePercent(entity.getConfidencePercent());
            history.setPredictedHighChangePct(entity.getPredictedHighChangePct());
            history.setPredictedLowChangePct(entity.getPredictedLowChangePct());
            history.setPredictedCloseChangePct(entity.getPredictedCloseChangePct());
            history.setActualVsPredictedDirectionMatch(entity.getActualVsPredictedDirectionMatch());
            history.setModelValidationAccuracyPercent(entity.getModelValidationAccuracyPercent());
            history.setRetrainingFeedbackScore(entity.getRetrainingFeedbackScore());
            entityManager.persist(history);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public List<PredictionComparisonEntity> findByPredictionDate(LocalDate predictionDate) {
        return entityManager.createQuery(
                        "SELECT p FROM PredictionComparisonEntity p WHERE p.predictionDate = :predictionDate ORDER BY p.symbol",
                        PredictionComparisonEntity.class)
                .setParameter("predictionDate", predictionDate)
                .getResultList();
    }

    @Override
    public List<PredictionComparisonEntity> findBySymbolAndPredictionDate(String symbol, LocalDate predictionDate) {
        return entityManager.createQuery(
                        "SELECT p FROM PredictionComparisonEntity p WHERE p.symbol = :symbol AND p.predictionDate = :predictionDate ORDER BY p.actualDate",
                        PredictionComparisonEntity.class)
                .setParameter("symbol", symbol)
                .setParameter("predictionDate", predictionDate)
                .getResultList();
    }

    @Override
    public MonthlyStatementSummaryDto getMonthlyStatementSummary(UUID userId, LocalDate fromDate, LocalDate toDate) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Valid fromDate and toDate are required");
        }

        MonthlyStatementSummaryDto summary = new MonthlyStatementSummaryDto();
        summary.setUserId(userId);
        summary.setFromDate(fromDate);
        summary.setToDate(toDate);

        String query = "SELECT s.tradeType, COALESCE(SUM(s.totalAmount), 0), COUNT(s.id) " +
                "FROM StatementEntity s " +
                "WHERE s.userId = :userId AND s.tradeDate BETWEEN :fromDate AND :toDate " +
                "GROUP BY s.tradeType";

        List<Object[]> rows = entityManager.createQuery(query, Object[].class)
                .setParameter("userId", userId)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();

        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSellAmount = BigDecimal.ZERO;
        long buyRecordsCount = 0L;
        long sellRecordsCount = 0L;

        for (Object[] row : rows) {
            String tradeType = row[0] != null ? row[0].toString() : null;
            BigDecimal amount = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            Long count = row[2] != null ? Long.parseLong(row[2].toString()) : 0L;

            if ("BUY".equalsIgnoreCase(tradeType)) {
                totalBuyAmount = amount;
                buyRecordsCount = count;
            } else if ("SELL".equalsIgnoreCase(tradeType)) {
                totalSellAmount = amount;
                sellRecordsCount = count;
            }
        }

        summary.setTotalBuyAmount(totalBuyAmount);
        summary.setTotalSellAmount(totalSellAmount);
        summary.setBuyRecordsCount(buyRecordsCount);
        summary.setSellRecordsCount(sellRecordsCount);
        summary.setNetAmount(totalSellAmount.subtract(totalBuyAmount));
        return summary;
    }

    @Override
    public List<NotificationEntity> findAllNotifications() {
        return entityManager.createQuery("SELECT n FROM NotificationEntity n ORDER BY n.createdAt DESC", NotificationEntity.class)
                .getResultList();
    }

    @Override
    public List<NotificationEntity> findUnreadNotifications() {
        return entityManager.createQuery("SELECT n FROM NotificationEntity n WHERE n.read = false ORDER BY n.createdAt DESC", NotificationEntity.class)
                .getResultList();
    }

    @Override
    @Transactional
    public NotificationEntity saveNotification(NotificationEntity notification) {
        if (notification == null) {
            return null;
        }
        if (notification.getId() == null) {
            entityManager.persist(notification);
        } else {
            entityManager.merge(notification);
        }
        entityManager.flush();
        return notification;
    }

    @Override
    @Transactional
    public NotificationEntity markNotificationRead(UUID id) {
        NotificationEntity notification = entityManager.find(NotificationEntity.class, id);
        if (notification == null) {
            return null;
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        entityManager.merge(notification);
        entityManager.flush();
        return notification;
    }


    @Override
    public List<HistoricalMarketEntity> findHistoricalMarketDataByFilters(MarketHistoryFiltersDto filters) {
        if (filters == null) {
            return List.of();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<HistoricalMarketEntity> query = builder.createQuery(HistoricalMarketEntity.class);
        Root<HistoricalMarketEntity> root = query.from(HistoricalMarketEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getNextDayHigh() != null && !filters.getNextDayHigh().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayHigh", filters.getNextDayHigh()));
        }
        if (filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "nextDayLow", filters.getNextDayLow()));
        }
        if (filters.getConfidenceScore() != null && !filters.getConfidenceScore().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidenceScore", filters.getConfidenceScore()));
        }
        if (filters.getCreatedDate() != null) {
            predicates.add(builder.equal(root.get("createdDate"), filters.getCreatedDate()));
        }
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("createdDate")), builder.asc(root.get("symbol")));
        return entityManager.createQuery(query).getResultList();
    }


    @Override
    public List<HistoricalPredictionEntity> findHistoricalPredictionDataByFilters(HistoricalPredictionFiltersDto filters) {
        if (filters == null) {
            return List.of();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<HistoricalPredictionEntity> query = builder.createQuery(HistoricalPredictionEntity.class);
        Root<HistoricalPredictionEntity> root = query.from(HistoricalPredictionEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getNextDayHigh() != null && !filters.getNextDayHigh().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "predictedHigh", filters.getNextDayHigh()));
        }
        if (filters.getNextDayLow() != null && !filters.getNextDayLow().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "predictedLow", filters.getNextDayLow()));
        }
        if (filters.getConfidenceScore() != null && !filters.getConfidenceScore().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidencePercent", filters.getConfidenceScore()));
        }
        if (filters.getCreatedDate() != null) {
            predicates.add(builder.equal(root.get("createdDate"), filters.getCreatedDate()));
        }

        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("createdDate")), builder.asc(root.get("symbol")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public List<HistoricalDashboardEntity> findHistoricalDashboardDataByFilters(HistoricalDashboardFiltersDto filters) {
        if (filters == null) {
            return List.of();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<HistoricalDashboardEntity> query = builder.createQuery(HistoricalDashboardEntity.class);
        Root<HistoricalDashboardEntity> root = query.from(HistoricalDashboardEntity.class);
        List<Predicate> predicates = new ArrayList<>();

        if (filters.getSymbol() != null && !filters.getSymbol().isEmpty()) {
            predicates.add(buildStringInPredicate(builder, root.get("symbol"), filters.getSymbol()));
        }
        if (filters.getNextDayHighDiff() != null && !filters.getNextDayHighDiff().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "marketPredictionHighDiff", filters.getNextDayHighDiff()));
        }
        if (filters.getNextDayLowDiff() != null && !filters.getNextDayLowDiff().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "marketPredictionLowDiff", filters.getNextDayLowDiff()));
        }
        if (filters.getConfidenceScoreDiff() != null && !filters.getConfidenceScoreDiff().isEmpty()) {
            predicates.add(buildMultiValueRangePredicate(builder, root, "confidenceScoreDiff", filters.getConfidenceScoreDiff()));
        }
        if (filters.getCreatedDate() != null) {
            predicates.add(builder.equal(root.get("createdDate"), filters.getCreatedDate()));
        }

        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(builder.desc(root.get("createdDate")), builder.asc(root.get("symbol")));
        return entityManager.createQuery(query).getResultList();
    }
}

