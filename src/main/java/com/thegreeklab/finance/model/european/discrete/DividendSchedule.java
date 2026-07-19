package com.thegreeklab.finance.model.european.discrete;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Immutable schedule of deterministic discrete cash dividends.
 *
 * <p>Entries are copied and sorted by ex-dividend timestamp during
 * construction. Pricing models may select the subset applicable to a specific
 * valuation time and option expiration.</p>
 */
public final class DividendSchedule {
    private final List<CashDividend> dividends;

    /**
     * Creates a chronologically ordered dividend schedule.
     *
     * @param dividends cash dividends to include in the schedule
     * @throws NullPointerException if {@code dividends} or any of its entries is null
     */
    public DividendSchedule(List<CashDividend> dividends) {
        Objects.requireNonNull(dividends, "Dividends cannot be null.");
        this.dividends = dividends.stream()
                .map(dividend -> Objects.requireNonNull(
                        dividend,
                        "Dividend cannot be null."
                ))
                .sorted(Comparator.comparingLong(CashDividend::exTimestampNanos))
                .toList();
    }

    /**
     * Returns the immutable, chronologically ordered dividends.
     *
     * @return sorted cash-dividend entries
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Stream.toList() creates an unmodifiable list of immutable records."
    )
    public List<CashDividend> dividends() {
        return dividends;
    }

}
