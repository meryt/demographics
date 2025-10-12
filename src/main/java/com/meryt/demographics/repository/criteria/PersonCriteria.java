package com.meryt.demographics.repository.criteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.base.CaseFormat;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import com.meryt.demographics.domain.person.Gender;

/**
 * Contains the criteria used by the PersonRepository to filter a findAll request.
 */
@Getter
@Setter
public class PersonCriteria {

    /**
     * The 0-based page number to load
     */
    private Integer page;
    /**
     * The number of records per page
     */
    private Integer pageSize;
    /**
     * The name of the field to sort by, optionally comma-delimited for multiple fields. For example:
     *
     * "firstName"
     * "firstName,comeliness DESC"
     */
    private String sortBy;

    /**
     * If non-null, loads only people who were alive on this date
     */
    private LocalDate aliveOnDate;
    /**
     * If non-null, loads only people of this gender
     */
    private Gender gender;

    /**
     * If non-null, loads only people who are story characters
     */
    private Boolean isStoryCharacter;

    /**
     * If non-null, loads only people who are at least this age (converted to bornBefore date)
     */
    private Integer minAge;

    /**
     * If non-null, loads only people who are at most this age (converted to bornAfter date)
     */
    private Integer maxAge;

    /**
     * The reference date used for age calculations when aliveOnDate is not set.
     * If both aliveOnDate and currentDate are null, LocalDate.now() is used.
     */
    private LocalDate currentDate;

    /**
     * Get a Spring PageRequest object from the parameters on this object
     */
    public PageRequest getPageRequest() {
        int pageNum = page == null ? 0 : page;
        int perPage = pageSize == null ? 100 : pageSize;
        return PageRequest.of(pageNum, perPage, getSortOrder());
    }

    public String getLimitAndOffset() {
        PageRequest pageRequest = getPageRequest();
        return String.format(" LIMIT %d OFFSET %d", pageRequest.getPageSize(),
                pageRequest.getPageSize() * pageRequest.getPageNumber());
    }

    public JoinsAndConditions getJoinsAndConditions() {
        JoinsAndConditions joinsAndConditions = new JoinsAndConditions();
        if (aliveOnDate != null) {
            joinsAndConditions.whereClauses.add("p.birth_date <= :aliveOnDate");
            joinsAndConditions.whereClauses.add("p.death_date >= :aliveOnDate");
            joinsAndConditions.parameters.put("aliveOnDate", aliveOnDate);
        }
        if (gender != null) {
            joinsAndConditions.whereClauses.add("p.gender = :gender");
            joinsAndConditions.parameters.put("gender", gender.name());
        }
        if (isStoryCharacter != null) {
            joinsAndConditions.whereClauses.add("p.story_character = :isStoryCharacter");
            joinsAndConditions.parameters.put("isStoryCharacter", isStoryCharacter);
        }
        
        // Handle age filtering by converting to birth date constraints
        LocalDate referenceDate = aliveOnDate != null ? aliveOnDate : 
                                 (currentDate != null ? currentDate : LocalDate.now());
        
        if (minAge != null) {
            // For minAge, we want people who are at least minAge years old (inclusive)
            // Someone born on referenceDate - minAge years should be included
            LocalDate maxBirthDate = referenceDate.minusYears(minAge);
            joinsAndConditions.whereClauses.add("p.birth_date <= :maxBirthDate");
            joinsAndConditions.parameters.put("maxBirthDate", maxBirthDate);
        }
        
        if (maxAge != null) {
            // For maxAge, we want people who are at most maxAge years old (inclusive)
            // Someone born on referenceDate - maxAge years should be included (exactly maxAge years old)
            // Someone born on referenceDate - maxAge years - 364 days should be included (maxAge years and 364 days old)
            // This is equivalent to referenceDate - (maxAge + 1) years + 1 day
            LocalDate minBirthDate = referenceDate.minusYears(maxAge + 1).plusDays(1);
            joinsAndConditions.whereClauses.add("p.birth_date >= :minBirthDate");
            joinsAndConditions.parameters.put("minBirthDate", minBirthDate);
        }
        
        // If either minAge or maxAge is specified, ensure the person is alive at the reference date
        if (minAge != null || maxAge != null) {
            joinsAndConditions.whereClauses.add("(p.death_date IS NULL OR p.death_date > :referenceDate)");
            joinsAndConditions.parameters.put("referenceDate", referenceDate);
        }

        joinsAndConditions.orderBys = getOrderBys(joinsAndConditions);

        return joinsAndConditions;
    }

    private List<String> getOrderBys(@NonNull JoinsAndConditions joinsAndConditions) {
        String s = !StringUtils.hasText(sortBy) ? "birthDate" : sortBy;
        List<String> items = new ArrayList<>();
        for (String sortByString : s.split(",")) {
            String[] parts = sortByString.split(" ", 2);
            String property = parts[0].trim();
            boolean reversed = false;
            if (parts.length > 1 && !!StringUtils.hasText(parts[1])) {
                reversed = parts[1].trim().equalsIgnoreCase("DESC");
            }
            items.add(getOrderByForProperty(property, reversed, joinsAndConditions));
        }
        return items;
    }

    /**
     * Covert the sortBy string into a Spring Sort object, including splitting on commas and checking for reversed
     * sort orders using DESC
     */
    private Sort getSortOrder() {
        String s = !StringUtils.hasText(sortBy) ? "birthDate" : sortBy;
        Sort sort = null;
        for (String sortByString : s.split(",")) {
            String[] parts = sortByString.split(" ", 2);
            String property = parts[0].trim();
            boolean reversed = false;
            if (parts.length > 1 && !!StringUtils.hasText(parts[1])) {
                reversed = parts[1].trim().equalsIgnoreCase("DESC");
            }
            Sort by = getSortForProperty(property, reversed);
            sort = sort == null
                    ? by
                    : sort.and(by);
        }
        return sort;
    }

    private String getOrderByForProperty(@NonNull String property,
                                         boolean reversed,
                                         @NonNull JoinsAndConditions joinsAndConditions) {
        String column;
        switch (property) {
            case "birthDate":
            case "deathDate":
            case "firstName":
            case "lastName":
            case "comeliness":
                column = "p." + camelCaseToSnakeCase(property);
                break;
            case "name":
                column = "(p.first_name || ' ' || COALESCE(p.last_name, '')";
                break;
            case "capital":
                if (aliveOnDate == null) {
                    throw new IllegalArgumentException("Unable to sort by capital if no aliveOnDate criteria is specified");
                }
                column = "COALESCE(pc.capital, 0.0)";
                joinsAndConditions.joins.add(
                        "LEFT JOIN person_capital pc ON p.id = pc.person_id AND DATERANGE(pc.from_date, pc.to_date) @> CAST(:aliveOnDate AS DATE)");
                break;
            default:
                throw new IllegalArgumentException("No sort by defined for property " + property);
        }

        return reversed ? column + " DESC" : column;
    }

    /**
     * Get a sort clause based on a property name
     * @param propertyName the property name, like "birthDate"
     * @param reversed if true, a DESC sort order will be used on this property
     */
    private Sort getSortForProperty(@NonNull String propertyName, boolean reversed) {
        Sort.Order order = Sort.Order.by(propertyName);
        return reversed
                ? Sort.by(order).descending()
                : Sort.by(order);
    }

    private static String camelCaseToSnakeCase(@NonNull String camelCase) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, camelCase);
    }

    @Getter
    public final class JoinsAndConditions {
        List<String> whereClauses = new ArrayList<>();
        Set<String> joins = new HashSet<>();
        Map<String, Object> parameters = new HashMap<>();
        List<String> orderBys = new ArrayList<>();

        public String getWhereClause() {
            return whereClauses.stream().collect(Collectors.joining("\nAND "));
        }

        public String getOrderBy() {
            return " ORDER BY " + orderBys.stream().collect(Collectors.joining(", "));
        }
    }

}
